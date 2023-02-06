package ru.yandex.market.core.supplier.registration;

import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.abo.api.client.AboAPI;
import ru.yandex.market.api.cpa.CPADataPusher;
import ru.yandex.market.api.cpa.CpaIsPartnerInterfaceSyncService;
import ru.yandex.market.api.cpa.yam.dto.OrganizationInfoDTO;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestService;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.agency.AgencyService;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.bpmn.BpmnOperationService;
import ru.yandex.market.core.business.PartnerToBusinessConverter;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.client.remove.RemoveClientEnvironmentService;
import ru.yandex.market.core.contact.ContactLinkService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.contact.model.ContactEmail;
import ru.yandex.market.core.contact.model.ContactWithEmail;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feed.supplier.SupplierFeedService;
import ru.yandex.market.core.geobase.model.RegionConstants;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;
import ru.yandex.market.core.param.model.UnitedCatalogStatus;
import ru.yandex.market.core.partner.PartnerRegistrationHelper;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.registration.PartnerActivationService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.supplier.MemCachedSupplierService;
import ru.yandex.market.core.supplier.SupplierBasicAttributes;
import ru.yandex.market.core.supplier.SupplierState;
import ru.yandex.market.core.supplier.model.AgencySupplierRegistrationException;
import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.model.*;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.campaign.exception.TooManyCampaignsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Юнит тесты на {@link SupplierRegistrationService}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "../SupplierServiceTest.before.csv")
@ExtendWith(MockitoExtension.class)
class SupplierRegistrationServiceTest extends FunctionalTest {
    private static final long CONTACT_ID = 1L;
    private static final Collection<ParamType> GOOD_CONTENT_PARAM_TYPES = List.of(
            ParamType.GOOD_CONTENT_ALLOWED,
            ParamType.TAXONOMY_MAPPING_ALLOWED,
            ParamType.NEED_CONTENT_ALLOWED
    );
    private static final Long CLIENT_ID = 999L;

    @Autowired
    private PrepayRequestService prepayRequestService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private ContactLinkService contactLinkService;

    @Autowired
    private PartnerActivationService partnerActivationService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MemCachedSupplierService supplierService;

    @Autowired
    private BalanceContactService balanceContactService;

    @Autowired
    private NotificationService notificationService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ParamService paramService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    private SupplierFeedService supplierFeedService;

    @Autowired // mock
    private AboAPI aboPublicRestClient;

    @Autowired // mock
    private CheckouterShopApi checkouterShopApi;

    @Autowired // mock
    private CheckouterAPI checkouterAPI;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private PartnerRegistrationHelper partnerRegistrationHelper;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Autowired
    private PartnerToBusinessConverter partnerToBusinessConverter;

    @Autowired
    private CPADataPusher cpaDataPusher;

    @Autowired
    ProtocolService protocolService;

    @Autowired
    private Clock clock;

    private SupplierRegistrationService supplierRegistrationService;

    @Autowired
    private RemoveClientEnvironmentService removeClientEnvironmentService;

    @Autowired
    private BpmnOperationService bpmnOperationService;

    @Autowired
    private MbiBpmnClient mbiBpmnClient;


    @Nonnull
    private static ContactEmail createEmail(String email) {
        return new ContactEmail(0L, email, true, true);
    }

    private static ContactWithEmail contact() {
        ContactWithEmail contact = new ContactWithEmail();
        contact.setFirstName("name");
        contact.setLastName("lastName");
        contact.setPosition("position");
        contact.setEmails(Collections.singleton(new ContactEmail(1L, "email@yandex.ru", false, true)));
        contact.setPhone("+7-800-555-3535");
        return contact;
    }

    private static void assertSupplierStateEquals(SupplierState expected, SupplierState actual) {
        assertThat(actual.getCampaignId()).isEqualTo(expected.getCampaignId());
        assertThat(actual.getClientId()).isEqualTo(expected.getClientId());
        assertThat(actual.getInfo()).isEqualTo(expected.getInfo());
        assertThat(actual.getDatasourceId()).isEqualTo(expected.getDatasourceId());
        assertThat(actual.getPrepayRequestId()).isEqualTo(expected.getPrepayRequestId());
        assertThat(actual.getSupplierType()).isEqualTo(expected.getSupplierType());
        assertThat(actual.getHasMapping()).isEqualTo(expected.getHasMapping());
    }

