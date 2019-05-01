package net.darkhax.parabox.handler;

import net.darkhax.parabox.block.TileEntityParabox;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;

public class ItemHandlerParabox implements IItemHandler {

	TileEntityParabox box;
	ItemStack target = ItemStack.EMPTY;

	public ItemHandlerParabox(TileEntityParabox box) {
		this.box = box;
	}

	@Override
	public int getSlots() {
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (OreDictionary.itemMatches(target, stack, false)) {
			ItemStack copy = stack.copy();
			if (!simulate) box.provideItem(copy);
			copy.shrink(1);
			return copy;
		}
		return stack;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int slot) {
		return 1;
	}

	public ItemStack getTarget() {
		return target;
	}

	public void setTarget(ItemStack stack) {
		this.target = stack;
	}

	public void randomizeTarget() {
		this.target = this.box.genRandomItem();
	}

}
