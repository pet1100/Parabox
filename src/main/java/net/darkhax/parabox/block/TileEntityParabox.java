package net.darkhax.parabox.block;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.darkhax.bookshelf.block.tileentity.TileEntityBasicTickable;
import net.darkhax.parabox.Parabox;
import net.darkhax.parabox.util.ParaboxUserData;
import net.darkhax.parabox.util.WorldSpaceTimeManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;

public class TileEntityParabox extends TileEntityBasicTickable {

	public static int cycleTime = 12000;
	public static int rfPerTick = 400;
	public static float cycleFactor = 2F;
	public static NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());
	public static List<ItemStack> validStacks = new ArrayList<>(30000);

	protected EnergyHandlerParabox energyHandler = new EnergyHandlerParabox(rfPerTick * 2, rfPerTick * 2);
	protected ItemHandlerParabox itemHandler = new ItemHandlerParabox(this);
	protected double remainingTicks = 0;
	protected int generatedPoints = 0;
	protected int ticksOnline = 0;
	protected boolean active = false;
	protected Set<UUID> activators = new HashSet<>();
	protected Set<UUID> deactivators = new HashSet<>();
	protected Set<UUID> collapsers = new HashSet<>();
	protected ItemStack targetStack = ItemStack.EMPTY;

	public int getPower() {
		return this.energyHandler.getEnergyStored();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (this.isActive() && capability == CapabilityEnergy.ENERGY) return (T) this.energyHandler;
		if (this.isActive() && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T) this.itemHandler;
		return super.getCapability(capability, facing);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return this.isActive() && (capability == CapabilityEnergy.ENERGY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) || super.hasCapability(capability, facing);
	}

	@Override
	public void writeNBT(NBTTagCompound dataTag) {
		dataTag.setInteger("StoredPower", this.energyHandler.getEnergyStored());
		dataTag.setDouble("RemainingTicks", this.remainingTicks);
		dataTag.setLong("Points", this.generatedPoints);
		dataTag.setBoolean("Active", this.active);
		dataTag.setTag("Item", this.targetStack.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void readNBT(NBTTagCompound dataTag) {
		this.energyHandler.setEnergy(dataTag.getInteger("StoredPower"));
		this.remainingTicks = dataTag.getDouble("RemainingTicks");
		this.generatedPoints = dataTag.getInteger("Points");
		this.active = dataTag.getBoolean("Active");
		this.targetStack = new ItemStack(dataTag.getCompoundTag("Item"));
	}

	@Override
	public void onEntityUpdate() {
		if (this.world.isRemote || !this.active) return;
		if ((WorldSpaceTimeManager.getWorldData() == null || !WorldSpaceTimeManager.getWorldData().getBackupFile().exists()) && this.active && !WorldSpaceTimeManager.isSaving() && !WorldSpaceTimeManager.requireSaving()) {
			this.setActive(false);
		}

		this.ticksOnline++;
		if (this.ticksOnline < 0) this.ticksOnline = 0;

		int power = energyHandler.getEnergyStored();

		SpeedFactor factor = SpeedFactor.getForPower(this, power);

		if (this.ticksOnline % 20 == 0) {
			this.sync();
		}

		this.remainingTicks -= factor.ticksPerTick;
		this.energyHandler.setEnergy(0);

		if (this.remainingTicks <= 0) {
			this.generatedPoints++;
			for (Entry<UUID, ParaboxUserData> data : WorldSpaceTimeManager.getWorldData().getUserData())
				data.getValue().setPoints(this.generatedPoints);
			WorldSpaceTimeManager.saveCustomWorldData();
			Parabox.sendMessage(TextFormatting.LIGHT_PURPLE, "info.parabox.update.daily", this.getRFTNeeded());
			this.remainingTicks = cycleTime;
			this.energyHandler.setCapacity(getRFTNeeded() * 2);
			this.energyHandler.setInput(getRFTNeeded() * 2);
			this.targetStack = genRandomItem();
		}

	}

	public int getRFTNeeded() {
		return generatedPoints == 0 ? rfPerTick : floor(rfPerTick * generatedPoints * cycleFactor);
	}

	public ItemStack getReqItem() {
		if (!this.world.isRemote && this.targetStack.isEmpty()) targetStack = genRandomItem();
		return targetStack;
	}

	ItemStack genRandomItem() {
		Random rand = new Random();
		rand.setSeed(this.ticksOnline ^ this.pos.getZ() * rand.nextLong());
		return validStacks.get(rand.nextInt(validStacks.size()));
	}

	public void provideItem() {
		this.remainingTicks -= 1200;
		Parabox.sendMessage(TextFormatting.GOLD, "info.parabox.update.item", targetStack.getDisplayName());
		this.targetStack = genRandomItem();
	}

	public int getRemainingTicks() {
		return floor(this.remainingTicks);
	}

	public static int floor(double f) {
		return MathHelper.floor(f);
	}

	public IEnergyStorage getEnergyHandler() {
		return this.energyHandler;
	}

	public void setActive(boolean state) {
		this.active = state;

		if (active) {
			WorldSpaceTimeManager.initiateWorldBackup();
			this.generatedPoints = 0;
			this.remainingTicks = cycleTime;
			this.ticksOnline = 0;
			this.energyHandler.setCapacity(getRFTNeeded() * 2);
			this.energyHandler.setInput(getRFTNeeded() * 2);
			this.targetStack = genRandomItem();
			this.sync();
		} else {
			this.generatedPoints = 0;

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
			entries.add(I18n.format("parabox.status.power", format.format(this.getPower())));
			entries.add(I18n.format("parabox.status.target", format.format(this.getRFTNeeded() / 2), format.format(this.getRFTNeeded() * 2)));
			entries.add(I18n.format("parabox.status.item", this.targetStack.getDisplayName()));
			entries.add(I18n.format("parabox.status.speed", format.format(SpeedFactor.getForPower(this, this.getPower()).ticksPerTick)));
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

	public int getGeneratedPoints() {
		return this.generatedPoints;
	}

	public enum SpeedFactor {
		OFF(0, 0.5F, 0),
		SLOWEST(0.5F, 0.7F, 0.33),
		SLOW(0.7F, 0.9F, 0.66),
		NORMAL(0.9F, 1.1F, 1),
		FAST(1.1F, 1.5F, 1.1),
		FASTEST(1.5F, 2F, 1.2);

		float min, max;
		double ticksPerTick;

		SpeedFactor(float min, float max, double ticksPerTick) {
			this.min = min;
			this.max = max;
			this.ticksPerTick = ticksPerTick;
		}

		boolean isInRange(TileEntityParabox pbx, int rft) {
			return min * pbx.getRFTNeeded() <= rft && max * pbx.getRFTNeeded() > rft;
		}

		public static SpeedFactor getForPower(TileEntityParabox pbx, int rft) {
			for (SpeedFactor f : SpeedFactor.values()) {
				if (f.isInRange(pbx, rft)) return f;
			}
			return SpeedFactor.FASTEST;
		}
	}
}