    @BeforeEach
    void before() {
        when(balanceContactService.getClientIdByUid(anyLong())).thenReturn(CLIENT_ID);
        when(balanceService.getClient(eq(CLIENT_ID))).thenReturn(new ClientInfo(CLIENT_ID, ClientType.PHYSICAL));

        supplierRegistrationService = new SupplierRegistrationService(
                balanceService,
                prepayRequestService,
                partnerService,
                contactService,
                contactLinkService,
                partnerActivationService,
                supplierService,
                notificationService,
                applicationEventPublisher,
                paramService,
                supplierFeedService,
                environmentService,
                campaignService,
                agencyService,
                partnerToBusinessConverter,
                partnerRegistrationHelper,
                removeClientEnvironmentService,
                bpmnOperationService
        );
        when(checkouterAPI.shops()).thenReturn(checkouterShopApi);
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }

    @Test
    @DbUnitDataSet(before = "SupplierRegister.before.csv")
    void testRegistrationLimit() {
        environmentService.setValue(SupplierRegistrationService.ENV_CAMPAIGNS_LIMIT, "-1");
        assertThatExceptionOfType(TooManyCampaignsException.class).isThrownBy(
                () -> registerSupplier(
                        1L, SupplierBasicAttributes.of("name"), contact(), false, false,
                        null, 300L, null, null
                )
        );
        environmentService.removeAllValues(SupplierRegistrationService.ENV_CAMPAIGNS_LIMIT);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @NullSource
    @DbUnitDataSet(before = "SupplierRegister.before.csv")
    void testAdvAgreeFlagSaved(Boolean advAgreeFlag) {
        ContactWithEmail contact = contact();
        contact.setAdvAgree(advAgreeFlag);

        registerSupplier(
                1L, SupplierBasicAttributes.of("name"), contact, false, false,
                null, 300L, null, null
        );

        ContactWithEmail savedContact = contactService.getContactWithEmail(CONTACT_ID);
        assertThat(savedContact.getAdvAgree()).isEqualTo(advAgreeFlag);
    }


    @Test
    @DbUnitDataSet(before = "SupplierRegister.before.csv", after = "SupplierRegister.after.csv")
    void testEventPublished() {
        ContactWithEmail contact = contact();

        registerSupplier(
                1L, SupplierBasicAttributes.of("name"), contact, false, false,
                null, 300L, null, null
        );
        ArgumentCaptor<SupplierRegistrationEvent> eventArgument =
                ArgumentCaptor.forClass(SupplierRegistrationEvent.class);
        verify(applicationEventPublisher).publishEvent(eventArgument.capture());
        SupplierState expected = new SupplierState.Builder()
                .setCampaignId(1L)
                .setClientId(999L)
                .setInfo(SupplierBasicAttributes.of("name"))
                .setDatasourceId(1L)
                .setPrepayRequestId(1L)
                .setSupplierType(SupplierType.THIRD_PARTY)
                .setHasMapping(false)
                .build();
        assertSupplierStateEquals(expected, eventArgument.getValue().getSupplierState());
        verify(marketIdServiceImplBase, times(1)).updateConatctAccesses(any(), any());
        verify(balanceContactService, never()).linkUid(eq(1L), anyLong(), anyLong(), anyLong());
    }

    @Test
    void testWithCommonEmail() {
        ContactWithEmail first = new ContactWithEmail();
        first.setEmails(ImmutableSet.of(createEmail("test1@yndx.ru"), createEmail("test2@yndx.ru")));
        ContactWithEmail second = new ContactWithEmail();
        second.setEmails(ImmutableSet.of(createEmail("test2@yndx.ru"), createEmail("test3@yndx.ru")));
        assertThat(ContactWithEmail.isEmailMatched(first, second)).isTrue();
        assertThat(ContactWithEmail.isEmailMatched(second, first)).isTrue();
    }

    /**
     * Проверяет, что создается бизнес для поставщика.
     */
    @Test
    @DbUnitDataSet(before = "SupplierRegister.before.csv", after = "SupplierRegister.createBusiness.after.csv")
    void tesGoodContentParamUpdateEmptySwitchOnGcParam() {
        registerSupplier(
                1L, SupplierBasicAttributes.of("name"), contact(), false, false,
                null, 300L, null, "name"
        );
        assertThat(paramService.listParams(1L)
                .stream()
                .filter(p -> GOOD_CONTENT_PARAM_TYPES.contains(p.getType()))
                .filter(ParamValue::getValueAsBoolean)
        )
                .hasSize(3);
        verify(marketIdServiceImplBase, times(1)).updateConatctAccesses(any(), any());
        verify(balanceContactService, never()).linkUid(eq(1L), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(before = "SupplierRegister.before.csv", after = "SupplierRegister.linkToBusiness.after.csv")
    void tesGoodContentParamUpdateSwitchOnGcParam() {
        registerSupplier(
                1L, SupplierBasicAttributes.of("name"), contact(), false, false,
                null, 300L, null, null
        );
        assertThat(paramService.listParams(1L)
                .stream()
                .filter(p -> GOOD_CONTENT_PARAM_TYPES.contains(p.getType()))
                .filter(ParamValue::getValueAsBoolean)
        )
                .hasSize(3);
    }

    @Test
    @DbUnitDataSet(before = "SupplierRegister.before.csv")
    void registerSupplierFulfillment() {
        // when
        var result = registerSupplier(
                1L, SupplierBasicAttributes.of("name"), contact(), false, false,
                null, 300L, null, null
        );

        // then
        var datasourceId = result.getSupplier().getDatasourceId();
        assertRegisteredSupplierIsNewbie();
        assertThat(paramService.getParamNumberValue(
                ParamType.HOME_REGION, datasourceId).longValue()).isEqualTo(RegionConstants.RUSSIA);
        assertThat(paramService.getParamNumberValue(
                ParamType.LOCAL_DELIVERY_REGION, datasourceId).longValue()).isEqualTo(RegionConstants.MOSCOW);
        assertThat(paramService.getParamBooleanValue(ParamType.EXPORT_OWN_REGION, datasourceId)).isTrue();
        assertThat(partnerTypeAwareService.isFulfillmentSupplier(datasourceId)).isTrue(); // non-cached
        assertThat(partnerTypeAwareService.getPartnerTypeAwareInfo(datasourceId).isFulfillment()).isTrue(); // cached
        verifyNoInteractions(aboPublicRestClient); // при регистрации нам нечего там делать
        verifyNoInteractions(checkouterShopApi); // при регистрации нам нечего там делать
        verify(balanceContactService, never()).linkUid(eq(1L), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(before = {"SupplierRegister.before.csv", "SupplierRegister.disabledFFServices.before.csv"},
            after = "SupplierRegister.disabledFFServices.after.csv")
    void registerSupplierFulfillmentWithDisabledFFServices() {
        registerSupplier(
                1L, SupplierBasicAttributes.of("name"), contact(), false, false,
                null, 300L, null, null
        );
        verify(balanceContactService, never()).linkUid(eq(1L), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(
            before = "SupplierRegister.before.csv",
            after = "SupplierRegister.withOrganizationInfo.after.csv"
    )
    void registerSupplierWithOrganizationInfo() {
        // given
        var organizationInfo = OrganizationInfoDTO.builder()
                .factAddress("address-ph")
                .inn("INN-XXX")
                .juridicalAddress("address-j")
                .name("Yandex")
                .ogrn("OGRN-XXX")
                .type(OrganizationType.OAO)
                .build();

        // when
        var result = registerSupplier(
                1L, SupplierBasicAttributes.of("name"), contact(), false, false,
                null, 300L, organizationInfo, null
        );

        // then
        assertThat(result.getSupplier().getInfo().name()).isEqualTo("name");
        verify(balanceContactService, never()).linkUid(eq(1L), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(before = "SupplierRegister.before.csv")
    void registerSupplierCrossdock() {
        // when
        var result = registerSupplier(
                1L, SupplierBasicAttributes.of("name"), contact(), false, true,
                null, 300L, null, null
        );

        // then
        var datasourceId = result.getSupplier().getDatasourceId();
        var cutoffInfos = featureService.getCutoffs(datasourceId, FeatureType.CROSSDOCK);
        assertThat(cutoffInfos).hasSize(1);
        var cutoffInfo = cutoffInfos.iterator().next();
        assertThat(cutoffInfo.getId()).isNotNull();
        assertThat(cutoffInfo.getFeatureCutoffType()).isEqualTo(FeatureCutoffType.PARTNER);
        assertRegisteredSupplierIsNewbie();
        assertThat(paramService.getParamBooleanValue(ParamType.CPA_IS_PARTNER_INTERFACE, datasourceId, false)).isTrue();
        assertThat(partnerTypeAwareService.isCrossdockSupplier(datasourceId)).isTrue(); // non-cached
        assertThat(partnerTypeAwareService.getPartnerTypeAwareInfo(datasourceId).isCrossdock()).isTrue(); // cached
        verifyNoInteractions(aboPublicRestClient); // при регистрации нам нечего там делать
        verifyNoInteractions(checkouterShopApi); // при регистрации нам нечего там делать
        verify(balanceContactService, never()).linkUid(eq(1L), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(before = "SupplierRegister.before.csv", after = "SupplierRegister.dropship.after.csv")
    void registerSupplierDropship() {
        environmentService.setValue(SupplierRegistrationService.SET_PAYMENT_CONTROL_ENABLED, "true");
        // when
        var result = registerSupplier(
                1L, SupplierBasicAttributes.of("name"), contact(), true, false,
                null, 300L, null, null
        );

        // then
        var datasourceId = result.getSupplier().getDatasourceId();
        assertRegisteredSupplierIsNewbie();
        assertThat(paramService.getParamBooleanValue(ParamType.CPA_IS_PARTNER_INTERFACE, datasourceId, false)).isTrue();
        assertThat(paramService.getParamBooleanValue(ParamType.DROPSHIP_AVAILABLE, datasourceId, false)).isTrue();
        assertThat(partnerTypeAwareService.isDropship(datasourceId)).isTrue(); // non-cached
        assertThat(partnerTypeAwareService.getPartnerTypeAwareInfo(datasourceId).isDropship()).isTrue(); // cached
        assertThat(featureService.getFeature(datasourceId, FeatureType.ORDER_AUTO_ACCEPT).getStatus())
                .isEqualTo(ParamCheckStatus.SUCCESS);
        verifyNoInteractions(aboPublicRestClient); // при регистрации нам нечего там делать
        verifyNoInteractions(checkouterShopApi); // при регистрации нам нечего там делать
        verify(cpaDataPusher, atLeastOnce()).pushShopInfoToCheckout(datasourceId);
        assertThat(paramService.getParamBooleanValue(ParamType.DROPSHIP_AVAILABLE, datasourceId, false)).isTrue();
        assertThat(paramService.getParamBooleanValue(ParamType.PAYMENT_CONTROL_ENABLED, datasourceId, false))
                .isTrue();
        verify(balanceContactService, never()).linkUid(eq(1L), anyLong(), anyLong(), anyLong());
    }

    @Test
    void testWithoutCommonEmail() {
        ContactWithEmail first = new ContactWithEmail();
        first.setEmails(ImmutableSet.of(createEmail("test1@yndx.ru"), createEmail("test2@yndx.ru")));
        ContactWithEmail second = new ContactWithEmail();
        second.setEmails(ImmutableSet.of(createEmail("test3@yndx.ru"), createEmail("test4@yndx.ru")));
        assertThat(ContactWithEmail.isEmailMatched(first, second)).isFalse();
        assertThat(ContactWithEmail.isEmailMatched(second, first)).isFalse();
    }

    @Test
    @DbUnitDataSet(before = "SupplierRegistrationServiceTest.testRegisterSupplierForAgency.before.csv")
    void testRegisterSupplierForAgency() {
        // given
        final long uid = 100504L;
        final long clientId = 222L;

        // when
        when(balanceContactService.getClientIdByUid(uid)).thenReturn(clientId);

        // then
        assertThatExceptionOfType(AgencySupplierRegistrationException.class).isThrownBy(
                () -> registerSupplier(
                        uid, null, null, false, false, null,
                        300L, null, null
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "SupplierRegister.before.csv")
    void testRegisterSupplierForBalanceAgency() {
        // given
        final long uid = 1L;
        final long clientId = 222L;

        // when
        when(balanceContactService.getClientIdByUid(uid)).thenReturn(clientId);
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo(clientId, ClientType.OAO, true, clientId));

        // then
        assertThatExceptionOfType(AgencySupplierRegistrationException.class).isThrownBy(
                () -> registerSupplier(
                        uid, SupplierBasicAttributes.of("agencySup"), contact(), false, false, null,
                        300L, null, null
                )
        );
    }

    static Stream<Arguments> testRegisterSupplierOnSubclientData() {
        return Stream.of(
                Arguments.of(103/*clientId*/, "domain.ru", 300L),
                Arguments.of(104/*clientId*/, "x-dom.ru", 401L),
                Arguments.of(105/*clientId*/, "ozon.com", 500L)
        );
    }

    @ParameterizedTest
    @MethodSource("testRegisterSupplierOnSubclientData")
    @DbUnitDataSet(before = {"SupplierRegister.before.csv", "RegisterSupplierOnSubclient.before.csv"})
    void testRegisterSupplierOnSubclient(long clientId, String domain, long expectedBusinessId) {
        final long uid = 1L;
        // when
        when(balanceService.getClient(eq(clientId))).thenReturn(new ClientInfo(clientId, ClientType.ZAO));
        when(balanceContactService.getClientIdByUid(uid)).thenReturn(clientId);
        ContactWithEmail contact = contact();
        contact.setLogin("lgn");
        SupplierRegistrationResult supplierRegistrationResult =
                registerSupplier(uid,
                        SupplierBasicAttributes.of("name", domain),
                        contact, false, false, null, expectedBusinessId,
                        null, null);
        assertThat(supplierRegistrationResult.getBusinessId()).isEqualTo(expectedBusinessId);
        verify(balanceContactService, never()).linkUid(eq(uid), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Регистрация поставщика на прямого клиент. Все другие магазины под бизнесом - на субклиентах")
    @DbUnitDataSet(
            before = "testRegisterSupplierOnBusinessWithSubclients.before.csv",
            after = "testRegisterSupplierOnBusinessWithSubclients.after.csv"
    )
    void testRegisterSupplierOnBusinessWithSubclients() {
        long requestUid = 1L;
        long requestClientId = 105L;

        // when
        when(balanceService.getClient(eq(requestClientId))).thenReturn(new ClientInfo(requestClientId, ClientType.ZAO));
        when(balanceContactService.getClientIdByUid(requestUid)).thenReturn(requestClientId);
        ContactWithEmail contact = contact();
        contact.setLogin("lgn");

        registerSupplier(requestUid,
                SupplierBasicAttributes.of("name", "dom.ru"),
                contact, false, false, null, 400L, null, null);
        verify(balanceContactService, never()).linkUid(eq(requestUid), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(
            before = "AddSuperAdminOnSupplierRegister.before.csv",
            after = "AddSuperAdminOnSupplierRegister.after.csv"
    )
    void testAddSuperAdminAndFFLinkOnSupplierRegister() {
        // when
        var result = registerSupplier(
                1L, SupplierBasicAttributes.of("name"), contact(), false, false,
                null, 300L, null, null
        );

        // then
        assertThat(result.getSupplier().getSupplierType()).isEqualTo(SupplierType.THIRD_PARTY);
    }

    @Test
    @DbUnitDataSet(
            before = "AddSuperAdminOnSupplierRegisterWithDefault.before.csv",
            after = "AddSuperAdminOnSupplierRegisterWithDefault.after.csv"
    )
    void testAddSuperAdminAndFFLinkOnSupplierRegisterWithDefaultFeed() {
        // when
        var result = registerSupplier(
                1L, SupplierBasicAttributes.of("name"), contact(), false, false,
                null, 300L, null, null
        );

        // then
        assertThat(result.getSupplier().getSupplierType()).isEqualTo(SupplierType.THIRD_PARTY);
    }

    private void assertRegisteredSupplierIsNewbie() {
        assertThat(paramService.listParams(1L).stream()
                .filter(p -> p.getType() == ParamType.IS_NEWBIE)
                .map(ParamValue::getValueAsBoolean)
                .findFirst()
        ).hasValue(true);
    }

    static Stream<Arguments> registerAllSuppliersWithUnitedCatalogData() {
        return Stream.of(
                Arguments.of(108L, "x-dom.ru", 800L, UnitedCatalogStatus.SUCCESS),
                Arguments.of(109L, "space.ru", 900L, UnitedCatalogStatus.SUCCESS),
                Arguments.of(110L, "shop.ru", 960L, UnitedCatalogStatus.SUCCESS),
                Arguments.of(111L, "market.ru", 963L, UnitedCatalogStatus.SUCCESS)
        );
    }

    @ParameterizedTest
    @MethodSource("registerAllSuppliersWithUnitedCatalogData")
    @DbUnitDataSet(before = "RegisterWithUnionCatalog.before.csv")
    void registerAllSuppliersWithUnitedCatalog(
            long clientId, String domain, long businessId, UnitedCatalogStatus unitedCatalogStatus
    ) {
        checkRegisteredSupplierUCatStatus(clientId, domain, businessId, unitedCatalogStatus);
    }

    private void checkRegisteredSupplierUCatStatus(
            long clientId, String domain, long businessId, UnitedCatalogStatus unitedCatalogStatus
    ) {
        // given
        final long uid = 1L;
        when(balanceService.getClient(eq(clientId))).thenReturn(new ClientInfo(clientId, ClientType.ZAO));
        when(balanceContactService.getClientIdByUid(uid)).thenReturn(clientId);
        ContactWithEmail contact = contact();
        contact.setLogin("lgn");

        // when
        var supplierRegistrationResult = registerSupplier(
                uid, SupplierBasicAttributes.of("name", domain),
                contact, false, false, null, businessId, null, null
        );

        // then
        assertThat(supplierRegistrationResult.getBusinessId()).isEqualTo(businessId);
        assertThat(paramService.getParamEnumValueOrDefault(ParamType.UNITED_CATALOG_STATUS,
                supplierRegistrationResult.getSupplier().getDatasourceId(),
                UnitedCatalogStatus.class)
        ).isEqualTo(unitedCatalogStatus);
    }

    @Test
    @DbUnitDataSet(before = "SupplierRegister.before.csv")
    void registerSupplierWithUnitedCatalogFirst() {
        // when
        var supplierRegistrationResult = registerSupplier(
                1L, SupplierBasicAttributes.of("name", "some.ru"),
                contact(), false, false, null, 300L, null, null
        );

        // then
        assertThat(paramService.getParamEnumValueOrDefault(ParamType.UNITED_CATALOG_STATUS,
                supplierRegistrationResult.getSupplier().getDatasourceId(),
                UnitedCatalogStatus.class)
        ).isEqualTo(UnitedCatalogStatus.SUCCESS);
        verify(balanceContactService, never()).linkUid(eq(1L), anyLong(), anyLong(), anyLong());
    }

    private SupplierRegistrationResult registerSupplier(
            long uid,
            SupplierBasicAttributes supplierBasicAttributes,
            @Nullable ContactWithEmail notificationContact,
            boolean dropship,
            boolean crossdock,
            @Nullable Long marketId,
            long businessId,
            @Nullable OrganizationInfoDTO organizationInfo,
            @Nullable String businessName
    ) {
        return protocolService.actionInTransaction(
                ActionType.SUPPLIER_REGISTER,
                uid,
                "comment",
                (transactionStatus, actionId) -> supplierRegistrationService.registerSupplier(
                        uid,
                        supplierBasicAttributes,
                        notificationContact,
                        dropship,
                        crossdock,
                        marketId,
                        businessId,
                        actionId,
                        organizationInfo,
                        businessName
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "SupplierRegister.before.csv")
    void testSetPiApiParamBpmn() {
        environmentService.setValue(CpaIsPartnerInterfaceSyncService.CHANGE_ORDER_PROCESSING_METHOD_ENABLED, "true");
        Mockito.when(mbiBpmnClient.postProcess(Mockito.eq(
                new ProcessInstanceRequest()
                        .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                        .params(Map.of(
                                "uid", "1",
                                "partnerId", "1",
                                "isPartnerInterface", "true",
                                "operationId", "1",
                                "partnerInterface", "true"
                        ))
        ))).thenReturn(
                new ProcessStartResponse().records(List.of(
                        (ProcessStartInstance) new ProcessStartInstance()
                                .started(true)
                                .processInstanceId("id")
                                .status(ProcessStatus.ACTIVE)
                ))
        );

        registerSupplier(
                1L, SupplierBasicAttributes.of("name"), contact(), true, false,
                null, 300L, null, null
        );

        Mockito.verify(mbiBpmnClient)
                .postProcess(
                        Mockito.eq(
                                new ProcessInstanceRequest()
                                        .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                                        .params(Map.of(
                                                "uid", "1",
                                                "partnerId", "1",
                                                "isPartnerInterface", "true",
                                                "operationId", "1",
                                                "partnerInterface", "true"
                                        ))
                        )
                );
    }
}
