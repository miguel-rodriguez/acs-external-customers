package com.alfresco.support.alfrescoCMIS.model;

public class Search {
	private String term;

	public void setTerm(String term) {
		this.term = term;
	}

	public String getTerm(){
		return this.term;
	}
	
	@Override
	public String toString() {
		return "Search [term=" + term + "]";
	}
}