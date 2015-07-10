package com.fda.dim.featx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.InstanceList.CrossValidationIterator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class TopicModeler {

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
		HashMap<String, ParallelTopicModel> allParallelTopicModels = new HashMap<String, ParallelTopicModel>();

		HashMap<String, InstanceList> trainInstanceLists = new HashMap<String, InstanceList>();
		HashMap<String, InstanceList> testInstanceLists = new HashMap<String, InstanceList>();

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
				trainInstanceLists.put(currentCategory + String.valueOf(fold),
						currCrossVal.nextSplit()[0]);
				testInstanceLists.put(currentCategory + String.valueOf(fold),
						currCrossVal.nextSplit()[1]);
			}
			reader.close();
		}

		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
		// Note that the first parameter is passed as the sum over topics,
		// while
		// the second is the parameter for a single dimension of the
		// Dirichlet
		// prior.
		int numTopics = 20;
		ParallelTopicModel currentModel = new ParallelTopicModel(numTopics,
				1.0, 0.01);
		allParallelTopicModels.put(currentCategory, currentModel);
		currentModel.addInstances(currentInstanceList);

		// Use two parallel samplers, which each look at one half the corpus
		// and
		// combine
		// statistics after every iteration.
		currentModel.setNumThreads(2);

		// Run the model for 50 iterations and stop (this is for testing
		// only,
		// for real applications, use 1000 to 2000 iterations)
		currentModel.setNumIterations(50);
		currentModel.estimate();
	}
}