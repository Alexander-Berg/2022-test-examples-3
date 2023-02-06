package ru.yandex.market.deepmind.common.assertions.custom;

import org.assertj.core.api.ObjectEnumerableAssert;

import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;

public interface ServiceOffersAssertions<SELF extends ServiceOffersAssertions<SELF>>
    extends ObjectEnumerableAssert<SELF, ServiceOfferReplica> {

    SELF containsExactlyIds(int... ids);

    SELF containsExactlyInAnyOrderIds(int... ids);

    SELF containsExactlyInAnyOrderShopSkuKeys(ServiceOfferKey... keys);
}
