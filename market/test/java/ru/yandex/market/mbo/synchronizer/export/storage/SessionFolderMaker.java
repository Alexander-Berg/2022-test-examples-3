package ru.yandex.market.mbo.synchronizer.export.storage;

import org.apache.commons.io.IOUtils;
import ru.yandex.market.mbo.synchronizer.export.ExporterUtils;
import ru.yandex.market.mbo.synchronizer.export.MD5SUMS;
import ru.yandex.market.mbo.synchronizer.export.Md5Writer;
import ru.yandex.market.mbo.synchronizer.export.io.Md5CountingOutputStream;
import ru.yandex.market.mbo.synchronizer.export.io.Md5OutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author ayratgdl
 * @date 14.06.17
 */
public class SessionFolderMaker {
    public static final String FILE_NAME = "folder1" + File.separator + "file1_1.txt";
    public static  final String FILE_CONTENT = "file_1_1.txt Content";
    public static final String MD5SUMS_CONTENT = computeMd5(FILE_CONTENT) + MD5SUMS.SEPARATOR + FILE_NAME;

    private Path uploadsDir;

    public SessionFolderMaker(File uploadsDir) {
        this.uploadsDir = uploadsDir.toPath();
    }

    public Path createSessionFolder(String sessionId) throws IOException {
        Path sessionDir = uploadsDir.resolve(sessionId);
        writeFile(sessionDir.resolve(MD5SUMS.MD5SUMS_FILE_NAME), MD5SUMS_CONTENT);
        writeFile(sessionDir.resolve(FILE_NAME), FILE_CONTENT);
        return sessionDir;
    }

    private static String computeMd5(String content) {
        try {
            Md5OutputStream md5Out = new Md5CountingOutputStream(new ByteArrayOutputStream());
            IOUtils.write(content, md5Out, StandardCharsets.UTF_8);
            return md5Out.getMd5();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeFile(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        try (Md5Writer writer = ExporterUtils.getWriter(file.toFile())) {
            writer.write(content);
        }
    }
}
