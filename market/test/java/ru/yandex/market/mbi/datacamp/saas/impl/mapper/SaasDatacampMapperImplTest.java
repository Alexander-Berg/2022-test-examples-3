package ru.yandex.market.mbi.datacamp.saas.impl.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.datacamp.saas.impl.AbstractSaasDatacampTest;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.CompositeStatus;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.DataCampSearchAttribute;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.SaasDocType;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.ServiceLocalSearchAttribute;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferFilter;
import ru.yandex.market.saas.search.SaasSearchRequest;
import ru.yandex.market.saas.search.keys.SaasSearchAttribute;

/**
 * Date: 21.06.2021
 * Project: arcadia-market_mbi_datacamp-client
 *
 * @author alexminakov
 */
class SaasDatacampMapperImplTest extends AbstractSaasDatacampTest {

    private final SaasDatacampMapper saasDatacampMapper = new SaasDatacampMapperImpl();

    @DisplayName("Конвертация SaasOfferFilter -> SaasSearchRequest")
    @Test
    void map_allFields_correctResult() {
        SaasSearchRequest request = getSaasSearchAttributeListMap();
        Map<SaasSearchAttribute, List<String>> searchMap = request.getSearchMap();

        checkProperty(SaasDocType.OFFER.getName(), searchMap.get(DataCampSearchAttribute.SEARCH_DOC_TYPE));
        checkProperty("Ikea", searchMap.get(DataCampSearchAttribute.SEARCH_VENDOR));
        checkProperty("587098539", searchMap.get(DataCampSearchAttribute.SEARCH_CATEGORY_ID));
        checkProperty("339091", searchMap.get(DataCampSearchAttribute.SEARCH_MARKET_CATEGORY_ID));
        checkProperty("301428000", searchMap.get(DataCampSearchAttribute.SEARCH_VARIANT_ID));
        checkProperty("20112002", searchMap.get(DataCampSearchAttribute.SEARCH_GROUP_ID));

        checkProperties(
                List.of(10462382L, 10462383L, 10462384L),
                searchMap.get(DataCampSearchAttribute.SEARCH_SHOP_ID)
        );
        checkProperties(
                List.of(1L, 3L),
                searchMap.get(new ServiceLocalSearchAttribute(
                        DataCampSearchAttribute.SEARCH_RESULT_OFFER_STATUS,
                        10462383
                ))
        );
        checkProperties(
                List.of(1L, 5L),
                searchMap.get(DataCampSearchAttribute.SEARCH_RESULT_CONTENT_STATUS)
        );

        checkCompositeStatuses(T_PARTNER_STATUSES, searchMap);
        checkCompositeStatuses(T_SUPPLY_STATUSES, searchMap);
    }

    @Nonnull
    private SaasSearchRequest getSaasSearchAttributeListMap() {
        SaasOfferFilter filter = getDefaultOfferFilterBuilder()
                .build();

        SaasSearchRequest request = saasDatacampMapper.map(filter);

        Assertions.assertThat(request.getPrefix())
                .isEqualTo((int) T_BUSINESS_ID);
        Assertions.assertThat(request.getSearchMap())
                .hasSize(18);

        return request;
    }

    private void checkProperty(String expected, @Nonnull List<String> actual) {
        Assertions.assertThat(actual)
                .hasSize(1);
        Assertions.assertThat(actual.get(0))
                .isEqualTo(expected);
    }

    private void checkProperties(@Nonnull List<Long> expected, @Nonnull List<String> actual) {
        Assertions.assertThat(actual)
                .hasSameSizeAs(expected);
        List<Long> actualConverted = actual.stream()
                .map(Long::parseLong)
                .collect(Collectors.toUnmodifiableList());
        for (int i = 0; i < expected.size(); i++) {
            Assertions.assertThat(actualConverted.get(i))
                    .isEqualTo(expected.get(i));
        }
    }

    private void checkCompositeStatuses(@Nonnull List<? extends CompositeStatus> expected,
                                        Map<SaasSearchAttribute, List<String>> searchMap) {
        for (CompositeStatus compositeStatus : expected) {
            List<String> status = searchMap.get(compositeStatus);
            Assertions.assertThat(status)
                    .hasSize(1);
            Assertions.assertThat(Integer.parseInt(status.get(0)))
                    .isEqualTo(compositeStatus.getStatusValue());
        }
    }
}
