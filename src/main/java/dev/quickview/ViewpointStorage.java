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
            if (result == null) {
                return new ArrayList<>();
            }
            // 归一化缺字段：JSON 若为手工编辑/外部工具写入，name / dimension 缺失时 Gson 会反序列化成 null，
            // 而 Text.literal(null)（PlainTextContent.of 直接 isEmpty）与 Identifier.tryParse(null)（trySplitOn
            // 直接 indexOf）都会 NPE——前者界面打开即崩、后者把异常抛进点击事件。数组里的 null 元素剔除。
            List<Viewpoint> normalized = new ArrayList<>(result.size());
            for (Viewpoint vp : result) {
                if (vp == null) {
                    continue;
                }
                if (vp.getName() == null) {
                    vp.setName("");
                }
                if (vp.getDimension() == null) {
                    vp.setDimension("");
                }
                normalized.add(vp);
            }
            return normalized;
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