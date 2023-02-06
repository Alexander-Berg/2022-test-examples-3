package ru.yandex.market.mbo.synchronizer.export;

import com.google.common.io.ByteStreams;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Базовый класса для тестов extractor-ов.
 *
 * @author ayratgdl
 * @date 18.04.18
 */
public abstract class ExtractorBaseTestClass {
    protected BaseExtractor extractor;
    protected BaseExtractor actualExtractor;
    private Path rootTmpDir;
    private String outputFilePrefix;

    @Before
    public void setUp() throws Exception {
        rootTmpDir = Files.createTempDirectory(null);

        actualExtractor = createExtractor();
        extractor = new CustomExtractor(actualExtractor);
        ExtractorWriterService extractorWriterService = new ExtractorWriterService();
        extractor.setExtractorWriterService(extractorWriterService);

        ExportRegistry registry = new ExportRegistry();
        registry.setRootPath(rootTmpDir.toAbsolutePath().toString());
        registry.afterPropertiesSet();
        actualExtractor.setRegistry(registry);

        registry.processStart();
    }

    @After
    public void tearDown() throws Exception {
        if (rootTmpDir != null) {
            MoreFiles.deleteRecursively(rootTmpDir, RecursiveDeleteOption.ALLOW_INSECURE);
        }
    }

    protected abstract BaseExtractor createExtractor();

    protected byte[] getExtractContent() {
        try (InputStream input = ExporterUtils.getInputStream(actualExtractor.getOutputFile(outputFilePrefix))) {
            return ByteStreams.toByteArray(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected byte[] performAndGetExtractContent() {
        try {
            extractor.perform("");
            return getExtractContent();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class CustomExtractor extends BaseExtractor {
        private BaseExtractor actualExtractor;

        CustomExtractor(BaseExtractor actualExtractor) {
            this.actualExtractor = actualExtractor;
        }

        @Override
        public void perform(String dir) throws Exception {
            outputFilePrefix = dir;
            actualExtractor.perform(dir);
        }
    }
}
