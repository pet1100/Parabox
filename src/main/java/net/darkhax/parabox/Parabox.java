package net.darkhax.parabox;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.darkhax.bookshelf.network.NetworkHandler;
import net.darkhax.bookshelf.registry.RegistryHelper;
import net.darkhax.parabox.block.BlockParabox;
import net.darkhax.parabox.block.ItemBlockParabox;
import net.darkhax.parabox.block.TileEntityParabox;
import net.darkhax.parabox.block.v2.BlockParaboxV2;
import net.darkhax.parabox.block.v2.TileEntityParaboxV2;
import net.darkhax.parabox.gui.GuiHandler;
import net.darkhax.parabox.network.PacketActivate;
import net.darkhax.parabox.network.PacketConfirmReset;
import net.darkhax.parabox.network.PacketRefreshGui;
import net.darkhax.parabox.proxy.Proxy;
import net.darkhax.parabox.util.BlacklistedFileUtils;
import net.darkhax.parabox.util.ParaboxItemManager;
import net.darkhax.parabox.util.WorldSpaceTimeManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
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
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = Parabox.MODID, name = Parabox.NAME, version = "@VERSION@", dependencies = "required-after:bookshelf;required-after:prestige", certificateFingerprint = "@FINGERPRINT@")
public class Parabox {

	public static final String MODID = "parabox";
	public static final String NAME = "Parabox";
	public static final Logger LOG = LogManager.getLogger(Parabox.NAME);
	public static final RegistryHelper REGISTRY = new RegistryHelper(MODID).enableAutoRegistration().setTab(CreativeTabs.MISC);
	public static final NetworkHandler NETWORK = new NetworkHandler(MODID);

	@Instance(MODID)
	public static Parabox instance;

	@SidedProxy(clientSide = "net.darkhax.parabox.proxy.ClientProxy", serverSide = "net.darkhax.parabox.proxy.Proxy")
	public static Proxy proxy;

	static Configuration config;

	@EventHandler
	public void onPreInit(FMLPreInitializationEvent event) {
		NETWORK.register(PacketActivate.class, Side.SERVER);
		NETWORK.register(PacketConfirmReset.class, Side.SERVER);
		NETWORK.register(PacketRefreshGui.class, Side.CLIENT);
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

		BlockParabox block = new BlockParabox();
		REGISTRY.registerBlock(block, new ItemBlockParabox(block), "parabox");
		GameRegistry.registerTileEntity(TileEntityParabox.class, block.getRegistryName());

		block = new BlockParaboxV2();
		REGISTRY.registerBlock(block, new ItemBlockParabox(block), "empowered_parabox");
		GameRegistry.registerTileEntity(TileEntityParaboxV2.class, block.getRegistryName());

		config = new Configuration(event.getSuggestedConfigurationFile());
		for (String s : config.getStringList("Backup Blacklist", "general", new String[] { "playerdata", "advancements", "level.dat" }, "The names of files/folders that will not be restored by a state backup."))
			BlacklistedFileUtils.IGNORED.add(s);
		TileEntityParabox.rfPerTick = config.getInt("RF/t", "general", 400, 1, Integer.MAX_VALUE, "Power usage factor per cycle.");
		TileEntityParabox.cycleTime = config.getInt("Cycle Time", "general", 12000, 1, Integer.MAX_VALUE, "Tick time for a single cycle.");
		TileEntityParabox.updateMessages = config.getBoolean("Update Messages", "general", true, "If parabox broadcasts when an item is received.");

		if (config.hasChanged()) config.save();
		MinecraftForge.EVENT_BUS.register(proxy);
	}

	@EventHandler
	public void init(FMLInitializationEvent e) {
		ParaboxItemManager.readConfigs(config);
	}

	@EventHandler
	public void onPostInit(FMLPostInitializationEvent event) {
		ParaboxItemManager.load();
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
			args = fixBlockPos(args);
			final TextComponentTranslation translation = new TextComponentTranslation(text, args);
			translation.getStyle().setColor(color);
			s.getPlayerList().sendMessage(translation, false);
		}
	}

	public static void sendMessage(EntityPlayer player, TextFormatting color, String text, Object... args) {
		if (!player.world.isRemote) {
			args = fixBlockPos(args);
			final TextComponentTranslation translation = new TextComponentTranslation(text, args);
			translation.getStyle().setColor(color);
			player.sendStatusMessage(translation, false);
		}
	}

	static Object[] fixBlockPos(Object[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof BlockPos) {
				BlockPos pos = (BlockPos) args[i];
				args[i] = String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
			}
		}
		return args;
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