package ru.yandex.market.partner.mvc.controller.partner.replication;

import java.util.List;

import Market.DataCamp.DataCampUnitedOffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.model.search.filter.ResultContentStatus;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * Тесты для {@link PartnerReplicationController}.
 */
@DbUnitDataSet(before = "PartnerReplicationControllerTest.before.csv")
class PartnerReplicationControllerTest extends FunctionalTest {

    @Autowired
    private SaasService saasService;

    @Autowired
    @Qualifier("environmentService")
    private EnvironmentService environmentService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private FF4ShopsClient ff4ShopsClient;

    @Test
    void testNotFoundAcceptor() {
        var e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(
                        baseUrl + "/partner/replication/last-acceptor/offers?campaign_id={id}&placement_type" +
                                "={FULFILLMENT}",
                        20, "FULFILLMENT"
                )
        );
        Assertions.assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void incorrectDonorPartnerProgramStatus() {
        Assertions.assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.post(
                        baseUrl + "/partner/replication/registration?campaignId=30&_user_id=20",
                        new RegisterForReplicationRequest(PartnerPlacementProgramType.DROPSHIP, "whname")
                ));
    }

    @Test
    void testGetAcceptorOffersCount() {
        Mockito.when(saasService.searchBusinessOffers(Mockito.any()))
                .thenReturn(SaasSearchResult.builder()
                        .setTotalCount(10)
                        .build()
                );

        //language=json
        var expected = "" +
                "{\n" +
                "  \"acceptorOffersCount\": 10,\n" +
                "  \"donorOffersCount\": 10,\n" +
                "  \"readyForSupplyAcceptorOffersCount\": 10,\n" +
                "  \"problemAcceptorOffersCount\": 0\n" +
                "}";
        var response = FunctionalTestHelper.get(
                baseUrl + "/partner/replication/last-acceptor/offers?campaign_id={id}&placement_type={FULFILLMENT}",
                10, "FULFILLMENT"
        );
        JsonTestUtil.assertEquals(response, expected);
    }

    /**
     * Проверяет lite-репликацию FBS бизнес-админом.
     */
    @Test
    @DbUnitDataSet(after = {"testRegisterForReplication.after.csv",
            "testRegisterForReplication.businessAdmin.after.csv"})
    void testRegisterForReplication() {
        RegisterForReplicationRequest request = new RegisterForReplicationRequest(
                PartnerPlacementProgramType.DROPSHIP, "Москва");
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/replication/registration?campaignId=10&_user_id=20", request);
        JsonTestUtil.assertEquals(response, "{\"campaignId\": 1, \"partnerId\": 1}");
    }

    /**
     * Проверяет lite-репликацию DBS->FBS бизнес-админом.
     */
    @Test
    @DbUnitDataSet(after = {"testRegisterDbsForReplication.after.csv",
            "testRegisterForReplication.businessAdmin.after.csv"})
    void testRegisterDbsForReplication() {
        RegisterForReplicationRequest request = new RegisterForReplicationRequest(
                PartnerPlacementProgramType.DROPSHIP, "Москва");
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/replication/registration?campaignId=50&_user_id=30", request);
        JsonTestUtil.assertEquals(response, "{\"campaignId\": 1, \"partnerId\": 1}");
    }

    /**
     * Проверяет lite-репликацию FBS shop-админом.
     */
    @Test
    @DbUnitDataSet(after = {"testRegisterForReplication.after.csv",
            "testRegisterForReplication.shopAdmin.after.csv"})
    void testShopAdminRegisterForReplication() {
        RegisterForReplicationRequest request = new RegisterForReplicationRequest(
                PartnerPlacementProgramType.DROPSHIP, "Москва");
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/replication/registration?campaignId=10&_user_id=10", request);
        JsonTestUtil.assertEquals(response, "{\"campaignId\": 1, \"partnerId\": 1}");
    }

    @Test
    void testGetLastAcceptor() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/partner/replication/last-acceptor?campaignId={id}&placement_type={FULFILLMENT}",
                10, "FULFILLMENT"
        );
        //language=json
        var expected = "{\"partnerId\":2,\"campaignId\":20}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testAcceptorNotFound() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/partner/replication/last-acceptor?campaignId={id}&placement_type={DROPSHIP}",
                10, "DROPSHIP"
        );
        //language=json
        var expected = "{\"partnerId\":0,\"campaignId\":0}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testGetAcceptorOffersCountDatacamp() {
        environmentService.setValue("PartnerReplicationController.use.datacamp", "true");

        Mockito.when(saasService.searchBusinessOffers(Mockito.any()))
                .thenReturn(SaasSearchResult.builder()
                        .setTotalCount(10)
                        .build()
                );

        DataCampUnitedOffer.UnitedOffer tarelkaUnitedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "testGetAcceptorOffersCountDatacamp.proto.json",
                getClass()
        );

        SearchBusinessOffersResult searchBusinessOffersResult = SearchBusinessOffersResult.builder()
                .setOffers(List.of(tarelkaUnitedOffer))
                .build();

        doReturn(searchBusinessOffersResult).when(dataCampShopClient).searchBusinessOffers(
                eq(SearchBusinessOffersRequest.builder()
                        .setPartnerId(2L)
                        .setBusinessId(3L)
                        .setPageRequest(SeekSliceRequest.firstN(1000))
                        .setScanLimit(30001)
                        .addResultContentStatuses(List.of(
                                ResultContentStatus.HAS_CARD_MARKET,
                                ResultContentStatus.HAS_CARD_PARTNER
                        ))
                        .setWithRetry(true)
                        .build()
                )
        );

        //language=json
        var expected = "" +
                "{\n" +
                "  \"acceptorOffersCount\": 10,\n" +
                "  \"donorOffersCount\": 10,\n" +
                "  \"readyForSupplyAcceptorOffersCount\": 1,\n" +
                "  \"problemAcceptorOffersCount\": 9\n" +
                "}";
        var response = FunctionalTestHelper.get(
                baseUrl + "/partner/replication/last-acceptor/offers?campaign_id={id}&placement_type={FULFILLMENT}",
                10, "FULFILLMENT"
        );
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(
            before = "testNoCpaIsPartnerInterfaceListener.before.csv",
            after = "testNoCpaIsPartnerInterfaceListener.after.csv"
    )
    void testNoInteractionsWithCpaIsPartnerInterfaceListenerWhileReplicatingFbsToFbs() {
        RegisterForReplicationRequest request = new RegisterForReplicationRequest(
                PartnerPlacementProgramType.DROPSHIP, "Москва");
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/replication/registration?campaignId=10&_user_id=20", request);
        JsonTestUtil.assertEquals(response, "{\"campaignId\": 1, \"partnerId\": 1}");

        Mockito.verifyNoInteractions(checkouterAPI, lmsClient, ff4ShopsClient);
    }
}
