package net.darkhax.parabox.block.v2;

import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import net.darkhax.parabox.Parabox;
import net.darkhax.parabox.block.TileEntityParabox;
import net.darkhax.parabox.util.ParaboxItemManager;
import net.darkhax.parabox.util.ParaboxUserData;
import net.darkhax.parabox.util.SpeedFactor;
import net.darkhax.parabox.util.WorldSpaceTimeManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

public class TileEntityParaboxV2 extends TileEntityParabox {

	double itemFactor = 1;

	@Override
	public void onEntityUpdate() {
		if (this.world.isRemote || !this.active) return;
		if ((WorldSpaceTimeManager.getWorldData() == null || !WorldSpaceTimeManager.getWorldData().getBackupFile().exists()) && this.active && !WorldSpaceTimeManager.isSaving() && !WorldSpaceTimeManager.requireSaving()) {
			this.deactivate();
		}

		this.ticksOnline++;
		if (this.ticksOnline < 0) this.ticksOnline = 0;

		power = energyHandler.getEnergyStored();

		SpeedFactor factor = SpeedFactor.getForPower(this, power);

		if (this.ticksOnline % 20 == 0) {
			this.sync();
		}

		this.cycleTimeLeft -= Math.min(1, factor.getTicksPerTick());
		this.energyHandler.setEnergy(0);
		this.itemFactor = Math.max(1, factor.getTicksPerTick());

		if (this.cycleTimeLeft <= 0) {
			this.points += 3;
			for (Entry<UUID, ParaboxUserData> data : WorldSpaceTimeManager.getWorldData().getUserData())
				data.getValue().setPoints(this.points);
			WorldSpaceTimeManager.saveCustomWorldData();
			Parabox.sendMessage(TextFormatting.LIGHT_PURPLE, "info.parabox.emp.update.daily", format.format(this.getRFTNeeded()), format.format(getCycleTime() / (20D * 60)));
			this.cycleTimeLeft = getCycleTime();
			this.energyHandler.updateValues(getRFTNeeded() * 2);
			this.itemHandler.randomizeTarget();
		}
	}

	@Override
	public ItemStack genRandomItem() {
		Random rand = new Random();
		rand.setSeed(this.ticksOnline ^ world.rand.nextInt(2500) * rand.nextLong());
		return ParaboxItemManager.EMPOWERED_ITEMS.get(rand.nextInt(ParaboxItemManager.EMPOWERED_ITEMS.size()));
	}

	@Override
	public void provideItem(ItemStack stack) {
		this.cycleTimeLeft -= 2400 * this.itemFactor;
		String oldName = this.itemHandler.getTarget().getDisplayName();
		this.itemHandler.randomizeTarget();
		if (updateMessages) {
			Parabox.sendMessage(TextFormatting.GOLD, "info.parabox.emp.update.item", oldName, format.format(2400 * this.itemFactor / (20 * 60)), this.itemHandler.getTarget().getDisplayName());
		}
	}

	@Override
	public double getCycleTime() {
		return super.getCycleTime() * 2 + 2400 * (points / 3);
	}

	@Override
	public int getRFTNeeded() {
		return 3 * (points == 0 ? rfPerTick : floor(0.33 * rfPerTick * points * cycleFactor));
	}

	@Override
	public List<String> getInfo(List<String> entries, EntityPlayer player) {
		if (this.active) {
			entries.add(I18n.format("parabox.status.power", format.format(this.getPower())));
			entries.add(I18n.format("parabox.status.target", format.format(this.getRFTNeeded() / 2), format.format(this.getRFTNeeded() * 2)));
			entries.add(I18n.format("parabox.status.item", this.itemHandler.getTarget().getDisplayName()));
			entries.add(I18n.format("parabox.status.speed", format.format(SpeedFactor.getForPower(this, this.getPower()).getTicksPerTick())));
			entries.add(I18n.format("parabox.status.cycle", Parabox.ticksToTime(this.getRemainingTicks())));
			entries.add(I18n.format("parabox.status.points", this.points));
		} else {
			entries.add(I18n.format("parabox.status.offline"));
		}
		return entries;
	}

}
