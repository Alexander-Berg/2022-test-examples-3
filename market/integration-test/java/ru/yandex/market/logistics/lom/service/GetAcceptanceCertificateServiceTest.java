package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistic.api.model.common.PartnerType;
import ru.yandex.market.logistics.lom.jobs.model.RegistryIdPayload;
import ru.yandex.market.logistics.lom.jobs.processor.GetAcceptanceCertificateService;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics.werewolf.model.entity.DocOrder;
import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createRegistryIdPayload;
import static ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat.PDF;

@DisplayName("Отправка заявки на получение АПП по реестру")
class GetAcceptanceCertificateServiceTest extends AbstractExternalServiceTest {

    private static final RegistryIdPayload PAYLOAD = createRegistryIdPayload(1L, "123");
    private static final ResourceLocation RESOURCE_LOCATION = ResourceLocation.create("lom-doc-test", "1");
    private static final byte[] BYTES = new byte[10];

    @Autowired
    private GetAcceptanceCertificateService getAcceptanceCertificateService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private WwClient wwClient;

    @Autowired
    private MdsS3Client mdsS3Client;

    @BeforeEach
    void setUp() throws MalformedURLException {
        doReturn(Optional.of(
            LmsFactory.createLogisticsPointResponse(2L, 20L, "warehouse", PointType.WAREHOUSE).build()
        ))
            .when(lmsClient).getLogisticsPoint(2L);

        doReturn(Optional.of(LmsFactory.createLegalInfo(1L))).when(lmsClient).getPartnerLegalInfo(48L);
        when(mdsS3Client.getUrl(RESOURCE_LOCATION)).thenReturn(getUrl());
    }

    @AfterEach
    void verifyInteractions() {
        verifyNoMoreInteractions(lmsClient, mdsS3Client, wwClient);
    }

    @Test
    @DisplayName("Успешно отправлен запрос на создание АПП в WW (СД)")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_all_ok.xml")
    @ExpectedDatabase(
        value = "/controller/document/after/confirmed_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateAcceptanceCertificate() {
        RtaOrdersData data = ordersDataBuilder(List.of(
            docOrderBuilder().assessedCost(BigDecimal.valueOf(1001)).build())
        ).build();
        when(wwClient.generateReceptionTransferAct(data, PDF)).thenReturn(BYTES);
        getAcceptanceCertificateService.processPayload(PAYLOAD);
        verifyWW(data);
    }

    @Test
    @DisplayName("Успешно отправлен запрос на создание АПП в WW (СД) для синего")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_blue_assessed_value_999.xml")
    @ExpectedDatabase(
        value = "/controller/document/after/confirmed_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateAcceptanceCertificateBlueTest() {
        RtaOrdersData data = ordersDataBuilder(List.of(
            docOrderBuilder().assessedCost(BigDecimal.valueOf(999)).build()
        )).build();
        when(wwClient.generateReceptionTransferAct(data, PDF)).thenReturn(BYTES);
        getAcceptanceCertificateService.processPayload(PAYLOAD);
        verifyWW(data);
    }

    @Test
    @DisplayName("Успешно отправлен запрос на создание АПП в WW (СД)")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_assessed_value_999.xml")
    @ExpectedDatabase(
        value = "/controller/document/after/confirmed_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateAcceptanceCertificateWithAssessedValueLessThan1000() {
        RtaOrdersData data = ordersDataBuilder(List.of(
            docOrderBuilder().assessedCost(BigDecimal.valueOf(999)).build())
        ).build();
        when(wwClient.generateReceptionTransferAct(data, PDF)).thenReturn(BYTES);
        getAcceptanceCertificateService.processPayload(PAYLOAD);
        verifyWW(data);
    }

    @Test
    @DisplayName("Успешно отправлен запрос на создание АПП в WW (СД) - фейковый заказ")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_fake.xml")
    @ExpectedDatabase(
        value = "/controller/document/after/confirmed_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateAcceptanceCertificateFakeOrder() {
        RtaOrdersData data = ordersDataBuilder(List.of(docOrderBuilder().build()))
            .senderLegalName("sender-name")
            .build();
        when(wwClient.generateReceptionTransferAct(data, PDF)).thenReturn(BYTES);
        getAcceptanceCertificateService.processPayload(PAYLOAD);
        verifyWW(data);
    }

