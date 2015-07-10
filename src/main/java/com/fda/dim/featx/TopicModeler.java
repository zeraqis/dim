package com.fda.dim.featx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.InstanceList.CrossValidationIterator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class TopicModeler {
	static Logger slf4jLogger = LoggerFactory
			.getLogger(TopicModeler.class);
	public static InstanceList createInstanceList() {
		// Begin by importing documents from text to feature sequences
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		// String stopPath="/home/spyros/git/fda/dim/assets/stopwords.txt";
		String stopPath = "/tudelft.net/staff-bulk/ewi/insy/mmc/nathan/dim-data/en.txt";

		// Pipes: lowercase, tokenize, remove stopwords, map to features
		pipeList.add(new CharSequenceLowercase());
		pipeList.add(new CharSequence2TokenSequence(Pattern
				.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
		pipeList.add(new TokenSequenceRemoveStopwords(new File(stopPath),
				"UTF-8", false, false, false));
		pipeList.add(new TokenSequence2FeatureSequence());

		InstanceList instances = new InstanceList(new SerialPipes(pipeList));

		return instances;
	}

	public static void main(String[] args) throws Exception {

		// String filepath = "/home/spyros/Documents/data/yelp_full.mallet";
		String filepath = "/tudelft.net/staff-bulk/ewi/insy/mmc/nathan/dim-data/yelp_full.mallet";
		// BufferedReader br = new BufferedReader(new FileReader(
		// new File(filepath)));
		String line;
		Gson gson = new Gson();
		Type malletInstanceType = new TypeToken<Instance>() {
		}.getType();

		File[] malletFiles = new File("").listFiles();
		HashMap<String, InstanceList> allInstanceLists = new HashMap<String, InstanceList>();
		// HashMap<String, ParallelTopicModel> allParallelTopicModels = new
		// HashMap<String, ParallelTopicModel>();

		HashMap<CategoryFoldPair, InstanceList> trainInstanceLists = new HashMap<CategoryFoldPair, InstanceList>();
		HashMap<CategoryFoldPair, InstanceList> testInstanceLists = new HashMap<CategoryFoldPair, InstanceList>();

		HashMap<CategoryFoldPair, ParallelTopicModel> allTrainParallelTopicModels = new HashMap<CategoryFoldPair, ParallelTopicModel>();

		String[] categoriesArray = { "Restaurants", "Nightlife", "Bars",
				"Arts_&_Entertainment", "Hotels_&_Travel", "Shopping",
				"Casinos", "Beauty_&_Spas", "Automotive", "Health_&_Medical",
				"Home_Services", "Performing_Arts", "Grocery", "Fashion",
				"Fitness_&_Instruction" };
		ArrayList<String> allCategories = new ArrayList<String>();
		allCategories.addAll(Arrays.asList(categoriesArray));

		// read all files and create the InstanceLists
		for (File malletFile : malletFiles) {

			// get the category from the file name
			String currentCategory = malletFile.getName().split(".")[1];
			if (!allCategories.contains(currentCategory)) {
				continue;
			}

			// create reader to read the file
			BufferedReader reader = new BufferedReader(new FileReader(
					malletFile));

			// store a new instance list instance in the map
			allInstanceLists.put(currentCategory,
					TopicModeler.createInstanceList());
			InstanceList currentInstanceList = allInstanceLists
					.get(currentCategory);

			int counter = 0;

			while ((line = reader.readLine()) != null) {
				Instance instance = gson.fromJson(line, malletInstanceType);
				currentInstanceList.addThruPipe(instance);

				counter++;
				if (counter == 5000) {
					break;
				}
			}
			CrossValidationIterator currCrossVal = currentInstanceList
					.crossValidationIterator(10);
			int fold = 1;
			while (currCrossVal.hasNext()) {

				// trainInstanceLists.put(new CategoryFoldPair(currentCategory,
				// fold),
				// currCrossVal.nextSplit()[0]);

				// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
				// Note that the first parameter is passed as the sum over
				// topics,
				// while
				// the second is the parameter for a single dimension of the
				// Dirichlet
				// prior.
				int numTopics = 20;
				ParallelTopicModel currentModel = new ParallelTopicModel(
						numTopics, 1.0, 0.01);
				currentModel.addInstances(currentInstanceList);

				// Use two parallel samplers, which each look at one half the
				// corpus
				// and
				// combine
				// statistics after every iteration.
				currentModel.setNumThreads(2);

				// Run the model for 50 iterations and stop (this is for testing
				// only,
				// for real applications, use 1000 to 2000 iterations)
				currentModel.setNumIterations(50);
				currentModel.estimate();

				// store the model in the Map
				allTrainParallelTopicModels.put(new CategoryFoldPair(
						currentCategory, fold), currentModel);

				// create the test instance set
				testInstanceLists.put(new CategoryFoldPair(currentCategory,
						fold), currCrossVal.nextSplit()[1]);

				fold++;

			}

			// group all instances for each fold

			reader.close();
		}
		HashMap<Integer, InstanceList> foldInstanceLists = new HashMap<Integer, InstanceList>();
		for (int i = 1; i <= 10; i++) {
			for (CategoryFoldPair pair : testInstanceLists.keySet()) {

				if (pair.fold == i) {

					if (!foldInstanceLists.containsKey(i)) {
						foldInstanceLists.put(i, new InstanceList());
					}

					foldInstanceLists.get(i)
							.addAll(testInstanceLists.get(pair));
				}
			}
		}

		double MRR = 0;

		for (int i = 1; i <= 10; i++) {
			InstanceList currTestInstanceList = foldInstanceLists.get(Integer
					.valueOf(i));
			for (Instance testInstance : currTestInstanceList) {
				Map<String, Double> categoryProb = new HashMap<String, Double>();
				for (String category : allCategories) {
					for (Entry<CategoryFoldPair, ParallelTopicModel> entry : allTrainParallelTopicModels
							.entrySet()) {
						if (entry.getKey().category.equals(category)
								&& entry.getKey().fold == i) {
							TopicInferencer currInferer = entry.getValue()
									.getInferencer();
							double[] topicDistribution = currInferer
									.getSampledDistribution(testInstance, 50,
											10, 10);

							categoryProb.put(category,
									average(topicDistribution));

						}
					}
				}
				String actualCategory = (String) testInstance.getTarget();
				Map<String, Double> sortedCategoryProb = sortByValue(categoryProb);
				double rank = 1;
				for (Entry<String, Double> entry : sortedCategoryProb
						.entrySet()) {
					if (entry.getKey().equals(actualCategory)) {
						MRR += 1 / rank;
						break;
					}
					rank++;
				}
			}
		}
		slf4jLogger.info("MRR : {}", MRR);
	}

	public static double average(double[] numbers) {
		double sum = 0;
		for (double b : numbers) {
			sum += b;
		}

		return sum / numbers.length;
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(
			Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
}

class CategoryFoldPair {
	public String category;
	public int fold;

	public CategoryFoldPair(String category, int fold) {
		this.category = category;
		this.fold = fold;
	}
}
