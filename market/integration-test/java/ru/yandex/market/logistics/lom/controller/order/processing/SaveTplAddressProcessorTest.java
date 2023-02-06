package ru.yandex.market.logistics.lom.controller.order.processing;

import java.math.BigDecimal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.Address;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.model.TplAddressChangedPayload;
import ru.yandex.market.logistics.lom.jobs.processor.SaveTplAddressProcessor;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@ParametersAreNonnullByDefault
@DatabaseSetup("/service/les/save_tpl_address/before/order.xml")
@DisplayName("Тесты обработчика задач SAVE_TPL_ADDRESS")
class SaveTplAddressProcessorTest extends AbstractContextualTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    SaveTplAddressProcessor saveTplAddressProcessor;

    @DisplayName("Проверка десериализации payload'a задачи")
    @SneakyThrows
    @Test
    void deserializePayload() {
        TplAddressChangedPayload payload = objectMapper.readValue(
            extractFileContent("service/les/save_tpl_address/before/payload.json"),
            TplAddressChangedPayload.class
        );
        softly.assertThat(payload)
            .usingRecursiveComparison()
            .ignoringFields("sequenceId")
            .isEqualTo(createTaskPayload(createAddress().build(), "Новый комментарий"));
    }

    @DisplayName("Сохранение адреса из TPL")
    @Test
    @ExpectedDatabase(
        value = "/service/les/save_tpl_address/after/address_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newAddressSaved() {
        ProcessingResult processingResult = saveTplAddressProcessor.processPayload(
            createTaskPayload(createAddress().build(), null)
        );
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
    }

    @DisplayName("Невалидный адрес из TPL")
    @Test
    @ExpectedDatabase(
        value = "/service/les/save_tpl_address/before/order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newAddressInvalid() {
        softly.assertThatThrownBy(
            () -> saveTplAddressProcessor.processPayload(createTaskPayload(createAddress().country("").build(), null))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(
                "[" +
                    "FieldError(propertyPath=recipient.address.country, message=country must be not empty), " +
                    "FieldError(propertyPath=recipient.personalAddressId, " +
                        "message=order recipient personal field address must be not null)" +
                "]"
            );
    }

    @DisplayName("Обновление комментария")
    @Test
    @ExpectedDatabase(
        value = "/service/les/save_tpl_address/after/comment_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newCommentSaved() {
        ProcessingResult processingResult = saveTplAddressProcessor.processPayload(createTaskPayload(
            null,
            "Новый комментарий"
        ));
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
    }

    @DisplayName("Адрес и комментарий отсутствуют в payload'e таски, удаление комментария")
    @Test
    @ExpectedDatabase(
        value = "/service/les/save_tpl_address/after/comment_deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void commentDeleted() {
        ProcessingResult processingResult = saveTplAddressProcessor.processPayload(createTaskPayload(null, null));
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
    }

    @Test
    @DisplayName("Ничего не обновляется, если в пэйлоуде отсутствует дом")
    @ExpectedDatabase(
        value = "/service/les/save_tpl_address/before/order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void nothingHappensIfHouseIsEmptyInAddressPayload() {
        softly.assertThat(saveTplAddressProcessor.processPayload(createTaskPayload(
                    createAddress().house("").build(),
                    null
                )).getStatus()
            )
            .isEqualTo(ProcessingResultStatus.UNPROCESSED);
    }

    @Nonnull
    private TplAddressChangedPayload createTaskPayload(@Nullable Address address, @Nullable String comment) {
        return new TplAddressChangedPayload(
            "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1",
            1,
            "1",
            address,
            comment
        );
    }

    @Nonnull
    private Address.AddressBuilder createAddress() {
        return Address.builder()
            .country("Россия")
            .locality("Москва")
            .region("Москва и Московская область")
            .street("Новая улица")
            .house("Новый дом")
            .housing("Новый корпус")
            .room("Новая квартира")
            .zipCode("12345")
            .porch("Новый подъезд")
            .floor(1)
            .latitude(BigDecimal.valueOf(55.018803))
            .longitude(BigDecimal.valueOf(82.933952))
            .geoId(12345)
            .intercom("Новый домофон");
    }
}
