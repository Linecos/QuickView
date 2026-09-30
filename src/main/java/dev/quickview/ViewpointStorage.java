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
    private static final Path BASE_DIR = FabricLoader.getInstance().getGameDir().resolve("quickview");

    public static Path getStoragePath(String context, String dimension) {
        return BASE_DIR.resolve(sanitizeName(context)).resolve(sanitizeName(dimension) + ".json");
    }

    public static List<Viewpoint> load(String context, String dimension) {
        Path path = getStoragePath(context, dimension);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            Type listType = new TypeToken<List<Viewpoint>>() {}.getType();
            List<Viewpoint> result = GSON.fromJson(reader, listType);
            return result != null ? new ArrayList<>(result) : new ArrayList<>();
        } catch (Exception e) {
            QuickViewClient.LOGGER.error("Failed to load viewpoints for context '{}' dimension '{}': {}", context, dimension, e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void save(String context, String dimension, List<Viewpoint> viewpoints) {
        Path path = getStoragePath(context, dimension);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(viewpoints, writer);
            }
        } catch (Exception e) {
            QuickViewClient.LOGGER.error("Failed to save viewpoints for context '{}' dimension '{}': {}", context, dimension, e.getMessage());
        }
    }

    private static String sanitizeName(String name) {
        if (name == null) {
            return "unknown";
        }
        String safe = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        safe = safe.replaceAll("[. ]+$", "");
        if (safe.isEmpty() || safe.equals(".") || safe.equals("..")) {
            safe = "unknown";
        }
        if (isReservedWindowsName(safe)) {
            safe = "_" + safe;
        }
        return safe;
    }

    private static boolean isReservedWindowsName(String name) {
        int dot = name.indexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        String upper = base.toUpperCase();
        if (upper.equals("CON") || upper.equals("PRN") || upper.equals("AUX") || upper.equals("NUL")) {
            return true;
        }
        return upper.matches("COM[1-9]") || upper.matches("LPT[1-9]");
    }
}