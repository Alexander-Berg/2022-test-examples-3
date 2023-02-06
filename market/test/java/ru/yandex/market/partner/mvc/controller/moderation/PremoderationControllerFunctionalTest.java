package ru.yandex.market.partner.mvc.controller.moderation;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static ru.yandex.market.partner.util.FunctionalTestHelper.get;

/**
 * Тест на логику работы {@link  PremoderationController}.
 */
@ExtendWith(MockitoExtension.class)
@DbUnitDataSet(before = "premoderation/premoderationControllerCutoffs.before.csv")
public class PremoderationControllerFunctionalTest extends FunctionalTest {

    /**
     * Проверка на закрытие только отключение ORDER_PENDING_EXPIRED
     */
    @Test
    @DbUnitDataSet(after = "premoderation/premoderationControllerCloseOrderPendingExpired.after.csv")
    void closeCutoffOrderPendingExpired() {
        closeCutoff(200);
    }

    /**
     * Проверяем, что снимаем только "ручные" отключения
     */
    @ParameterizedTest
    @CsvSource({"100", "300", "400"})
    @DbUnitDataSet(after = "premoderation/premoderationControllerCutoffs.before.csv")
    void closeOnlyValidCutoffs(long campaignId) {
        closeCutoff(campaignId);
    }

    private void closeCutoff(long campaignId) {
        get(String.format("%s/premoderation/close-cutoffs/manual?id=%d", baseUrl, campaignId));
    }
}
