package ru.yandex.market.adv.promo.tms.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.logbroker.model.DatacampMessageLogbrokerEvent;
import ru.yandex.market.adv.promo.tms.command.dao.PromoYTDao;
import ru.yandex.market.adv.promo.tms.command.model.PromoInfo;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerBusinessDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnersBusinessResponse;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.adv.promo.tms.command.ClearCashbackOffersCommand.COMMAND_NAME;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createBasicOffer;

public class ClearCashbackOffersCommandTest extends FunctionalTest {
    @Autowired
    private ClearCashbackOffersCommand command;

    @Autowired
    private Terminal terminal;
    private StringWriter terminalWriter;

    @Autowired
    private PromoYTDao promoYTDao;

    @Autowired
    private DataCampClient dataCampClient;

    @Autowired
    private SaasService saasDataCampShopService;

    @Autowired
    private LogbrokerEventPublisher<DatacampMessageLogbrokerEvent> logbrokerService;

    @Autowired
    private MbiApiClient mbiApiClient;

    @BeforeEach
    public void setUpConfigurations() {
        terminalWriter = new StringWriter();

        when(terminal.getWriter()).thenReturn(spy(new PrintWriter(terminalWriter)));
        when(terminal.confirm(anyString())).thenReturn(true);
    }

    @Test
    void clearOffersForDeletedCashbackTest() {
        long partnerId = 12345;
        String cashbackId = "12345_promo";
        String cashbackWithoutClearingId = "12345_cashback";

        doReturn(List.of(new PromoInfo(cashbackId, partnerId, partnerId)))
                .when(promoYTDao).getDeletedCashbackPromos(anyString());

        mockSaasResponse();
        mockBusinessRequest();
        mockOffersForClearing(cashbackId, cashbackWithoutClearingId);

        CommandInvocation commandInvocation =
                new CommandInvocation(COMMAND_NAME, new String[0], Collections.emptyMap());
        command.executeCommand(commandInvocation, terminal);

        //Проверка корректности чистки офферов
        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(logbrokerService, times(1)).publishEvent(eventCaptor.capture());
        DatacampMessageLogbrokerEvent event = eventCaptor.getValue();
        DatacampMessageOuterClass.DatacampMessage msg = event.getPayload();
        List<DataCampOffer.Offer> offers = msg.getOffersList().get(0).getOfferList();
        assertEquals(3, offers.size());
        Assertions.assertEquals("offer1", offers.get(0).getIdentifiers().getOfferId());
        checkOfferHasCashbackPromo(cashbackWithoutClearingId, offers.get(0));
        Assertions.assertEquals("offer2", offers.get(1).getIdentifiers().getOfferId());
        checkOfferHasCashbackPromo(cashbackWithoutClearingId, offers.get(1));
    }

    @Test
    @DbUnitDataSet(after = "ClearCashbackOffersCommandTest/processFinishedCashbackTest/after.csv")
    void processFinishedCashbackTest() {

        doReturn(
                List.of(
                        new PromoInfo("12345_promo", 12345, null)
                )
        )
                .when(promoYTDao).getAllCustomCashbackPromosInfo(anyString());

        mockSaasResponse();
        mockBusinessRequest();

        CommandInvocation commandInvocation =
                new CommandInvocation(COMMAND_NAME, new String[0], Collections.emptyMap());
        command.executeCommand(commandInvocation, terminal);
    }

    private void checkOfferHasCashbackPromo(String promoId, DataCampOffer.Offer offer) {
        assertTrue(offer.getPromos().hasPartnerCashbackPromos());
        assertFalse(offer.getPromos().hasAnaplanPromos());
        assertEquals(1, offer.getPromos().getPartnerCashbackPromos().getPromosCount());
        DataCampOfferPromos.Promo promo1 = offer.getPromos().getPartnerCashbackPromos().getPromos(0);
        Assertions.assertEquals(promoId, promo1.getId());
    }

    private void mockBusinessRequest() {
        doReturn(
                new PartnersBusinessResponse(
                        List.of(
                                new PartnerBusinessDTO(12345, 12345L)
                        )
                )
        ).when(mbiApiClient).getBusinessesForPartners(anyCollection());
    }

