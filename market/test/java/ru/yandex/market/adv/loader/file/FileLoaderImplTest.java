package ru.yandex.market.adv.loader.file;

import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Date: 23.11.2021
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
class FileLoaderImplTest {

    private final FileLoader fileLoader = new FileLoaderImpl();

    @DisplayName("Загрузка файла в строку из classpath завершилась успехом")
    @Test
    void loadFile_correctFile_success() {
        Assertions.assertThat(fileLoader.loadFile("test.csv", this.getClass()))
                .isEqualTo("TABLE.MODULE.ONE\n");
    }

    @DisplayName("Загрузка файла в массив байт из classpath завершилась успехом")
    @Test
    void loadFileBinary_correctFile_success() {
        Assertions.assertThat(fileLoader.loadFileBinary("test.csv", this.getClass()))
                .isEqualTo("TABLE.MODULE.ONE\n".getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("ConstantConditions")
    @DisplayName("Загрузка данных из ресурса в строку завершилась успехом")
    @Test
    void readResource_correctFile_success() {
        Assertions.assertThat(fileLoader.readResource(this.getClass().getResource("test.csv").getFile()))
                .isEqualTo("TABLE.MODULE.ONE\n");
    }
}
