package net.darkhax.parabox.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import net.darkhax.parabox.Parabox;
import net.minecraft.client.AnvilConverterException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiListWorldSelection;
import net.minecraft.client.gui.GuiListWorldSelectionEntry;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldSummary;

public class GuiPostParabox extends GuiDisconnected {

	String world;

	public GuiPostParabox(String world) {
		super(null, "parabox.collapsed", new TextComponentTranslation("parabox.reset"));
		this.world = world;
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) {
			ISaveFormat isaveformat = this.mc.getSaveLoader();
			List<WorldSummary> list = null;

			try {
				list = isaveformat.getSaveList();
			} catch (AnvilConverterException e) {
				e.printStackTrace();
				return;
			}

			Collections.sort(list);
			GuiWorldSelection sec = new GuiWorldSelection(this);
			sec.setWorldAndResolution(mc, width, height);
			GuiListWorldSelection guiList = new GuiListWorldSelection(sec, this.mc, this.width, this.height, 32, this.height - 64, 36);
			List<Pair<WorldSummary, GuiListWorldSelectionEntry>> entries = new ArrayList<>();
			for (WorldSummary worldsummary : list) {
				entries.add(Pair.of(worldsummary, new GuiListWorldSelectionEntry(guiList, worldsummary, this.mc.getSaveLoader())));
			}
			entries.forEach(Parabox.proxy::tryOpenList);
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		this.buttonList.get(0).displayString = I18n.format("parabox.return", world);
	}

}
