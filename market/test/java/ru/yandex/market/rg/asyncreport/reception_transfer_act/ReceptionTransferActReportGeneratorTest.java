package ru.yandex.market.rg.asyncreport.reception_transfer_act;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.yam.service.PrepayRequestService;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.core.fulfillment.mds.ReportsMdsStorage;
import ru.yandex.market.core.id.service.MarketIdGrpcService;
import ru.yandex.market.core.ww.WwActGenerationService;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReceptionTransferActReportGeneratorTest extends FunctionalTest {

    private static final long PARTNER_ID = 777L;

    @Autowired
    private WwClient wwClient;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private MarketIdGrpcService marketIdGrpcService;

    @Autowired
    private PrepayRequestService prepayRequestService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ReportsMdsStorage<ReportsType> reportsMdsStorage;

    private WwActGenerationService wwActGenerationService;

    @Autowired
    private ReceptionTransferActReportGenerator receptionTransferActReportGenerator;

    @BeforeEach
    void setUp() {
        wwActGenerationService = new WwActGenerationService(checkouterClient, marketIdGrpcService, prepayRequestService,
                wwClient, environmentService);
        when(checkouterClient.getOrders(any(), any(), any())).thenReturn(getEmptyOrders());
    }

    @Test
    @DisplayName("Успешная генерация отчета")
    void testSuccessGenerateReport() {
        var dataToReport = getReportData();
        wwActGenerationService.generatePdf(dataToReport);
        ArgumentCaptor<RtaOrdersData> dataFromReportCaptor = ArgumentCaptor.forClass(RtaOrdersData.class);
        verify(wwClient).generateReceptionTransferAct(dataFromReportCaptor.capture(), eq(DocumentFormat.PDF));
        verifyRtaOrdersInfo(dataToReport,dataFromReportCaptor.getValue());
    }

    @Test
    @DisplayName("Ошибка генерации если в отчете пустой список товаров")
    void testErrorReport() {
        ReportResult reportResult = receptionTransferActReportGenerator.generate(Long.toString(PARTNER_ID),
                new ReceptionTransferActReportGeneratorParams(PARTNER_ID));
        Assertions.assertEquals(reportResult.getReportGenerationInfo().getDescription(),"Report is empty");
    }

    RtaOrdersData getReportData() {
       var docOrder = WwActGenerationService.convertToDocOrder(prepareOrder());
       var orders = List.of(docOrder);
       return RtaOrdersData.builder()
               .senderId(Long.toString(PARTNER_ID))
               .orders(orders)
               .partnerLegalName("test_partner_name")
               .senderLegalName("test_sender_name")
               .shipmentDate(LocalDate.of(2021, 9, 9))
               .shipmentId("678")
               .build();
   }

    private Order prepareOrder() {
        var order = new Order();
        var delivery = new Delivery();
        var parcel = new Parcel();
        parcel.setWeight(1000L);
        parcel.setBoxes(Collections.singletonList(new ParcelBox()));
        delivery.setParcels(Collections.singletonList(parcel));
        order.setDelivery(delivery);
        order.setItemsTotal(BigDecimal.valueOf(3300));
        order.setShopOrderId("12345");
        order.setId(PARTNER_ID);
        return order;
    }

    private void verifyRtaOrdersInfo(RtaOrdersData dataToReport, RtaOrdersData dataFromReport) {
        assertThat(dataToReport.getOrders().size()).isEqualTo(dataFromReport.getOrders().size());

        for (int i = 0; i < dataToReport.getOrders().size(); i++) {
            var docOrderTo = dataToReport.getOrders().get(i);
            var docOrderFrom = dataFromReport.getOrders().get(i);
            assertThat(docOrderTo.getAssessedCost()).isEqualTo(docOrderFrom.getAssessedCost());
            assertThat(docOrderTo.getPartnerId()).isEqualTo(docOrderFrom.getPartnerId());
            assertThat(docOrderTo.getWeight()).isEqualTo(docOrderFrom.getWeight());
            assertThat(docOrderTo.getPlacesCount()).isEqualTo(docOrderFrom.getPlacesCount());
            assertThat(docOrderTo.getYandexId()).isEqualTo(docOrderFrom.getYandexId());
        }

        assertThat(dataToReport.getPartnerLegalName()).isEqualTo(dataFromReport.getPartnerLegalName());
        assertThat(dataToReport.getSenderLegalName()).isEqualTo(dataFromReport.getSenderLegalName());
        assertThat(dataToReport.getShipmentDate()).isEqualTo(dataFromReport.getShipmentDate());
        assertThat(dataToReport.getShipmentId()).isEqualTo(dataFromReport.getShipmentId());
        assertThat(dataToReport.getSenderId()).isEqualTo(dataFromReport.getSenderId());

    }

    private Pager getEmptyPager() {
        return new Pager(0, 0, 0, 0, 0, 0);
    }

    private PagedOrders getEmptyOrders() {
        return new PagedOrders(Collections.EMPTY_LIST, getEmptyPager());
    }
}
