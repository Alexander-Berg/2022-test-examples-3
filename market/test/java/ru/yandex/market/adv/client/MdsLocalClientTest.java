package ru.yandex.market.adv.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
@Disabled
@SpringBootTest(classes = {
        MdsAutoconfiguration.class,
        CommonBeanAutoconfiguration.class
})
@TestPropertySource(locations = "/application_local.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MdsLocalClientTest extends AbstractTest {

    @Autowired
    private MdsClient mdsClient;

    @DisplayName("Загрузка корректного файла в локальную папку прошла успешно.")
    @Test
    @Order(10)
    void upload_correctFile_success() throws IOException {
        ClassPathResource resource = new ClassPathResource("ru/yandex/market/adv/client/test.txt");
        mdsClient.upload("test2.txt", new StreamContentProvider(resource.getInputStream()));
    }

    @DisplayName("Скачивание файла из локальной папки прошло успешно.")
    @Test
    @Order(20)
    void download_file_success() {
        String data = mdsClient.download("test2.txt", inputStream -> {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                return bufferedReader.readLine();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        Assertions.assertThat(data)
                .isEqualTo("123456");
    }

    @DisplayName("Получение ссылки на файл из локальной папки прошло успешно.")
    @Test
    @Order(30)
    void getUrl_file_success() {
        Assertions.assertThat(mdsClient.getUrl("test2.txt").toString())
                .endsWith("/test2.txt");
    }

    @DisplayName("Файл присутствует в локальной папке.")
    @Test
    @Order(40)
    void contains_file_true() {
        Assertions.assertThat(mdsClient.contains("test2.txt"))
                .isTrue();
    }

    @DisplayName("Удаление файла из локальной папки прошло успешно.")
    @Test
    @Order(50)
    void delete_file_success() {
        mdsClient.delete("test2.txt");
    }

    @DisplayName("Файл отсутствует в локальной папке.")
    @Test
    @Order(60)
    void contains_wrongFile_false() {
        Assertions.assertThat(mdsClient.contains("test2.txt"))
                .isFalse();
    }
}
