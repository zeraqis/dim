package com.fda.dim.data;

import java.util.List;

public class CategorizerPojo {
	public List<String> categories;
	public String businessId;
	public String reviewId;
	public String reviewText;

	public void setVars(List<String> categories, String businessId,
			String reviewId, String reviewText) {
		this.categories = categories;
		this.businessId = businessId;
		this.reviewId = reviewId;
		this.reviewText = reviewText;
	}
}
