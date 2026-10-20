package dev.quickview;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ViewpointStorage {
    /** 文件格式版本号。字段/语义变更时递增，load 侧可据此做迁移。 */
    private static final int SCHEMA_VERSION = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path BASE_DIR = FabricLoader.getInstance().getGameDir().resolve("quickview");
    private static final Type LIST_TYPE = new TypeToken<List<Viewpoint>>() {}.getType();

    /** 磁盘上的文件结构：{ "version": 1, "viewpoints": [ ... ] }。 */
    private static final class StorageFile {
        private int version = SCHEMA_VERSION;
        private List<Viewpoint> viewpoints = new ArrayList<>();
    }

    public static Path getStoragePath(String context, String dimension) {
        // 路径名清洗逻辑移至 StorageNames（纯函数，便于单测）
        return BASE_DIR.resolve(StorageNames.sanitize(context)).resolve(StorageNames.sanitize(dimension) + ".json");
    }

    public static List<Viewpoint> load(String context, String dimension) {
        Path path = getStoragePath(context, dimension);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || root.isJsonNull()) {
                return new ArrayList<>();
            }

            JsonArray array;
            if (root.isJsonObject()) {
                // 当前格式
                JsonElement viewpoints = root.getAsJsonObject().get("viewpoints");
                if (viewpoints == null || !viewpoints.isJsonArray()) {
                    return new ArrayList<>();
                }
                array = viewpoints.getAsJsonArray();
            } else if (root.isJsonArray()) {
                // 兼容 1.0.1 及更早版本的裸数组格式（不做迁移，下次 save 时自动升级）
                array = root.getAsJsonArray();
            } else {
                return new ArrayList<>();
            }

            Type listType = LIST_TYPE;
            List<Viewpoint> result = GSON.fromJson(array, listType);
            return result != null ? new ArrayList<>(result) : new ArrayList<>();
        } catch (Exception e) {
            QuickViewClient.LOGGER.error("Failed to load viewpoints for context '{}' dimension '{}': {}", context, dimension, e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void save(String context, String dimension, List<Viewpoint> viewpoints) {
        StorageFile file = new StorageFile();
        file.viewpoints = viewpoints;
        // 原子写入（临时文件 + 原子替换）统一在 JsonFile 里处理
        JsonFile.write(getStoragePath(context, dimension), file, GSON,
                String.format("viewpoints for context '%s' dimension '%s'", context, dimension));
    }
}