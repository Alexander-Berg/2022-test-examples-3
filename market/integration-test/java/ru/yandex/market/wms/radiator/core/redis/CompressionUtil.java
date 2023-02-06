package ru.yandex.market.wms.radiator.core.redis;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompressionUtil {
    private static final Logger logger = LoggerFactory.getLogger(CompressionUtil.class);

    public static void extract(File archive, File targetDirectory) {
        logger.error("Extracting " + archive.getAbsolutePath());

        try (BufferedInputStream bis = new BufferedInputStream(FileUtils.openInputStream(archive));
             CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(bis);
             TarArchiveInputStream i = new TarArchiveInputStream(cis))
        {
            TarArchiveEntry entry;
            while ((entry = i.getNextTarEntry()) != null) {
                if (!i.canReadEntryData(entry)) {
                    // log something?
                    continue;
                }
                File f = targetDirectory.toPath().resolve(entry.getName()).toFile();
                if (entry.isSymbolicLink() || entry.isLink()) {
                    Files.createSymbolicLink(f.toPath(), Path.of(entry.getLinkName()));
                } else if (entry.isDirectory()) {
                    if (!f.isDirectory() && !f.mkdirs()) {
                        throw new IOException("failed to create directory " + f);
                    }
                } else {
                    File parent = f.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("failed to create directory " + parent);
                    }
                    try (OutputStream o = Files.newOutputStream(f.toPath())) {
                        IOUtils.copy(i, o);

                        if ((entry.getMode() & 0111) != 0) {
                            f.setExecutable(true);
                        }
                    }
                }
            }
        } catch (IOException | CompressorException e) {
            ExceptionUtils.rethrow(e);
        }
    }
}
