package net.darkhax.parabox.util;

import com.google.gson.annotations.Expose;

public class ParaboxUserData {

	@Expose
	private int points;

	public int getPoints() {
		return this.points;
	}

	public void setPoints(int points) {
		this.points = points;
	}
}