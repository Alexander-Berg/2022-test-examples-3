package ru.yandex.market.ff.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.logistics.iris.client.model.response.ProbabilityInfoResponse;
import ru.yandex.market.logistics.iris.client.model.response.ProbabilityItemInfo;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;

public class MeasurementEnricherServiceTest extends IntegrationTest {

    private static final long SERVICE_ID = 100L;
    private static final SupplierSkuKey FIRST_SKU = new SupplierSkuKey(3, "sku1");
    private static final SupplierSkuKey SECOND_SKU = new SupplierSkuKey(3, "sku2");
    private static final SupplierSkuKey THIRD_SKU = new SupplierSkuKey(2, "1");

    @Autowired
    private MeasurementEnricherService enricherService;

    @Autowired
    private CalendaringService service;

    @Test
    @DatabaseSetup("classpath:service/measurement_enrich/before-enrich-supply.xml")
    @ExpectedDatabase(
            value = "classpath:service/measurement_enrich/after-success-enrich-supply.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessEnrichOneItemFromShopRequest() {
        Mockito.when(measurementApiClient.getProbabilityInfo(any()))
                .thenReturn(new ProbabilityInfoResponse(ImmutableList.of(
                        new ProbabilityItemInfo(String.valueOf(FIRST_SKU.getSupplierId()),
                                                FIRST_SKU.getSku(),
                                                BigDecimal.ZERO
                        ),
                        new ProbabilityItemInfo(String.valueOf(SECOND_SKU.getSupplierId()),
                                                SECOND_SKU.getSku(),
                                                BigDecimal.valueOf(100)
                        )
                )));

        final LocalDateTime requestedDate = LocalDate.of(2019, 11, 11).atStartOfDay();
        final ShopRequest request = getShopRequest(RequestType.SUPPLY, requestedDate);

        enricherService.enrich(request, request.getItems());
    }

    @Test
    @DatabaseSetup("classpath:service/measurement_enrich/before-enrich-crossdoc.xml")
    @ExpectedDatabase(
            value = "classpath:service/measurement_enrich/before-enrich-crossdoc.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotEnrichCrossDocShopRequest() {
        Mockito.when(measurementApiClient.getProbabilityInfo(any()))
                .thenReturn(new ProbabilityInfoResponse(ImmutableList.of(
                        new ProbabilityItemInfo(String.valueOf(FIRST_SKU.getSupplierId()),
                                                FIRST_SKU.getSku(),
                                                BigDecimal.ZERO
                        ),
                        new ProbabilityItemInfo(String.valueOf(SECOND_SKU.getSupplierId()),
                                                SECOND_SKU.getSku(),
                                                BigDecimal.valueOf(100)
                        )
                )));

        final LocalDateTime requestedDate = LocalDate.of(2019, 11, 10).atStartOfDay();
        final ShopRequest request = getShopRequest(RequestType.CROSSDOCK, requestedDate);

        enricherService.enrich(request, request.getItems());
    }

