package ru.yandex.market.deepmind.common.assertions.custom;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.assertj.core.api.FactoryBasedNavigableIterableAssert;

import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;

public class IterableGeneralServiceOffersAssert
    extends FactoryBasedNavigableIterableAssert<
        IterableGeneralServiceOffersAssert,
        Iterable<? extends ServiceOfferReplica>, ServiceOfferReplica, ServiceOfferAssertions>
    implements ServiceOffersAssertions<IterableGeneralServiceOffersAssert> {

    private IterableGeneralServiceOffersAssert(Iterable<? extends ServiceOfferReplica> offers) {
        super(offers, IterableGeneralServiceOffersAssert.class, ServiceOfferAssertions::assertThat);
    }

    public static IterableGeneralServiceOffersAssert assertThat(Iterable<? extends ServiceOfferReplica> actual) {
        return new IterableGeneralServiceOffersAssert(actual)
            .usingElementComparatorIgnoringFields("businessOfferId", "lastVersion");
    }

    @Override
    public IterableGeneralServiceOffersAssert containsExactlyIds(int... ids) {
        List<Long> actualIds = StreamSupport.stream(actual.spliterator(), false)
            .map(ServiceOfferReplica::getBusinessOfferId)
            .collect(Collectors.toList());
        Object[] expectedIds = Arrays.stream(ids).mapToLong(value -> (long) value).boxed().toArray();

        iterables.assertContainsExactly(info, actualIds, expectedIds);

        return myself;
    }

    @Override
    public IterableGeneralServiceOffersAssert containsExactlyInAnyOrderIds(int... ids) {
        List<Long> actualIds = StreamSupport.stream(actual.spliterator(), false)
            .map(ServiceOfferReplica::getBusinessOfferId)
            .collect(Collectors.toList());
        Object[] expectedIds = Arrays.stream(ids).mapToLong(value -> (long) value).boxed().toArray();

        iterables.assertContainsExactlyInAnyOrder(info, actualIds, expectedIds);

        return myself;
    }

    @Override
    public IterableGeneralServiceOffersAssert containsExactlyInAnyOrderShopSkuKeys(ServiceOfferKey... keys) {
        List<ServiceOfferKey> actualKeys = StreamSupport.stream(actual.spliterator(), false)
            .map(s -> new ServiceOfferKey(s.getSupplierId(), s.getShopSku()))
            .collect(Collectors.toList());

        iterables.assertContainsExactlyInAnyOrder(info, actualKeys, keys);

        return myself;
    }
}
