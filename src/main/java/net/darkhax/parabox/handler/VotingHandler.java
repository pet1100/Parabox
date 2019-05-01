package net.darkhax.parabox.handler;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.darkhax.parabox.Parabox;
import net.darkhax.parabox.block.TileEntityParabox;
import net.darkhax.parabox.util.WorldSpaceTimeManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class VotingHandler {

	protected TileEntityParabox box;
	protected Set<UUID> activators = new HashSet<>();
	protected Set<UUID> deactivators = new HashSet<>();
	protected Set<UUID> collapsers = new HashSet<>();

	public VotingHandler(TileEntityParabox box) {
		this.box = box;
	}

	public void voteActivate(EntityPlayer voter) {
		activators.add(voter.getGameProfile().getId());
		Parabox.sendMessage(TextFormatting.RED, "parabox.status.active_vote", voter.getDisplayName());
		if (hasEveryoneVoted(activators)) {
			Parabox.sendMessage(TextFormatting.RED, "parabox.status.vote_success");
			box.activate();
			activators.clear();
		} else Parabox.sendMessage(TextFormatting.RED, "parabox.status.more_votes");
	}

	public void voteDeactivate(EntityPlayer voter) {
		deactivators.add(voter.getGameProfile().getId());
		Parabox.sendMessage(TextFormatting.RED, "parabox.status.deactivate_vote", voter.getDisplayName());
		if (hasEveryoneVoted(deactivators)) {
			Parabox.sendMessage(TextFormatting.RED, "parabox.status.vote_success");
			box.deactivate();
			deactivators.clear();
		} else Parabox.sendMessage(TextFormatting.RED, "parabox.status.more_votes");
	}

	public void voteCollapse(EntityPlayer voter) {
		collapsers.add(voter.getGameProfile().getId());
		Parabox.sendMessage(TextFormatting.RED, "parabox.status.collapse_vote", voter.getDisplayName());
		if (hasEveryoneVoted(collapsers)) {
			Parabox.sendMessage(TextFormatting.RED, "parabox.status.vote_success");
			WorldSpaceTimeManager.triggerCollapse((WorldServer) box.getWorld());
			collapsers.clear();
		} else Parabox.sendMessage(TextFormatting.RED, "parabox.status.more_votes");
	}

	protected boolean hasEveryoneVoted(Set<UUID> voters) {
		PlayerList pList = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList();
		Set<UUID> filtered = voters.stream().filter(id -> pList.getPlayerByUUID(id) != null).collect(Collectors.toSet());
		return filtered.size() == pList.getPlayers().size();
	}

}
