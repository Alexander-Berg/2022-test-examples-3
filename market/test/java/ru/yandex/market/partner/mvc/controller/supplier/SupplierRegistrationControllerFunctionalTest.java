package ru.yandex.market.partner.mvc.controller.supplier;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.mockito.MemCachedClientFactoryMock;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.agency.AgencyService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.id.service.MarketIdGrpcService;
import ru.yandex.market.core.state.event.PartnerChangesProtoLBEvent;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.event.BaseLogbrokerEvent;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.data.PartnerDataOuterClass;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.JsonTestUtil.getJsonHttpEntity;
import static ru.yandex.market.core.supplier.registration.SupplierRegistrationService.ENV_CAMPAIGNS_LIMIT;
import static ru.yandex.market.partner.util.FunctionalTestHelper.post;

/**
 * Функциональные тесты на {@link ru.yandex.market.partner.mvc.controller.supplier.SupplierRegistrationController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "SupplierRegistrationControllerFunctionalTest.csv")
class SupplierRegistrationControllerFunctionalTest extends AbstractSupplierFunctionalTest {
    @Autowired
    private AgencyService agencyService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private MarketIdGrpcService marketIdGrpcService;

    @Autowired
    private MemCachedClientFactoryMock memCachedClientFactoryMock;

    @Autowired
    private LogbrokerEventPublisher<PartnerChangesProtoLBEvent> logbrokerPartnerChangesEventPublisher;

    @BeforeEach
    void setUp() {
        when(logbrokerPartnerChangesEventPublisher.publishEventAsync(any(PartnerChangesProtoLBEvent.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
    }

    @Test
    void registerSupplierForNonExistingShop() {
        // given
        var uid = 100504L;
        var campaignId = 20201001L; // some non-existent
        //language=json
        var requestBody = "" +
                "{" +
                "    \"dropship\": false," +
                "    \"crossdock\": false" +
                "}";
        //language=json
        var responseError = "" +
                "{" +
                "   \"code\": \"BAD_PARAM\"," +
                "   \"details\": {" +
                "       \"entity_name\": \"campaign\"," +
                "       \"entity_id\": \"20201001\"," +
                "       \"subcode\": \"ENTITY_NOT_FOUND\"" +
                "   }" +
                "}";

        // when-then
        assertResponsesWithErrors(
                () -> post(suppliersFromShop(campaignId, uid), getJsonHttpEntity(requestBody)),
                HttpStatus.NOT_FOUND,
                responseError
        );
        assertCachesRemainAlmostEmpty();
    }

    @Test
    void registerSupplierForExistingNonShop() {
        // given
        var uid = 100504L;
        var campaignId = 10777L; // exists, but not a shop
        //language=json
        var requestBody = "" +
                "{" +
                "    \"dropship\": false," +
                "    \"crossdock\": false" +
                "}";
        //language=json
        var responseError = "" +
                "{" +
                "   \"code\": \"UNAUTHORIZED\"," +
                "   \"details\": {" +
                "       \"reason\": \"not a shop\"," +
                "       \"subcode\": \"FORBIDDEN_OPERATION\"" +
                "   }" +
                "}";

        // when-then
        assertResponsesWithErrors(
                () -> post(suppliersFromShop(campaignId, uid), getJsonHttpEntity(requestBody)),
                HttpStatus.FORBIDDEN,
                responseError
        );
    }

    @Test
    @DbUnitDataSet(after = "SupplierRegistrationControllerFunctionalTest.registerSupplierForExistingShop.after.csv")
    void registerSupplierForExistingShop() {
        var partnerEventsCaptor = ArgumentCaptor.forClass(PartnerChangesProtoLBEvent.class);
        // given
        var uid = 100505L;
        var campaignId = 199L; // campaign for existing shop
        when(agencyService.isAgency(anyLong()))
                .thenReturn(false); // more representative test
        when(balanceService.getClient(anyLong()))
                .thenAnswer(invocation -> new ClientInfo(invocation.getArgument(0), ClientType.OOO));
        environmentService.setValue(ENV_CAMPAIGNS_LIMIT, "100"); // there's already one campaign
        doReturn(Optional.of(MarketAccount.newBuilder()
                .setMarketId(12345L)
                .setLegalInfo(LegalInfo.newBuilder()
                        .setInn("INN")
                        .setLegalAddress("LA")
                        .setLegalName("Yandex")
                        .setPhysicalAddress("PA")
                        .setRegistrationNumber("OGRN")
                        .setType("OOO")
                        .build()
                )
                .build()
        )).when(marketIdGrpcService).findByPartner(anyLong(), any(CampaignType.class)); // this is spy, not mock

        //language=json
        var requestBody = "" +
                "{" +
                "    \"dropship\": false," +
                "    \"crossdock\": false" +
                "}";

        //language=json
        var responseBody = "" +
                "{" +
                "   \"campaignId\": 1," +
                "   \"datasourceId\": 1," +
                "   \"status\": \"7\"," +
                "   \"domain\": \"ya.ru\"," +
                "   \"name\": \"Yandex FBY\"," +
                "   \"businessId\": 100" +
                "}";

        // when
        var response = post(suppliersFromShop(campaignId, uid), getJsonHttpEntity(requestBody));

        // then
        JsonTestUtil.assertEquals(response, responseBody);

        verify(logbrokerPartnerChangesEventPublisher, atLeastOnce()).publishEventAsync(partnerEventsCaptor.capture());
        List<PartnerChangesProtoLBEvent> partnerChangesToLB = partnerEventsCaptor.getAllValues();
        partnerChangesToLB.stream().map(PartnerChangesProtoLBEvent::getPayload)
                .filter(payload -> payload.getPartnerId() == 1L)
                .forEach(payload -> {
                            Assertions.assertEquals(PartnerDataOuterClass.PartnerType.SUPPLIER, payload.getType());
                            Assertions.assertEquals("Yandex FBY", payload.getInternalName());
                            Assertions.assertEquals(1L, payload.getCampaignId());
                        }
                );
        Assertions.assertTrue(partnerChangesToLB.stream()
                .map(BaseLogbrokerEvent::getPayload)
                .filter(payload -> payload.getPartnerId() == 1L)
                .filter(payload -> payload.getPlacementProgramsCount() > 0)
                .anyMatch(payload ->
                        payload.getPlacementPrograms(0) == PartnerDataOuterClass.PlacementProgramType.FULFILLMENT));
        assertThat(partnerChangesToLB.stream()
                .map(PartnerChangesProtoLBEvent::getPayload)
                .map(PartnerDataOuterClass.PartnerData::getGeneralInfo)
                .map(GeneralData.GeneralDataInfo::getActionType)
                .collect(Collectors.toList())).contains(GeneralData.ActionType.CREATE, GeneralData.ActionType.UPDATE);
    }

    @Test
    @DbUnitDataSet(before = "testSupplierRegistrationOnSameContact.before.csv",
            after = "testSupplierRegistrationOnSameContact.after.csv")
    void testRegistrationSuccessOnBrokenContact() {
        var uid = 100505L;
        var campaignId = 199L;
        when(agencyService.isAgency(anyLong()))
                .thenReturn(false);
        when(balanceService.getClient(anyLong()))
                .thenAnswer(invocation -> new ClientInfo(invocation.getArgument(0), ClientType.OOO));
        environmentService.setValue(ENV_CAMPAIGNS_LIMIT, "100");
        doReturn(Optional.of(MarketAccount.newBuilder()
                .setMarketId(12345L)
                .setLegalInfo(LegalInfo.newBuilder()
                        .setInn("INN")
                        .setLegalAddress("LA")
                        .setLegalName("Yandex")
                        .setPhysicalAddress("PA")
                        .setRegistrationNumber("OGRN")
                        .setType("OOO")
                        .build()
                )
                .build()
        )).when(marketIdGrpcService).findByPartner(anyLong(), any(CampaignType.class)); // this is spy, not mock

        //language=json
        var requestBody = "" +
                "{" +
                "    \"dropship\": false," +
                "    \"crossdock\": false" +
                "}";
        post(suppliersFromShop(campaignId, uid), getJsonHttpEntity(requestBody));
    }

    String suppliersFromShop(long campaignId, long uid) {
        return baseUrl + "/suppliers/from-shop?euid=" + uid + "&campaign_id=" + campaignId;
    }

    private void assertCachesRemainAlmostEmpty() {
        assertThat(memCachedClientFactoryMock.getCaches().values())
                .as("cache should be written only after successful tx commit")
                .allSatisfy(cache ->
                        // при ошибках регистрации в кешах должно быть либо пусто,
                        // либо только подгруженные данные для первоначальных проверок
                        assertThat(cache).satisfiesAnyOf(
                                cacheEmpty -> assertThat(cacheEmpty).isEmpty(),
                                cacheAlmostEmpty -> assertThat(cacheAlmostEmpty).containsOnlyKeys("AS_agencies")
                        )
                );
    }

    private SupplierRegistrationDTO createSupplierRegistrationDTO() {
        SupplierRegistrationDTO supplierRegistrationDTO = new SupplierRegistrationDTO();
        supplierRegistrationDTO.setDomain("testDomain.com");
        supplierRegistrationDTO.setName("testName");
        supplierRegistrationDTO.setInternalName("testName");
        NotificationContactDTO notificationContactDTO = new NotificationContactDTO();
        notificationContactDTO.setFirstName("First");
        notificationContactDTO.setSecondName("Second");
        notificationContactDTO.setLastName("Last");
        notificationContactDTO.setEmail("some@mail.com");
        notificationContactDTO.setPhone("+78889303367");
        notificationContactDTO.setAdvAgree(false);
        supplierRegistrationDTO.setNotificationContact(notificationContactDTO);
        return supplierRegistrationDTO;
    }
}
