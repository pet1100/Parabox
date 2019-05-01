package net.darkhax.parabox.block.v2;

import net.darkhax.parabox.block.BlockParabox;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockParaboxV2 extends BlockParabox {

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityParaboxV2();
	}

}
