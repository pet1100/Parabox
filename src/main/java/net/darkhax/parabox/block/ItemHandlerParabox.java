package net.darkhax.parabox.block;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;

public class ItemHandlerParabox implements IItemHandler {

	TileEntityParabox box;

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
		if (OreDictionary.itemMatches(box.getReqItem(), stack, false)) {
			ItemStack copy = stack.copy();
			copy.shrink(1);
			if (!simulate) box.provideItem();
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

}
