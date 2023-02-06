package ru.yandex.market.crm.platform.mappers;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.Gender;
import ru.yandex.market.crm.platform.commons.MobilePlatform;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.MetrikaMobileApp;
import ru.yandex.market.crm.platform.models.MetrikaMobileApp.AppInfo;
import ru.yandex.market.crm.platform.models.MetrikaMobileApp.AppUserInfo;
import ru.yandex.market.crm.platform.models.MetrikaMobileApp.Model;
import ru.yandex.market.crm.platform.models.MetrikaMobileApp.NetworkInfo;
import ru.yandex.market.crm.platform.models.MetrikaMobileApp.ScreenInfo;

import static org.junit.Assert.assertEquals;

public class MetrikaMobileAppMapperTest {

    private MetrikaMobileAppMapper mapper;

    @Before
    public void setUp() {
        mapper = new MetrikaMobileAppMapper(new String[]{"1389598", "1542200"}, value -> true);
    }

    @Test
    public void testMap() {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2019-01-28 " +
                "23:49:12\ttimezone=+0000\tAPIKey=1389598\t" +
                "APIKey128=36ef1512-b352-4415-87bd-cf45c4f7e46b\tAttributionID=1\tAttributionIDUUIDHash" +
                "=10173781769844021688\t" +
                "StartDate=2019-01-29\tUUID=8ba547cbedba8ac4a3954eeeb530bd24\tDeviceID" +
                "=80e16f5f65ca86ddb47c36b173b652e9\t" +
                "UUIDHash=1270081423411478989\tDeviceIDHash=8025896208285132860\tAppPlatform=android\tOperatingSystem" +
                "=android\t" +
                "AppFramework=NATIVE\tAppVersionName=1.32\tAppBuildNumber=1511\tAppDebuggable=false\tKitVersion" +
                "=3002002\t" +
                "Manufacturer=Samsung\tModel=Galaxy S9\tOriginalManufacturer=samsung\tOriginalModel=SM-G960F" +
                "\tOSVersion=8.0.0\t" +
                "OSApiLevel=26\tScreenWidth=2220\tScreenHeight=1080\tScreenDPI=480\tScaleFactor=3\tAndroidID" +
                "=a5afc689dda0a416\t" +
                "ADVID=6d2df242-d112-4124-a7c8-4acbe095fc9c\tLimitAdTracking=false\tClientKitVersion=3002002" +
                "\tKitBuildType=202\t" +
                "KitBuildNumber=11391\tSendTimestamp=1548719351\tReceiveDate=2019-01-29\tReceiveTimestamp=1548719352" +
                "\t" +
                "SessionID=10000000185\tSessionType=SESSION_BACKGROUND\tDeviceIDSessionIDHash=17638049185461894549\t" +
                "StartTime=2019-01-29 01:37:07\tStartTimestamp=1548715027\tStartTimeZone=10800RegionTimeZone=10800" +
                "\tLocale=ru_RU\t" +
                "LocationSource=MISSING\tLocationEnabled=true\tWifiAccessPointState=UNKNOWN\tConnectionType=CONN_CELL" +
                "\tNetworkType=LTE\t" +
                "CountryCode=250\tOperatorID=1\tOperatorName=MTS " +
                "RUS\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\tCells_Lacs=[0]\t" +
                "Cells_CountriesCodes=[250]\tCells_OperatorsIDs=[1]\tCells_OperatorsNames=[\"MTS " +
                "RUS\"]\tCells_AreConnected=[1]\t" +
                "Cells_Types=[DEFAULT]\tCells_PhysicalsCellsIDs=[0]\tSimCards_CountriesCodes=[250" +
                "]\tSimCards_OperatorsIDs=[1]\t" +
                "SimCards_OperatorsNames=[\"MTS RUS\"]\tSimCards_AreRoaming=[0]\tSimCards_IccIDs=[\"\"]\t" +
                "NetworksInterfaces_Names=[\"p2p0\",\"wlan0\"]\tNetworksInterfaces_Macs=[\"26181DAEC98C\"," +
                "\"24181DAEC98C\"]\t" +
                "DeviceType=PHONE\tEventID=501059405783114299\tEventNumber=130\tEventDate=2019-01-29\tEventDateTime" +
                "=2019-01-29 02:49:05\t" +
                "EventTimestamp=1548719345\tEventTimeZone=10800\tEventTimeOffset=4318\tEventType=EVENT_PUSH_TOKEN" +
                "\tEventName=Push token\t" +
                "EventSource=sdk\tEventFirstOccurrence=undefined\tEventValue=dXp5aiavOnI:APA91bG7O" +
                "-LyF3Cb5Cefge4vasjUi3gTU7O4lqy5IZn9FIEhQZU" +
                "4r867vyF-uRtn0AZXLFf1S7Hvui6YePoiOGqoIMkIdX_Hc2OCBoNhwKiT54A2Giwva2XOljt8t1Cx4cWZp3x2QkPc\t" +
                "EventEnvironment={\"appmetrica_push_version\":\"12\"}\tEventEnvironmentParsedParams_Key1" +
                "=[\"appmetrica_push_version\"]\t" +
                "EventEnvironmentParsedParams_Key2=[\"12\"]\tClickDate=1970-01-01\tRegionID=2\tAppID=ru.beru" +
                ".android\tClientIP=::ffff:217.66.152.179\t" +
                "ClientIPHash=2883377048318444598\tClientPort=60346\tSex=1\tAge=25\tPushAndroidGroup=[\"null\"]\t" +
                "PushAndroidGroupEnabled=[1]PushAndroidChannel=[\"SalesChannelId\"," +
                "\"OrdersChannelId\"]\tPushAndroidChannelEnabled=[1,1]\t" +
                "PushAndroidChannelGroup=[0,0]\tPushEnabled=1\tDeduplicationEnabled=1\tProfileAttributeVersion" +
                "=25983126530424962\t" +
                "AccountID=123321123";

        List<MetrikaMobileApp> parsed = mapper.apply(line.getBytes());
        assertEquals(1, parsed.size());

        MetrikaMobileApp expected = MetrikaMobileApp.newBuilder()
                .setUid(Uids.create(UidType.UUID, "8ba547cbedba8ac4a3954eeeb530bd24"))
                .addUids(Uids.create(UidType.PUID, 123321123))
                .setDeviceType(MetrikaMobileApp.DeviceType.PHONE)
                .setDeviceId("80e16f5f65ca86ddb47c36b173b652e9")
                .setDeviceIdHash("8025896208285132860")
                .setAid("6d2df242-d112-4124-a7c8-4acbe095fc9c")
                .setModel(Model.newBuilder()
                        .setManufacturer("Samsung")
                        .setModel("Galaxy S9"))
                .setOriginalModel(Model.newBuilder()
                        .setManufacturer("samsung")
                        .setModel("SM-G960F"))
                .setOs("ANDROID")
                .setOsVersion("8.0.0")
                .setScreenInfo(ScreenInfo.newBuilder()
                        .setDpi(480)
                        .setWidth(2220)
                        .setHeight(1080)
                        .setScale("3"))
                .setLocale("ru_RU")
                .setGeoId(2)
                .setNetworkInfo(NetworkInfo.newBuilder()
                        .setType("LTE")
                        .setMcc(250)
                        .setMnc(1)
                        .setOperator("MTS RUS"))
                .setAppId(1389598)
                .setAppInfo(AppInfo.newBuilder()
                        .setPlatform(MobilePlatform.ANDROID)
                        .setPlatformId("ru.beru.android")
                        .setVersion("1.32")
                        .setBuild("1511"))
                .setPushToken("dXp5aiavOnI:APA91bG7O-LyF3Cb5Cefge4vasjUi3gTU7O4lqy5IZn9FIEhQZU" +
                        "4r867vyF-uRtn0AZXLFf1S7Hvui6YePoiOGqoIMkIdX_Hc2OCBoNhwKiT54A2Giwva2XOljt8t1Cx4cWZp3x2QkPc")
                .setUserInfo(AppUserInfo.newBuilder()
                        .setGender(Gender.MALE)
                        .setAge(25))
                .setUpdateTime("2019-01-29 02:49:05")
                .build();

        assertEquals(expected, parsed.get(0));
    }

