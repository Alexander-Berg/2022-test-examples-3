package ru.yandex.market.partner.feed;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * @author stani on 17.09.18.
 */

@DbUnitDataSet(before = "RevokeYMLClaimServantletTest.before.csv")
public class RevokeYMLClaimServantletTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(after = "requestModeration.after.csv")
    void testRequestModeration() {
        FunctionalTestHelper.get(baseUrl + "/revokeYMLClaim?id={campaignId}&format=json", 10101L);
    }

    @Test
    @DbUnitDataSet(after = "modertionNotNeeded.after.csv")
    void testNotRequestModeration() {
        FunctionalTestHelper.get(baseUrl + "/revokeYMLClaim?id={campaignId}&format=json", 10102L);
    }

    @Test
    @DbUnitDataSet(after = "testCloseCutoffs.after.csv")
    @DisplayName("Проверка закрытия катофов у CPC TECHNICAL_YML и DSBS FEED")
    void testCloseCutoffs() {
        FunctionalTestHelper.get(baseUrl + "/revokeYMLClaim?id={campaignId}&format=json", 10103L);
    }

    @Test
    @DbUnitDataSet(after = "canceledSelfCheck.after.csv")
    void testCanceledDBSSelfCheck() {
        FunctionalTestHelper.get(baseUrl + "/revokeYMLClaim?id={campaignId}&format=json", 10104L);
    }

    @Test
    @DbUnitDataSet(after = "canceledApiDebug.after.csv")
    void testCanceledApiDebug() {
        FunctionalTestHelper.get(baseUrl + "/revokeYMLClaim?id={campaignId}&format=json", 10105L);
    }

    @Test
    @DisplayName("Проверка, что не DBS на SELF_CHECK не 500-ит")
    void testCanceledNotDbs() {
        Assertions.assertDoesNotThrow(() -> FunctionalTestHelper.get(
                baseUrl + "/revokeYMLClaim?id={campaignId}&format=json",
                10106L
        ));
    }
}
