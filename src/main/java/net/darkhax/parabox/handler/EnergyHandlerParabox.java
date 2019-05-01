package net.darkhax.parabox.handler;

import net.minecraftforge.energy.EnergyStorage;

public class EnergyHandlerParabox extends EnergyStorage {

	public EnergyHandlerParabox(int capacity, int maxTransfer) {
		super(capacity, maxTransfer, 0, 0);
	}

	public void setEnergy(int energy) {
		this.energy = energy;
	}

	public void updateValues(int newRFT) {
		this.capacity = newRFT;
		this.maxReceive = newRFT;
		this.setEnergy(Math.min(energy, newRFT));
	}
}