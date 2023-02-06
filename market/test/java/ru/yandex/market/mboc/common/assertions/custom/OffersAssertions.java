package ru.yandex.market.mboc.common.assertions.custom;

import org.assertj.core.api.ObjectEnumerableAssert;

import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;

/**
 * @author s-ermakov
 */
public interface OffersAssertions<SELF extends OffersAssertions<SELF>>
    extends ObjectEnumerableAssert<SELF, Offer> {

    SELF containsExactlyIds(int... ids);

    SELF containsExactlyInAnyOrderIds(int... ids);

    SELF containsExactlyInAnyOrderBusinessSkuKeys(BusinessSkuKey... keys);
}
