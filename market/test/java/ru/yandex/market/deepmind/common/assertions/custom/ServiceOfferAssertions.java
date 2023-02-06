package ru.yandex.market.deepmind.common.assertions.custom;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.internal.Iterables;
import org.assertj.core.internal.Objects;
import org.assertj.core.internal.TypeComparators;

import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;

import static org.assertj.core.internal.TypeComparators.defaultTypeComparators;

public class ServiceOfferAssertions extends AbstractObjectAssert<ServiceOfferAssertions, ServiceOfferReplica> {

    protected Iterables iterables = Iterables.instance();
    private final Objects objects = Objects.instance();
    private final TypeComparators comparatorByType = defaultTypeComparators();

    public ServiceOfferAssertions(ServiceOfferReplica actual) {
        super(actual, ServiceOfferAssertions.class);
    }

    public static ServiceOfferAssertions assertThat(ServiceOfferReplica actual) {
        return new ServiceOfferAssertions(actual);
    }

    @Override
    public ServiceOfferAssertions isEqualTo(Object expected) {
        super.usingDefaultComparator();

        return super.isEqualToIgnoringGivenFields(expected,
            "lastVersion");
    }

    public ServiceOfferAssertions isEqualToWithoutYtStamp(Object expected) {
        super.usingDefaultComparator();

        return super.isEqualToIgnoringGivenFields(expected,
            "lastVersion");
    }

    @Override
    public ServiceOfferAssertions isNotEqualTo(Object other) {
        super.usingDefaultComparator();
        return super.isNotEqualTo(other);
    }

    public ServiceOfferAssertions hasApprovedMapping(long mappingId) {
        super.isNotNull();
        objects.assertEqual(info, actual.getMskuId(), mappingId);
        return myself;
    }

    public ServiceOfferAssertions hasCategoryId(long categoryId) {
        super.isNotNull();
        objects.assertEqual(info, actual.getCategoryId(), categoryId);
        return myself;
    }

    public ServiceOfferAssertions hasBusinessId(int businessId) {
        super.isNotNull();
        objects.assertEqual(info, actual.getBusinessId(), businessId);
        return myself;
    }

    public ServiceOfferAssertions hasSupplierId(int supplierId) {
        super.isNotNull();
        objects.assertEqual(info, actual.getSupplierId(), supplierId);
        return myself;
    }

    public ServiceOfferAssertions hasTitle(String title) {
        super.isNotNull();
        objects.assertEqual(info, actual.getTitle(), title);
        return myself;
    }

}
