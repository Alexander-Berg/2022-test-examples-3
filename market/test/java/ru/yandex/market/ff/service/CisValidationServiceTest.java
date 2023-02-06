package ru.yandex.market.ff.service;

import java.util.HashMap;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.CisHandleMode;
import ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.enrichment.util.ValidationErrorsUtils;
import ru.yandex.market.ff.model.bo.EnrichmentResultContainer;
import ru.yandex.market.ff.model.bo.SupplierContentMapping;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.RequestItemCargoTypesRepository;
import ru.yandex.market.ff.repository.RequestItemMarketBarcodeRepository;
import ru.yandex.market.ff.repository.RequestItemMarketVendorCodeRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link CisValidationServiceTest}.
 */
public class CisValidationServiceTest extends IntegrationTest {

    private static final ShopRequest REQUEST = new ShopRequest();

    private static final String ARTICLE_1 = "art1";
    private static final String ARTICLE_2 = "art2";
    private static final String ARTICLE_3 = "art3";
    private static final String ARTICLE_4 = "art4";
    private static final String ARTICLE_5 = "art5";

    private static final String CIS_1 = "000000011111";
    private static final String CIS_2 = "000000022222";

    private static final Integer CARGO_TYPE_CIS_REQUIRED = 980;
    private static final Integer CARGO_TYPE_CIS_DISTINCT = 985;
    private static final Integer CARGO_TYPE_DIRTY = 955;

    private SupplierMappingService supplierMappingService = Mockito.mock(SupplierMappingService.class);

    @Autowired
    RequestItemMarketVendorCodeRepository requestItemMarketVendorCodeRepository;

    @Autowired
    RequestItemMarketBarcodeRepository requestItemMarketBarcodeRepository;

    @Autowired
    RequestItemCargoTypesRepository requestItemCargoTypesRepository;

    @Autowired
    ShopRequestRepository shopRequestRepository;

    @Autowired
    RequestItemService requestItemService;

    @Autowired
    RequestValidationService requestValidationService;

    ImmutableMap<SupplierSkuKey, SupplierContentMapping> mapping;

    @Autowired
    CisValidationService cisValidationService;

    @BeforeEach
    void init() {
        mapping = ImmutableMap.of(
                new SupplierSkuKey(1, ARTICLE_1), SupplierContentMapping.builder(ARTICLE_1, 1L, "title")
                        .setCargoTypes(ImmutableSet.of(CARGO_TYPE_CIS_REQUIRED))
                        .setCisHandleMode(CisHandleMode.ACCEPT_ONLY_DECLARED)
                        .build(),
                new SupplierSkuKey(1, ARTICLE_2), SupplierContentMapping.builder(ARTICLE_2, 1L, "title")
                        .setCargoTypes(ImmutableSet.of(CARGO_TYPE_CIS_DISTINCT))
                        .setCisHandleMode(CisHandleMode.ACCEPT_ONLY_DECLARED)
                        .build(),
                new SupplierSkuKey(2, ARTICLE_3), SupplierContentMapping.builder(ARTICLE_3, 1L, "title")
                        .setCargoTypes(ImmutableSet.of(CARGO_TYPE_CIS_REQUIRED))
                        .setCisHandleMode(CisHandleMode.NO_RESTRICTION)
                        .build(),
                new SupplierSkuKey(1, ARTICLE_4), SupplierContentMapping.builder(ARTICLE_4, 1L, "title")
                        .setCargoTypes(ImmutableSet.of(CARGO_TYPE_DIRTY))
                        .setCisHandleMode(CisHandleMode.ACCEPT_ONLY_DECLARED)
                        .build(),
                new SupplierSkuKey(2, ARTICLE_5), SupplierContentMapping.builder(ARTICLE_5, 1L, "title")
                        .setCargoTypes(ImmutableSet.of(CARGO_TYPE_CIS_DISTINCT))
                        .setCisHandleMode(CisHandleMode.ACCEPT_ONLY_DECLARED)
                        .build());

        when(supplierMappingService.getMarketSkuMapping(any(ShopRequest.class), anyList()))
                .thenReturn(mapping);
    }

