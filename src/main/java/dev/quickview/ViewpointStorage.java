package dev.quickview;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ViewpointStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("quickview");

    public static Path getStoragePath(String dimension) {
        String safeName = dimension.replace(":", "_");
        return CONFIG_DIR.resolve(safeName + ".json");
    }

    public static List<Viewpoint> load(String dimension) {
        Path path = getStoragePath(dimension);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            Type listType = new TypeToken<List<Viewpoint>>() {}.getType();
            List<Viewpoint> result = GSON.fromJson(reader, listType);
            return result != null ? new ArrayList<>(result) : new ArrayList<>();
        } catch (Exception e) {
            QuickViewClient.LOGGER.error("Failed to load viewpoints for dimension {}: {}", dimension, e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void save(String dimension, List<Viewpoint> viewpoints) {
        Path path = getStoragePath(dimension);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(viewpoints, writer);
            }
        } catch (Exception e) {
            QuickViewClient.LOGGER.error("Failed to save viewpoints for dimension {}: {}", dimension, e.getMessage());
        }
    }
}