    private void mockOffersForClearing(
            String cashbackForClearingId,
            String cashbackWithoutClearingId
    ) {
        List<String> promoIds = Collections.emptyList();
        List<String> cashbackPromoIds = List.of(cashbackForClearingId, cashbackWithoutClearingId);

        DataCampOffer.Offer basicOffer1 = createBasicOffer("offer1", 11111, 100);
        DataCampOffer.Offer serviceOffer1 =
                createServiceOffer(basicOffer1, 100, 1, promoIds, cashbackPromoIds);
        DataCampOffer.Offer basicOffer2 = createBasicOffer("offer2", 11111, 100);
        DataCampOffer.Offer serviceOffer2 =
                createServiceOffer(basicOffer2, 100, 1, promoIds, cashbackPromoIds);
        DataCampOffer.Offer basicOffer3 = createBasicOffer("offer3", 11111, 100);
        DataCampOffer.Offer serviceOffer3 =
                createServiceOffer(basicOffer3, 100, 1, promoIds, cashbackPromoIds);

        OffersBatch.UnitedOffersBatchResponse unitedResponse = OffersBatch.UnitedOffersBatchResponse.newBuilder()
                .addEntries(OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .setUnitedOffer(DataCampUnitedOffer.UnitedOffer.newBuilder()
                                .setBasic(basicOffer1)
                                .putActual(1, DataCampUnitedOffer.ActualOffers.newBuilder()
                                        .putWarehouse(1, basicOffer1)
                                        .build())
                                .putService(1, serviceOffer1)
                                .build())
                        .build())
                .addEntries(OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .setUnitedOffer(DataCampUnitedOffer.UnitedOffer.newBuilder()
                                .setBasic(basicOffer2)
                                .putActual(1, DataCampUnitedOffer.ActualOffers.newBuilder()
                                        .putWarehouse(1, basicOffer2)
                                        .build())
                                .putService(1, serviceOffer2)
                                .build())
                        .build())
                .addEntries(OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .setUnitedOffer(DataCampUnitedOffer.UnitedOffer.newBuilder()
                                .setBasic(basicOffer3)
                                .putActual(1, DataCampUnitedOffer.ActualOffers.newBuilder()
                                        .putWarehouse(1, basicOffer3)
                                        .build())
                                .putService(1, serviceOffer3)
                                .build())
                        .build())
                .build();
        doReturn(unitedResponse).when(dataCampClient).getBusinessUnitedOffers(anyLong(), anyCollection(), any());
    }

    private DataCampOffer.Offer createServiceOffer(
            DataCampOffer.Offer basicOffer,
            long seconds,
            int nanos,
            List<String> promoIds,
            List<String> cashbackPromoIds
    ) {
        return DataCampOffer.Offer.newBuilder()
                .setIdentifiers(basicOffer.getIdentifiers())
                .setPromos(DataCampOfferPromos.OfferPromos.newBuilder()
                        .setPartnerPromos(DataCampOfferPromos.Promos.newBuilder()
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setTimestamp(Timestamp.newBuilder()
                                                .setSeconds(seconds)
                                                .setNanos(nanos)
                                                .build())
                                        .build())
                                .addAllPromos(promoIds.stream().map(promoId ->
                                        DataCampOfferPromos.Promo.newBuilder()
                                                .setId(promoId)
                                                .build()).collect(Collectors.toList())
                                )
                                .build())
                        .setPartnerCashbackPromos(
                                DataCampOfferPromos.Promos.newBuilder()
                                        .addAllPromos(cashbackPromoIds.stream().map(promoId ->
                                                DataCampOfferPromos.Promo.newBuilder()
                                                        .setId(promoId)
                                                        .build()).collect(Collectors.toList())
                                        )

                        )
                        .setAnaplanPromos(
                                DataCampOfferPromos.MarketPromos.newBuilder()
                                        .setAllPromos(DataCampOfferPromos.Promos.newBuilder()
                                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                        .setTimestamp(Timestamp.newBuilder()
                                                                .setSeconds(seconds)
                                                                .setNanos(nanos)
                                                                .build())
                                                        .build())
                                                .addAllPromos(List.of(
                                                        DataCampOfferPromos.Promo.newBuilder()
                                                                .setId("another_promo")
                                                                .build())
                                                )
                                                .build())
                                        .build()
                        )
                        .build())
                .build();

    }

    private void mockSaasResponse() {
        SaasOfferInfo saasOfferInfo1 = SaasOfferInfo.newBuilder()
                .addShopId(12345L)
                .addOfferId("offer1")
                .build();
        SaasOfferInfo saasOfferInfo2 = SaasOfferInfo.newBuilder()
                .addShopId(12345L)
                .addOfferId("offer2")
                .build();
        SaasOfferInfo saasOfferInfo3 = SaasOfferInfo.newBuilder()
                .addShopId(12345L)
                .addOfferId("offer3")
                .build();
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(List.of(saasOfferInfo1, saasOfferInfo2, saasOfferInfo3))
                .setTotalCount(1)
                .build();
        doReturn(saasSearchResult)
                .doReturn(
                        SaasSearchResult.builder()
                                .setOffers(Collections.emptyList())
                                .setTotalCount(0)
                                .build()
                )
                .when(saasDataCampShopService).searchBusinessOffers(any());
    }
}
