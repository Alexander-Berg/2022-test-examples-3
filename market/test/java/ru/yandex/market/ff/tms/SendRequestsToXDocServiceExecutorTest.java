package ru.yandex.market.ff.tms;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.yam.dto.OrganizationInfoDTO;
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDTO;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.model.converter.BaseLgwClientConverter;
import ru.yandex.market.ff.service.LgwRequestService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CisHandleMode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Consignment;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Contractor;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Inbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item.ItemBuilder;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Service;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Интеграционный тесты для {@link SendRequestsToXDocServiceExecutor}.
 *
 */
class SendRequestsToXDocServiceExecutorTest extends IntegrationTest {

    private static final Long SUPPLIER_ID = 1L;

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private LgwRequestService lgwRequestService;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private MbiApiClient mbiApiClient;

    private SendRequestsToXDocServiceExecutor executor;

    @Autowired
    private HistoryAgent historyAgent;

    @BeforeEach
    void init() {
        executor = new SendRequestsToXDocServiceExecutor(shopRequestFetchingService, lgwRequestService,
            Executors.newSingleThreadExecutor(), historyAgent);

        final PrepayRequestDTO prepayRequest = new PrepayRequestDTO();
        final OrganizationInfoDTO organizationInfo = createOrganizationInfo();

        prepayRequest.setOrganizationInfo(organizationInfo);
        when(mbiApiClient.getPrepayRequest(anyLong(), anyLong()))
            .thenReturn(prepayRequest);

    }

    @Test
    @DatabaseSetup("classpath:tms/send-xdoc-requests-to-service/before.xml")
    @ExpectedDatabase(value = "classpath:tms/send-xdoc-requests-to-service/after.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void testJobUseMarketNameModeOff() throws GatewayApiException {
        executor.doJob(null);
        verifyInbound(false);
    }

    private void verifyInbound(boolean useMarketName) throws GatewayApiException {
        // Проверка отправки первой поставки.
        Inbound inbound = inbound("10", "2017-01-01", "test",
            getConsignments(
                "10",
                unitId("1", "SHOPSKU1"),
                Collections.unmodifiableList(Lists.newArrayList(
                    new Barcode("11", null, null),
                    new Barcode("22", null, null))),
                new BigDecimal("50.50"),
                useMarketName ? "market_name1" : "offer_1",
                3,
                1,
                true,
                useMarketName ? Collections.singletonList("marketVendorCode1")
                    : Collections.singletonList("vendorCode1"),
                10,
                Collections.emptyList(),
                    List.of(String.format(BaseLgwClientConverter.MARKET_URL_TEMPLATE, 1))
            ));

        Mockito.verify(fulfillmentClient).createInbound(inbound, new Partner(1L));
    }

    private static Inbound inbound(String id, String date, String comment, List<Consignment> consignments) {
        final ResourceId resourceId = ResourceId.builder()
            .setYandexId(id)
            .setPartnerId(null)
            .build();

        Inbound.InboundBuilder inbound = new Inbound.InboundBuilder(
            resourceId, InboundType.DEFAULT, consignments, DateTimeInterval.fromFormattedValue(date + "/" + date));
        inbound.setComment(comment);
        return inbound.build();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static Consignment getConsignment(String yandexId, UnitId unitId, List<Barcode> barcodes, BigDecimal price,
                                              String name, int count, int boxCount,
                                              boolean hasLifeTime,
                                              List<String> vendorCodes, Integer boxCapacity,
                                              List<Service> inboundServices,
                                              List<String> urls) {

        final ResourceId resourceId = ResourceId.builder()
            .setYandexId(yandexId)
            .setPartnerId(null)
            .build();

        final ItemBuilder builder = new ItemBuilder(name, count, price, CargoType.UNKNOWN, Collections.emptyList());
        builder.setUnitId(unitId);
        builder.setArticle(unitId.getArticle());
        builder.setBarcodes(barcodes);
        builder.setBoxCount(boxCount);
        builder.setHasLifeTime(hasLifeTime);
        builder.setVendorCodes(vendorCodes);
        builder.setBoxCapacity(boxCapacity);
        builder.setInboundServices(inboundServices);
        builder.setUrls(urls);
        builder.setCisHandleMode(CisHandleMode.NOT_DEFINED);
        builder.setInstances(Collections.emptyList());
        builder.setContractor(new Contractor(String.valueOf(unitId.getVendorId()), "supplier" + unitId.getVendorId()));
        return new Consignment(resourceId, builder.build(), null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static List<Consignment> getConsignments(String yandexId, UnitId unitId, List<Barcode> barcodes,
                                                     BigDecimal price, String name, int count,
                                                     int boxCount, boolean hasLifeTime,
                                                     List<String> vendorCodes, Integer boxCapacity,
                                                     List<Service> inboundServices,
                                                     List<String> urls) {

        return Collections.singletonList(getConsignment(yandexId, unitId, barcodes, price,
            name, count, boxCount, hasLifeTime, vendorCodes, boxCapacity, inboundServices, urls));
    }

    private static OrganizationInfoDTO createOrganizationInfo() {
        return OrganizationInfoDTO.builder()
                .type(OrganizationType.IP)
                .inn("someInn")
                .ogrn("someOgrn")
                .kpp("someKpp")
                .name("someOrgName")
                .accountNumber("accNumber")
                .corrAccountNumber("corrAccNumber")
                .bankName("bankName")
                .bik("someBik")
                .factAddress("factAddr")
                .juridicalAddress("jurAddr")
                .build();
    }

    private UnitId unitId(String marketSku, String shopSku) {
        return new UnitId(marketSku, SUPPLIER_ID, shopSku);
    }
}