    @Test
    @DisplayName("Успешно отправлен запрос на создание АПП в WW (СД) - брать название партнера, если нет incorporation")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_no_incorporation.xml")
    @ExpectedDatabase(
        value = "/controller/document/after/confirmed_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateAcceptanceCertificateReadableNameOrder() {
        when(lmsClient.getPartner(20L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().readableName("Partner readable 20").build()));
        RtaOrdersData data = ordersDataBuilder(List.of(docOrderBuilder().build()))
            .partnerLegalName("Partner readable 20")
            .build();
        when(wwClient.generateReceptionTransferAct(data, PDF)).thenReturn(BYTES);
        getAcceptanceCertificateService.processPayload(PAYLOAD);
        verify(lmsClient).getPartner(20L);
        verifyWW(data);
    }

    @Test
    @DisplayName("Успешно отправлен запрос на создание АПП в WW (СД) - не указаны ВГХ грузомест")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_all_ok.xml")
    @DatabaseSetup(
        value = "/service/getacceptancecertificate/update/set_null_dimensions_to_place.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/document/after/confirmed_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void withoutPlacesDimensions() {
        RtaOrdersData data = ordersDataBuilder(List.of(
            docOrderBuilder()
                .weight(BigDecimal.valueOf(15))
                .build()
        ))
            .build();
        when(wwClient.generateReceptionTransferAct(data, PDF)).thenReturn(BYTES);
        getAcceptanceCertificateService.processPayload(PAYLOAD);
        verifyWW(data);
    }

    @Test
    @DisplayName("Успешно отправлен запрос на создание АПП в WW (СД) - несколько заказов")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_all_ok.xml")
    @DatabaseSetup(
        value = "/service/getacceptancecertificate/before/additional_order.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/document/after/confirmed_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateAcceptanceCertificateSeveralOrders() {
        RtaOrdersData data = ordersDataBuilder(List.of(
            docOrderBuilder().build(),
            DocOrder.builder()
                .assessedCost(BigDecimal.valueOf(200))
                .partnerId("test-external-id-4")
                .yandexId("2-LOinttest-2")
                .placesCount(1)
                .weight(BigDecimal.valueOf(40))
                .build()
        ))
            .build();
        when(wwClient.generateReceptionTransferAct(data, PDF)).thenReturn(BYTES);
        getAcceptanceCertificateService.processPayload(PAYLOAD);
        verifyWW(data);
    }

    @Test
    @DisplayName("Успешно отправлен запрос на создание АПП в WW (СЦ)")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_all_ok_ff.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/document/after/confirmed_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void getCertificateSuccessFfWW() {
        RtaOrdersData data = ordersDataBuilderFF(List.of(docOrderBuilderFF().build())).build();
        when(wwClient.generateReceptionTransferAct(data, PDF)).thenReturn(BYTES);
        getAcceptanceCertificateService.processPayload(PAYLOAD);
        verifyWW(data);
    }

