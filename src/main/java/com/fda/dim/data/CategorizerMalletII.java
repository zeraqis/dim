package com.fda.dim.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.types.Instance;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class CategorizerMalletII {
	static Logger slf4jLogger = LoggerFactory
			.getLogger(CategorizerMalletII.class);

	public static void main(String[] args) {
		try {

			

			Gson gson = new Gson();
			Type categoryType = new TypeToken<List<String>>() {
			}.getType();
			BufferedReader businessBr = new BufferedReader(
					new FileReader(
							"/tudelft.net/staff-bulk/ewi/insy/mmc/nathan/yelp_dataset_challenge_academic_dataset/yelp_academic_dataset_business.json"));

			Map<String, List<String>> businessCategory = new HashMap<String, List<String>>();

			JsonParser parser = new JsonParser();

			String line;

			while ((line = businessBr.readLine()) != null) {
				JsonElement jsonElement = parser.parse(line);
				JsonObject jsonObject = jsonElement.getAsJsonObject();
				String businessId = jsonObject.get("business_id").getAsString()
						.trim();
				List<String> categories = gson.fromJson(
						jsonObject.get("categories").getAsJsonArray(),
						categoryType);
				businessCategory.put(businessId, categories);
			}

			businessBr.close();

			// slf4jLogger.info("Business Map : {}", businessCategory);

			BufferedReader reviewBr = new BufferedReader(
					new FileReader(
							"/tudelft.net/staff-bulk/ewi/insy/mmc/nathan/yelp_dataset_challenge_academic_dataset/yelp_academic_dataset_review.json"));

			int count = 0;

			while ((line = reviewBr.readLine()) != null) {
				count++;
				
				if(count % 1000 == 0){
					slf4jLogger.info("Line : {}", count);
				}
				
				JsonElement jsonElement = parser.parse(line);
				JsonObject jsonObject = jsonElement.getAsJsonObject();
				String reviewId = jsonObject.get("review_id").getAsString()
						.trim();
				String businessId = jsonObject.get("business_id").getAsString()
						.trim();
				List<String> categories = businessCategory.get(businessId);
				String reviewText = jsonObject.get("text").getAsString().trim();
				for (String category : categories) {
					Instance malletInstance = new Instance(reviewText,category,reviewId,null);
					Type malletInstanceType = new TypeToken<Instance>() {
					}.getType();
					
					FileWriter writer = new FileWriter(
							"/tudelft.net/staff-bulk/ewi/insy/mmc/nathan/dim-data/yelp_full."+ category.replaceAll(" ", "_").replaceAll("/", "-") +".mallet", true);
					
					String json = gson.toJson(malletInstance,
							malletInstanceType);
					writer.append(json);
					writer.append("\n");
					
					writer.flush();
					writer.close();
				}
			}
			reviewBr.close();

			slf4jLogger.info("Finished");
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