    @Test
    @DatabaseSetup("classpath:service/cis-validation-service/before.xml")
    void failWhenNonUniqueCisFound() {
        Map<Long, EnrichmentResultContainer> actual = new HashMap<>();
        Map<Long, EnrichmentResultContainer> expected = new HashMap<>();

        ShopRequest request = shopRequestRepository.findById(2L);
        cisValidationService.validate(request, mapping, actual);

        ValidationErrorsUtils.addValidationError(2L, RequestItemErrorType.NON_UNIQUE_CIS, expected,
                Map.of(RequestItemErrorAttributeType.NON_UNIQUE_CIS, CIS_1));
        ValidationErrorsUtils.addValidationError(3L, RequestItemErrorType.NON_UNIQUE_CIS, expected,
                Map.of(RequestItemErrorAttributeType.NON_UNIQUE_CIS, CIS_1));

        assertThat(actual.size(), is(expected.size()));
        assertThat(actual.keySet(), is(expected.keySet()));
        assertThat(actual.get(2L).getValidationErrors(), is(expected.get(2L).getValidationErrors()));
        assertThat(actual.get(3L).getValidationErrors(), is(expected.get(3L).getValidationErrors()));
    }

    @Test
    @DatabaseSetup("classpath:service/cis-validation-service/before.xml")
    void failWhenNonUniqueCisesFound() {
        Map<Long, EnrichmentResultContainer> actual = new HashMap<>();
        Map<Long, EnrichmentResultContainer> expected = new HashMap<>();

        ShopRequest request = shopRequestRepository.findById(3L);
        cisValidationService.validate(request, mapping, actual);

        ValidationErrorsUtils.addValidationError(4L, RequestItemErrorType.NON_UNIQUE_CIS, expected,
                Map.of(RequestItemErrorAttributeType.NON_UNIQUE_CIS, CIS_1 + ", " + CIS_2));
        ValidationErrorsUtils.addValidationError(5L, RequestItemErrorType.NON_UNIQUE_CIS, expected,
                Map.of(RequestItemErrorAttributeType.NON_UNIQUE_CIS, CIS_1 + ", " + CIS_2));

        assertThat(actual.size(), is(expected.size()));
        assertThat(actual.keySet(), is(expected.keySet()));
        assertThat(actual.get(4L).getValidationErrors(), is(expected.get(4L).getValidationErrors()));
        assertThat(actual.get(5L).getValidationErrors(), is(expected.get(5L).getValidationErrors()));
    }

    @Test
    @DatabaseSetup("classpath:service/cis-validation-service/before.xml")
    void failOnWrongNumberOfUniqueCisFound() {
        Map<Long, EnrichmentResultContainer> actual = new HashMap<>();
        Map<Long, EnrichmentResultContainer> expected = new HashMap<>();

        ShopRequest request = shopRequestRepository.findById(1L);
        cisValidationService.validate(request, mapping, actual);

        ValidationErrorsUtils
                .addValidationError(1L, RequestItemErrorType.INVALID_COUNT_FOR_DISTINCT_CIS_IDENTIFIER, expected);
        assertThat(actual.size(), is(expected.size()));
        assertThat(actual.keySet(), is(expected.keySet()));
        assertThat(actual.get(1L).getValidationErrors(), is(expected.get(1L).getValidationErrors()));
    }

