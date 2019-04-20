package net.darkhax.parabox.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import net.minecraft.util.math.BlockPos;

public class ParaboxWorldData {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	@Expose
	private UUID worldId;

	@Expose
	private boolean shouldDelete = false;

	@Expose
	private Map<UUID, ParaboxUserData> data = new HashMap<>();

	@Expose
	private long parabox;

	public boolean isShouldDelete() {

		return this.shouldDelete;
	}

	public void setShouldDelete(boolean shouldDelete) {

		this.shouldDelete = shouldDelete;
	}

	public ParaboxUserData getOrCreateData(UUID userId) {
		ParaboxUserData data = this.data.get(userId);
		if (data == null) {
			this.data.put(userId, new ParaboxUserData());
			WorldSpaceTimeManager.saveCustomWorldData();
		}
		return data;
	}

	public BlockPos getParabox() {
		return BlockPos.fromLong(parabox);
	}

	public void setParabox(BlockPos pos) {
		this.parabox = pos.toLong();
	}

	public Set<Entry<UUID, ParaboxUserData>> getUserData() {

		return this.data.entrySet();
	}

	private ParaboxWorldData(UUID id) {

		this.worldId = id;
	}

	public UUID getWorldId() {

		return this.worldId;
	}

	public File getBackupFile() {

		return new File(WorldHelper.getBackupDir(), this.getWorldId().toString().toLowerCase() + ".zip");
	}

	public void save(File worldFile) {

		final File dataFile = new File(worldFile, "parabox.json");

		try (FileWriter writer = new FileWriter(dataFile)) {

			GSON.toJson(this, writer);
		}

		catch (final IOException e) {

			e.printStackTrace();
		}
	}

	public static ParaboxWorldData getData(File worldFile) {

		final File dataFile = new File(worldFile, "parabox.json");

		if (!dataFile.exists()) {

			new ParaboxWorldData(UUID.randomUUID()).save(worldFile);
		}

		try (FileReader reader = new FileReader(dataFile)) {

			ParaboxWorldData wData = GSON.fromJson(reader, ParaboxWorldData.class);
			if (wData == null) wData = new ParaboxWorldData(UUID.randomUUID());
			if (wData.data == null) wData.data = new HashMap<>();
			return wData;
		}

		catch (final IOException e) {

			e.printStackTrace();
		}

		return null;
	}
}