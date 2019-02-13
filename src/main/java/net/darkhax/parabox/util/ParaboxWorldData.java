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
import com.google.gson.annotations.Expose;

import net.minecraft.util.math.BlockPos;

public class ParaboxWorldData {

	private static final Gson GSON = new Gson();

	@Expose
	private final UUID worldId;

	@Expose
	private boolean shouldDelete = false;

	@Expose
	private final Map<UUID, ParaboxUserData> data = new HashMap<>();

	@Expose
	private long parabox;

	public boolean isShouldDelete() {

		return this.shouldDelete;
	}

	public void setShouldDelete(boolean shouldDelete) {

		this.shouldDelete = shouldDelete;
	}

	public ParaboxUserData getUserData(UUID userId) {

		return this.data.get(userId);
	}

	public BlockPos getParabox() {
		return BlockPos.fromLong(parabox);
	}

	public void setParabox(BlockPos pos) {
		this.parabox = pos.toLong();
	}

	public void addUser(UUID userId, ParaboxUserData data) {

		this.data.put(userId, data);
	}

	public void removeUser(UUID userId) {

		this.data.remove(userId);
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

			return GSON.fromJson(reader, ParaboxWorldData.class);
		}

		catch (final IOException e) {

			e.printStackTrace();
		}

		return null;
	}
}