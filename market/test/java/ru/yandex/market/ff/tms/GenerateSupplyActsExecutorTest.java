package ru.yandex.market.ff.tms;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.config.HistoryAgencyContextBeanConfig;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.service.ActGenerationService;
import ru.yandex.market.ff.service.RequestDocumentService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinkDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinksDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerOrgInfoDTO;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Интеграционный тест для {@link GenerateSupplyActsExecutor}.
 *
 * @author avetokhin 28/02/18.
 */
class GenerateSupplyActsExecutorTest extends IntegrationTest {

    private static final long SUPPLIER_ID = 10265914;

    private static final Set<DeliveryServiceType> NECESSARY_DELIVERY_SERVICE_TYPES =
            EnumSet.of(DeliveryServiceType.FULFILLMENT, DeliveryServiceType.CROSSDOCK);

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private RequestDocumentService requestDocumentService;

    @Autowired
    private Collection<ActGenerationService> actGenerationServices;

    private GenerateSupplyActsExecutor executor;

    @Autowired
    private HistoryAgent historyAgent;

    @BeforeEach
    void init() throws MalformedURLException {
        when(mdsS3Client.getUrl(any())).thenReturn(new URL("http://localhost/act.pdf"));
        executor = new GenerateSupplyActsExecutor(
                shopRequestFetchingService,
                requestDocumentService,
                actGenerationServices,
                Executors.newSingleThreadExecutor(),
                historyAgent,
                () -> new HistoryAgencyContextBeanConfig().getHistoryContext());

        Mockito.when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class)))
                .thenReturn(Collections.singletonList(getWarehouseLogisticsPointResponse()));
    }

    @Test
    @DatabaseSetup("classpath:tms/generate-reception-transfer-act/before.xml")
    @ExpectedDatabase(value = "classpath:tms/generate-reception-transfer-act/after.xml", assertionMode = NON_STRICT)
    void testGenerateReceptionTransferAct() {
        executor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/doc-updated/before.xml")
    @ExpectedDatabase(value = "classpath:tms/doc-updated/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testDocumentUpdated() {
        when(mbiApiClient.getPartnerFulfillments(eq(SUPPLIER_ID), eq(NECESSARY_DELIVERY_SERVICE_TYPES)))
                .thenReturn(new PartnerFulfillmentLinksDTO(
                        Collections.singleton(getPartnerFulfillmentLinkDto(SUPPLIER_ID, 47778))));
        when(mbiApiClient.getPartnerInfo(eq(SUPPLIER_ID))).thenReturn(getPartnerInfoDto(SUPPLIER_ID));

        executor.doJob(null);
    }

    private LogisticsPointResponse getWarehouseLogisticsPointResponse() {
        return LogisticsPointResponse.newBuilder().id(1234L).name("Rostov").address(getAddress())
                .phones(Collections.singleton(getPhone())).build();
    }

    private Address getAddress() {
        return Address.newBuilder()
                .settlement("Котельники")
                .street("Яничкин проезд")
                .house("7")
                .comment("терминал БД-6")
                .build();
    }

    private Phone getPhone() {
        return new Phone("+79165678901", null, null, null);
    }

    private PartnerInfoDTO getPartnerInfoDto(long partnerId) {
        return new PartnerInfoDTO(partnerId, null, null, null, null, null, null, getPartnerOrgInfoDto(), false, null);
    }

    private PartnerOrgInfoDTO getPartnerOrgInfoDto() {
        return new PartnerOrgInfoDTO(null, null, "Regression Supplier 2018225", null,
                "000001, Тестинг Яндекс.Маркета, фейковый поставщик 1", null, null, null);
    }

    private PartnerFulfillmentLinkDTO getPartnerFulfillmentLinkDto(long partnerId, long serviceId) {
        return new PartnerFulfillmentLinkDTO(partnerId, serviceId, null, DeliveryServiceType.CROSSDOCK);
    }
}
