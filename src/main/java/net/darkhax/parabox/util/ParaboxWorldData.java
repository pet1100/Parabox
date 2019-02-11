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

public class ParaboxWorldData {

    private static final Gson GSON = new Gson();

    @Expose
    private final UUID worldId;

    @Expose
    private boolean shouldDelete = false;

    @Expose
    private final Map<UUID, ParaboxUserData> confirmations = new HashMap<>();

    public boolean isShouldDelete () {

        return this.shouldDelete;
    }

    public void setShouldDelete (boolean shouldDelete) {

        this.shouldDelete = shouldDelete;
    }

    public ParaboxUserData getUserData (UUID userId) {

        return this.confirmations.get(userId);
    }

    public void addUser (UUID userId, ParaboxUserData data) {

        this.confirmations.put(userId, data);
    }

    public void removeUser (UUID userId) {

        this.confirmations.remove(userId);
    }

    public Set<Entry<UUID, ParaboxUserData>> getUserData () {

        return this.confirmations.entrySet();
    }

    private ParaboxWorldData (UUID id) {

        this.worldId = id;
    }

    public UUID getWorldId () {

        return this.worldId;
    }

    public File getBackupFile () {

        return new File(WorldHelper.getBackupDir(), this.getWorldId().toString().toLowerCase() + ".zip");
    }

    public void save (File worldFile) {

        final File dataFile = new File(worldFile, "parabox.json");

        try (FileWriter writer = new FileWriter(dataFile)) {

            GSON.toJson(this, writer);
        }

        catch (final IOException e) {

            e.printStackTrace();
        }
    }

    public static ParaboxWorldData getData (File worldFile) {

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