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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class Categorizer {
	static Logger slf4jLogger = LoggerFactory.getLogger(Categorizer.class);

	public static void main(String[] args) {
		try {

			FileWriter writer = new FileWriter(
					"/tudelft.net/staff-bulk/ewi/insy/mmc/nathan/dim-data/yelp_full.json");

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
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
				slf4jLogger.info("Line : {}", count++);
				CategorizerPojo categorizedData = new CategorizerPojo();
				JsonElement jsonElement = parser.parse(line);
				JsonObject jsonObject = jsonElement.getAsJsonObject();
				String reviewId = jsonObject.get("review_id").getAsString()
						.trim();
				String businessId = jsonObject.get("business_id").getAsString()
						.trim();
				List<String> categories = businessCategory.get(businessId);
				String reviewText = jsonObject.get("text").getAsString().trim();
				categorizedData.setVars(categories, businessId, reviewId,
						reviewText);
				Type typeCategorizedData = new TypeToken<CategorizerPojo>() {
				}.getType();
				String json = gson.toJson(categorizedData, typeCategorizedData);
				writer.append(json);
				writer.append("\n");
			}
			reviewBr.close();

			slf4jLogger.info("Finished");
			writer.flush();
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
