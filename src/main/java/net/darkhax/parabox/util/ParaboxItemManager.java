package net.darkhax.parabox.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.darkhax.parabox.Parabox;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

public class ParaboxItemManager {

	public static final List<ItemStack> CRAFTING_BLACKLIST = new ArrayList<>();
	public static final List<String> CRAFTING_MOD_BLACKLIST = new ArrayList<>();
	public static final List<ItemStack> CRAFTING_ITEMS = new ArrayList<>(30000);
	public static final List<ItemStack> EMPOWERED_ITEMS = new ArrayList<>(500);

	public static boolean dumpCrafting = false;
	public static boolean dumpEmpowered = false;

	public static void readConfigs(Configuration c) {
		loadCraftingConfigs(c);
		loadEmpoweredConfigs(c);
		if (c.hasChanged()) c.save();
	}

	public static void load() {
		loadCraftingList();
		loadEmpoweredList();
	}

	static boolean isAllowed(ItemStack stack) {
		if (ParaboxItemManager.CRAFTING_MOD_BLACKLIST.contains(stack.getItem().getRegistryName().getNamespace())) return false;
		for (ItemStack s : ParaboxItemManager.CRAFTING_BLACKLIST) {
			if (OreDictionary.itemMatches(s, stack, false)) return false;
		}
		return true;
	}

	static void loadCraftingList() {
		for (IRecipe recipe : ForgeRegistries.RECIPES) {
			if (!recipe.isDynamic()) ParaboxItemManager.CRAFTING_ITEMS.add(recipe.getRecipeOutput().copy());
		}
		List<ItemStack> stacks = CRAFTING_ITEMS.stream().filter(ParaboxItemManager::isAllowed).collect(Collectors.toList());
		CRAFTING_ITEMS.clear();
		CRAFTING_ITEMS.addAll(stacks);
		CRAFTING_MOD_BLACKLIST.clear();
		CRAFTING_BLACKLIST.clear();

		if (dumpCrafting) {
			Parabox.LOG.info("Starting crafting list dump!");
			CRAFTING_ITEMS.forEach(s -> Parabox.LOG.info("Entry: {}", s.getItem().getRegistryName() + ":" + s.getMetadata()));
			Parabox.LOG.info("Total Entries: {}", CRAFTING_ITEMS.size());
		}
	}

	static void loadCraftingConfigs(Configuration c) {
		String[] modids = c.getStringList("Crafting Banned Mods", "items", new String[0], "Mod ids that are not allowed in the parabox crafting item list.");
		String[] items = c.getStringList("Crafting Banned Items", "items", new String[0], "Items that are not allowed in the parabox crafting item list.  Format modid:name:meta");
		CRAFTING_MOD_BLACKLIST.addAll(Arrays.asList(modids));
		for (String s : items) {
			String[] split = s.split(":");
			if (split.length != 3) {
				Parabox.LOG.error("Invalid configuration entry {} in crafting blacklist will be ignored.", s);
				continue;
			}
			int meta = 0;
			try {
				meta = Integer.parseInt(split[2]);
			} catch (NumberFormatException ex) {
				Parabox.LOG.error("Invalid configuration entry {} in crafting blacklist will be ignored.", s);
				continue;
			}
			Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(split[0], split[1]));
			if (i == null || i == Items.AIR) {
				Parabox.LOG.error("Invalid configuration entry {} in crafting blacklist will be ignored.", s);
				continue;
			}
			CRAFTING_BLACKLIST.add(new ItemStack(i, 1, meta));
		}
	}

	static void loadEmpoweredList() {
		if (dumpEmpowered) {
			Parabox.LOG.info("Starting empowered list dump!");
			EMPOWERED_ITEMS.forEach(s -> Parabox.LOG.info("Entry: {}", s.getItem().getRegistryName() + ":" + s.getMetadata()));
			Parabox.LOG.info("Total Entries: {}", EMPOWERED_ITEMS.size());
		}
	}

	static void loadEmpoweredConfigs(Configuration c) {
		String[] items = c.getStringList("Empowered Parabox Items", "items", new String[0], "The list of valid items for the Empowered Parabox.  Format modid:name:meta");
		for (String s : items) {
			String[] split = s.split(":");
			if (split.length != 3) {
				Parabox.LOG.error("Invalid configuration entry {} in empowered whitelist will be ignored.", s);
				continue;
			}
			int meta = 0;
			try {
				meta = Integer.parseInt(split[2]);
			} catch (NumberFormatException ex) {
				Parabox.LOG.error("Invalid configuration entry {} in empowered whitelist will be ignored.", s);
				continue;
			}
			Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(split[0], split[1]));
			if (i == null || i == Items.AIR) {
				Parabox.LOG.error("Invalid configuration entry {} in empowered whitelist will be ignored.", s);
				continue;
			}
			EMPOWERED_ITEMS.add(new ItemStack(i, 1, meta));
		}
	}

}
