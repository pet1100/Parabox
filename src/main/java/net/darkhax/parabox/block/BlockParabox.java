package net.darkhax.parabox.block;

import net.darkhax.bookshelf.block.BlockTileEntity;
import net.darkhax.parabox.Parabox;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockParabox extends BlockTileEntity {

	public BlockParabox() {

		super(Material.ROCK);
		this.setResistance(6000000.0F);
		this.setHardness(50.0F);
		this.setResistance(2000.0F);
		this.setSoundType(SoundType.STONE);
		this.setLightOpacity(0);
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {

		return new TileEntityParabox();
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (worldIn.isRemote) {
			playerIn.openGui(Parabox.instance, 0, worldIn, pos.getX(), pos.getY(), pos.getZ());
		}

		return true;
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		final TileEntityParabox box = getParabox(world, pos);

		if (box != null) {
			if (!world.isRemote) box.setActive(false);
			return super.removedByPlayer(state, world, pos, player, willHarvest);
		}
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {

		return false;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {

		return false;
	}

	public static TileEntityParabox getParabox(World world, BlockPos pos) {

		final TileEntity tile = world.getTileEntity(pos);

		return tile instanceof TileEntityParabox ? (TileEntityParabox) tile : null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getRenderLayer() {

		return BlockRenderLayer.TRANSLUCENT;
	}
}