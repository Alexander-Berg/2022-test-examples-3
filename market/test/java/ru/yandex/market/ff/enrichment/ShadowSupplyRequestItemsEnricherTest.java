package ru.yandex.market.ff.enrichment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.configuration.MockSupplierMappingServiceConfig;
import ru.yandex.market.ff.model.bo.InboundAllowance;
import ru.yandex.market.ff.model.bo.SupplierContentMapping;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.RequestItemService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.SupplierMappingService;
import ru.yandex.market.logistic.api.utils.TimeZoneUtil;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsForDayResponse;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.TimeSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseFreeSlotsResponse;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCargoTypesDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;


@ContextConfiguration(classes = MockSupplierMappingServiceConfig.class)
public class ShadowSupplyRequestItemsEnricherTest extends IntegrationTest {

    @Autowired
    private ShadowSupplyRequestItemsEnricher shadowSupplyRequestItemsEnricher;

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private RequestItemService requestItemService;

    @Autowired
    private SupplierMappingService supplierMappingService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private CalendaringServiceClientApi calendaringServiceClient;

    private static final int CARGO_TYPE = 123;

    @BeforeEach
    void init() {

        whenGetMapping();

        PartnerCargoTypesDto partnerCargoTypesDto = new PartnerCargoTypesDto(303L, 303L, Set.of(CARGO_TYPE));

        Mockito.when(lmsClient.getPartnerCargoTypes(any())).thenReturn(
                List.of(partnerCargoTypesDto)
        );

        FreeSlotsForDayResponse freeSlotsForDayResponse =
                new FreeSlotsForDayResponse(LocalDate.of(2018, 1, 1), TimeZoneUtil.DEFAULT_OFFSET,
                        List.of(new TimeSlotResponse(LocalTime.of(10, 0), LocalTime.of(11, 0))));

        when(calendaringServiceClient.getSlotsWithoutQuotaCheck(any())).thenReturn(new FreeSlotsResponse(List.of(
                new WarehouseFreeSlotsResponse(303, List.of(freeSlotsForDayResponse)))));
    }

    private void whenGetMapping() {
        Map<Long, List<InboundAllowance>> inboundAllowancePerService = Map.of(
                303L, List.of(InboundAllowance.inboundAllowed())
        );

        SupplierContentMapping sku1 =
                SupplierContentMapping.builder("8806325-626671", 101115898826L, "LION Восстанавливающий шампунь")
                        .setInboundAllowancePerService(inboundAllowancePerService)
                        .setCargoTypes(Set.of(CARGO_TYPE))
                        .build();

        SupplierContentMapping sku2 =
                SupplierContentMapping.builder("8806325-624745", 100399249546L, "LION Зубная паста отбеливающая")
                        .setInboundAllowancePerService(inboundAllowancePerService)
                        .setCargoTypes(Set.of(CARGO_TYPE))
                        .build();

        SupplierContentMapping sku3 =
                SupplierContentMapping.builder("8806325-624714", 101103346739L, "LION Зубная паста")
                        .setInboundAllowancePerService(inboundAllowancePerService)
                        .setCargoTypes(Set.of(CARGO_TYPE))
                        .build();
        Map<String, SupplierContentMapping> mapping = Map.of(
                "8806325-626671", sku1,
                "8806325-624745", sku2,
                "8806325-624714", sku3
        );

        Mockito.when(supplierMappingService.getMarketSkuMapping(anyLong(), anyCollection(), any(), anyInt(), anySet()))
                .thenReturn(mapping);
    }

    @Test
    @DatabaseSetup("classpath:enrichment/shadow-supply-request-items-enricher/before.xml")
    @Transactional
    public void validOptionIfRequiredQuotaEqualsToAvailableQuotaTest() {

        long requestId = 7627939;
        Optional<ShopRequest> request = shopRequestFetchingService.getRequest(requestId);

        List<RequestItem> items = requestItemService.findAllByRequestId(requestId);

        RequestValidationResult requestValidationResult =
                shadowSupplyRequestItemsEnricher.enrichAndValidate(request.get(), items);

        assertions.assertThat(requestValidationResult.getValidRequestOptions())
                .contains(RequestOption.builder().date(LocalDate.of(2018, 1, 1)).serviceId(303L).build());

    }

}
