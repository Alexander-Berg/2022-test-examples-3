package ru.yandex.market.core.util;

import java.io.UncheckedIOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Date: 01.07.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@SuppressWarnings({"checkstyle:lineLength", "ConstantConditions"})
class ResourceUtilsTest {

    private static final String URL =
            "https://download.cdn.yandex.net/from/yandex.ru/support/ru/marketplace/files/marketplace-catalog-standard-testing.xlsx";

    @DisplayName("Получение расширения файла. Выкидываем исключение, если у файла нет расширения.")
    @Test
    void getFileExtension_withoutExtensionClassPath_throwIllegalArgumentException() {
        Resource resource = new ClassPathResource("ResourceUtils/withoutExtension", ResourceUtils.class);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ResourceUtils.getFileExtension(resource.getFilename()))
                .withMessage("Unknown extension of file.");
    }

    @DisplayName("Получение расширения файла. Корректное расширение.")
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            ".xlsm,united/feed/marketplace-prices.xlsm",
            ".jpg,supplier/certificate/empty.jpg"
    })
    void getFileExtension_withExtensionClassPath_extension(String extension, String path) {
        Resource resource = new ClassPathResource(path);
        assertThat(ResourceUtils.getFileExtension(resource.getFilename()))
                .isEqualTo(extension);
    }

    @DisplayName("Получение ресурса по URL. Выкидываем исключение, если ресурс не корректен.")
    @Test
    void createUrlResource_incorrectUrl_throwUncheckedIOException() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> ResourceUtils.createUrlResource("united/feed/marketplace-stock.xlsx"));
    }

    @Disabled("В arcadia не работает получение ресурса по URL. Не надежно для unit-тестов.")
    @DisplayName("Получение расширения файла, полученного по URL. Выкидываем исключение, если у файла нет расширения.")
    @Test
    void getFileExtension_withoutExtensionUrl_throwIllegalArgumentException() {
        Resource resource = ResourceUtils.createUrlResource("https://yandex.ru/search");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ResourceUtils.getFileExtension(resource.getFilename()))
                .withMessage("Unknown extension of file.");
    }

    @Disabled("В arcadia не работает получение ресурса по URL. Не надежно для unit-тестов.")
    @DisplayName("Получение расширения файла, полученного по URL. Корректное расширение .xlsx.")
    @Test
    void getFileExtension_withExtensionUrl_extensionXlsx() {
        Resource resource = ResourceUtils.createUrlResource(URL);
        assertThat(ResourceUtils.getFileExtension(resource.getFilename()))
                .isEqualTo(".xlsx");
    }

    @Disabled("В arcadia не работает получение ресурса по URL. Не надежно для unit-тестов.")
    @DisplayName("Получение ресурса по URL. Выкидываем исключение, если ресурс не корректен.")
    @Test
    void createUrlResource_correctUrl_resource() {
        assertThat(ResourceUtils.createUrlResource(URL).getFilename())
                .isEqualTo("marketplace-catalog-standard-testing.xlsx");
    }
}
