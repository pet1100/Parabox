package net.darkhax.parabox.util;

import net.darkhax.parabox.block.TileEntityParabox;

public enum SpeedFactor {
	OFF(0, 0.5F, 0),
	SLOWEST(0.5F, 0.7F, 0.33),
	SLOW(0.7F, 0.9F, 0.66),
	NORMAL(0.9F, 1.1F, 1),
	FAST(1.1F, 1.5F, 1.1),
	FASTEST(1.5F, 2F, 1.2);

	float min, max;
	double ticksPerTick;

	SpeedFactor(float min, float max, double ticksPerTick) {
		this.min = min;
		this.max = max;
		this.ticksPerTick = ticksPerTick;
	}

	public boolean isInRange(TileEntityParabox pbx, int rft) {
		return min * pbx.getRFTNeeded() <= rft && max * pbx.getRFTNeeded() > rft;
	}

	public double getTicksPerTick() {
		return ticksPerTick;
	}

	public static SpeedFactor getForPower(TileEntityParabox pbx, int rft) {
		for (SpeedFactor f : SpeedFactor.values()) {
			if (f.isInRange(pbx, rft)) return f;
		}
		return SpeedFactor.FASTEST;
	}
}