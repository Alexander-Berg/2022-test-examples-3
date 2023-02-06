package ru.yandex.market.mbi.api.controller.partner;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.BpmnClientService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.delivery.AsyncTarifficatorService;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.ff4shops.FF4ShopsPartnerState;
import ru.yandex.market.core.ff4shops.PartnerFulfillmentLinkForFF4Shops;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.core.supplier.state.PartnerServiceLinkLogbrokerEvent;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinkDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinksDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.partner.service.link.PartnerServiceLinkOuterClass;
import ru.yandex.market.mbi.partner.service.link.PartnerServiceLinkOuterClass.PartnerServiceLink.LinkUpdateType;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Functional tests for {@link PartnerFulfillmentController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "PartnerDeliveryServicesControllerTest.csv")
class PartnerFulfillmentControllerTest extends FunctionalTest {

    private static final long MARKET_ID = 123;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private FF4ShopsClient ff4ShopsClient;

    @Autowired
    private BpmnClientService bpmnClientService;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private AsyncTarifficatorService asyncTarifficatorService;

    @Autowired
    @Qualifier("partnerFfLinkLbProducer")
    private LogbrokerEventPublisher<PartnerServiceLinkLogbrokerEvent> partnerFfLinkLbProducer;


    @BeforeEach
    void init() {
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            MarketAccount marketAccount = MarketAccount.newBuilder().setMarketId(MARKET_ID).build();
            GetByPartnerResponse response = GetByPartnerResponse.newBuilder()
                    .setMarketId(marketAccount).setSuccess(true).build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();

            return null;
        }).when(marketIdServiceImplBase).getByPartner(any(), any());

        when(partnerFfLinkLbProducer.publishEventAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(partnerFfLinkLbProducer);
    }

    @Test
    @DisplayName("Поставщик не найден")
    void notFound() {
        PartnerFulfillmentLinksDTO partnerFulfillments = mbiApiClient.getPartnerFulfillments(4L);
        assertTrue(partnerFulfillments.getPartnerFulfillmentLinks().isEmpty());
    }

    @Test
    @DisplayName("Получение фулфиллментов поставщика")
    void found() {
        PartnerFulfillmentLinksDTO partnerFulfillments = mbiApiClient.getPartnerFulfillments(1L);
        PartnerFulfillmentLinksDTO expected = new PartnerFulfillmentLinksDTO(
                Collections.singletonList(
                        new PartnerFulfillmentLinkDTO(1L, 147L, 101L, DeliveryServiceType.FULFILLMENT))
        );

        assertEquals(
                expected,
                partnerFulfillments
        );
    }

    @Test
    @DisplayName("Получение сервисов указанного типа")
    void foundFiltered() {
        Set<DeliveryServiceType> serviceTypes = new HashSet<>();
        serviceTypes.add(DeliveryServiceType.DROPSHIP);
        serviceTypes.add(DeliveryServiceType.CROSSDOCK);
        serviceTypes.add(DeliveryServiceType.DROPSHIP_BY_SELLER);
        PartnerFulfillmentLinksDTO deliveryLinks = mbiApiClient.getPartnerFulfillments(108L, serviceTypes);

        assertEquals(deliveryLinks.getPartnerFulfillmentLinks().size(), 2);
        List<Long> serviceIds =
                deliveryLinks.getPartnerFulfillmentLinks()
                        .stream()
                        .map(PartnerFulfillmentLinkDTO::getServiceId)
                        .collect(Collectors.toList());
        assertThat(serviceIds, containsInAnyOrder(777L, 779L));
    }

    @Test
    @DisplayName("Получение всех сервисов, если не указан фильтр")
    void foundAllIfWithoutFilter() {
        PartnerFulfillmentLinksDTO deliveryLinks = mbiApiClient.getPartnerFulfillments(108L, null);

        assertEquals(deliveryLinks.getPartnerFulfillmentLinks().size(), 3);
        List<Long> serviceIds =
                deliveryLinks.getPartnerFulfillmentLinks()
                        .stream()
                        .map(PartnerFulfillmentLinkDTO::getServiceId)
                        .collect(Collectors.toList());
        assertThat(serviceIds, containsInAnyOrder(147L, 777L, 779L));

        deliveryLinks = mbiApiClient.getPartnerFulfillments(108L, Collections.emptySet());

        assertEquals(deliveryLinks.getPartnerFulfillmentLinks().size(), 3);
        serviceIds =
                deliveryLinks.getPartnerFulfillmentLinks()
                        .stream()
                        .map(PartnerFulfillmentLinkDTO::getServiceId)
                        .collect(Collectors.toList());
        assertThat(serviceIds, containsInAnyOrder(147L, 777L, 779L));
    }

    @Test
    @DisplayName("Фиксируем ответ в xml-формате")
    void xmlRepresentation() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(
                "http://localhost:" + port + "/partners/1/fulfillments",
                String.class
        );
        MbiAsserts.assertXmlEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<partner-fulfillment>\n" +
                        "   <links>\n" +
                        "      <links partner-id=\"1\" service-id=\"147\" feed-id=\"101\" service-type=\"fulfillment\"/>\n" +
                        "   </links>\n" +
                        "</partner-fulfillment>",
                responseEntity.getBody()
        );
    }

    @Test
    @DisplayName("Фиксируем ответ в xml-формате для crossborderов")
    void xmlRepresentationCrossborder() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(
                "http://localhost:" + port + "/partners/2/fulfillments",
                String.class
        );
        MbiAsserts.assertXmlEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<partner-fulfillment>\n" +
                        "   <links>\n" +
                        "      <links partner-id=\"2\" service-id=\"146\" service-type=\"carrier\" />\n" +
                        "   </links>\n" +
                        "</partner-fulfillment>",
                responseEntity.getBody()
        );
    }

    @Test
    @DisplayName("Добавление дропшип склада дропшип партнеру, у которого есть фф склад")
    @DbUnitDataSet(after = "PartnerFulfillmentControllerTest.after.csv")
    void createPartnerDropshipLink() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);

        var responseBuilder
                = EnhancedRandom.random(BusinessWarehouseResponse.Builder.class);
        responseBuilder.marketId(MARKET_ID);
        responseBuilder.partnerId(777L);
        var getPartnerResp = responseBuilder.build();

        when(lmsClient.getBusinessWarehouseForPartner(777L)).thenReturn(Optional.of(getPartnerResp));

        ResponseEntity<String> response = FunctionalTestHelper.post(
                URL_PREFIX + port + "/partners/1/fulfillments",
                new HttpEntity<>(
                        // language=xml
                        "<partner-fulfillment-link service-id=\"777\" />",
                        headers)
        );

        verify(ff4ShopsClient, times(1)).updatePartnerState(
                FF4ShopsPartnerState.newBuilder()
                        .withPartnerId(1L)
                        .withBusinessId(300L)
                        .withFeatureType(FeatureType.DROPSHIP)
                        .withFeatureStatus(ParamCheckStatus.DONT_WANT)
                        .withCpaIsPartnerInterface(true)
                        .withFulfillmentLinks(Collections.singletonList(
                                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                                        .withServiceId(777L)
                                        .withFeedId(1L)
                                        .withDeliveryServiceType(DeliveryServiceType.DROPSHIP)
                                        .build()))
                        .withPushStocksIsEnabled(false)
                        .build()
        );
        verify(lmsClient, times(1)).getBusinessWarehouseForPartner(777L);
        verify(lmsClient, times(2)).getPartner(777L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("<partner-fulfillment-link partner-id=\"1\" " +
                "service-id=\"777\" feed-id=\"1\" service-type=\"dropship\"/>", response.getBody());

        checkLb(1, 777, LinkUpdateType.UPDATE_TYPE_UPDATE);
    }

    @Test
    @DisplayName("Добавление фф склада дропшип партнеру, у которого есть фф склад")
    @DbUnitDataSet(after = "PartnerFulfillmentControllerTestAddFF.after.csv")
    void createPartnerFulfillmentLink() {
        var responseBuilder
                = EnhancedRandom.random(BusinessWarehouseResponse.Builder.class);
        responseBuilder.marketId(MARKET_ID);
        responseBuilder.partnerId(144L);
        var getPartnerResp = responseBuilder.build();

        when(lmsClient.getBusinessWarehouseForPartner(144L)).thenReturn(Optional.of(getPartnerResp));

        doAnswer(invocation -> {
                    throw new RuntimeException("retry!");
                })
                .doAnswer(invocation -> null).when(ff4ShopsClient).updatePartnerState(any());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        ResponseEntity<String> response = FunctionalTestHelper.post(
                URL_PREFIX + port + "/partners/1/fulfillments",
                new HttpEntity<>(
                        // language=xml
                        "<partner-fulfillment-link service-id=\"144\" />",
                        headers)
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("<partner-fulfillment-link partner-id=\"1\" " +
                "service-id=\"144\" feed-id=\"1\" service-type=\"fulfillment\"/>", response.getBody());

        //Т.к. прикрепление ФФ через ручку отключено, addWarehouse не должна вызваться
        verify(dataCampShopClient, never()).addWarehouse(anyLong(), anyLong(), anyLong(), any());
        checkLb(1, 144, LinkUpdateType.UPDATE_TYPE_UPDATE);
    }

    @Test
    @DisplayName("Добавление фф склада дропшип партнеру, у которого нет фф склада")
    @DbUnitDataSet(after = "PartnerFulfillmentControllerTestAddNewFF.after.csv")
    void createNewPartnerFulfillmentLink() {
        var responseBuilder
                = EnhancedRandom.random(BusinessWarehouseResponse.Builder.class);
        responseBuilder.marketId(MARKET_ID);
        responseBuilder.partnerId(144L);
        var getPartnerResp = responseBuilder.build();

        when(lmsClient.getBusinessWarehouseForPartner(144L)).thenReturn(Optional.of(getPartnerResp));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        ResponseEntity<String> response = FunctionalTestHelper.post(
                URL_PREFIX + port + "/partners/104/fulfillments",
                new HttpEntity<>(
                        // language=xml
                        "<partner-fulfillment-link service-id=\"144\" />",
                        headers)
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("<partner-fulfillment-link partner-id=\"104\" " +
                "service-id=\"144\" feed-id=\"1\" service-type=\"fulfillment\"/>", response.getBody());
        verify(ff4ShopsClient, times(1)).updatePartnerState(any());
        checkLb(104, 144, LinkUpdateType.UPDATE_TYPE_UPDATE);
    }

    @Test
    @DisplayName("Ошибка при привязке дропшип поставщика к кроссдок складу")
    void dropshipPartnerToCrossdockFulfillmentLink() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePartnerFulfillmentLink(104L, 779L)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        verify(ff4ShopsClient, times(0)).updatePartnerState(any());
        verify(bpmnClientService, never()).startProcess(any());
    }

    @Test
    @DisplayName("Ошибка при привязке кроссдок поставщика к дропшип складу")
    void crossdockPartnerToDropshipFulfillmentLink() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePartnerFulfillmentLink(105L, 777L)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        verify(ff4ShopsClient, times(0)).updatePartnerState(any());
        verify(bpmnClientService, never()).startProcess(any());
    }

    @Test
    @DisplayName("Ошибка отсутствия склада в LMS")
    void shopPartnerToAnyFulfillmentLink() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePartnerFulfillmentLink(107L, 143L)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        verify(ff4ShopsClient, times(0)).updatePartnerState(any());
        verify(bpmnClientService, never()).startProcess(any());
    }

    @Test
    @DisplayName("Ошибка при привязке любого поставщика к carrier складу")
    void anyPartnerToCarrierFulfillmentLink() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePartnerFulfillmentLink(104L, 146L)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        verify(ff4ShopsClient, times(0)).updatePartnerState(any());
        verify(lmsClient, times(1)).getBusinessWarehouseForPartner(anyLong());
        verify(bpmnClientService, never()).startProcess(any());
    }

    @Test
    @DisplayName("Добавление дропшип склада дропшип партнеру, у которых разные market id")
    void linkSupplierToFfServiceWithDifferentMarketId() {
        var responseBuilder
                = EnhancedRandom.random(BusinessWarehouseResponse.Builder.class);
        responseBuilder.marketId(MARKET_ID + 1);
        responseBuilder.partnerId(777L);
        var getPartnerResp = responseBuilder.build();

        when(lmsClient.getBusinessWarehouseForPartner(777L)).thenReturn(Optional.of(getPartnerResp));

        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePartnerFulfillmentLink(1L, 777L)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        verify(ff4ShopsClient, times(0)).updatePartnerState(any());
        verify(lmsClient, times(1)).getBusinessWarehouseForPartner(anyLong());
        verify(bpmnClientService, never()).startProcess(any());
    }

    @Test
    @DisplayName("Связь между дропшип поставщиком и дропшип складом не меняется если она уже существует")
    void updatePartnerFulfillmentLink() {
        var responseBuilder
                = EnhancedRandom.random(BusinessWarehouseResponse.Builder.class);
        responseBuilder.marketId(MARKET_ID);
        responseBuilder.partnerId(778L);
        var getPartnerResp = responseBuilder.build();

        when(lmsClient.getBusinessWarehouseForPartner(778L)).thenReturn(Optional.of(getPartnerResp));


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        ResponseEntity<String> response = FunctionalTestHelper.post(
                URL_PREFIX + port + "/partners/104/fulfillments",
                new HttpEntity<>(
                        // language=xml
                        "<partner-fulfillment-link service-id=\"778\" />",
                        headers)
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "<partner-fulfillment-link partner-id=\"104\" service-id=\"778\" feed-id=\"14\" " +
                        "service-type=\"dropship\"/>", response.getBody());
        verify(ff4ShopsClient, times(1)).updatePartnerState(
                FF4ShopsPartnerState.newBuilder()
                        .withPartnerId(104L)
                        .withBusinessId(300L)
                        .withFeatureType(FeatureType.DROPSHIP)
                        .withFeatureStatus(ParamCheckStatus.SUCCESS)
                        .withCpaIsPartnerInterface(false)
                        .withFulfillmentLinks(Collections.singletonList(
                                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                                        .withServiceId(778L)
                                        .withFeedId(14L)
                                        .withDeliveryServiceType(DeliveryServiceType.DROPSHIP)
                                        .build()))
                        .withPushStocksIsEnabled(false)
                        .build()
        );
        verify(bpmnClientService, never()).startProcess(any());
        checkLb(104, 778, LinkUpdateType.UPDATE_TYPE_UPDATE);
    }

    @Test
    @DisplayName("Добавление новой связи партнёра со складом ФФ через mbi-api-client")
    void updatePartnerFulfillmentLinkByMbiApiClient() {
        var responseBuilder
                = EnhancedRandom.random(BusinessWarehouseResponse.Builder.class);
        responseBuilder.marketId(MARKET_ID);
        responseBuilder.partnerId(777L);
        var getPartnerResp = responseBuilder.build();

        when(lmsClient.getBusinessWarehouseForPartner(777L)).thenReturn(Optional.of(getPartnerResp));

        assertEquals(
                new PartnerFulfillmentLinkDTO(1L, 777L, 1L, DeliveryServiceType.DROPSHIP),
                mbiApiClient.updatePartnerFulfillmentLink(1L, 777L));
        verify(ff4ShopsClient, times(1)).updatePartnerState(
                FF4ShopsPartnerState.newBuilder()
                        .withPartnerId(1L)
                        .withBusinessId(300L)
                        .withFeatureType(FeatureType.DROPSHIP)
                        .withFeatureStatus(ParamCheckStatus.DONT_WANT)
                        .withCpaIsPartnerInterface(true)
                        .withFulfillmentLinks(Collections.singletonList(
                                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                                        .withServiceId(777L)
                                        .withFeedId(1L)
                                        .withDeliveryServiceType(DeliveryServiceType.DROPSHIP)
                                        .build()))
                        .withPushStocksIsEnabled(false)
                        .build()
        );
        verify(bpmnClientService, never()).startProcess(any());
        checkLb(1, 777, LinkUpdateType.UPDATE_TYPE_UPDATE);
    }

    @Test
    @DisplayName("Добавление новой связи ДСБС партнёра со складом ФФ через mbi-api-client")
    void updateDBSBFulfillmentLinkByMbiApiClient() {
        var getPartnerResp = EnhancedRandom.random(BusinessWarehouseResponse.Builder.class)
                .marketId(null)
                .partnerId(1000L)
                .partnerType(PartnerType.DROPSHIP_BY_SELLER)
                .build();

        when(lmsClient.getBusinessWarehouseForPartner(1000L)).thenReturn(Optional.of(getPartnerResp));

        assertEquals(
                new PartnerFulfillmentLinkDTO(109L, 1000L, 124L, DeliveryServiceType.DROPSHIP_BY_SELLER),
                mbiApiClient.updatePartnerFulfillmentLink(109L, 1000L));
        verify(bpmnClientService, never()).startProcess(any());
        verify(asyncTarifficatorService).syncShopMetaData(eq(109L), eq(ActionType.UPDATE_FF_SERVICE_LINK));
        checkLb(109, 1000, LinkUpdateType.UPDATE_TYPE_UPDATE);
    }

    @Test
    @DisplayName("Добавление новой связи ДСБС партнёра со складом ФФ через mbi-api-client. Дефолтный фид")
    void updateDBSBFulfillmentLinkDefaultFeed() {
        var getPartnerResp = EnhancedRandom.random(BusinessWarehouseResponse.Builder.class)
                .marketId(null)
                .partnerId(1000L)
                .partnerType(PartnerType.DROPSHIP_BY_SELLER)
                .build();

        when(lmsClient.getBusinessWarehouseForPartner(1000L)).thenReturn(Optional.of(getPartnerResp));

        assertEquals(
                new PartnerFulfillmentLinkDTO(110L, 1000L, 125L, DeliveryServiceType.DROPSHIP_BY_SELLER),
                mbiApiClient.updatePartnerFulfillmentLink(110L, 1000L));
        verify(bpmnClientService, never()).startProcess(any());
        checkLb(110, 1000, LinkUpdateType.UPDATE_TYPE_UPDATE);
    }

    @Test
    @DisplayName("Привязать к неимпортированному складу, дозабрать склад через LMS, создать вторую связь со складом")
    @DbUnitDataSet(after = "PartnerFulfillmentControllerTestNewLmsFF.after.csv")
    void updatePartnerFulfillmentLinkWithNotImportedFulfilment() {
        var response = BusinessWarehouseResponse.newBuilder()
                .partnerId(791L)
                .partnerType(PartnerType.DROPSHIP)
                .marketId(MARKET_ID)
                .name("Imported dropship ff")
                .readableName("Imported dropship readable ff")
                .partnerStatus(PartnerStatus.INACTIVE)
                .build();
        var response791 = PartnerResponse.newBuilder()
                .id(791L)
                .partnerType(PartnerType.DROPSHIP)
                .marketId(MARKET_ID)
                .name("Imported dropship ff")
                .readableName("Imported dropship readable ff")
                .status(PartnerStatus.INACTIVE)
                .build();

        when(lmsClient.getBusinessWarehouseForPartner(791L)).thenReturn(Optional.of(response));
        when(lmsClient.getPartner(791L)).thenReturn(Optional.of(response791));

        assertEquals(new PartnerFulfillmentLinkDTO(104L, 791L, 14L, null),
                mbiApiClient.updatePartnerFulfillmentLink(104L, 791L));

        verify(lmsClient).getBusinessWarehouseForPartner(791L);
        verify(lmsClient).getPartner(791L);
        // проверяем, что включаем новый склад в LMS
        verify(lmsClient).changePartnerStatus(791L, PartnerStatus.ACTIVE);

        verify(partnerFfLinkLbProducer).publishEventAsync(any());

        verifyNoMoreInteractions(lmsClient);
        checkLb(104, 791, LinkUpdateType.UPDATE_TYPE_UPDATE);
    }

    @Test
    @DisplayName("Привязать к неимпортированному складу, дозабрать склад через LMS, создать связь со складом (для Еды/Лавки)")
    @DbUnitDataSet(before = "PartnerFulfillmentControllerTestNewLmsEat.before.csv", after = "PartnerFulfillmentControllerTestNewLmsEat.after.csv")
    void updatePartnerFulfillmentLinkEats() {
        //add
        BusinessWarehouseResponse lmsResponse = BusinessWarehouseResponse.newBuilder()
                .partnerId(792L)
                .partnerType(PartnerType.RETAIL)
                .marketId(MARKET_ID)
                .name("warehouse eats_and_lavka")
                .readableName("warehouse eats_and_lavka readable")
                .partnerStatus(PartnerStatus.INACTIVE)
                .build();

        when(lmsClient.getBusinessWarehouseForPartner(792L)).thenReturn(Optional.of(lmsResponse));

        assertEquals(
                new PartnerFulfillmentLinkDTO(210L, 792L, null, DeliveryServiceType.RETAIL),
                mbiApiClient.updatePartnerFulfillmentLink(210L, 792L));

        verify(lmsClient).getBusinessWarehouseForPartner(anyLong());
        verifyNoMoreInteractions(ff4ShopsClient);
        verifyNoMoreInteractions(lmsClient);
        //update
        BusinessWarehouseResponse lmsResponse2 = BusinessWarehouseResponse.newBuilder()
                .partnerId(793L)
                .partnerType(PartnerType.RETAIL)
                .marketId(MARKET_ID)
                .name("warehouse eats_and_lavka")
                .readableName("warehouse eats_and_lavka editable")
                .partnerStatus(PartnerStatus.INACTIVE)
                .build();

        when(lmsClient.getBusinessWarehouseForPartner(793L)).thenReturn(Optional.of(lmsResponse2));

        assertEquals(
                new PartnerFulfillmentLinkDTO(211L, 793L, null, DeliveryServiceType.RETAIL),
                mbiApiClient.updatePartnerFulfillmentLink(211L, 793L));

        verify(lmsClient).getBusinessWarehouseForPartner(anyLong());
        verifyNoMoreInteractions(ff4ShopsClient);
        verifyNoMoreInteractions(lmsClient);
        verify(partnerFfLinkLbProducer, times(2)).publishEventAsync(any());
    }

    @Test
    @DisplayName("Ошибка при привязке к несуществующему в LMS складу")
    void updatePartnerFulfillmentLinkWithNotExistentFulfilment() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.updatePartnerFulfillmentLink(105L, 404L)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        verify(lmsClient, times(1)).getBusinessWarehouseForPartner(anyLong());
        verify(bpmnClientService, never()).startProcess(any());
    }

    @Test
    @DisplayName("Удаление связи партнёра со складом ФФ")
    @DbUnitDataSet(after = "PartnerFulfillmentControllerTest.delete.csv")
    void deletePartnerFulfillmentLink() {
        mbiApiClient.deletePartnerFulfillmentLinks(3, Set.of(555L));
        verify(dataCampShopClient).removeWarehouse(3, 555, 200);
        verifyNoMoreInteractions(lmsClient, dataCampShopClient);
        checkLb(3, 555, LinkUpdateType.UPDATE_TYPE_DELETE);
    }

    @Test
    @DisplayName("Отправка велком уведомления в почту 1629294066 при создании первого экспресс склада")
    @DbUnitDataSet(after = "PartnerFulfillmentControllerTestAddNewExpress.after.csv")
    void welcomeNotificationAfterCreateFirstLinkForExpress() {
        var responseBuilder
                = EnhancedRandom.random(BusinessWarehouseResponse.Builder.class);
        responseBuilder.marketId(MARKET_ID);
        responseBuilder.partnerId(155L);
        var getPartnerResp = responseBuilder.build();

        when(lmsClient.getBusinessWarehouseForPartner(155L)).thenReturn(Optional.of(getPartnerResp));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        ResponseEntity<String> response = FunctionalTestHelper.post(
                URL_PREFIX + port + "/partners/106/fulfillments",
                new HttpEntity<>(
                        // language=xml
                        "<partner-fulfillment-link service-id=\"155\" />",
                        headers)
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("<partner-fulfillment-link partner-id=\"106\" " +
                "service-id=\"155\" feed-id=\"1006\" service-type=\"dropship\"/>", response.getBody());
        verify(ff4ShopsClient, times(1)).updatePartnerState(any());
        checkLb(106, 155, LinkUpdateType.UPDATE_TYPE_UPDATE);
        var reqCaptor = verifySentNotificationType(partnerNotificationClient, 1, 1629294066L);
        assertEquals(106L, reqCaptor.getValue().getDestination().getShopId());
    }

    @Test
    @DisplayName("При удалении экспрес склада не отправляем велком уведомления в почту 1629294066")
    @DbUnitDataSet(after = "PartnerFulfillmentControllerTestDeleteExpress.after.csv")
    void welcomeNotificationAfterDeleteLinkForExpress() {
        var responseBuilder
                = EnhancedRandom.random(BusinessWarehouseResponse.Builder.class);
        responseBuilder.marketId(MARKET_ID);
        responseBuilder.partnerId(155L);
        var getPartnerResp = responseBuilder.build();

        when(lmsClient.getBusinessWarehouseForPartner(155L)).thenReturn(Optional.of(getPartnerResp));

        FunctionalTestHelper.delete(
                URL_PREFIX + port + "/partners/106/fulfillments?serviceId=155"
        );
        verifyNoInteractions(partnerNotificationClient);
    }

    private void checkLb(long partnerId, long serviceId, LinkUpdateType changeType) {
        var captor = ArgumentCaptor.forClass(PartnerServiceLinkLogbrokerEvent.class);
        verify(partnerFfLinkLbProducer).publishEventAsync(captor.capture());

        var expected = PartnerServiceLinkOuterClass.PartnerServiceLink.newBuilder()
                .setPartnerId(partnerId)
                .setServiceId(serviceId)
                .setUpdateType(changeType)
                .build();

        var actual = captor.getValue().getPayload();
        ProtoTestUtil.assertThat(actual)
                .ignoringFieldsMatchingRegexes(".*updatedAt.*", ".*feedId.*")
                .isEqualTo(expected);
    }

}
