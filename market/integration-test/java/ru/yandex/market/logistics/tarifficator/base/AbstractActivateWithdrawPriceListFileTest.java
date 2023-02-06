package ru.yandex.market.logistics.tarifficator.base;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;
import ru.yandex.market.logistics.tarifficator.util.ValidationUtil;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractActivateWithdrawPriceListFileTest extends AbstractContextualTest {
    private static final Instant TIME_11_AM = Instant.parse("2020-02-02T11:00:00.00Z");

    @BeforeEach
    void setup() {
        clock.setFixed(TIME_11_AM, ZoneOffset.UTC);
    }

    @AfterEach
    void after() {
        clock.clearFixed();
    }

    @Test
    @DisplayName("Успешно активировать файл заборного прайс-листа")
    @DatabaseSetup("/controller/withdraw/price-list-files/db/before/withdraw-price-list-file-activation.xml")
    @ExpectedDatabase(
        value = "/controller/withdraw/price-list-files/db/after/withdraw-price-list-file-activation-success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateFileSuccess() throws Exception {
        activateFile(1L)
            .andExpect(status().isOk())
            .andExpect(TestUtils.noContent());
    }

    @Test
    @DisplayName("Успешно активировать новый файл заборного прайс-листа и деактивировать старый")
    @DatabaseSetup("/controller/withdraw/price-list-files/db/before/withdraw-price-list-file-activate-new.xml")
    @ExpectedDatabase(
        value = "/controller/withdraw/price-list-files/db/after/withdraw-price-list-file-deactivate-old.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateFileSuccessDeactivateOld() throws Exception {
        activateFile(2L)
            .andExpect(status().isOk())
            .andExpect(TestUtils.noContent());
    }

    @Test
    @DisplayName("Активировать несуществующий файл заборного прайс-листа")
    void activateFileNotExist() throws Exception {
        activateFile(1L)
            .andExpect(status().isNotFound())
            .andExpect(ValidationUtil.errorMessage("Failed to find [WITHDRAW_PRICE_LIST_FILE] with ids [[1]]"));
    }

    @Test
    @DisplayName("Активировать файл с критической ошибкой")
    @DatabaseSetup("/controller/withdraw/price-list-files/db/before/withdraw-price-list-file-activation.xml")
    @DatabaseSetup(
        value = "/controller/withdraw/price-list-files/db/before/withdraw-price-list-file-status-error.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/withdraw/price-list-files/db/after/price-list-is-not-activated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateFileInWrongStatus() throws Exception {
        activateFile(1L)
            .andExpect(status().isBadRequest())
            .andExpect(ValidationUtil.errorMessage(
                "Wrong file processing status ERROR. Only [SUCCESS, PARTIAL_SUCCESS] allowed for activation"
            ));
    }

    @Test
    @DisplayName("Активировать уже активированный файл")
    @DatabaseSetup("/controller/withdraw/price-list-files/db/before/withdraw-price-list-file-activation.xml")
    @DatabaseSetup(
        value = "/controller/withdraw/price-list-files/db/before/already-activated.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/withdraw/price-list-files/db/before/already-activated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateFileAlreadyActivated() throws Exception {
        activateFile(1L)
            .andExpect(status().isBadRequest())
            .andExpect(ValidationUtil.errorMessage("Withdraw price list 1 has been already activated"));
    }

    @Test
    @DisplayName("Активировать уже деактивированный файл")
    @DatabaseSetup("/controller/withdraw/price-list-files/db/before/withdraw-price-list-file-activation.xml")
    @DatabaseSetup(
        value = "/controller/withdraw/price-list-files/db/before/already-deactivated.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/withdraw/price-list-files/db/before/already-deactivated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateFileAlreadyDeactivated() throws Exception {
        activateFile(1L)
            .andExpect(status().isBadRequest())
            .andExpect(ValidationUtil.errorMessage("Withdraw price list 1 has been already activated"));
    }

    protected abstract ResultActions activateFile(long priceListFileId) throws Exception;
}
