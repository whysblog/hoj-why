package top.hcode.hoj.utils;

import cn.hutool.core.io.FileUtil;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

/** Security helpers for handling untrusted uploaded archives and names. */
public final class SecurityFileUtils {
    private SecurityFileUtils() {}

    public static String safeFileName(String name) {
        if (name == null || name.trim().isEmpty()) return "upload";
        return Paths.get(name).getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public static void safeUnzip(File archive, File destination) {
        try { safeUnzipChecked(archive, destination); } catch (IOException e) { throw new IllegalArgumentException("压缩包解压失败", e); }
    }

    private static void safeUnzipChecked(File archive, File destination) throws IOException {
        Path root = destination.toPath().toAbsolutePath().normalize();
        FileUtil.mkdir(root.toFile());
        long total = 0;
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zin.getNextEntry()) != null) {
                Path target = root.resolve(entry.getName()).normalize();
                if (!target.startsWith(root)) throw new IOException("压缩包包含非法路径");
                if (entry.isDirectory()) { Files.createDirectories(target); continue; }
                Files.createDirectories(target.getParent());
                try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    int read;
                    while ((read = zin.read(buffer)) != -1) {
                        total += read;
                        if (total > 256L * 1024 * 1024) throw new IOException("压缩包解压后超过大小限制");
                        out.write(buffer, 0, read);
                    }
                }
            }
        }
    }
}