    @Test
    public void testParseClientEventWithNoEventValue() throws IOException {
        String line = IOUtils.toString(getClass().getResourceAsStream("with_no_event_value.tskv"));

        List<MetrikaMobileApp> parsed = mapper.apply(line.getBytes());
        assertEquals(1, parsed.size());
    }

    @Test
    public void testSkipEventWithProductApp() {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2019-01-28 " +
                      "23:49:12\ttimezone=+0000\tAPIKey=2780002\t" +
                      "APIKey128=36ef1512-b352-4415-87bd-cf45c4f7e46b\tAttributionID=1\tAttributionIDUUIDHash" +
                      "=10173781769844021688\t" +
                      "StartDate=2019-01-29\tUUID=8ba547cbedba8ac4a3954eeeb530bd24\tDeviceID" +
                      "=80e16f5f65ca86ddb47c36b173b652e9\t" +
                      "UUIDHash=1270081423411478989\tDeviceIDHash=8025896208285132860\tAppPlatform=android\tOperatingSystem" +
                      "=android\t" +
                      "AppFramework=NATIVE\tAppVersionName=1.32\tAppBuildNumber=1511\tAppDebuggable=false\tKitVersion" +
                      "=3002002\t" +
                      "Manufacturer=Samsung\tModel=Galaxy S9\tOriginalManufacturer=samsung\tOriginalModel=SM-G960F" +
                      "\tOSVersion=8.0.0\t" +
                      "OSApiLevel=26\tScreenWidth=2220\tScreenHeight=1080\tScreenDPI=480\tScaleFactor=3\tAndroidID" +
                      "=a5afc689dda0a416\t" +
                      "ADVID=6d2df242-d112-4124-a7c8-4acbe095fc9c\tLimitAdTracking=false\tClientKitVersion=3002002" +
                      "\tKitBuildType=202\t" +
                      "KitBuildNumber=11391\tSendTimestamp=1548719351\tReceiveDate=2019-01-29\tReceiveTimestamp=1548719352" +
                      "\t" +
                      "SessionID=10000000185\tSessionType=SESSION_BACKGROUND\tDeviceIDSessionIDHash=17638049185461894549\t" +
                      "StartTime=2019-01-29 01:37:07\tStartTimestamp=1548715027\tStartTimeZone=10800RegionTimeZone=10800" +
                      "\tLocale=ru_RU\t" +
                      "LocationSource=MISSING\tLocationEnabled=true\tWifiAccessPointState=UNKNOWN\tConnectionType=CONN_CELL" +
                      "\tNetworkType=LTE\t" +
                      "CountryCode=250\tOperatorID=1\tOperatorName=MTS " +
                      "RUS\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\tCells_Lacs=[0]\t" +
                      "Cells_CountriesCodes=[250]\tCells_OperatorsIDs=[1]\tCells_OperatorsNames=[\"MTS " +
                      "RUS\"]\tCells_AreConnected=[1]\t" +
                      "Cells_Types=[DEFAULT]\tCells_PhysicalsCellsIDs=[0]\tSimCards_CountriesCodes=[250" +
                      "]\tSimCards_OperatorsIDs=[1]\t" +
                      "SimCards_OperatorsNames=[\"MTS RUS\"]\tSimCards_AreRoaming=[0]\tSimCards_IccIDs=[\"\"]\t" +
                      "NetworksInterfaces_Names=[\"p2p0\",\"wlan0\"]\tNetworksInterfaces_Macs=[\"26181DAEC98C\"," +
                      "\"24181DAEC98C\"]\t" +
                      "DeviceType=PHONE\tEventID=501059405783114299\tEventNumber=130\tEventDate=2019-01-29\tEventDateTime" +
                      "=2019-01-29 02:49:05\t" +
                      "EventTimestamp=1548719345\tEventTimeZone=10800\tEventTimeOffset=4318\tEventType=EVENT_PUSH_TOKEN" +
                      "\tEventName=Push token\t" +
                      "EventSource=sdk\tEventFirstOccurrence=undefined\tEventValue=dXp5aiavOnI:APA91bG7O" +
                      "-LyF3Cb5Cefge4vasjUi3gTU7O4lqy5IZn9FIEhQZU" +
                      "4r867vyF-uRtn0AZXLFf1S7Hvui6YePoiOGqoIMkIdX_Hc2OCBoNhwKiT54A2Giwva2XOljt8t1Cx4cWZp3x2QkPc\t" +
                      "EventEnvironment={\"appmetrica_push_version\":\"12\"}\tEventEnvironmentParsedParams_Key1" +
                      "=[\"appmetrica_push_version\"]\t" +
                      "EventEnvironmentParsedParams_Key2=[\"12\"]\tClickDate=1970-01-01\tRegionID=2\tAppID=ru.beru" +
                      ".android\tClientIP=::ffff:217.66.152.179\t" +
                      "ClientIPHash=2883377048318444598\tClientPort=60346\tSex=1\tAge=25\tPushAndroidGroup=[\"null\"]\t" +
                      "PushAndroidGroupEnabled=[1]PushAndroidChannel=[\"SalesChannelId\"," +
                      "\"OrdersChannelId\"]\tPushAndroidChannelEnabled=[1,1]\t" +
                      "PushAndroidChannelGroup=[0,0]\tPushEnabled=1\tDeduplicationEnabled=1\tProfileAttributeVersion" +
                      "=25983126530424962\t" +
                      "AccountID=123321123";

        List<MetrikaMobileApp> parsed = mapper.apply(line.getBytes());
        assertEquals(0, parsed.size());
    }
}
