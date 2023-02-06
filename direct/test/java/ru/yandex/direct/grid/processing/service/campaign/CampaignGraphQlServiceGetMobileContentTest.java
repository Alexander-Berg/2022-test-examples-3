package ru.yandex.direct.grid.processing.service.campaign;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.converter.MobileContentInfoConverter;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTracker;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTrackerTrackingSystem;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentAvatarSize;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreCountry;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.model.campaign.GdTrackingSystem;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting.PHONE;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting.TABLET;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting.CELLULAR;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting.WI_FI;
import static ru.yandex.direct.core.entity.mobilecontent.service.MobileContentService.generateUrlString;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentFromStoreUrl;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.FunctionalUtils.mapSet;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignGraphQlServiceGetMobileContentTest {

    private static final String ANDROID_STORE_URL = "https://play.google.com/store/apps/details?id=com.ya.test";
    private static final String ANDROID_TRACKER_URL = "https://app.adjust.com/a1bc42?gps_adid={google_aid}";
    private static final String ANDROID_IMPRESSION_URL = "https://view.adjust.com/impression/a1bc42?gps_adid={google_aid}";
    private static final String ANDROID_TRACKER_ID = "a1bc42";
    private static final String ANDROID_MIN_OS_VERSION_FROM_STORE = "8.0";
    private static final String ANDROID_STORE_CONTENT_ID = "com.ya.test";
    private static final String ANDROID_ICON_HASH = "androidIconHash";
    private static final String IOS_STORE_URL = "https://apps.apple.com/ru/app/angry-birds-classic/id343200656";
    private static final String IOS_TRACKER_URL = "https://app.adjust.com/162zy4z?idfa={ios_ifa}";
    private static final String IOS_IMPRESSION_URL = "https://view.adjust.com/impression/162zy4z?idfa={ios_ifa}";
    private static final String IOS_TRACKER_ID = "162zy4z";
    private static final String IOS_MIN_OS_VERSION_FROM_STORE = "10.0";
    private static final String IOS_STORE_CONTENT_ID = "id343200656";
    private static final String IOS_ICON_HASH = "iosIconHash";
    private static final Long VALID_GOAL_ID = 5L;
    private static final Long INVALID_MOBILE_APP_ID = 12345L;
    private static final String STORE_COUNTRY = StoreCountry.RU.toString();
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        ... on GdMobileContentCampaign {\n"
            + "          id\n"
            + "          name\n"
            + "          networkTargeting\n"
            + "          deviceTypeTargeting\n"
            + "          mobileContentInfo {\n"
            + "             currentMinimalOsVersion\n"
            + "             minimalOsVersionFromStore\n"
            + "             mobileContentName\n"
            + "             osType\n"
            + "             storeContentId\n"
            + "             storeCountry\n"
            + "             storeHref\n"
            + "             iconUrl\n"
            + "             iconHash\n"
            + "             tracker {\n"
            + "                 url\n"
            + "                 impressionUrl\n"
            + "                 trackerId\n"
            + "                 trackingSystem\n"
            + "             }\n"
            + "          }\n"
            + "          strategy {\n"
            + "             ... on GdStrategyOptimizeInstalls {\n"
            + "                 goalId\n"
            + "             }\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private GridContextProvider gridContextProvider;

    private GridGraphQLContext context;
    private UserInfo userInfo;
    private ClientInfo clientInfo;
    private Long androidMobileAppId;
    private String androidMobileAppName;
    private String androidCurrentMinimalOSVersion;
    private Long iosMobileAppId;
    private String iosMobileAppName;
    private String iosCurrentMinimalOSVersion;

    @Before
    public void before() {
        steps.trustedRedirectSteps().addValidCounters();

        userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        int shard = userInfo.getShard();

        MobileContentInfo androidMobileContentInfo = new MobileContentInfo()
                .withClientInfo(clientInfo)
                .withMobileContent(mobileContentFromStoreUrl(ANDROID_STORE_URL)
                        .withClientId(clientInfo.getClientId().asLong())
                        .withMinOsVersion(ANDROID_MIN_OS_VERSION_FROM_STORE)
                        .withOsType(OsType.ANDROID)
                        .withIconHash(ANDROID_ICON_HASH)
                        .withStoreCountry(STORE_COUNTRY)
                        .withStoreContentId(ANDROID_STORE_CONTENT_ID));
        androidMobileContentInfo = steps.mobileContentSteps().createMobileContent(shard, androidMobileContentInfo);

        MobileAppTracker androidTracker = new MobileAppTracker()
                .withUrl(ANDROID_TRACKER_URL)
                .withTrackerId(ANDROID_TRACKER_ID)
                .withImpressionUrl(ANDROID_IMPRESSION_URL)
                .withTrackingSystem(MobileAppTrackerTrackingSystem.ADJUST);
        MobileAppInfo androidMobileAppInfo = steps.mobileAppSteps()
                .createMobileApp(clientInfo, androidMobileContentInfo, ANDROID_STORE_URL, androidTracker);
        androidMobileAppId = androidMobileAppInfo.getMobileAppId();
        androidMobileAppName = androidMobileAppInfo.getMobileContentInfo().getMobileContent().getName();
        androidCurrentMinimalOSVersion = androidMobileAppInfo.getMobileApp().getMinimalOperatingSystemVersion();
        androidCurrentMinimalOSVersion =
                nvl(androidCurrentMinimalOSVersion, MobileContentInfoConverter.MIN_OS_VERSION.get(OsType.ANDROID));

        MobileContentInfo iosMobileContentInfo = new MobileContentInfo()
                .withClientInfo(clientInfo)
                .withMobileContent(mobileContentFromStoreUrl(IOS_STORE_URL)
                        .withClientId(clientInfo.getClientId().asLong())
                        .withMinOsVersion(IOS_MIN_OS_VERSION_FROM_STORE)
                        .withOsType(OsType.IOS)
                        .withIconHash(IOS_ICON_HASH)
                        .withStoreCountry(STORE_COUNTRY)
                        .withStoreContentId(IOS_STORE_CONTENT_ID));
        iosMobileContentInfo = steps.mobileContentSteps().createMobileContent(shard, iosMobileContentInfo);

        MobileAppTracker iosTracker = new MobileAppTracker()
                .withUrl(IOS_TRACKER_URL)
                .withTrackerId(IOS_TRACKER_ID)
                .withImpressionUrl(IOS_IMPRESSION_URL)
                .withTrackingSystem(MobileAppTrackerTrackingSystem.ADJUST);
        MobileAppInfo iosMobileAppInfo = steps.mobileAppSteps()
                .createMobileApp(clientInfo, iosMobileContentInfo, IOS_STORE_URL, iosTracker);
        iosMobileAppId = iosMobileAppInfo.getMobileAppId();
        iosMobileAppName = iosMobileAppInfo.getMobileContentInfo().getMobileContent().getName();
        iosCurrentMinimalOSVersion = iosMobileAppInfo.getMobileApp().getMinimalOperatingSystemVersion();
        iosCurrentMinimalOSVersion =
                nvl(iosCurrentMinimalOSVersion, MobileContentInfoConverter.MIN_OS_VERSION.get(OsType.IOS));

        context = ContextHelper.buildContext(userInfo.getUser()).withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetCampaign_WithAndroidApp() {
        var campaignInfo = createMobileContentCampaign(EnumSet.of(MobileAppDeviceTypeTargeting.PHONE),
                EnumSet.of(MobileAppNetworkTargeting.WI_FI), VALID_GOAL_ID, androidMobileAppId);

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getTypedCampaign().getId());

        Long campaignId = Long.parseLong(rowset.get(0).get("id").toString());
        String name = rowset.get(0).get("name").toString();
        List<String> networkTargeting = (List) rowset.get(0).get("networkTargeting");
        List<String> deviceTypeTargeting = (List) rowset.get(0).get("deviceTypeTargeting");
        Map<String, Object> strategy = (Map<String, Object>) rowset.get(0).get("strategy");

        Map<String, Object> mobileContentInfoRowset = (Map<String, Object>) rowset.get(0).get("mobileContentInfo");
        String storeHref = mobileContentInfoRowset.get("storeHref").toString();
        String minimalOsVersionFromStore = mobileContentInfoRowset.get("minimalOsVersionFromStore").toString();
        String osType = mobileContentInfoRowset.get("osType").toString();
        String storeContentId = mobileContentInfoRowset.get("storeContentId").toString();
        String storeCountry = mobileContentInfoRowset.get("storeCountry").toString();
        String currentMinimalOsVersion = mobileContentInfoRowset.get("currentMinimalOsVersion").toString();
        String mobileContentName = mobileContentInfoRowset.get("mobileContentName").toString();
        String iconHash = mobileContentInfoRowset.get("iconHash").toString();
        String iconUrl = mobileContentInfoRowset.get("iconUrl").toString();

        Map<String, Object> tracker = (Map<String, Object>) mobileContentInfoRowset.get("tracker");
        String url = tracker.get("url").toString();
        String impressionUrl = tracker.get("impressionUrl").toString();
        String trackerId = tracker.get("trackerId").toString();
        String trackingSystem = tracker.get("trackingSystem").toString();

        String expectedIconUrl = generateUrlString(MobileContentInfoConverter.AVATARS_MDS_HOST,
                OsType.ANDROID, ANDROID_ICON_HASH, MobileContentAvatarSize.ICON);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(campaignId).as("id кампании")
                    .isEqualTo(campaignInfo.getTypedCampaign().getId());
            soft.assertThat(name).as("название кампании")
                    .isEqualTo(campaignInfo.getTypedCampaign().getName());
            soft.assertThat(minimalOsVersionFromStore).as("минимальная версия ОС в store")
                    .isEqualTo(ANDROID_MIN_OS_VERSION_FROM_STORE);
            soft.assertThat(storeHref).as("URL приложения")
                    .isEqualTo(ANDROID_STORE_URL);
            soft.assertThat(networkTargeting).as("таргетинг на тип подключения к сети")
                    .containsExactly(WI_FI.name());
            soft.assertThat(deviceTypeTargeting).as("таргетинг на мобильное устройство")
                    .containsExactly(PHONE.name());
            soft.assertThat(osType).as("тип ОС и store")
                    .isEqualTo(OsType.ANDROID.name());
            soft.assertThat(storeContentId).as("идентификатор приложения в store")
                    .isEqualTo(ANDROID_STORE_CONTENT_ID);
            soft.assertThat(storeCountry).as("страна стора")
                    .isEqualTo(STORE_COUNTRY);
            soft.assertThat(currentMinimalOsVersion).as("минимальная версия ОС приложения")
                    .isEqualTo(androidCurrentMinimalOSVersion);
            soft.assertThat(mobileContentName).as("название контента из стора")
                    .isEqualTo(androidMobileAppName);
            soft.assertThat(strategy).as("цель стратегии")
                    .containsEntry("goalId", VALID_GOAL_ID);
            soft.assertThat(iconHash).as("хеш иконки приложения")
                    .isEqualTo(ANDROID_ICON_HASH);
            soft.assertThat(iconUrl).as("url иконки приложения")
                    .isEqualTo(expectedIconUrl);
            soft.assertThat(url).as("ссылка трекера")
                    .isEqualTo(ANDROID_TRACKER_URL);
            soft.assertThat(impressionUrl).as("ссылка трекера на показ")
                    .isEqualTo(ANDROID_IMPRESSION_URL);
            soft.assertThat(trackerId).as("id трекера")
                    .isEqualTo(ANDROID_TRACKER_ID);
            soft.assertThat(trackingSystem).as("тип трекера")
                    .isEqualTo(GdTrackingSystem.ADJUST.name());
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetCampaign_WithIosApp() {
        var campaignInfo = createMobileContentCampaign(EnumSet.of(MobileAppDeviceTypeTargeting.TABLET),
                EnumSet.of(MobileAppNetworkTargeting.WI_FI, MobileAppNetworkTargeting.CELLULAR), VALID_GOAL_ID,
                iosMobileAppId);

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getTypedCampaign().getId());

        Long campaignId = Long.parseLong(rowset.get(0).get("id").toString());
        String name = rowset.get(0).get("name").toString();
        List<String> networkTargeting = (List) rowset.get(0).get("networkTargeting");
        List<String> deviceTypeTargeting = (List) rowset.get(0).get("deviceTypeTargeting");
        Map<String, Object> strategy = (Map<String, Object>) rowset.get(0).get("strategy");

        Map<String, Object> mobileContentInfoRowset = (Map<String, Object>) rowset.get(0).get("mobileContentInfo");
        String storeHref = mobileContentInfoRowset.get("storeHref").toString();
        String minimalOsVersionFromStore = mobileContentInfoRowset.get("minimalOsVersionFromStore").toString();
        String osType = mobileContentInfoRowset.get("osType").toString();
        String storeContentId = mobileContentInfoRowset.get("storeContentId").toString();
        String storeCountry = mobileContentInfoRowset.get("storeCountry").toString();
        String currentMinimalOsVersion = mobileContentInfoRowset.get("currentMinimalOsVersion").toString();
        String mobileContentName = mobileContentInfoRowset.get("mobileContentName").toString();
        String iconHash = mobileContentInfoRowset.get("iconHash").toString();
        String iconUrl = mobileContentInfoRowset.get("iconUrl").toString();

        Map<String, Object> tracker = (Map<String, Object>) mobileContentInfoRowset.get("tracker");
        String url = tracker.get("url").toString();
        String impressionUrl = tracker.get("impressionUrl").toString();
        String trackerId = tracker.get("trackerId").toString();
        String trackingSystem = tracker.get("trackingSystem").toString();

        String expectedIconUrl = generateUrlString(MobileContentInfoConverter.AVATARS_MDS_HOST,
                OsType.IOS, IOS_ICON_HASH, MobileContentAvatarSize.ICON);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(campaignId).as("id кампании")
                    .isEqualTo(campaignInfo.getTypedCampaign().getId());
            soft.assertThat(name).as("название кампании")
                    .isEqualTo(campaignInfo.getTypedCampaign().getName());
            soft.assertThat(minimalOsVersionFromStore).as("минимальная версия ОС в store")
                    .isEqualTo(IOS_MIN_OS_VERSION_FROM_STORE);
            soft.assertThat(storeHref).as("URL приложения")
                    .isEqualTo(IOS_STORE_URL);
            soft.assertThat(networkTargeting).as("таргетинг на тип подключения к сети")
                    .containsExactlyInAnyOrder(WI_FI.name(), CELLULAR.name());
            soft.assertThat(deviceTypeTargeting).as("таргетинг на мобильное устройство")
                    .containsExactly(TABLET.name());
            soft.assertThat(osType).as("тип ОС и store")
                    .isEqualTo(OsType.IOS.name());
            soft.assertThat(storeContentId).as("идентификатор приложения в store")
                    .isEqualTo(IOS_STORE_CONTENT_ID);
            soft.assertThat(storeCountry).as("страна стора")
                    .isEqualTo(STORE_COUNTRY);
            soft.assertThat(currentMinimalOsVersion).as("минимальная версия ОС приложения")
                    .isEqualTo(iosCurrentMinimalOSVersion);
            soft.assertThat(mobileContentName).as("название контента из стора")
                    .isEqualTo(iosMobileAppName);
            soft.assertThat(strategy).as("цель стратегии")
                    .containsEntry("goalId", VALID_GOAL_ID);
            soft.assertThat(iconHash).as("хеш иконки приложения")
                    .isEqualTo(IOS_ICON_HASH);
            soft.assertThat(iconUrl).as("url иконки приложения")
                    .isEqualTo(expectedIconUrl);
            soft.assertThat(url).as("ссылка трекера")
                    .isEqualTo(IOS_TRACKER_URL);
            soft.assertThat(impressionUrl).as("ссылка трекера на показ")
                    .isEqualTo(IOS_IMPRESSION_URL);
            soft.assertThat(trackerId).as("id трекера")
                    .isEqualTo(IOS_TRACKER_ID);
            soft.assertThat(trackingSystem).as("тип трекера")
                    .isEqualTo(GdTrackingSystem.ADJUST.name());
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetCampaign_WithoutTracker() {
        MobileAppInfo mobileAppInfo = steps.mobileAppSteps().createDefaultMobileApp(clientInfo);

        var campaignInfo = createMobileContentCampaign(EnumSet.of(MobileAppDeviceTypeTargeting.PHONE),
                EnumSet.of(MobileAppNetworkTargeting.WI_FI), VALID_GOAL_ID, mobileAppInfo.getMobileAppId());

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getTypedCampaign().getId());
        Map<String, Object> mobileContentInfoRowset = (Map<String, Object>) rowset.get(0).get("mobileContentInfo");
        Map<String, Object> tracker = (Map<String, Object>) mobileContentInfoRowset.get("tracker");

        assertThat(tracker).isNull();
    }

    public static Object[] networkTargetingParameters() {
        return new Object[][]{
                {"Передан список из WI_FI", EnumSet.of(MobileAppNetworkTargeting.WI_FI)},
                {"Передан список из CELLULAR", EnumSet.of(MobileAppNetworkTargeting.CELLULAR)},
                {"Передан список из WI_FI,CELLULAR",
                        EnumSet.of(MobileAppNetworkTargeting.WI_FI, MobileAppNetworkTargeting.CELLULAR)},
        };
    }

    /**
     * Проверка получения разных значений таргетинга на тип подключения к сети (networkTargeting)
     */
    @Test
    @Parameters(method = "networkTargetingParameters")
    @TestCaseName("{0}")
    @SuppressWarnings("unchecked")
    public void getCampaign_CheckNetworkTargeting(@SuppressWarnings("unused") String description,
                                                  EnumSet<MobileAppNetworkTargeting> networkTargeting) {
        var campaignInfo = createMobileContentCampaign(EnumSet.of(MobileAppDeviceTypeTargeting.PHONE),
                networkTargeting, VALID_GOAL_ID, androidMobileAppId);

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getTypedCampaign().getId());

        Set<String> networkTargetingFromRequest = new HashSet<>((List) rowset.get(0).get("networkTargeting"));

        assertThat(networkTargetingFromRequest).as("таргетинг на тип подключения к сети")
                .containsExactlyElementsOf(mapSet(networkTargeting, Enum::name));
    }

    public static Object[] deviceTypeTargetingParameters() {
        return new Object[][]{
                {"Передан список из PHONE", EnumSet.of(MobileAppDeviceTypeTargeting.PHONE)},
                {"Передан список из TABLET", EnumSet.of(MobileAppDeviceTypeTargeting.TABLET)},
                {"Передан список из PHONE,TABLET",
                        EnumSet.of(MobileAppDeviceTypeTargeting.PHONE, MobileAppDeviceTypeTargeting.TABLET)}
        };
    }

    /**
     * Проверка получения разных значений таргетинга на мобильное устройство (DeviceTypeTargeting)
     */
    @Test
    @Parameters(method = "deviceTypeTargetingParameters")
    @TestCaseName("{0}")
    @SuppressWarnings("unchecked")
    public void getCampaign_CheckDeviceTypeTargeting(@SuppressWarnings("unused") String description,
                                                     EnumSet<MobileAppDeviceTypeTargeting> deviceTypeTargetings) {
        var campaignInfo = createMobileContentCampaign(deviceTypeTargetings,
                EnumSet.of(MobileAppNetworkTargeting.WI_FI), VALID_GOAL_ID, androidMobileAppId);

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getTypedCampaign().getId());

        Set<String> deviceTypeTargetingFromRequest = new HashSet<>((List) rowset.get(0).get("deviceTypeTargeting"));

        assertThat(deviceTypeTargetingFromRequest).as("таргетинг на мобильное устройство")
                .containsExactlyElementsOf(mapSet(deviceTypeTargetings, Enum::name));
    }

    /**
     * Проверка получения минимальная версия ОС в store, когда в базе нет этого значения для приложения
     * -> возвращается наименьшее значение
     */
    @Test
    @SuppressWarnings("unchecked")
    public void getCampaign_WithoutMinOs() {
        var campaignInfo = createMobileContentCampaign(
                EnumSet.of(MobileAppDeviceTypeTargeting.PHONE),
                EnumSet.of(MobileAppNetworkTargeting.WI_FI), VALID_GOAL_ID, INVALID_MOBILE_APP_ID);

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getTypedCampaign().getId());

        Map<String, Object> mobileContentInfoRowset = (Map<String, Object>) rowset.get(0).get("mobileContentInfo");
        String minimalOsVersionFromStore = mobileContentInfoRowset.get("minimalOsVersionFromStore").toString();

        assertThat(minimalOsVersionFromStore).as("минимальная версия ОС в store")
                .isEqualTo("1.0");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getCampaign_WithoutIcons() {
        var campaignInfo = createMobileContentCampaign(
                EnumSet.of(MobileAppDeviceTypeTargeting.PHONE),
                EnumSet.of(MobileAppNetworkTargeting.WI_FI), VALID_GOAL_ID, INVALID_MOBILE_APP_ID);

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getTypedCampaign().getId());

        Map<String, Object> mobileContentInfoRowset = (Map<String, Object>) rowset.get(0).get("mobileContentInfo");
        Object iconHash = mobileContentInfoRowset.get("iconHash");
        Object iconUrl = mobileContentInfoRowset.get("iconUrl");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(iconHash).as("хеш иконки приложения")
                    .isNull();
            soft.assertThat(iconUrl).as("url иконки приложения")
                    .isNull();
        });
    }

    /**
     * Проверка получения default цели CPI стратегии, когда в базе нет цели для данной кампании
     */
    @Test
    @SuppressWarnings("unchecked")
    public void getCampaign_WithCpiStrategyAndWithoutGoalId() {
        var campaignInfo = createMobileContentCampaign(
                EnumSet.of(MobileAppDeviceTypeTargeting.PHONE),
                EnumSet.of(MobileAppNetworkTargeting.WI_FI), null, androidMobileAppId);

        List<Map<String, Object>> rowset = sendRequestAndGetRowset(campaignInfo.getTypedCampaign().getId());

        Map<String, Object> strategy = (Map<String, Object>) rowset.get(0).get("strategy");

        assertThat(strategy).as("цель стратегии")
                .containsEntry("goalId", CampaignConstants.DEFAULT_CPI_GOAL_ID);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> sendRequestAndGetRowset(Long campaignId) {
        GdCampaignsContainer campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        campaignsContainer.getFilter().setCampaignIdIn(singleton(campaignId));

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));

        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());

        Map<String, Object> clientData = (Map<String, Object>) ((Map<String, Object>) result.getData()).get("client");
        Map<String, Object> adGroupsData = (Map<String, Object>) clientData.get("campaigns");
        return (List<Map<String, Object>>) adGroupsData.get("rowset");
    }

    private MobileContentCampaignInfo createMobileContentCampaign(
            EnumSet<MobileAppDeviceTypeTargeting> mobileAppDeviceTypeTargetings,
            EnumSet<MobileAppNetworkTargeting> mobileAppNetworkTargetings,
            @Nullable Long cpiStrategyGoalId, Long mobileAppId) {
        return steps.mobileContentCampaignSteps().createCampaign(clientInfo,
                defaultMobileContentCampaignWithSystemFields(clientInfo)
                        .withStrategy((DbStrategy) averageCpiStrategy(cpiStrategyGoalId)
                                .withAutobudget(CampaignsAutobudget.YES))
                        .withDeviceTypeTargeting(mobileAppDeviceTypeTargetings)
                        .withNetworkTargeting(mobileAppNetworkTargetings)
                        .withMobileAppId(mobileAppId));
    }
}
