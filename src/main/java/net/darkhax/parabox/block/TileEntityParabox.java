package net.darkhax.parabox.block;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.darkhax.bookshelf.block.tileentity.TileEntityBasicTickable;
import net.darkhax.parabox.Parabox;
import net.darkhax.parabox.util.ParaboxUserData;
import net.darkhax.parabox.util.WorldSpaceTimeManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class TileEntityParabox extends TileEntityBasicTickable {

	private static final int max = Integer.MAX_VALUE;
	private static final int maxRecieve = 120000;
	private static final int cycleTime = 24000;
	private static final NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());

	protected EnergyHandlerParabox energyHandler;
	protected int remainingTicks = 0;
	protected int generatedPoints = 0;
	protected boolean active = false;
	protected Set<UUID> activators = new HashSet<>();
	protected Set<UUID> deactivators = new HashSet<>();
	protected Set<UUID> collapsers = new HashSet<>();

	public TileEntityParabox() {

		this.energyHandler = new EnergyHandlerParabox(max, maxRecieve);
	}

	public int getPower() {

		return this.energyHandler.getEnergyStored();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {

		if (!this.isActive()) {

		return null; }

		if (capability == CapabilityEnergy.ENERGY) {

		return (T) this.energyHandler; }

		return super.getCapability(capability, facing);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {

		if (!this.active) {

		return false; }

		return capability == CapabilityEnergy.ENERGY;
	}

	@Override
	public void writeNBT(NBTTagCompound dataTag) {

		dataTag.setInteger("StoredPower", this.energyHandler.getEnergyStored());
		dataTag.setInteger("RemainingTicks", this.remainingTicks);
		dataTag.setLong("Points", this.generatedPoints);
		dataTag.setBoolean("Active", this.active);
	}

	@Override
	public void readNBT(NBTTagCompound dataTag) {

		this.energyHandler = new EnergyHandlerParabox(max, maxRecieve, dataTag.getInteger("StoredPower"));
		this.remainingTicks = dataTag.getInteger("RemainingTicks");
		this.generatedPoints = dataTag.getInteger("Points");
		this.active = dataTag.getBoolean("Active");
	}

	@Override
	public void onEntityUpdate() {

		if ((WorldSpaceTimeManager.getWorldData() == null || !WorldSpaceTimeManager.getWorldData().getBackupFile().exists()) && this.active && !WorldSpaceTimeManager.isSaving() && !WorldSpaceTimeManager.requireSaving()) {

			this.setActive(false);
		}

		if (this.world.isRemote || !this.active) { return; }

		if (this.remainingTicks < 0) {

			this.remainingTicks = cycleTime;
		}

		this.remainingTicks--;

		if (this.remainingTicks % 20 == 0) {

			this.sync();
		}

		// Check if a day has passed.
		if (this.remainingTicks == 0) {

			final int requiredPower = this.getRequiredPower();

			// Power demands met.
			if (this.energyHandler.getEnergyStored() >= requiredPower) {

				this.generatedPoints++;
				this.energyHandler.setEnergy(this.energyHandler.getEnergyStored() - requiredPower);
				for (Entry<UUID, ParaboxUserData> data : WorldSpaceTimeManager.getWorldData().getUserData())
					data.getValue().setPoints(this.generatedPoints);
				WorldSpaceTimeManager.saveCustomWorldData();
				Parabox.sendMessage(TextFormatting.LIGHT_PURPLE, "info.parabox.update.daily", this.getRequiredPower());
			}

			// Power demands not met.
			else {

				Parabox.sendMessage(TextFormatting.RED, "info.parabox.update.deactivate", this.generatedPoints);
				this.setActive(false);
			}
		}

		// 30 second warning, and one minute warnng.
		else if ((this.remainingTicks == 600 || this.remainingTicks == 1200) && this.energyHandler.getEnergyStored() < this.getRequiredPower()) {

			Parabox.sendMessage(TextFormatting.RED, "info.parabox.update.warn", this.generatedPoints, this.getMissingPower(), Parabox.ticksToTime(this.getRemainingTicks()));
		}
	}

	public int getMissingPower() {

		return Math.max(this.getRequiredPower() - this.energyHandler.getEnergyStored(), 0);
	}

	public int getRequiredPower() {

		return (this.generatedPoints + 1) * 100000;
	}

	public int getRemainingTicks() {

		return this.remainingTicks;
	}

	public IEnergyStorage getEnergyHandler() {

		return this.energyHandler;
	}

	public void setActive(boolean state) {

		this.active = state;

		if (state) {
			WorldSpaceTimeManager.initiateWorldBackup();
		} else {

			this.generatedPoints = 0;
			this.remainingTicks = 0;

			for (Entry<UUID, ParaboxUserData> data : WorldSpaceTimeManager.getWorldData().getUserData()) {
				data.getValue().setPoints(0);
			}

			WorldSpaceTimeManager.saveCustomWorldData();
			WorldSpaceTimeManager.handleFailState();
		}
	}

	public boolean isActive() {

		return this.active;
	}

	public List<String> getInfo(List<String> entries, EntityPlayer player) {

		if (this.active) {

			entries.add(I18n.format("parabox.status.target", format.format(this.getRequiredPower())));
			entries.add(I18n.format("parabox.status.power", format.format(this.getPower())));
			entries.add(I18n.format("parabox.status.missing", format.format(this.getMissingPower())));
			entries.add(I18n.format("parabox.status.cycle", Parabox.ticksToTime(this.getRemainingTicks())));
			entries.add(I18n.format("parabox.status.points", this.generatedPoints));
		}

		else {

			entries.add(I18n.format("parabox.status.offline"));
		}

		return entries;
	}

	public void voteActivate(EntityPlayer voter) {
		activators.add(voter.getGameProfile().getId());
		Parabox.sendMessage(TextFormatting.RED, "parabox.status.active_vote", voter.getDisplayName());
		if (hasEveryoneVoted(activators)) {
			Parabox.sendMessage(TextFormatting.RED, "parabox.status.vote_success");
			setActive(true);
			activators.clear();
		} else Parabox.sendMessage(TextFormatting.RED, "parabox.status.more_votes");
	}

	public void voteDeactivate(EntityPlayer voter) {
		deactivators.add(voter.getGameProfile().getId());
		Parabox.sendMessage(TextFormatting.RED, "parabox.status.deactivate_vote", voter.getDisplayName());
		if (hasEveryoneVoted(deactivators)) {
			Parabox.sendMessage(TextFormatting.RED, "parabox.status.vote_success");
			setActive(false);
			deactivators.clear();
		} else Parabox.sendMessage(TextFormatting.RED, "parabox.status.more_votes");
	}

	public void voteCollapse(EntityPlayer voter) {
		collapsers.add(voter.getGameProfile().getId());
		Parabox.sendMessage(TextFormatting.RED, "parabox.status.collapse_vote", voter.getDisplayName());
		if (hasEveryoneVoted(collapsers)) {
			Parabox.sendMessage(TextFormatting.RED, "parabox.status.vote_success");
			WorldSpaceTimeManager.triggerCollapse((WorldServer) world);
			collapsers.clear();
		} else Parabox.sendMessage(TextFormatting.RED, "parabox.status.more_votes");
	}

	private boolean hasEveryoneVoted(Set<UUID> voters) {
		PlayerList pList = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList();
		Set<UUID> filtered = voters.stream().filter(id -> pList.getPlayerByUUID(id) != null).collect(Collectors.toSet());
		return filtered.size() == pList.getPlayers().size();
	}
}