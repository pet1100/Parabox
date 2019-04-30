package net.darkhax.parabox.block;

import net.minecraftforge.energy.EnergyStorage;

public class EnergyHandlerParabox extends EnergyStorage {

	public EnergyHandlerParabox(int capacity, int maxTransfer) {
		super(capacity, maxTransfer, 0, 0);
	}

	public void setEnergy(int energy) {
		this.energy = energy;
	}

	public void setCapacity(int max) {
		this.capacity = max;
		this.setEnergy(Math.min(energy, max));
	}

	public void setInput(int max) {
		this.maxReceive = max;
	}
}