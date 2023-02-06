package ru.yandex.market.logistics.management.service.dbqueue;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.exception.BadRequestException;
import ru.yandex.market.logistics.management.exception.EntityNotFoundException;
import ru.yandex.market.logistics.management.queue.model.UpdateDbsPartnerCargoTypesPayload;
import ru.yandex.market.logistics.management.queue.processor.UpdateDbsPartnerCargoTypesProcessingService;

@DatabaseSetup("/data/service/dbqueue/updateDbsPartnerCargoTypes/before/general.xml")
@DisplayName("Тесты dbqueue таски для проставления карготипов про лекарства DBS партнерам")
class UpdateDbsPartnerCargoTypesProcessingServiceTest extends AbstractContextualAspectValidationTest {

    @Autowired
    private UpdateDbsPartnerCargoTypesProcessingService processingService;

    @Test
    @DisplayName("Несуществующий партнер")
    void partnerNotFound() {
        softly
            .assertThatThrownBy(
                () -> processingService.processPayload(new UpdateDbsPartnerCargoTypesPayload("12345", 3L))
            )
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Партнер не DBS типа")
    void nonDbsPartner() {
        softly
            .assertThatThrownBy(
                () -> processingService.processPayload(new UpdateDbsPartnerCargoTypesPayload("12345", 2L))
            )
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Partner is not of DROPSHIP_BY_SELLER type, cargo type update not performed. Partner id=2");
    }

    @Test
    @DisplayName("Карготип 900 отсутствует в БД")
    @DatabaseSetup("/data/service/dbqueue/updateDbsPartnerCargoTypes/before/cargo_types_no_900.xml")
    void type900NotFound() {
        softly
            .assertThatThrownBy(
                () -> processingService.processPayload(new UpdateDbsPartnerCargoTypesPayload("12345", 1L))
            )
            .isInstanceOf(BadRequestException.class)
            .hasMessage("400 BAD_REQUEST \"Нет карготипа с номером 900\"");
    }

    @Test
    @DisplayName("Карготип 907 отсутствует в БД")
    @DatabaseSetup("/data/service/dbqueue/updateDbsPartnerCargoTypes/before/cargo_types_no_907.xml")
    void type907NotFound() {
        softly
            .assertThatThrownBy(
                () -> processingService.processPayload(new UpdateDbsPartnerCargoTypesPayload("12345", 1L))
            )
            .isInstanceOf(BadRequestException.class)
            .hasMessage("400 BAD_REQUEST \"Нет карготипа с номером 907\"");
    }

    @Test
    @DisplayName("CAN_SELL_MEDICINE == true, CAN_DELIVER_MEDICINE == true")
    @DatabaseSetup("/data/service/dbqueue/updateDbsPartnerCargoTypes/before/cargo_types_all.xml")
    @DatabaseSetup("/data/service/dbqueue/updateDbsPartnerCargoTypes/before/params_true.xml")
    @ExpectedDatabase(
        value = "/data/service/dbqueue/updateDbsPartnerCargoTypes/after/params_true.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void canSellTrue() {
        processingService.processPayload(new UpdateDbsPartnerCargoTypesPayload("12345", 1L));
    }

    @Test
    @DisplayName("CAN_SELL_MEDICINE == false, CAN_DELIVER_MEDICINE == false")
    @DatabaseSetup("/data/service/dbqueue/updateDbsPartnerCargoTypes/before/cargo_types_all.xml")
    @DatabaseSetup("/data/service/dbqueue/updateDbsPartnerCargoTypes/before/params_false.xml")
    @ExpectedDatabase(
        value = "/data/service/dbqueue/updateDbsPartnerCargoTypes/after/params_false.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void canSellFalse() {
        processingService.processPayload(new UpdateDbsPartnerCargoTypesPayload("12345", 1L));
    }

    @Test
    @DisplayName("Параметры CAN_SELL_MEDICINE и CAN_DELIVER_MEDICINE не прописаны в БД")
    @DatabaseSetup("/data/service/dbqueue/updateDbsPartnerCargoTypes/before/cargo_types_all.xml")
    @DatabaseSetup("/data/service/dbqueue/updateDbsPartnerCargoTypes/before/params_missing.xml")
    @ExpectedDatabase(
        value = "/data/service/dbqueue/updateDbsPartnerCargoTypes/after/params_false.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void canSellMissing() {
        processingService.processPayload(new UpdateDbsPartnerCargoTypesPayload("12345", 1L));
    }
}
