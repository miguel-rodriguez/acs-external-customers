package com.alfresco.support.alfrescoCMIS.model;

public class Login {
	private String username;
	private String password;

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername(){
		return this.username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword(){
		return this.password;
	}

	@Override
	public String toString() {
		return "Login [username=" + username + ", password=" + password + "]";
	}
}