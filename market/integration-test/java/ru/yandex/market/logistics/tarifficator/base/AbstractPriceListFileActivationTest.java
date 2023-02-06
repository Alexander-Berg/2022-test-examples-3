package ru.yandex.market.logistics.tarifficator.base;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.jobs.producer.ActivatingPriceListProducer;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractPriceListFileActivationTest extends AbstractContextualTest {
    @Autowired
    private ActivatingPriceListProducer activatingPriceListProducer;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(activatingPriceListProducer);
    }

    @Test
    @DisplayName("Попытка запланировать активацию несуществующего файла прайс-листа")
    void activateManualNotFound() throws Exception {
        performPriceListFileActivation()
            .andExpect(status().isNotFound())
            .andExpect(TestUtils.jsonContent(
                "controller/price-list-files/response/file_not_found_response.json"
            ));
    }

    @Test
    @DisplayName("Попытка запланировать активацию прайс-листа с критическими ошибками")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-processed-with-critical-errors.xml")
    void activateManualError() throws Exception {
        performPriceListFileActivation()
            .andExpect(status().isBadRequest())
            .andExpect(TestUtils.jsonContent(
                "controller/price-list-files/response/activate-manual-invalid-status.json"
            ));
    }

    @Test
    @DisplayName("Попытка запланировать активацию уже активированных прайс-листов")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-processed-with-errors.xml")
    @DatabaseSetup(
        value = "/controller/price-list-files/db/before/price_lists_are_activated.xml",
        type = DatabaseOperation.REFRESH
    )
    void activateAlreadyActivatedPriceLists() throws Exception {
        performPriceListFileActivation()
            .andExpect(status().isBadRequest())
            .andExpect(TestUtils.jsonContent(
                "controller/price-list-files/response/activate-manual-already-activated.json"
            ));
    }

    @Test
    @DisplayName("Попытка запланировать активацию деактивированных прайс-листов")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-file-with-ended-price-lists.xml")
    @ExpectedDatabase(
        value = "/controller/price-list-files/db/after/activate-already-ended-price-list.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void activateAlreadyEndedPriceLists() throws Exception {
        performPriceListFileActivation().andExpect(status().isOk());
    }

    @Nonnull
    protected abstract ResultActions performPriceListFileActivation() throws Exception;

    @Nonnull
    protected abstract ResultMatcher successResult();
}