    @Test
    @DatabaseSetup("classpath:service/measurement_enrich/before-enrich-crossdoc.xml")
    @ExpectedDatabase(
            value = "classpath:service/measurement_enrich/after-success-enrich-two-items-crossdoc.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldEnrichTwoCrossDocItemsFromShopRequest() {
        Mockito.when(measurementApiClient.getProbabilityInfo(any()))
                .thenReturn(new ProbabilityInfoResponse(ImmutableList.of(
                        new ProbabilityItemInfo(String.valueOf(FIRST_SKU.getSupplierId()),
                                                FIRST_SKU.getSku(),
                                                BigDecimal.valueOf(52)
                        ),
                        new ProbabilityItemInfo(String.valueOf(SECOND_SKU.getSupplierId()),
                                                SECOND_SKU.getSku(),
                                                BigDecimal.valueOf(100)
                        )
                )));

        final LocalDateTime requestedDate = LocalDate.of(2019, 11, 11).atStartOfDay();
        final ShopRequest request = getShopRequest(RequestType.CROSSDOCK, requestedDate);

        enricherService.enrich(request, request.getItems());
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/measurement_enrich/before-enrich-crossdoc.xml"),
            @DatabaseSetup("classpath:service/measurement_enrich/before-disabled-enrich-shop-request.xml")})
    @ExpectedDatabase(
            value = "classpath:service/measurement_enrich/before-enrich-crossdoc.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(
            value = "classpath:service/measurement_enrich/before-disabled-enrich-shop-request.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotEnrichAnyOneIfMeasurementDisabled() {
        Mockito.when(measurementApiClient.getProbabilityInfo(any()))
                .thenReturn(new ProbabilityInfoResponse(ImmutableList.of(
                        new ProbabilityItemInfo(String.valueOf(FIRST_SKU.getSupplierId()),
                                                FIRST_SKU.getSku(),
                                                BigDecimal.valueOf(52)
                        ),
                        new ProbabilityItemInfo(String.valueOf(SECOND_SKU.getSupplierId()),
                                                SECOND_SKU.getSku(),
                                                BigDecimal.valueOf(100)
                        )
                )));

        final LocalDateTime requestedDate = LocalDate.of(2019, 11, 11).atStartOfDay();
        final ShopRequest request = getShopRequest(RequestType.CROSSDOCK, requestedDate);

        enricherService.enrich(request, request.getItems());
    }

    @Test
    @DatabaseSetup("classpath:service/measurement_enrich/before-enrich-crossdoc.xml")
    @ExpectedDatabase(
            value = "classpath:service/measurement_enrich/before-enrich-crossdoc.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotEnrichAnyOneIfRequestTypeTransfer() {
        Mockito.when(measurementApiClient.getProbabilityInfo(any()))
                .thenReturn(new ProbabilityInfoResponse(ImmutableList.of(
                        new ProbabilityItemInfo(String.valueOf(FIRST_SKU.getSupplierId()),
                                                FIRST_SKU.getSku(),
                                                BigDecimal.valueOf(52)
                        ),
                        new ProbabilityItemInfo(String.valueOf(SECOND_SKU.getSupplierId()),
                                                SECOND_SKU.getSku(),
                                                BigDecimal.valueOf(100)
                        )
                )));

        final LocalDateTime requestedDate = LocalDate.of(2019, 11, 11).atStartOfDay();
        final ShopRequest request = getShopRequest(RequestType.TRANSFER, requestedDate);

        enricherService.enrich(request, request.getItems());
    }

    @Test
    @DatabaseSetup("classpath:service/measurement_enrich/before-enrich-supply-with-existing-taken-limits.xml")
    @ExpectedDatabase(
            value = "classpath:service/measurement_enrich/after-enrich-supply-with-existing-taken-limits.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessEnrichOneItemWithExistingTakenLimits() {
        Mockito.when(measurementApiClient.getProbabilityInfo(any()))
                .thenReturn(new ProbabilityInfoResponse(ImmutableList.of(
                        new ProbabilityItemInfo(String.valueOf(FIRST_SKU.getSupplierId()),
                                FIRST_SKU.getSku(),
                                BigDecimal.valueOf(100)
                        ),
                        new ProbabilityItemInfo(String.valueOf(SECOND_SKU.getSupplierId()),
                                SECOND_SKU.getSku(),
                                BigDecimal.valueOf(100)
                        )
                )));

        final LocalDateTime requestedDate = LocalDate.of(2019, 11, 11).atStartOfDay();
        final ShopRequest request = getShopRequest(RequestType.SUPPLY, requestedDate);

        enricherService.enrich(request, request.getItems());
    }

    @NotNull
    private ShopRequest getShopRequest(RequestType requestType, LocalDateTime requestedDate) {
        ShopRequest request = new ShopRequest();
        request.setId(3L);

        RequestItem firstItem = new RequestItem();
        firstItem.setId(1L);
        firstItem.setRequestId(3L);
        firstItem.setSupplierId(FIRST_SKU.getSupplierId());
        firstItem.setArticle(FIRST_SKU.getSku());

        RequestItem secondItem = new RequestItem();
        secondItem.setId(2L);
        secondItem.setRequestId(3L);
        secondItem.setSupplierId(SECOND_SKU.getSupplierId());
        secondItem.setArticle(SECOND_SKU.getSku());

        request.setItems(ImmutableList.of(firstItem, secondItem));

        Supplier supplier = new Supplier();
        supplier.setId(SupplierType.THIRD_PARTY.getId());
        supplier.setSupplierType(SupplierType.THIRD_PARTY);

        request.setSupplier(supplier);
        request.setRequestedDate(requestedDate);
        request.setServiceId(SERVICE_ID);
        request.setType(requestType);
        return request;
    }

    @NotNull
    private ShopRequest getShopRequestWithOneItem(RequestType requestType, LocalDateTime requestedDate) {
        RequestItem item  = new RequestItem();
        item.setId(1L);
        item.setRequestId(1L);
        item.setSupplierId(2L);
        item.setArticle(THIRD_SKU.getSku());

        final Supplier supplier = new Supplier();
        supplier.setId(THIRD_SKU.getSupplierId());
        supplier.setName("supplier2");
        supplier.setSupplierType(SupplierType.THIRD_PARTY);

        final ShopRequest request = new ShopRequest();
        request.setId(1L);
        request.setSupplier(supplier);
        request.setRequestedDate(requestedDate);
        request.setServiceId(SERVICE_ID);
        request.setType(requestType);
        request.setItems(List.of(item));

        return request;
    }
}