    @Test
    @DatabaseSetup("classpath:service/cis-validation-service/before.xml")
    void failWhenNonUniqueCisesAndWrongNumberOfUniqueCisFound() {
        Map<Long, EnrichmentResultContainer> actual = new HashMap<>();
        Map<Long, EnrichmentResultContainer> expected = new HashMap<>();

        ShopRequest request = shopRequestRepository.findById(4L);
        cisValidationService.validate(request, mapping, actual);

        ValidationErrorsUtils.addValidationError(6L, RequestItemErrorType.NON_UNIQUE_CIS, expected,
                Map.of(RequestItemErrorAttributeType.NON_UNIQUE_CIS, CIS_1 + ", " + CIS_2));
        ValidationErrorsUtils
                .addValidationError(6L, RequestItemErrorType.INVALID_COUNT_FOR_DISTINCT_CIS_IDENTIFIER, expected);
        ValidationErrorsUtils.addValidationError(7L, RequestItemErrorType.NON_UNIQUE_CIS, expected,
                Map.of(RequestItemErrorAttributeType.NON_UNIQUE_CIS, CIS_1 + ", " + CIS_2));

        assertThat(actual.size(), is(expected.size()));
        assertThat(actual.keySet(), is(expected.keySet()));
        assertThat(actual.get(6L).getValidationErrors(), is(expected.get(6L).getValidationErrors()));
        assertThat(actual.get(7L).getValidationErrors(), is(expected.get(7L).getValidationErrors()));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/cis-validation-service/before.xml"),
            @DatabaseSetup("classpath:service/cis-validation-service/filter-suppliers.xml")
    })
    void failOnSupplierValidation() {
        Map<Long, EnrichmentResultContainer> actual = new HashMap<>();
        Map<Long, EnrichmentResultContainer> expected = new HashMap<>();

        ShopRequest request = shopRequestRepository.findById(7L);
        cisValidationService.validate(request, mapping, actual);

        ValidationErrorsUtils
                .addValidationError(10L, RequestItemErrorType.REQUESTS_WITH_CIS_ITEMS_IS_NOT_ALLOWED, expected);
        ValidationErrorsUtils
                .addValidationError(11L, RequestItemErrorType.REQUESTS_WITH_CIS_ITEMS_IS_NOT_ALLOWED, expected);
        ValidationErrorsUtils
                .addValidationError(11L, RequestItemErrorType.INVALID_COUNT_FOR_DISTINCT_CIS_IDENTIFIER, expected);

        assertThat(actual.size(), is(expected.size()));
        assertThat(actual.keySet(), is(expected.keySet()));
        assertThat(actual.get(10L).getValidationErrors(), is(expected.get(10L).getValidationErrors()));
        assertThat(actual.get(11L).getValidationErrors(), is(expected.get(11L).getValidationErrors()));
    }

    @Test
    @DatabaseSetup("classpath:service/cis-validation-service/before.xml")
    void shouldNotReturnValidationErrorOnWrongCisHandleMode() {
        ShopRequest request = shopRequestRepository.findById(5L);

        Map<Long, EnrichmentResultContainer> errors = new HashMap<>();
        cisValidationService.validate(request, mapping, errors);

        assertThat(errors.size(), is(0));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/cis-validation-service/before.xml"),
            @DatabaseSetup("classpath:service/cis-validation-service/filter-suppliers.xml")
    })
    void successWithSupplierFiler() {
        ShopRequest request = shopRequestRepository.findById(5L);

        Map<Long, EnrichmentResultContainer> errors = new HashMap<>();
        cisValidationService.validate(request, mapping, errors);

        assertThat(errors.size(), is(0));
    }

    @Test
    @DatabaseSetup("classpath:service/cis-validation-service/before.xml")
    void shouldNotReturnValidationErrorOnWrongCargoType() {
        ShopRequest request = shopRequestRepository.findById(6L);

        Map<Long, EnrichmentResultContainer> errors = new HashMap<>();
        cisValidationService.validate(request, mapping, errors);

        assertThat(errors.size(), is(0));
    }

    @Test
    @DatabaseSetup("classpath:service/cis-validation-service/cis-validations-enabled.xml")
    void successShouldMakeCisValidations() {
        REQUEST.setNeedConfirmation(false);

        assertThat(cisValidationService.shouldMakeCisValidations(REQUEST), is(true));
    }

    @Test
    @DatabaseSetup("classpath:service/cis-validation-service/cis-validations-enabled.xml")
    void failShouldMakeCisValidationsOnWrongNeedConfirmationStatus() {
        REQUEST.setNeedConfirmation(true);

        assertThat(cisValidationService.shouldMakeCisValidations(REQUEST), is(false));
    }

    @Test
    void failShouldMakeCisValidationsOnWrongCisValidationEnabledStatus() {
        REQUEST.setNeedConfirmation(false);

        assertThat(cisValidationService.shouldMakeCisValidations(REQUEST), is(false));
    }
}
