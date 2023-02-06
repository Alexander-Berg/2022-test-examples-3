package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithMobileContent;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore.HUAWEI_APP_GALLERY;
import static ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore.SAMSUNG_GALAXY_STORE;
import static ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore.VIVO_APP_STORE;
import static ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore.XIAOMI_GET_APPS;
import static ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting.PHONE;
import static ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting.TABLET;
import static ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting.CELLULAR;
import static ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting.WI_FI;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignWithMobileContentValidatorTest {

    private final static long MOBILE_APP_ID = 5L;

    private CampaignWithMobileContentValidator validator;
    private MobileContentCampaign campaign;

    @Before
    public void init() {
        campaign = new MobileContentCampaign()
                .withDeviceTypeTargeting(EnumSet.of(PHONE))
                .withNetworkTargeting(EnumSet.of(WI_FI))
                .withMobileAppId(MOBILE_APP_ID)
                .withAlternativeAppStores(EnumSet.of(HUAWEI_APP_GALLERY));
    }

    @SuppressWarnings("unused")
    private static Object[] mobileAppIdParameters() {
        return new Object[][]{
                {"Передан существующий appId -> нет ошибок", Set.of(MOBILE_APP_ID), MOBILE_APP_ID, false, null},
                {"Передан не существующий appId -> OBJECT_NOT_FOUND", Set.of(MOBILE_APP_ID), 6L, false,
                        objectNotFound()},
                {"Не передан appId -> CANNOT_BE_NULL", Set.of(MOBILE_APP_ID), null, false, CommonDefects.notNull()},
                {"Передан appId = 0 и это разрешено -> ok", Set.of(MOBILE_APP_ID), 0L, true, null},
                {"Передан appId = 0 и это не разрешено -> ошибка", Set.of(MOBILE_APP_ID), 0L, false, objectNotFound()},
                {"У клиента нет приложений -> OBJECT_NOT_FOUND", Set.of(), MOBILE_APP_ID, false, objectNotFound()},
        };
    }

    /**
     * Проверка валидации при разных передаваемых значениях id приложения (MobileAppId)
     */
    @Test
    @Parameters(method = "mobileAppIdParameters")
    @TestCaseName("[{index}] {0}")
    public void testOfMobileAppId(@SuppressWarnings("unused") String description,
                                  Set<Long> clientMobileAppIds,
                                  Long mobileAppId,
                                  boolean allowZeroMobileAppId,
                                  @Nullable Defect expectedDefect) {
        validator = new CampaignWithMobileContentValidator(clientMobileAppIds,
                false, false, allowZeroMobileAppId);

        campaign.withMobileAppId(mobileAppId);

        ValidationResult vr = validator.apply(campaign);
        if (expectedDefect == null) {
            assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
        } else {
            Path expectedPath = path(field(CampaignWithMobileContent.MOBILE_APP_ID));
            assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(expectedPath, expectedDefect))));
        }
    }

    @SuppressWarnings("unused")
    private static Object[] networkTargetingParameters() {
        return new Object[][]{
                {"Передан список из WI_FI -> нет ошибок", EnumSet.of(WI_FI), false, null},
                {"Передан список из CELLULAR -> нет ошибок", EnumSet.of(CELLULAR), false, null},
                {"Передан список из WI_FI,CELLULAR -> нет ошибок", EnumSet.of(WI_FI, CELLULAR), false, null},
                {"Передан пустой список networkTargeting -> CANNOT_BE_EMPTY",
                        EnumSet.noneOf(MobileAppNetworkTargeting.class), false,
                        CollectionDefects.notEmptyCollection()},
                {"Передан null список networkTargeting и это не разрешено -> CANNOT_BE_NULL", null, false,
                        CommonDefects.notNull()},
                {"Передан null список networkTargeting и это разрешено -> нет ошибок", null, true, null},
        };
    }

    /**
     * Проверка валидации при разных значениях таргетинга на тип подключения к сети (networkTargeting)
     */
    @Test
    @Parameters(method = "networkTargetingParameters")
    @TestCaseName("[{index}] {0}")
    public void testOfNetworkTargeting(@SuppressWarnings("unused") String description,
                                       EnumSet<MobileAppNetworkTargeting> networkTargeting,
                                       boolean allowNullNetworkTargeting,
                                       @Nullable Defect expectedDefect) {
        validator = new CampaignWithMobileContentValidator(Set.of(MOBILE_APP_ID), false,
                allowNullNetworkTargeting, false);

        campaign.withNetworkTargeting(networkTargeting);

        ValidationResult vr = validator.apply(campaign);
        if (expectedDefect == null) {
            assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
        } else {
            Path expectedPath = path(field(CampaignWithMobileContent.NETWORK_TARGETING));
            assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(expectedPath, expectedDefect))));
        }
    }

    @SuppressWarnings("unused")
    private static Object[] deviceTypeTargetingParameters() {
        return new Object[][]{
                {"Передан список из PHONE -> нет ошибок", EnumSet.of(PHONE), false, null},
                {"Передан список из TABLET -> нет ошибок", EnumSet.of(TABLET), false, null},
                {"Передан список из PHONE,TABLET -> нет ошибок", EnumSet.of(PHONE, TABLET), false, null},
                {"Передан пустой список deviceTypeTargeting -> CANNOT_BE_EMPTY",
                        EnumSet.noneOf(MobileAppDeviceTypeTargeting.class), false,
                        CollectionDefects.notEmptyCollection()},
                {"Передан null список deviceTypeTargeting и это не разрешено -> CANNOT_BE_NULL", null, false,
                        CommonDefects.notNull()},
                {"Передан null список deviceTypeTargeting и это разрешено -> нет ошибок", null, true, null},
        };
    }

    /**
     * Проверка валидации при разных значениях таргетинга на мобильное устройство (DeviceTypeTargeting)
     */
    @Test
    @Parameters(method = "networkTargetingParameters")
    @TestCaseName("[{index}] {0}")
    public void testOfDeviceTypeTargeting(@SuppressWarnings("unused") String description,
                                          EnumSet<MobileAppDeviceTypeTargeting> deviceTypeTargeting,
                                          boolean allowNullDeviceTypeTargeting,
                                          @Nullable Defect expectedDefect) {
        validator = new CampaignWithMobileContentValidator(Set.of(MOBILE_APP_ID), allowNullDeviceTypeTargeting,
                false, false);

        campaign.withDeviceTypeTargeting(deviceTypeTargeting);

        ValidationResult vr = validator.apply(campaign);
        if (expectedDefect == null) {
            assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
        } else {
            Path expectedPath = path(field(CampaignWithMobileContent.DEVICE_TYPE_TARGETING));
            assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(expectedPath, expectedDefect))));
        }
    }

    @SuppressWarnings("unused")
    private static Object[] alternativeAppStoresParameters() {
        return new Object[][]{
                {"Передан список из HUAWEI_APP_GALLERY -> нет ошибок", EnumSet.of(HUAWEI_APP_GALLERY), null},
                {"Передан список из XIAOMI_GET_APPS -> нет ошибок", EnumSet.of(XIAOMI_GET_APPS), null},
                {"Передан список из HUAWEI_APP_GALLERY, XIAOMI_GET_APPS, SAMSUNG_GALAXY_STORE, VIVO_STORE -> нет ошибок",
                        EnumSet.of(HUAWEI_APP_GALLERY, XIAOMI_GET_APPS, SAMSUNG_GALAXY_STORE, VIVO_APP_STORE),
                        null},
                {"Передан пустой список alternativeAppStores -> нет ошибок",
                        EnumSet.noneOf(MobileAppAlternativeStore.class),
                        null},
                {"Передан null список alternativeAppStores и это разрешено -> нет ошибок", null, null},
        };
    }

    /**
     * Проверка валидации при разных значениях таргетинга на мобильное устройство (DeviceTypeTargeting)
     */
    @Test
    @Parameters(method = "alternativeAppStoresParameters")
    @TestCaseName("[{index}] {0}")
    public void testOfAltAppStores(@SuppressWarnings("unused") String description,
                                   EnumSet<MobileAppAlternativeStore> altAppStores,
                                   @Nullable Defect expectedDefect) {
        validator = new CampaignWithMobileContentValidator(Set.of(MOBILE_APP_ID), false,
                false, false);

        campaign.withAlternativeAppStores(altAppStores);

        ValidationResult vr = validator.apply(campaign);
        if (expectedDefect == null) {
            assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
        } else {
            Path expectedPath = path(field(CampaignWithMobileContent.ALTERNATIVE_APP_STORES));
            assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(expectedPath, expectedDefect))));
        }
    }
}
