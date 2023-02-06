package ru.yandex.market.adv.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.adv.config.CommonBeanAutoconfiguration;
import ru.yandex.market.adv.config.MdsAutoconfiguration;
import ru.yandex.market.adv.test.AbstractTest;
import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;

/**
 * Date: 09.02.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@SpringBootTest(classes = {
        MdsAutoconfiguration.class,
        CommonBeanAutoconfiguration.class
})
@TestPropertySource(locations = "/application_mock.properties")
class MdsMockClientTest extends AbstractTest {

    @Autowired
    private MdsClient mdsClient;

    @DisplayName("Удаление файлов, которые есть в classpath, не вернуло ошибку.")
    @Test
    void delete_files_success() {
        mdsClient.delete("test.txt", "marketplace-auction-list.xlsm");
    }

    @DisplayName("Удаление файлов, которых нет в classpath, вернуло ошибку.")
    @Test
    void delete_files_exception() {
        Assertions.assertThatThrownBy(() -> mdsClient.delete("test.txt", "marketplace-auction-list.xlsm", "1.json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File 1.json doesn't exist.");
    }

    @DisplayName("Загрузка файла из classpath прошла успешно.")
    @Test
    void download_file_success() {
        String data = mdsClient.download("test.txt", inputStream -> {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                return bufferedReader.readLine();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        Assertions.assertThat(data)
                .isEqualTo("123456");
    }

    @DisplayName("Загрузка файла из classpath прошла успешно.")
    @Test
    void upload_correctFile_success() throws IOException {
        ClassPathResource resource = new ClassPathResource("ru/yandex/market/adv/client/marketplace-auction-list.xlsm");
        mdsClient.upload("marketplace-auction-list.xlsm", new StreamContentProvider(resource.getInputStream()));
    }

    @DisplayName("Файл присутствует в classpath.")
    @Test
    void contains_file_true() {
        Assertions.assertThat(mdsClient.contains("marketplace-auction-list.xlsm"))
                .isTrue();
    }

    @DisplayName("Файл отсутствует в classpath.")
    @Test
    void contains_wrongFile_false() {
        Assertions.assertThat(mdsClient.contains("test2.txt"))
                .isFalse();
    }

    @DisplayName("Получение ссылки на файл из classpath прошло успешно.")
    @Test
    void getUrl_file_success() {
        Assertions.assertThat(mdsClient.getUrl("marketplace-auction-list.xlsm").toString())
                .endsWith("ru/yandex/market/adv/client/marketplace-auction-list.xlsm");
    }
}
