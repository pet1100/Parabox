package net.darkhax.parabox.util;

import java.util.Locale;

import com.bloodnbonesgaming.topography.config.ConfigurationManager;

import net.minecraft.world.World;

public class TopographyStuff {

	public static boolean isCompactMachineLand(World world) {
		String s = ConfigurationManager.getInstance().getGeneratorSettings();
		return s.toLowerCase(Locale.ROOT).contains("compactsky") && world.provider.getDimension() == 144;
	}

}
