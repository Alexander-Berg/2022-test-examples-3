package ru.yandex.market.ff.dbqueue.service;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.model.bo.AggregatedRequestItemsInfo;
import ru.yandex.market.ff.model.dbqueue.RequestDailyReportByWarehousePayload;
import ru.yandex.market.ff.model.dto.ArrivedRequestsListRowDTO;
import ru.yandex.market.ff.model.entity.FulfillmentInfo;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.ShopRequestPeriodicalReport;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.repository.ShopRequestPeriodicalReportRepository;
import ru.yandex.market.ff.service.ArrivedRequestsListGenerationService;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.FulfillmentInfoService;
import ru.yandex.market.ff.service.MdsS3Service;
import ru.yandex.market.ff.service.PechkinNotificationService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestDailyReportByWarehouseProcessingServiceTest {


    @Test
    public void test() {

        ShopRequestFetchingService fetchingService = mock(ShopRequestFetchingService.class);
        ShopRequest sr = new ShopRequest();
        Supplier mySupplier = new Supplier();
        mySupplier.setId(123L);
        sr.setSupplier(mySupplier);
        sr.setId(123L);

        MdsS3Service mediaHost = mock(MdsS3Service.class);

        when(fetchingService.findRequestsArrivedInPeriod(anyLong(), any(), any(), any(), anySet(), anySet()))
                .thenReturn(Set.of(sr));
        ConcreteEnvironmentParamService params = mock(ConcreteEnvironmentParamService.class);
        when(params.getRequestDailyReportTelegramChannelName())
                .thenReturn("myChannel");
        when(params.getRequestDailyReportTelegramMessageTemplate())
                .thenReturn(
                        "Loren Ipsum... {{date}} :: {{warehouseId}} :: " +
                                "{{warehouseNameUnderscored}} :: {{supplierTypeName}}");

        when(params.getRequestDailyReportDocuBaseUrl())
                .thenReturn("https://yard.vs.market.yandex-team.ru/documents/request-daily-report/");

        RequestItemRepository requestItemRepository = mock(RequestItemRepository.class);

        when(requestItemRepository.findTotalSupplyInfoForRequest(any())).thenReturn(List.of(
                AggregatedRequestItemsInfo.builder()
                        .anyRealSupplierId("303")
                        .anyRealSupplierName("My Real Supplier Name")
                        .build()
        ));

        URL myurl = mock(URL.class);
        when(myurl.toString()).thenReturn("https://url");
        when(mediaHost.uploadFile(any(), any())).thenReturn(myurl);

        ShopRequestPeriodicalReportRepository repoWithLinks = mock(ShopRequestPeriodicalReportRepository.class);

        ShopRequestPeriodicalReport savedMeta = new ShopRequestPeriodicalReport();
        savedMeta.setId(303L);
        savedMeta.setFileName("Яндекс_Маркет_Софьино-1P-2022-04-17");
        when(repoWithLinks.save(any(ShopRequestPeriodicalReport.class)))
                .thenReturn(savedMeta);
        FulfillmentInfoService fulfillmwntInfo = mock(FulfillmentInfoService.class);
        FulfillmentInfo myWarehouse = mock(FulfillmentInfo.class);
        when(myWarehouse.getName()).thenReturn("Яндекс.Маркет Софьино");
        when(fulfillmwntInfo.getFulfillmentInfo(anyLong())).thenReturn(Optional.of(myWarehouse));


        ArrivedRequestsListGenerationService docuGenerator = mock(ArrivedRequestsListGenerationService.class);

        RequestDailyReportByWarehouseProcessingService service = new RequestDailyReportByWarehouseProcessingService(
                fetchingService,
                docuGenerator,
                fulfillmwntInfo,
                mediaHost,
                repoWithLinks,
                mock(PechkinNotificationService.class),
                params,
                requestItemRepository
        );


        RequestDailyReportByWarehousePayload payload =
                new RequestDailyReportByWarehousePayload(172L, SupplierType.FIRST_PARTY, LocalDate.of(2022, 2, 22));
        service.processPayload(payload);

        verify(mediaHost, times(1)).uploadFile(any(), any());
        verify(repoWithLinks, times(1)).save(any(ShopRequestPeriodicalReport.class));

        ArgumentCaptor<List<ArrivedRequestsListRowDTO>> listDocRows = ArgumentCaptor.forClass(List.class);
        verify(docuGenerator, times(1)).generateListFor1p(listDocRows.capture(), any(), any());

        Assertions.assertEquals(1, listDocRows.getValue().size());
        Assertions.assertEquals("My Real Supplier Name", listDocRows.getValue().get(0).getRealSupplierName());
        Assertions.assertEquals("303", listDocRows.getValue().get(0).getRealSupplierId());
    }

}
