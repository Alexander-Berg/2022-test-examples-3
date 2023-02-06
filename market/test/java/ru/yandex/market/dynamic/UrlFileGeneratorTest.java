package ru.yandex.market.dynamic;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import ru.yandex.market.tags.Components;
import ru.yandex.market.tags.Features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@Tag(Features.DYNAMIC)
@Tag(Components.MBI_BILLING)
public class UrlFileGeneratorTest {

    private static final String TEST_DATA = "1234";

    /**
     * Проверяет загрузку файла по URL локального временного файла.
     */
    @Test
    @DisplayName("Генератор динамика, загружающий файл по URL (локальный файл)")
    public void generate() throws Exception {
        File tmp = Files.createTempFile(null, null).toFile();
        File result = null;
        try {
            FileUtils.write(tmp, TEST_DATA, Charset.defaultCharset());
            UrlFileGenerator gen = new UrlFileGenerator("test.1",
                    tmp.getAbsoluteFile().toURI().toURL().toString(),
                    DynamicGenerationStatus.IN_PROGRESS);
            result = gen.generate(1);
            assertNotNull(result);
            assertEquals("Content should be equal",
                    TEST_DATA,
                    FileUtils.readFileToString(result, Charset.defaultCharset())
            );
        } finally {
            FileUtils.deleteQuietly(result);
            FileUtils.deleteQuietly(tmp);
        }
    }

}