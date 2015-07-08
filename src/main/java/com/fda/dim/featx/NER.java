package com.fda.dim.featx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class NER {
	static Logger slf4jLogger = LoggerFactory.getLogger(NER.class);

	public static void main(String[] args) {
		try {
			Gson gson = new Gson();
			Type malletInstanceType = new TypeToken<Instance>() {
			}.getType();

			InputStream modelIn = new FileInputStream(
					"/tudelft.net/staff-bulk/ewi/insy/mmc/nathan/dim-data/en-ner-location.bin");
			TokenNameFinderModel model = new TokenNameFinderModel(modelIn);

			NameFinderME modelME = new NameFinderME(model);

			ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

			pipeList.add(new CharSequenceLowercase());
			pipeList.add(new CharSequence2TokenSequence(Pattern
					.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
			pipeList.add(new TokenSequenceRemoveStopwords(
					new File(
							"/tudelft.net/staff-bulk/ewi/insy/mmc/nathan/dim-data/en.txt"),
					"UTF-8", false, false, false));
			// pipeList.add(new TokenSequence2FeatureSequence());

			InstanceList instanceList = new InstanceList(new SerialPipes(
					pipeList));

			BufferedReader br = new BufferedReader(
					new FileReader(
							"/tudelft.net/staff-bulk/ewi/insy/mmc/nathan/dim-data/yelp_full.mallet"));
			String line;
			while ((line = br.readLine()) != null) {
				slf4jLogger.info("Line : {}", line);
				Instance instance = gson.fromJson(line, malletInstanceType);
				instanceList.addThruPipe(instance);
				break;
			}

			Iterator<Instance> instanceIterator = instanceList.iterator();
			while (instanceIterator.hasNext()) {
				Instance instance = instanceIterator.next();
				TokenSequence tokens = (TokenSequence) instance.getData();
				Iterator<Token> tokensIterator = tokens.iterator();
				String[] tokensArray = new String[tokens.size()];
				int i = 0;
				while (tokensIterator.hasNext()) {
					tokensArray[i] = tokensIterator.next().getText();
					i++;
				}
				Span[] spans = modelME.find(tokensArray);
				slf4jLogger.info("spans :{}",spans.length);
				for (int j = 0; j < spans.length; j++) {
					slf4jLogger.info("Instance : {}", spans[j]);
				}
			}

			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
