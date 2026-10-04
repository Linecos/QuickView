package dev.quickview;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * JSON 落盘工具：统一「先写临时文件、再原子替换」的写入方式。
 *
 * <p>直接覆盖目标文件的话，写入过程中崩溃/断电会留下半截 JSON，把原有数据整个毁掉。
 */
final class JsonFile {
    private JsonFile() {
    }

    /** 把 {@code value} 以 JSON 写入 {@code path}；失败只记录日志，不抛出。 */
    static void write(Path path, Object value, Gson gson, String what) {
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                gson.toJson(value, writer);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            QuickViewClient.LOGGER.error("Failed to save {} to '{}': {}", what, path, e.getMessage());
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // 清理失败无所谓，下次写入会覆盖
            }
        }
    }
}
