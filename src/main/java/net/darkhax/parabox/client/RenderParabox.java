package net.darkhax.parabox.client;

import net.darkhax.parabox.Parabox;
import net.darkhax.parabox.block.TileEntityParabox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(value = Side.CLIENT, modid = Parabox.MODID)
public class RenderParabox extends TileEntitySpecialRenderer<TileEntityParabox> {

	@Override
	public void render(TileEntityParabox te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (te == null || !te.isActive()) return;
		if (!te.getWorld().isAirBlock(te.getPos().up()) || Minecraft.getMinecraft().world.rayTraceBlocks(playerVec(), new Vec3d(te.getPos().up()).add(0.5, 0.5, 0.5)) != null) return;

		GlStateManager.pushMatrix();
		int i = te.getWorld().getCombinedLight(te.getPos(), 0);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, i % 65536, i / 65536);

		boolean thirdPerson = Minecraft.getMinecraft().getRenderManager().options.thirdPersonView == 2;
		float viewerYaw = this.rendererDispatcher.entityYaw;
		GlStateManager.translate(x + 0.5, y + 1.9, z + 0.5);
		float angleRotateItem = !thirdPerson ? -viewerYaw : -viewerYaw % 360 + 180;

		GlStateManager.rotate(angleRotateItem, 0, 1, 0);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

		Minecraft.getMinecraft().getRenderItem().renderItem(te.getTarget(), TransformType.FIXED);

		GlStateManager.disableBlend();
		GlStateManager.popMatrix();

	}

	private Vec3d playerVec() {
		return Minecraft.getMinecraft().player.getPositionEyes(Minecraft.getMinecraft().getRenderPartialTicks());
	}

	@SubscribeEvent
	public static void blah(ModelRegistryEvent e) {
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityParabox.class, new RenderParabox());
	}

}
