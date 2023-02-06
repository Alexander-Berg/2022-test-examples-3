package ru.yandex.market.core.security.checker;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.market.core.campaign.CampaignStatusService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.security.Campaignable;
import ru.yandex.market.core.security.DefaultCampaignable;
import ru.yandex.market.core.security.DefaultCheckedParam;
import ru.yandex.market.security.model.Authority;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateParamValueCheckerTest {

    private static final Campaignable REQUEST = new DefaultCampaignable(10774, 123, 123);

    @Mock
    private CampaignStatusService campaignStatusService;

    private UpdateParamValueChecker updateParamValueChecker;

    @BeforeEach
    void setUp() {
        updateParamValueChecker = new UpdateParamValueChecker(campaignStatusService);
    }

    @Test
    void shouldNotAllowUnknownParameter() {
        assertFalse(updateParamValueChecker.checkTyped(
                new DefaultCheckedParam(REQUEST, ParamType.HOME_REGION),
                new Authority("test", "partner:I_AM_UNKNOWN_PARAM")
        ));
    }

    @Test
    void shouldAllowChangeEditableParameter() {
        assertTrue(updateParamValueChecker.checkTyped(
                new DefaultCheckedParam(REQUEST, ParamType.HOME_REGION),
                new Authority("test", "partner:HOME_REGION")
        ));
    }

    @Test
    void shouldNotAllowChangeNotEditableParameter() {
        assertFalse(updateParamValueChecker.checkTyped(
                new DefaultCheckedParam(REQUEST, ParamType.TARIFF),
                new Authority("test", "partner:HOME_REGION")
        ));
    }


    @Test
    void shouldAllowChangeNonSystemParameter() {
        assertTrue(updateParamValueChecker.checkTyped(
                new DefaultCheckedParam(REQUEST, ParamType.TARIFF),
                new Authority("test", "manager")
        ));
    }

    @Test
    void shouldNotAllowChangeSystemParameter() {
        assertFalse(updateParamValueChecker.checkTyped(
                new DefaultCheckedParam(REQUEST, ParamType.IS_CPC_ENABLED),
                new Authority("test", "manager")
        ));
    }

    @Test
    void testNotAllowedChangeLocalDeliveryRegion() {
        when(campaignStatusService.isShopPassModeration(10774L)).thenReturn(true);
        assertFalse(updateParamValueChecker.checkTyped(
                new DefaultCheckedParam(REQUEST, ParamType.LOCAL_DELIVERY_REGION),
                new Authority("test", "partner:LOCAL_DELIVERY_REGION")
        ));
    }

    @Test
    void testAllowedChangeLocalDeliveryRegion() {
        when(campaignStatusService.isShopPassModeration(10774L)).thenReturn(false);
        assertTrue(updateParamValueChecker.checkTyped(
                new DefaultCheckedParam(REQUEST, ParamType.LOCAL_DELIVERY_REGION),
                new Authority("test", "partner:LOCAL_DELIVERY_REGION")
        ));
    }

    @Test
    void testNotAllowedChangeDatasourceDomain() {
        when(campaignStatusService.isShopPassModeration(10774L)).thenReturn(true);
        when(campaignStatusService.isDomainSet(10774L)).thenReturn(true);
        assertFalse(updateParamValueChecker.checkTyped(
                new DefaultCheckedParam(REQUEST, ParamType.DATASOURCE_DOMAIN),
                new Authority("test", "partner:DATASOURCE_DOMAIN")
        ));
    }

    @Test
    void testAllowedChangeDatasourceDomain() {
        when(campaignStatusService.isShopPassModeration(10774L)).thenReturn(true);
        when(campaignStatusService.isDomainSet(10774L)).thenReturn(false);
        assertTrue(updateParamValueChecker.checkTyped(
                new DefaultCheckedParam(REQUEST, ParamType.DATASOURCE_DOMAIN),
                new Authority("test", "partner:DATASOURCE_DOMAIN")
        ));
    }

}
