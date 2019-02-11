package net.darkhax.parabox.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientProxy extends Proxy {

	boolean hasShutDown = false;

	@Override
	public void onGameShutdown() {
		hasShutDown = true;
	}

	@SubscribeEvent
	public void onGui(GuiOpenEvent e) {
		if (hasShutDown && e.getGui() instanceof GuiMultiplayer) {
			e.setCanceled(true);
			Minecraft.getMinecraft().displayGuiScreen(null);
			hasShutDown = false;
		}
	}

}
