package net.darkhax.parabox.block;

import net.darkhax.parabox.Parabox;
import net.darkhax.parabox.util.TopographyStuff;
import net.darkhax.parabox.util.WorldSpaceTimeManager;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

public class ItemBlockParabox extends ItemBlock {

	public ItemBlockParabox(Block block) {

		super(block);
	}

	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
		if (!isValidDimension(world)) {
			Parabox.sendMessage(player, TextFormatting.RED, "info.parabox.dimension");
			return false;
		}

		else if (!world.isRemote && BlockParabox.getParabox(world, WorldSpaceTimeManager.getWorldData().getParabox()) != null) {
			Parabox.sendMessage(player, TextFormatting.RED, "info.parabox.duplicate", WorldSpaceTimeManager.getWorldData().getParabox());
			return false;
		}

		if (super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) {
			if (!world.isRemote) {
				WorldSpaceTimeManager.getWorldData().setParabox(pos);
				Parabox.sendMessage(player, TextFormatting.GREEN, "info.parabox.created", pos);
			}
			return true;
		}

		return false;
	}

	boolean isValidDimension(World world) {
		if (Loader.isModLoaded("topography") && TopographyStuff.isCompactMachineLand(world)) return true;
		return world.provider.getDimension() == 0;
	}
}