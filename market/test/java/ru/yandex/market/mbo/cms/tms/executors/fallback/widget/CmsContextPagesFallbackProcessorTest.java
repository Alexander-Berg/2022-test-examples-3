package ru.yandex.market.mbo.cms.tms.executors.fallback.widget;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.cms.tms.executors.fallback.FallbackData;
import ru.yandex.market.mbo.cms.tms.extractors.CmsContextPagesExtractor;
import ru.yandex.market.mbo.cms.tms.utils.Md5FilesRegistry;

import static org.junit.Assert.assertEquals;

@SuppressWarnings({"magicnumber", "linelength", })
public class CmsContextPagesFallbackProcessorTest {
    public static final String TEMP_DIR = "test_tmp";
    private CmsContextPagesFallbackProcessor fallbackProcessor = new CmsContextPagesFallbackProcessor();

    @Before
    public void setup() throws IOException {
        File tempDir = new File(TEMP_DIR);
        FileUtils.deleteDirectory(tempDir);
    }

    @Test
    public void doSwitch() throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File oldWidgetsGzFile = compressGzipFile(classLoader.getResource("fallback/cmsWidgetsExtractor/old/cms" +
                "-widgets.txt").getFile());
        File oldRelationsGzFile = compressGzipFile(classLoader.getResource("fallback/cmsWidgetsExtractor/old/cms" +
                "-widgets-relations.txt").getFile());

        File newWidgetsTxtFile =
                new File(classLoader.getResource("fallback/cmsWidgetsExtractor/new/cms-widgets.txt").getFile());
        File newRelationsTxtFile = new File(classLoader.getResource("fallback/cmsWidgetsExtractor/new/cms-widgets" +
                "-relations.txt").getFile());

        Md5FilesRegistry registry = getFilesRegistry();

        writeToRegistry(registry, newWidgetsTxtFile, CmsContextPagesExtractor.PAGES_FILE);
        writeToRegistry(registry, newRelationsTxtFile, CmsContextPagesExtractor.RELATIONS_FILE);

        File newWidgetsGzFileInRegistry = registry.getFile(CmsContextPagesExtractor.PAGES_FILE);
        File newRelationsGzFileInRegistry = registry.getFile(CmsContextPagesExtractor.RELATIONS_FILE);

        registry.complete();
        File md5Sums = registry.getSumsFile();
        validateMd5(md5Sums, newWidgetsTxtFile, newRelationsTxtFile);

        FallbackData fallbackData = new FallbackData();

        fallbackData.appendData(CmsContextPagesExtractor.PAGES_FILE, "666");
        fallbackData.appendData(CmsContextPagesExtractor.PAGES_FILE, "666666");
        fallbackData.appendData(CmsContextPagesExtractor.RELATIONS_FILE, "device=desktop#domain=by#ds=1#format=json" +
                "#nid=666#type=article#zoom=full");
        fallbackData.appendData(CmsContextPagesExtractor.RELATIONS_FILE, "device=desktop#domain=by#ds=1#format=json" +
                "#nid=666666#type=article#zoom=full");
        fallbackData.appendData(CmsContextPagesExtractor.RELATIONS_FILE, "device=desktop#domain=by#ds=1#format=json" +
                "#nid=6#type=article#zoom=full");

        fallbackProcessor.doSwitch(new ExtractionFiles(oldWidgetsGzFile, oldRelationsGzFile), registry, fallbackData);

        registry.complete();

        File txtWidgetsFromRegistry = File.createTempFile(TEMP_DIR + "/cms-widgets", "txt");
        File txtRelationsFromRegistry = File.createTempFile(TEMP_DIR + "/cms-widgets-relations", "txt");
        decompressGzipFile(newWidgetsGzFileInRegistry, txtWidgetsFromRegistry);
        decompressGzipFile(newRelationsGzFileInRegistry, txtRelationsFromRegistry);

        File expectedWidgetsFile =
                new File(classLoader.getResource("fallback/cmsWidgetsExtractor/merge/cms-widgets.txt")
                        .getFile());

        File expectedRelationsFile =
                new File(classLoader.getResource("fallback/cmsWidgetsExtractor/merge/cms-widgets-relations.txt")
                        .getFile());

        assertEqualsExtractions(expectedWidgetsFile, txtWidgetsFromRegistry);
        assertEqualsExtractions(expectedRelationsFile, txtRelationsFromRegistry);

        validateMd5(md5Sums, txtWidgetsFromRegistry, txtRelationsFromRegistry);
    }

    private void validateMd5(File md5File, File widgetsTxtFile, File relationsMd5File) throws IOException {
        String widgetsMd5 = getFileMd5(widgetsTxtFile);
        String relationsMd5 = getFileMd5(relationsMd5File);
        File txtMd5File = File.createTempFile(TEMP_DIR + "/md5", "txt");
        decompressGzipFile(md5File, txtMd5File);
        List<String> md5Lines = Files.readAllLines(Paths.get(txtMd5File.toURI()));
        Map<String, String> md5Map = new HashMap<>();

        for (String line : md5Lines) {
            String[] parts = line.split(Md5FilesRegistry.MD5_SEPARATOR);
            String md5 = parts[0];
            String name = parts[1];
            md5Map.put(name, md5);
        }

        assertEquals(widgetsMd5, md5Map.get(CmsContextPagesExtractor.PAGES_FILE));
        assertEquals(relationsMd5, md5Map.get(CmsContextPagesExtractor.RELATIONS_FILE));
    }

    private String getFileMd5(File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return DigestUtils.md5Hex(IOUtils.toByteArray(fileInputStream));
        }
    }

    private Md5FilesRegistry getFilesRegistry() {
        Md5FilesRegistry result = new Md5FilesRegistry(new File(TEMP_DIR));
        return result;
    }

    private void writeToRegistry(Md5FilesRegistry registry, File source, String name) throws IOException {
        char[] buffer = new char[1024];
        int length;

        try (BufferedWriter writer = registry.createBufferedWriter(name, this);
             BufferedReader reader = new BufferedReader(new FileReader(source))) {

            while ((length = reader.read(buffer)) > 0) {
                writer.write(buffer, 0, length);
            }
        }
    }

    private static File compressGzipFile(String file) {
        File gzipFile = new File(file + ".gz");
        try (FileInputStream fis = new FileInputStream(file);
             FileOutputStream fos = new FileOutputStream(gzipFile);
             GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, len);
            }

            return gzipFile;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void decompressGzipFile(File gzipFile, File newFile) {
        try (FileInputStream fis = new FileInputStream(gzipFile);
             GZIPInputStream gis = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(newFile)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void assertEqualsExtractions(File expected, File actual) throws IOException {
        Set<String> expectedLines = new HashSet<>(Files.readAllLines(expected.toPath()));
        Set<String> actualLines = new HashSet<>(Files.readAllLines(actual.toPath()));

        assertEquals(expectedLines, actualLines);
    }
}
