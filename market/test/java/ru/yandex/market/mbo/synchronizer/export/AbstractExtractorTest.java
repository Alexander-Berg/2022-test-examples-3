package ru.yandex.market.mbo.synchronizer.export;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.synchronizer.export.storage.S3DumpStorageCoreService;

import static org.mockito.Mockito.mock;

public class AbstractExtractorTest {

    private TestExtractor extractor;
    private ExportRegistry registry = mock(ExportRegistry.class);

    @Before
    public void setUp() throws Exception {
        extractor = new TestExtractor();
        extractor.filesToBeExtracted = 1;
        extractor.setRegistry(registry);
    }

    @Test
    public void performExtractor() throws Exception {
        extractor.performExtractor("dir");
    }

    @Test(expected = IllegalStateException.class)
    public void performExtractorFilesToBeExtractedNotEquals() throws Exception {
        extractor.filesToBeExtracted = 2;
        extractor.performExtractor("dir");
    }

    private static class TestExtractor extends AbstractExtractor {

        private int filesToBeExtracted;

        @Override
        public void perform(String dir) throws Exception {
            validateAndRegisterThis("12345", "outputFilePath", new EmptyValidator());
        }

        @Override
        public int filesToBeExtracted() {
            return filesToBeExtracted;
        }
    }

    private static class EmptyValidator implements ExportFileValidator {

        @Override
        public boolean validate(String fQN) {
            return true;
        }

        @Override
        public boolean validateS3(S3DumpStorageCoreService s3DumpStorageCoreService, String keyName) {
            return true;
        }
    }
}