    @Test
    @DisplayName("Ошибка запроса на создание АПП в WW (СЦ) - нет активного ShipmentApplication")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_without_sa_ff.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    void getCertificateFailFfWWWithoutShipmentApplication() {
        softly.assertThatThrownBy(() -> getAcceptanceCertificateService.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Sorting center must have active application");
    }

    @Test
    @DisplayName("Успешно отправлен запрос на создание АПП в WW (СЦ) - фейковый заказ")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_fake_ff.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/document/after/confirmed_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void getCertificateSuccessFakeFfWW() {
        RtaOrdersData data = ordersDataBuilderFF(List.of(docOrderBuilderFF().build()))
            .senderLegalName("sender-name")
            .build();

        when(wwClient.generateReceptionTransferAct(data, PDF)).thenReturn(BYTES);
        getAcceptanceCertificateService.processPayload(PAYLOAD);
        verifyWW(data);
    }

    @Test
    @DisplayName("Успешно отправлен запрос на создание АПП в WW (СЦ) - брать название партнера, если нет incorporation")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_no_incorporation_ff.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/document/after/confirmed_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void getCertificateSuccessReadableNameFfWW() {
        when(lmsClient.getPartner(20L))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().readableName("Partner readable 20").build()));
        RtaOrdersData data = ordersDataBuilderFF(List.of(docOrderBuilderFF().build()))
            .partnerLegalName("Partner readable 20")
            .build();
        when(wwClient.generateReceptionTransferAct(data, PDF)).thenReturn(BYTES);
        getAcceptanceCertificateService.processPayload(PAYLOAD);
        verify(lmsClient).getPartner(20L);
        verifyWW(data);
    }

    @Test
    @DisplayName("Успешно отправлен запрос на создание АПП в WW (СЦ) - несколько заказов")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_all_ok_ff.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/service/getacceptancecertificate/before/additional_order.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/document/after/confirmed_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateAcceptanceCertificateSeveralOrdersFF() {
        RtaOrdersData data = ordersDataBuilderFF(List.of(
            docOrderBuilderFF().build(),
            DocOrder.builder()
                .assessedCost(BigDecimal.valueOf(200))
                .partnerId("test-external-id-4")
                .yandexId("2-LOinttest-2")
                .placesCount(1)
                .weight(BigDecimal.valueOf(40))
                .build()
        ))
            .build();
        when(wwClient.generateReceptionTransferAct(data, PDF)).thenReturn(BYTES);
        getAcceptanceCertificateService.processPayload(PAYLOAD);
        verifyWW(data);
    }

    @Test
    @DisplayName("Ошибка WW - недопустимый тип партнера")
    @DatabaseSetup("/service/getacceptancecertificate/before/get_acceptance_certificate_invalid_partner_type.xml")
    void getCertificateSuccessInvalidPartnerTypeWW() {
        softly.assertThatThrownBy(() -> getAcceptanceCertificateService.processPayload(PAYLOAD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported partner type " + PartnerType.DROPSHIP);
    }

    @Nonnull
    private static URL getUrl() throws MalformedURLException {
        return new URL("http", "localhost", 80, "/file");
    }

    @Nonnull
    private DocOrder.DocOrderBuilder docOrderBuilder() {
        return DocOrder.builder()
            .yandexId("2-LOinttest-1")
            .partnerId("test-external-id-2")
            .assessedCost(BigDecimal.valueOf(1001))
            .weight(BigDecimal.valueOf(20))
            .placesCount(1);
    }

    @Nonnull
    private DocOrder.DocOrderBuilder docOrderBuilderFF() {
        return DocOrder.builder()
            .yandexId("2-LOinttest-1")
            .partnerId("test-external-id-2")
            .assessedCost(BigDecimal.valueOf(1001))
            .weight(BigDecimal.valueOf(4))
            .placesCount(1);
    }

    @Nonnull
    private RtaOrdersData.RtaOrdersDataBuilder ordersDataBuilder(List<DocOrder> docOrders) {
        return RtaOrdersData.builder()
            .orders(docOrders)
            .shipmentId("000123")
            .shipmentDate(LocalDate.of(2019, 6, 11))
            .senderLegalName("credentials-incorporation")
            .partnerLegalName("OOO СД")
            .senderId("1");
    }

    @Nonnull
    private RtaOrdersData.RtaOrdersDataBuilder ordersDataBuilderFF(List<DocOrder> docOrders) {
        return RtaOrdersData.builder()
            .orders(docOrders)
            .shipmentId("ext1")
            .shipmentDate(LocalDate.of(2019, 6, 11))
            .senderLegalName("credentials-incorporation")
            .partnerLegalName("OOO СЦ")
            .senderId("1");
    }

    private void verifyWW(RtaOrdersData rtaOrdersData) {
        verify(wwClient).generateReceptionTransferAct(rtaOrdersData, PDF);
        verify(mdsS3Client).upload(eq(RESOURCE_LOCATION), any(StreamContentProvider.class));
        verify(mdsS3Client).getUrl(RESOURCE_LOCATION);
    }
}
