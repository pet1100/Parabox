package net.darkhax.parabox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.darkhax.bookshelf.network.NetworkHandler;
import net.darkhax.bookshelf.registry.RegistryHelper;
import net.darkhax.parabox.block.BlockParabox;
import net.darkhax.parabox.block.ItemBlockParabox;
import net.darkhax.parabox.block.TileEntityParabox;
import net.darkhax.parabox.gui.GuiHandler;
import net.darkhax.parabox.network.PacketActivate;
import net.darkhax.parabox.network.PacketConfirmReset;
import net.darkhax.parabox.network.PacketRefreshGui;
import net.darkhax.parabox.proxy.Proxy;
import net.darkhax.parabox.util.BlacklistedFileUtils;
import net.darkhax.parabox.util.WorldSpaceTimeManager;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;

@Mod(modid = Parabox.MODID, name = Parabox.NAME, version = "@VERSION@", dependencies = "required-after:bookshelf;required-after:prestige", certificateFingerprint = "@FINGERPRINT@")
public class Parabox {

	public static final String MODID = "parabox";
	public static final String NAME = "Parabox";
	public static final Logger LOG = LogManager.getLogger(Parabox.NAME);
	public static final RegistryHelper REGISTRY = new RegistryHelper(MODID).enableAutoRegistration().setTab(CreativeTabs.MISC);
	public static final NetworkHandler NETWORK = new NetworkHandler(MODID);

	private Block blockParabox;

	@Instance(MODID)
	public static Parabox instance;

	@SidedProxy(clientSide = "net.darkhax.parabox.proxy.ClientProxy", serverSide = "net.darkhax.parabox.proxy.Proxy")
	public static Proxy proxy;

	static Configuration config;
	static List<String> craftingBannedMods = new ArrayList<>();
	static List<ItemStack> craftingBannedItems = new ArrayList<>();
	static boolean dumpCraftingList = false;

	@EventHandler
	public void onPreInit(FMLPreInitializationEvent event) {
		NETWORK.register(PacketActivate.class, Side.SERVER);
		NETWORK.register(PacketConfirmReset.class, Side.SERVER);
		NETWORK.register(PacketRefreshGui.class, Side.CLIENT);
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

		this.blockParabox = new BlockParabox();
		REGISTRY.registerBlock(this.blockParabox, new ItemBlockParabox(this.blockParabox), "parabox");
		GameRegistry.registerTileEntity(TileEntityParabox.class, new ResourceLocation(MODID, "parabox"));
		config = new Configuration(event.getSuggestedConfigurationFile());
		for (String s : config.getStringList("Backup Blacklist", "general", new String[] { "playerdata", "advancements", "level.dat" }, "The names of files/folders that will not be restored by a state backup."))
			BlacklistedFileUtils.IGNORED.add(s);
		TileEntityParabox.rfPerTick = config.getInt("RF/t", "general", 400, 1, Integer.MAX_VALUE, "Power usage factor per cycle.");
		TileEntityParabox.cycleTime = config.getInt("Cycle Time", "general", 12000, 1, Integer.MAX_VALUE, "Tick time for a single cycle.");
		dumpCraftingList = config.getBoolean("Dump Crafting List", "items", false, "If the crafting list (post-filter) is dumped to the log.");

		if (config.hasChanged()) config.save();
		MinecraftForge.EVENT_BUS.register(proxy);
	}

	@EventHandler
	public void init(FMLInitializationEvent e) {
		String[] modids = config.getStringList("Crafting Banned Mods", "items", new String[0], "Mod ids that are not allowed in the parabox crafting item list.");
		String[] items = config.getStringList("Crafting Banned Items", "items", new String[0], "Items that are not allowed in the parabox crafting item list.  Format modid:name:meta");
		if (config.hasChanged()) config.save();
		craftingBannedMods = Arrays.asList(modids);
		for (String s : items) {
			String[] split = s.split(":");
			if (split.length != 3) {
				LOG.error("Invalid configuration entry {} in crafting blacklist will be ignored.", s);
				continue;
			}
			int meta = 0;
			try {
				meta = Integer.parseInt(split[2]);
			} catch (NumberFormatException ex) {
				LOG.error("Invalid configuration entry {} in crafting blacklist will be ignored.", s);
				continue;
			}
			Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(split[0], split[1]));
			if (i == null || i == Items.AIR) {
				LOG.error("Invalid configuration entry {} in crafting blacklist will be ignored.", s);
				continue;
			}
			craftingBannedItems.add(new ItemStack(i, 1, meta));
		}
	}

	@EventHandler
	public void onPostInit(FMLPostInitializationEvent event) {
		for (IRecipe recipe : ForgeRegistries.RECIPES) {
			if (!recipe.isDynamic()) TileEntityParabox.validStacks.add(recipe.getRecipeOutput().copy());
		}
		TileEntityParabox.validStacks = TileEntityParabox.validStacks.stream().filter(Parabox::isAllowed).collect(Collectors.toList());
		craftingBannedMods = null;
		craftingBannedItems = null;
		if (dumpCraftingList) {
			LOG.info("Starting crafting list dump!");
			TileEntityParabox.validStacks.forEach(s -> LOG.info("Entry: {}", s.getItem().getRegistryName() + ":" + s.getMetadata()));
			LOG.info("Total Entries: {}", TileEntityParabox.validStacks.size());
		}
	}

	static boolean isAllowed(ItemStack stack) {
		if (craftingBannedMods.contains(stack.getItem().getRegistryName().getNamespace())) return false;
		for (ItemStack s : craftingBannedItems) {
			if (OreDictionary.itemMatches(s, stack, false)) return false;
		}
		return true;
	}

	@EventHandler
	public void serverStart(FMLServerStartedEvent event) {
		WorldSpaceTimeManager.onGameInstanceStart();
	}

	@EventHandler
	public void serverStop(FMLServerStoppedEvent event) {
		WorldSpaceTimeManager.onGameInstanceClose();
	}

	public static void sendMessage(TextFormatting color, String text, Object... args) {
		MinecraftServer s = FMLCommonHandler.instance().getMinecraftServerInstance();
		if (s != null) {
			final TextComponentTranslation translation = new TextComponentTranslation(text, args);
			translation.getStyle().setColor(color);
			s.getPlayerList().sendMessage(translation, false);
		}
	}

	public static void sendMessage(EntityPlayer player, TextFormatting color, String text, Object... args) {
		if (!player.world.isRemote) {
			final TextComponentTranslation translation = new TextComponentTranslation(text, args);
			translation.getStyle().setColor(color);
			player.sendStatusMessage(translation, false);
		}
	}

	public static String ticksToTime(int ticks) {
		int i = ticks / 20;
		final int j = i / 60;
		i = i % 60;
		return i < 10 ? j + ":0" + i : j + ":" + i;
	}

	public static WorldServer overworld() {
		return FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0];
	}
}