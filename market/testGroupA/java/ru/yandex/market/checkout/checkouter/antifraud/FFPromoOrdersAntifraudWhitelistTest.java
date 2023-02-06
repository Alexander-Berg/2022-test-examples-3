package ru.yandex.market.checkout.checkouter.antifraud;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType;
import ru.yandex.market.checkout.checkouter.order.Buyer;

import static ru.yandex.market.checkout.checkouter.antifraud.FFPromoOrdersAntifraudTest.EXPECTED_ORDER_LIMIT;
import static ru.yandex.market.checkout.checkouter.antifraud.FFPromoOrdersAntifraudTest.FF_PROMO_FRAUD_MATCHER;

@Disabled
public class FFPromoOrdersAntifraudWhitelistTest extends AbstractFFPromoAntifraudTestBase {

    @Test
    public void testUidWhitelist() {
        final Buyer buyer1 = newRandomBuyer();
        final Buyer buyer2 = newRandomBuyer();
        final Buyer buyer3 = newRandomBuyer();
        final Buyer buyer4 = newRandomBuyer();
        final Buyer buyer5 = newRandomBuyer();
        final String cardNumber = "51234567****4321";

        // Include buyer 1-3 in whitelist with different delimiters
        setUidWhitelist(Set.of(buyer1.getUid(), buyer2.getUid(), buyer3.getUid()));

        // Check that no limits are applied for buyers 1-3 any more
        createAndPayOrdersWithBuyer(buyer1, cardNumber, EXPECTED_ORDER_LIMIT + 1);
        createAndPayOrdersWithBuyer(buyer2, cardNumber, EXPECTED_ORDER_LIMIT + 1);
        createAndPayOrdersWithBuyer(buyer3, cardNumber, EXPECTED_ORDER_LIMIT + 1);

        // Check that antifraud is still intact for buyers outside the whitelist
        tryFraudWithBuyer(() -> buyer4);

        // Check that bank card antifraud is still intact for buyers outside the whitelist
        createOrder(buyer5);
        payForOrder(cardNumber, true, true);

        // Clear the whitelist and check that antifraud has restored
        setUidWhitelist(Collections.emptySet());
        createOrder(buyer1, FF_PROMO_FRAUD_MATCHER);
    }

    private void setUidWhitelist(Set<Long> whitelist) {
        checkouterFeatureWriter.writeValue(CollectionFeatureType.ANTIFRAUD_UID_WHITE_LIST, whitelist);
    }
}
