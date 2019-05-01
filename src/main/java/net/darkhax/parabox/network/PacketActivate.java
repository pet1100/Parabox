package net.darkhax.parabox.network;

import net.darkhax.bookshelf.network.TileEntityMessage;
import net.darkhax.parabox.block.TileEntityParabox;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketActivate extends TileEntityMessage<TileEntityParabox> {

	public PacketActivate() {

	}

	public PacketActivate(BlockPos pos) {

		super(pos);
	}

	@Override
	public final IMessage handleMessage(MessageContext context) {

		super.handleMessage(context);
		return new PacketRefreshGui();
	}

	@Override
	public void getAction() {
		if (this.tile.isActive()) this.tile.getVoter().voteDeactivate(this.context.getServerHandler().player);
		else this.tile.getVoter().voteActivate(this.context.getServerHandler().player);
		this.tile.sync();
	}

}
