package ru.yandex.market.moderation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

public class PassModerationChecksExecutorTest extends FunctionalTest {
    @Autowired
    private PassDbsTestingChecksExecutor tested;

    /**
     * 1) (partner_id = 111)
     * У магазина пройдены все обязательные проверки -> Переводится в SUCCESS
     * 2) (partner_id = 222)
     * У магазина есть открытый CPA_PREMODERATION -> Не переводится в SUCCESS
     * 3) (partner_id = 333)
     * У магазина есть открытый SELF_CHECK -> Не переводится в SUCCESS
     * 4) (partner_id = 444)
     * У магазина есть открытые и CPA_PREMODERATION и SELF_CHECK -> Не переводится в SUCCESS
     * 5) (partner_id = 555)
     * Магазин уже размещается (фича MARKETPLACE_SELF_DELIVERY в SUCCESS) -> Ничего не происходит
     * 6) (partner_id = 666)
     * У магазина есть SELF_CHECK в состоянии PASSED -> Переводится в SUCCESS
     * 7) (partner_id = 777)
     * У магазина пройдены все проверки, и висит катоф BY_PARTNER -> Переводится в SUCCESS
     */
    @Test
    @DbUnitDataSet(before = "passModerationChecksExecutorTest.before.csv",
            after = "passModerationChecksExecutorTest.after.csv")
    void testSuccessfulJobExecution() {
        tested.doJob(null);

        verifySentNotificationType(partnerNotificationClient, 3, 1614169151L);
    }
}
