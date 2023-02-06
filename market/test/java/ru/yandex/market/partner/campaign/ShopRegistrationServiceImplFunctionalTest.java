package ru.yandex.market.partner.campaign;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.pay.legacy.PaymentSubMethod;
import ru.yandex.market.checkout.checkouter.shop.PaymentArticle;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopActualDeliveryRegionalSettings;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.PushPartnerStatus;
import ru.yandex.market.core.param.model.UnitedCatalogStatus;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.campaign.model.ShopRegistrationResult;
import ru.yandex.market.partner.mvc.controller.campaign.model.NotificationContact;
import ru.yandex.market.partner.mvc.controller.campaign.model.ShopRegistrationDTO;
import ru.yandex.market.partner.mvc.controller.campaign.model.ShopSubtype;
import ru.yandex.market.partner.mvc.controller.campaign.model.registration.WhiteShopRegistrationData;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.campaign.impl.ShopRegistrationServiceImpl.ENV_CAMPAIGNS_LIMIT_PARAM;
import static ru.yandex.market.partner.campaign.impl.ShopRegistrationServiceImpl.SET_PAYMENT_CONTROL_ENABLED;

@DbUnitDataSet(before = "ShopRegistrationServiceImplFunctionalTest.before.csv")
public class ShopRegistrationServiceImplFunctionalTest extends FunctionalTest {

    private static final long USER_ID = 123L;
    private static final long CLIENT_ID = 123L;
    private static final long MOSCOW_REGION_ID = 213L;
    private static final long RUSSIA_REGION_ID = 225L;

    @Autowired
    private ShopRegistrationService shopRegistrationService;

    @Autowired
    private PassportService passportService;

    @Autowired
    @Qualifier("environmentService")
    private EnvironmentService environmentService;

    @Autowired
    private BalanceContactService balanceContactService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private ParamService paramService;

    @BeforeEach
    public void init() {
        when(passportService.findUid(eq("some_login"))).thenReturn(USER_ID);
        when(passportService.getUserInfo(eq(USER_ID))).thenReturn(
                new UserInfo(
                        123L,
                        "Тестовый юзер",
                        "some_login@yandex.ru",
                        "some_login"
                )
        );
        when(balanceContactService.getClientIdByUid(eq(USER_ID))).thenReturn(CLIENT_ID);
        when(balanceService.getClient(eq(CLIENT_ID))).thenReturn(new ClientInfo(CLIENT_ID, ClientType.OAO, false, 0));
        initMockCheckouterApi();

        environmentService.setValue(ENV_CAMPAIGNS_LIMIT_PARAM, "100");
        environmentService.setValue(SET_PAYMENT_CONTROL_ENABLED, "true");
    }

    private void initMockCheckouterApi() {
        CheckouterShopApi shopApi = mock(CheckouterShopApi.class);
        when(shopApi.getShopData(anyLong())).thenReturn(
                new ShopMetaData(
                        1L,
                        1L,
                        1L,
                        PaymentClass.GLOBAL,
                        PaymentClass.GLOBAL,
                        null,
                        new PaymentArticle[]{new PaymentArticle("123", PaymentSubMethod.BANK_CARD, "")},
                        PrepayType.YANDEX_MARKET,
                        "inn",
                        "+7-987-987-87-87",
                        0,
                        Map.of(),
                        "ogrn",
                        "supplier1",
                        true,
                        true,
                        true,
                        true,
                        new ShopActualDeliveryRegionalSettings[1],
                        List.of(),
                        true,
                        true,
                        true
                )
        );
        when(checkouterAPI.shops()).thenReturn(shopApi);
    }

    @Test
    public void testRegisterAdvWithUnitedCatalogFlag() {
        WhiteShopRegistrationData registrationData = mockAdvRegistrationData();
        ShopRegistrationResult result = shopRegistrationService.registerShop(registrationData);
        long shopId = result.getDatasource().getId();

        assertThat(paramService.getParamStringValue(ParamType.UNITED_CATALOG_STATUS, shopId))
                .isEqualTo(UnitedCatalogStatus.SUCCESS.name());

        assertThat(paramService.getParamStringValue(ParamType.IS_PUSH_PARTNER, shopId))
                .isEqualTo(PushPartnerStatus.REAL.name());
    }

    @Test
    public void testRegisterDbs() {
        WhiteShopRegistrationData registrationData = mockDbsRegistrationData();
        ShopRegistrationResult result = shopRegistrationService.registerShop(registrationData);
        long shopId = result.getDatasource().getId();

        assertThat(paramService.getParamStringValue(ParamType.UNITED_CATALOG_STATUS, shopId))
                .isEqualTo(UnitedCatalogStatus.SUCCESS.name());

        assertThat(paramService.getParamStringValue(ParamType.IS_PUSH_PARTNER, shopId))
                .isEqualTo(PushPartnerStatus.REAL.name());

        assertThat(paramService.getParamBooleanValue(ParamType.PAYMENT_CONTROL_ENABLED, shopId, false))
                .isTrue();
    }

    private WhiteShopRegistrationData mockAdvRegistrationData() {
        ShopRegistrationDTO dto = new ShopRegistrationDTO();
        dto.setCampaignType(CampaignType.SHOP);
        dto.setDomain("domain.ru");
        dto.setInternalShopName("Some shop");
        dto.setLocalRegionId(MOSCOW_REGION_ID);
        dto.setRegionId(RUSSIA_REGION_ID);
        dto.setDrophipBySeller(true);
        dto.setOwnerLogin("some_login");
        dto.setBusinessId(10010L);

        dto.setNotificationContact(new NotificationContact(
                "Name",
                "Last name",
                "some_login@yandex.ru",
                "+7-999-987-98-87"));

        PartnerDefaultRequestHandler.PartnerHttpServRequest request =
                new PartnerDefaultRequestHandler.PartnerHttpServRequest(USER_ID,
                        null,
                        null,
                        null
                );
        WhiteShopRegistrationData registrationData =
                new WhiteShopRegistrationData(ShopSubtype.REGULAR, PartnerPlacementProgramType.CPC);
        registrationData.fill(dto, request);

        return registrationData;
    }

    private WhiteShopRegistrationData mockDbsRegistrationData() {
        ShopRegistrationDTO dto = new ShopRegistrationDTO();
        dto.setCampaignType(CampaignType.SHOP);
        dto.setDomain("domain.ru");
        dto.setInternalShopName("Some shop");
        dto.setLocalRegionId(MOSCOW_REGION_ID);
        dto.setRegionId(RUSSIA_REGION_ID);
        dto.setDrophipBySeller(true);
        dto.setOwnerLogin("some_login");
        dto.setBusinessId(10010L);

        dto.setNotificationContact(new NotificationContact(
                "Name",
                "Last name",
                "some_login@yandex.ru",
                "+7-999-987-98-87"));

        PartnerDefaultRequestHandler.PartnerHttpServRequest request =
                new PartnerDefaultRequestHandler.PartnerHttpServRequest(USER_ID,
                        null,
                        null,
                        null
                );
        WhiteShopRegistrationData registrationData =
                new WhiteShopRegistrationData(ShopSubtype.REGULAR, PartnerPlacementProgramType.DROPSHIP_BY_SELLER);
        registrationData.fill(dto, request);

        return registrationData;
    }
}
