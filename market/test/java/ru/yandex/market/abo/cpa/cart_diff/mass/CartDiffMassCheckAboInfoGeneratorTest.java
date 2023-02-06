package ru.yandex.market.abo.cpa.cart_diff.mass;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.cpa.cart_diff.CartDiffType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CartDiffMassCheckAboInfoGeneratorTest {

    @Test
    void testCartDiffTypeContainsDisableMarkdownEscapingAttribute() {
        Offer offer = new Offer();
        offer.setLocalDelivery(Collections.emptyList());

        String generated = CartDiffMassCheckAboInfoGenerator.generate(false, Collections.singletonList(
                new MassDiffResult(offer, null, null, CartDiffType.ACCEPT_FAILED)
        ));

        assertEquals("<abo-info><cartDiffs><cartDiff>" +
                "<cartDiffType disable-markdown-escaping=\"true\">ACCEPT_FAILED</cartDiffType>" +
                "<offers>" +
                "<offer><sku/><title/><price/><currency/><deliveryPrice/><deliveryCurrency/><dayFrom/></offer>" +
                "</offers>" +
                "</cartDiff></cartDiffs></abo-info>\n", generated);
    }
}
