package ru.yandex.market.logistics.lom.lms.converter;

import java.util.Optional;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPhoneByLogisticsPointId;
import ru.yandex.market.logistics.management.entity.response.core.Phone;

@ParametersAreNonnullByDefault
@DisplayName("Конвертация телефонов из моделей yt в модели lms")
class PhoneYtToLmsConverterTest extends AbstractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PhoneYtToLmsConverter phoneConverter = new PhoneYtToLmsConverter(objectMapper);

    @Test
    @DisplayName("Конвертация модели с несколькими телефонами")
    void convertWithMultiPhones() {
        YtPhoneByLogisticsPointId ytPhoneByLogisticsPointId = new YtPhoneByLogisticsPointId()
            .setId(1L)
            .setPhones(Optional.of(
                "{\"phones\":[{"
                    + "\"internal_number\":777,"
                    + "\"number\":\"+79781000615\""
                    + "},"
                    + "{"
                    + "\"internal_number\":null,"
                    + "\"number\":\"+79781000625\""
                    + "}]}"
            ));

        Set<Phone> expectedPhones = Set.of(
            new Phone("+79781000615", "777", null, null),
            new Phone("+79781000625", null, null, null)
        );

        softly.assertThat(phoneConverter.convert(ytPhoneByLogisticsPointId))
            .hasSameElementsAs(expectedPhones)
            .hasSize(expectedPhones.size());
    }

    @Test
    @DisplayName("Конвертация с одним телефоном")
    void convertWithOnePhone() {
        YtPhoneByLogisticsPointId ytPhoneByLogisticsPointId = new YtPhoneByLogisticsPointId()
            .setId(1L)
            .setPhones(Optional.of(
                "{\"phones\":[{"
                    + "\"internal_number\":777,"
                    + "\"number\":\"+79781000615\""
                    + "}]}"
            ));

        Set<Phone> expectedPhones = Set.of(new Phone("+79781000615", "777", null, null));

        softly.assertThat(phoneConverter.convert(ytPhoneByLogisticsPointId))
            .isEqualTo(expectedPhones);
    }

    @Test
    @DisplayName("Конвертация с пустым списком телефонов")
    void convertWithoutPhones() {
        YtPhoneByLogisticsPointId ytPhoneByLogisticsPointId = new YtPhoneByLogisticsPointId()
            .setId(1L)
            .setPhones(Optional.of("{\"phones\":[]}"));

        softly.assertThat(phoneConverter.convert(ytPhoneByLogisticsPointId)).isEmpty();
    }

    @Test
    @DisplayName("Конвертация со списком == null")
    void convertNullPhones() {
        YtPhoneByLogisticsPointId ytPhoneByLogisticsPointId = new YtPhoneByLogisticsPointId()
            .setId(1L)
            .setPhones(Optional.empty());

        softly.assertThat(phoneConverter.convert(ytPhoneByLogisticsPointId)).isEmpty();
    }

    @Test
    @DisplayName("При изменении формата хранения json в yt конвертер падает")
    void failOnJsonFormatChanges() {
        YtPhoneByLogisticsPointId ytPhoneByLogisticsPointId = new YtPhoneByLogisticsPointId()
            .setId(1L)
            .setPhones(Optional.of(
                "{[{"
                    + "\"internal_number\":777,"
                    + "\"number\":\"+79781000615\""
                    + "}]}"
            ));

        softly.assertThatCode(
            () -> phoneConverter.convert(ytPhoneByLogisticsPointId)
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Unexpected character");
    }

    @Test
    @DisplayName("Конвертация с null")
    void convertNullValue() {
        softly.assertThat(phoneConverter.convert(null)).isEmpty();
    }

    @Test
    @DisplayName("Конвертация с не заполненной строкой телефонов")
    void convertWithBlankPhones() {
        softly.assertThat(phoneConverter.convert(new YtPhoneByLogisticsPointId().setPhones(Optional.of(" "))))
            .isEmpty();
    }
}
