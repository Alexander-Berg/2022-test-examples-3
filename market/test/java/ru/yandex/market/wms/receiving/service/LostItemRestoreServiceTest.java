package ru.yandex.market.wms.receiving.service;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.model.dto.external.LostItemInfoDto;
import ru.yandex.market.wms.receiving.service.restore.LostItemRestoreService;


@DatabaseSetup(value = "/service/lost-item-restore/db.xml", connection = "wmwhseConnection")
@DatabaseSetup(value = "/service/lost-item-restore/archive.xml", connection = "archiveWmwhseConnection")
public class LostItemRestoreServiceTest extends ReceivingIntegrationTest {

    @Autowired
    private LostItemRestoreService restoreService;

    @Test
    public void getWrittenOffLostItemInfoTest() {
        LostItemInfoDto info = restoreService.findLostItemInformation("SERIAL_2");
        assertions.assertThat(info.getSku()).isEqualTo("SKU_2");
        assertions.assertThat(info.getStorerKey()).isEqualTo("STORER_2");
    }

    @Test
    @DatabaseSetup(value = "/service/lost-item-restore/lot-before.xml",
            connection = "wmwhseConnection", type = DatabaseOperation.INSERT)
    public void getWrittenOffLostItemInfoTestWithoutArchiveLot() {
        LostItemInfoDto info = restoreService.findLostItemInformation("SERIAL_40");
        assertions.assertThat(info.getSku()).isEqualTo("SKU_40");
        assertions.assertThat(info.getStorerKey()).isEqualTo("STORER_40");
    }

    @Test
    public void getArchivedLostItemInfoTest() {
        LostItemInfoDto info = restoreService.findLostItemInformation("SERIAL_3");
        assertions.assertThat(info.getSku()).isEqualTo("SKU_3");
        assertions.assertThat(info.getStorerKey()).isEqualTo("STORER_3");
    }

    @Test
    public void getActiveLostItemInfoTest() {
        assertions.assertThatThrownBy(() -> restoreService.findLostItemInformation("SERIAL_1"))
                .hasMessage("Товар числится в потерянных. Используйте стандартную процедуру возврата на баланс");
    }

    @Test
    public void getActiveItemInfoTest() {
        assertions.assertThatThrownBy(() -> restoreService.findLostItemInformation("SERIAL_4"))
                .hasMessage("Товар SERIAL_4 уже находится на балансе");
    }

    @Test
    public void getNotFoundItemInfoTest() {
        assertions.assertThatThrownBy(() -> restoreService.findLostItemInformation("SERIAL_10"))
                .hasMessage("Информация о товаре SERIAL_10 не найдена");
    }


    @Test
    @ExpectedDatabase(value = "/service/lost-item-restore/after-writtenoff-restore-db.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void restoreWrittenOffTest() {
        restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART01", "SERIAL_2", false);
    }

    @Test
    @DatabaseSetup(value = "/service/lost-item-restore/lot-before.xml",
            connection = "wmwhseConnection", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/service/lost-item-restore/after-writtenoff-restore-many-db.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void restoreSeveralWrittenOffTest() {
        restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART01", "SERIAL_2", false);
        restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART10", "SERIAL_20", false);
        restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART11", "SERIAL_21", false);
        restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART02", "SERIAL_22", false);
        restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART02", "SERIAL_23", false);
        restoreService.restoreFoundItem("RECEIPT_1", "DAMAGE01", "CART03", "SERIAL_24", true);
        restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART02", "SERIAL_40", false);
    }

    @Test
    @DatabaseSetup(value    = "/service/lost-item-restore/many-damaged-expired/before.xml",
            connection = "wmwhseConnection", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value    = "/service/lost-item-restore/many-damaged-expired/archive.xml",
            connection = "archiveWmwhseConnection", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/service/lost-item-restore/many-damaged-expired/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void restoreSeveralDamageAndExpiredWrittenOffTest() {
        restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART01", "SERIAL_2", false);
        restoreService.restoreFoundItem("RECEIPT_1", "DAMAGE01", "CART03", "SERIAL_20", true);
        restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART01", "SERIAL_21", false);
        restoreService.restoreFoundItem("RECEIPT_1", "DAMAGE01", "CART03", "SERIAL_22", true);
        restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART02", "SERIAL_23", false);
        restoreService.restoreFoundItem("RECEIPT_1", "DAMAGE01", "CART03", "SERIAL_24", true);
        //Expired
        restoreService.restoreFoundItem("RECEIPT_1", "DAMAGE01", "CART03", "SERIAL_30", true);
     }

    @Test
    public void restoreSeveralWithWrongCart() {
        restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART01", "SERIAL_2", false);
        restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART01", "SERIAL_20", false);
        assertions.assertThatThrownBy(
                () -> restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART01", "SERIAL_21", true))
                .hasMessage("Тара CART01 не предназначена для брака");

    }

    @Test
    public void restoreWithWrongCart() {
        assertions.assertThatThrownBy(
                () -> restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART_X", "SERIAL_2", false))
                .hasMessage("Тара CART_X не привязана к столу и в ней есть товары");
    }

    @Test
    public void restoreWithWrongCartWithEmptyLocXIdRecord() {
        assertions.assertThatThrownBy(
                () -> restoreService.restoreFoundItem("RECEIPT_1", "STAGE01", "CART_Y", "SERIAL_2", false))
                .hasMessage("Тара CART_Y не привязана к столу и в ней есть товары");
    }

    @Test
    public void restoreWrongReceiptType() {
        assertions.assertThatThrownBy(
                () -> restoreService.restoreFoundItem("RECEIPT_2", "STAGE01", "CART01", "SERIAL_2", false))
                .hasMessage("У поставки RECEIPT_2 неверный тип UNKNOWN");
    }

    @Test
    public void restoreWrongReceiptStatus() {
        assertions.assertThatThrownBy(
                () -> restoreService.restoreFoundItem("RECEIPT_3", "STAGE01", "CART01", "SERIAL_2", false))
                .hasMessage("У поставки RECEIPT_3 неверный статус CLOSED");
    }



}
