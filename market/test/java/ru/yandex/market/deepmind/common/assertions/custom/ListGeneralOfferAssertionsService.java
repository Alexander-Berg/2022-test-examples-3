package ru.yandex.market.deepmind.common.assertions.custom;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.FactoryBasedNavigableListAssert;

import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;

public class ListGeneralOfferAssertionsService
    extends FactoryBasedNavigableListAssert<
        ListGeneralOfferAssertionsService,
        List<? extends ServiceOfferReplica>,
        ServiceOfferReplica,
        ServiceOfferAssertions>
    implements ServiceOffersAssertions<ListGeneralOfferAssertionsService> {

    private ListGeneralOfferAssertionsService(List<? extends ServiceOfferReplica> actual) {
        super(actual, ListGeneralOfferAssertionsService.class, ServiceOfferAssertions::assertThat);
    }

    public static ListGeneralOfferAssertionsService assertThat(List<? extends ServiceOfferReplica> actual) {
        return new ListGeneralOfferAssertionsService(actual)
            .usingElementComparatorIgnoringFields("businessOfferId", "lastVersion");
    }

    @Override
    public ListGeneralOfferAssertionsService containsExactlyIds(int... ids) {
        List<Long> actualIds = actual.stream()
            .map(ServiceOfferReplica::getBusinessOfferId)
            .collect(Collectors.toList());

        Object[] expectedIds = Arrays.stream(ids).mapToLong(value -> (long) value).boxed().toArray();

        iterables.assertContainsExactly(info, actualIds, expectedIds);

        return myself;
    }

    @Override
    public ListGeneralOfferAssertionsService containsExactlyInAnyOrderIds(int... ids) {
        List<Long> actualIds = actual.stream()
            .map(ServiceOfferReplica::getBusinessOfferId)
            .collect(Collectors.toList());
        Object[] expectedIds = Arrays.stream(ids).mapToLong(value -> (long) value).boxed().toArray();

        iterables.assertContainsExactlyInAnyOrder(info, actualIds, expectedIds);

        return myself;
    }

    @Override
    public ListGeneralOfferAssertionsService containsExactlyInAnyOrderShopSkuKeys(ServiceOfferKey... keys) {
        List<ServiceOfferKey> actualKeys = actual.stream()
            .map(s -> new ServiceOfferKey(s.getSupplierId(), s.getShopSku()))
            .collect(Collectors.toList());

        iterables.assertContainsExactlyInAnyOrder(info, actualKeys, keys);

        return myself;
    }
}
