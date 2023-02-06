package ru.yandex.market.logshatter.parser.front;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;


public class MetrikaTskvLogParserTest {
    private LogParserChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new LogParserChecker(new MetrikaTskvLogParser());
        checker.setOrigin("market-health-dev");
        checker.setParam("allowedAppEvents", "EVENT_STATBOX,EVENT_CLIENT");
        checker.setParam(
            "apiKeysToServiceNames",
            "23107:market_front_touch,23104:market_front_touch,1389598:market_front_bluetouch"
        );
    }

    @Test
    public void parseAndroidAppMetric() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-26 10:25:49\t" +
            "timezone=+0000\tAPIKey=23107\tAPIKey128=a996e97c-9bed-4157-b05f-ab6b42fa46b6\tStartDate=2017-10-26\t" +
            "UUID=ce4642cd3123fb75fbf6c1abfeddd13e\tDeviceID=738ba4b7765246ae7f5f8b3992f4569f\t" +
            "UUIDHash=9045334822516910520\tDeviceIDHash=11487695674382257827\tAppPlatform=android\t" +
            "OperatingSystem=android\tAppFramework=NATIVE\tAppVersionName=5.11\tAppBuildNumber=1500\t" +
            "AppDebuggable=undefined\tKitVersion=262\tManufacturer=Samsung\tModel=Galaxy A7\t" +
            "OriginalManufacturer=samsung\tOriginalModel=SM-A700FD\tOSVersion=6.0.1\tOSApiLevel=23\t" +
            "ScreenWidth=1920\tScreenHeight=1080\tScreenDPI=480\tScaleFactor=3\tAndroidID=c7d47a16bb88e5b3\t" +
            "ADVID=71ec9bd6-39da-41e3-b050-c293ba337b5e\tClientKitVersion=262\tKitBuildType=202\t" +
            "KitBuildNumber=6484\tSendTimestamp=1509013550\tSendTimeZone=10800\tReceiveDate=2017-10-26\t" +
            "ReceiveTimestamp=1509013549\tSessionID=1509013541\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=17595071052827490338\t" +
            "StartTime=2017-10-26 13:25:41\tStartTimestamp=1509013541\t" +
            "StartTimeZone=10800\tRegionTimeZone=10800\tLocale=ru_RU\tLocationSource=LBS\t" +
            "Latitude=55.605796813964844\tLongitude=37.490509033203125\tLocationPrecision=324\t" +
            "LocationEnabled=undefined\tWifiAccessPointSsid=AndroidAP\tWifiAccessPointState=DISABLED\t" +
            "ConnectionType=CONN_CELL\tCountryCode=250\tOperatorID=99\t" +
            "Cells_CellsIDs=[197689353,2147483647,2147483647,2147483647,34415,3219,3220]\t" +
            "Cells_SignalsStrengths=[-78,-105,-103,-118,-81,-87,-97]\t" +
            "Cells_Lacs=[21677,2147483647,2147483647,2147483647,7753,7753,7753]\t" +
            "Cells_CountriesCodes=[250,2147483647,2147483647,2147483647,250,250,250]\t" +
            "Cells_OperatorsIDs=[99,2147483647,2147483647,2147483647,2,2,2]\t" +
            "Cells_OperatorsNames=['','','','','','','']\tCells_AreConnected=[1,0,0,0,1,0,0]\t" +
            "Cells_Types=[LTE,LTE,LTE,LTE,GSM,GSM,GSM]\tCells_PhysicalsCellsIDs=[50,414,240,325,0,0,0]\t" +
            "SimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[99]\tSimCards_OperatorsNames=['MegaFon']\t" +
            "SimCards_AreRoaming=[0]\tSimCards_IccIDs=['']\tNetworksInterfaces_Names=['wlan0','p2p0','dummy0']\t" +
            "NetworksInterfaces_Macs=['ACEE9E1017B3','AEEE9E1017B3','5E5B653FA1DB']\tDeviceType=PHONE\t" +
            "EventID=10057862631024411190\tEventNumber=24\tEventDate=2017-10-26\t" +
            "EventDateTime=2017-10-26 13:25:49\t" +
            "EventTimestamp=1509013549\tEventTimeZone=10800\tEventTimeOffset=8\t" +
            "EventType=EVENT_CLIENT\tEventName=TIMERS\t" +
            "EventValue={\"requestId\":\"1509013543308/0afa9b77932cacd6f1ce99ac5229ecda\",\"info\":" +
            "{\"pageId\":\"touch:index\"},\"name\":\"page\"," +
            "\"startTime\":1509013541,\"startTimeInMs\":1509013541123," +
            "\"timestamp\":1509013543,\"timestampInMs\":1509013543123," +
            "\"duration\":5925,\"portion\":\"loaded\",\"yandex_uid\":\"5371758881499875586\"}\t" +
            "EventValueJsonReference={0:\"0\",1:{1:\"1\"},2:\"2\",3:3,4:3,5:5,6:\"6\",7:\"7\"}\tPars" +
            "edParams_Key1=['requestId','info','name','startTime','timestamp','duration','portion','yandex" +
            "_uid']\tParsedParams_Key2=['1509013543308/0afa9b77932cacd6f1ce99ac5229ecda','pageId','page','150901" +
            "3543','1509013543','5925','loaded','5371758881499875586']\tParsedParams_Key3=['','touch:inde" +
            "x','','','','','','']\tParsedParams_Key4=['','','','','','','','']\tParsedParams_Key5=['',''" +
            ",'','','','','','']\tParsedParams_Key6=['','','','','','','','']\tParsedParams_Key7=['','','','','" +
            "','','','']\tParsedParams_Key8=['','','','','','','','']\tParsedParams_Key9=['','','','','','',''" +
            ",'']\tParsedParams_Key10=['','','','','','','','']\tParsedParams_ValueDouble=[0,0,0,1509013543,150901" +
            "3543,5925,0,5371758881499875000]\tParsedParams_Quantity=[1,1,1,1,1,1,1,1]\tClickDate=1970-01-01\tRe" +
            "gionID=114619\tAppID=ru.yandex.market\tClientIP=::ffff:83.220.236.195\tClientPort=58375\tSex=1\t" +
            "Age=45\tDeduplicationEnabled=1\n";

        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1509013543123L),
            "1509013543308/0afa9b77932cacd6f1ce99ac5229ecda", "market_front_touch", "android",
            114619, checker.getHost(), "page", "loaded",
            5925, "touch:index", keys, values, "Galaxy A7", "5.11", "1500",
            1509013543123L, 1509013541, 1509013541123L, "", "::ffff:83.220.236.195",
            "738ba4b7765246ae7f5f8b3992f4569f", "", ""
        );
    }

    @Test
    public void parseIosAppMetric() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 10:09:36\ttimezone=+0000\t" +
            "APIKey=23104\tAPIKey128=d0c8d8a8-9cff-4b00-b891-e081b41953a2\tStartDate=2017-10-12\t" +
            "UUID=99dd8f1afd46529dfdc316e13d881c32\tDeviceID=5B5E6CD9-B009-4635-89CA-A5558A3A3B6F\t" +
            "OriginalDeviceID=6C03EC48-DCA0-46E6-9548-079D042866A6UUIDHash=737899387808438411\t" +
            "DeviceIDHash=16327040839588031404\tIFV=5062B6CA-83A8-4D69-B261-6706CE4E3DF8\tAppPlatform=iOS\t" +
            "AppFramework=NATIVE\tAppVersionName=512\tAppBuildNumber=1613\tAppDebuggable=false\t" +
            "KitVersion=292\tManufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\t" +
            "OriginalModel=iPhone8,1\tOSVersion=11.0\tOSApiLevel=11\tScreenWidth=667\tScreenHeight=375\t" +
            "ScreenDPI=326\tScaleFactor=2\tClientKitVersion=292\tKitBuildType=101\tKitBuildNumber=8707\t" +
            "SendTimestamp=1507802976\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507802976\tSessionID=1507802917\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=7123113273064847859\tStartTime=2017-10-12 13:08:37\t" +
            "StartTimestamp=1507802917\tStartTimeZone=10800\tRegionTimeZone=10800\tLocale=en_RU\t" +
            "LocationSource=LBS\tLatitude=55.733821868896484\tLongitude=37.58698272705078\t" +
            "LocationPrecision=140\tLocationEnabled=undefined\tWifi_Macs=['2CD02D6B0EB0']\t" +
            "Wifi_SignalsStrengths=[0]\tWifi_Ssids=['Yandex']\tWifi_AreConnected=[1]\t" +
            "WifiAccessPointState=UNKNOWN\tConnectionType=CONN_WIFI\tNetworkType=LTE\tCountryCode=250\t" +
            "OperatorID=11\tOperatorName=YOTA\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\t" +
            "Cells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[11]\t" +
            "Cells_OperatorsNames=['YOTA']\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\t" +
            "Cells_PhysicalsCellsIDs=[0]\tDeviceType=PHONE\tEventID=16467856438443608079\tEventNumber=70\t" +
            "EventDate=2017-10-12\tEventDateTime=2017-10-12 13:09:35\tEventTimestamp=1507802975\t" +
            "EventTimeZone=10800\tEventTimeOffset=58\tEventType=EVENT_STATBOX\t" +
            "EventName=скорость > переход между экранами\t" +
            "EventValue={\"info\" : {\"fromPageId\" : \"touch:catalog\",\"toPageId\" : \"touch:product\"}," +
            "\"name\" : \"user-interaction-navigation-transition\",\"startTime\" : 1507802650," +
            "\"requestId\" : \"1507802975099/70DF58477F0440C48B20027DB0B040ED\",  " +
            "\"timestamp\" : 1507802656653,  \"duration\" : 240}\tRegionID=120542\t" +
            "AppID=ru.yandex.ymarket.inhouse\tClientIP=2a02:6b8:0:40c:cdf8:1438:8fe1:3552\tClientPort=58691\t" +
            "Sex=1\tAge=25\tAccountID=285830221\tAccountType=login\tDeduplicationEnabled=1\n";

        String[] keys = {"fromPageId", "toPageId"};
        String[] values = {"touch:catalog", "touch:product"};

        checker.check(
            line,
            new Date(1507802656653L),
            "1507802975099/70DF58477F0440C48B20027DB0B040ED", "market_front_touch", "iOS",
            120542, checker.getHost(), "user-interaction-navigation-transition", "",
            240, "", keys, values, "iPhone 6s", "512", "1613", 1507802656653L, 1507802650, 1507802650000L, "",
            "2a02:6b8:0:40c:cdf8:1438:8fe1:3552", "5B5E6CD9-B009-4635-89CA-A5558A3A3B6F", "", ""
        );
    }

    @Test
    public void parseBlueIosAppMetric() throws Exception {
        // TODO: настоящую строку
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2018-03-16 12:49:00\ttimezone=+0000\t" +
            "APIKey=1389598\tAPIKey128=36ef1512-b352-4415-87bd-cf45c4f7e46b\tStartDate=2018-03-16\t" +
            "UUID=e3afbc1ae734b2958c0fe19dd878972b\tDeviceID=BD57C292-A54C-498D-B2FB-C1D1A99604EE\t" +
            "OriginalDeviceID=156A0C25-3EDA-4DCD-A27A-1CF04782C509\tUUIDHash=4313577421739258724\t" +
            "DeviceIDHash=2790121906978891454\tIFV=6B52C34A-142F-44CF-80FF-96442D27EF10\tAppPlatform=iOS\t" +
            "OperatingSystem=ios\tAppFramework=NATIVE\tAppVersionName=102\tAppBuildNumber=1613\tAppDebuggable=true\t" +
            "KitVersion=296\tManufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\t" +
            "OriginalModel=iPhone 6s\t" +
            "OSVersion=11.2\tOSApiLevel=11\tScreenWidth=568\tScreenHeight=320\tScreenDPI=326\tScaleFactor=2\t" +
            "LimitAdTracking=false\tClientKitVersion=296\tKitBuildType=101\tKitBuildNumber=9251\t" +
            "SendTimestamp=1521204540\tSendTimeZone=10800\tReceiveDate=2018-03-16\tReceiveTimestamp=1521204540\t" +
            "SessionID=1521204504\tSessionType=SESSION_FOREGROUND\tDeviceIDSessionIDHash=4382274440071578399\t" +
            "StartTime=2018-03-16 15:48:24\tStartTimestamp=1521204504\tStartTimeZone=10800\tRegionTimeZone=-25200\t" +
            "Locale=en_US\tLocationSource=GPS\tLatitude=37.33233141\tLongitude=-122.0312186\t" +
            "LocationTimestamp=1521204502\tLocationPrecision=5\tLocationEnabled=true\tWifiAccessPointState=UNKNOWN\t" +
            "ConnectionType=CONN_WIFI\tOperatorName=Carrier\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\t" +
            "Cells_Lacs=[0]\tCells_CountriesCodes=[0]\tCells_OperatorsIDs=[0]\tCells_OperatorsNames=[\"Carrier\"]\t" +
            "Cells_AreConnected=[1]\tCells_Types=[DEFAULT]\tCells_PhysicalsCellsIDs=[0]\tDeviceType=PHONE\t" +
            "IsRooted=1\tEventID=6896685538951892436\tEventNumber=10\tEventDate=2018-03-16\t" +
            "EventDateTime=2018-03-16 15:48:59\tEventTimestamp=1521204539\tEventTimeZone=10800\t" +
            "EventTimeOffset=35\tEventType=EVENT_STATBOX\tEventName=скорость > переход\tEventSource=sdk\t" +
            "EventValue={  \"startTime\" : \"1521204539331\",  \"timestamp\" : \"1521204539331\",  \"requestId\" :" +
            " \"15212045017420E9944443E3A47BBB488F8FBCC21BFBB\",  \"name\" : \"show-cart\",  \"duration\" : \"312\"" +
            "}\tClickDate=1970-01-01\tRegionID=103464\tAppID=ru.yandex.blue.market.inhouse\t" +
            "ClientIP=2a02:6b8:0:40c:cdf8:1438:8fe1:3552\tClientPort=61377\tDeduplicationEnabled=1\t" +
            "ProfileAttributeVersion=25521576543780874\n";

        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1521204539331L),
            "15212045017420E9944443E3A47BBB488F8FBCC21BFBB", "market_front_bluetouch", "iOS",
            103464, checker.getHost(), "show-cart", "",
            312, "", keys, values, "iPhone 6s", "102", "1613", 1521204539331L, 1521204539, 1521204539331L, "",
            "2a02:6b8:0:40c:cdf8:1438:8fe1:3552", "BD57C292-A54C-498D-B2FB-C1D1A99604EE", "", ""
        );
    }

    @Test
    public void parseIosAppProperTimestampsMetric() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 10:09:36\ttimezone=+0000\t" +
            "APIKey=23104\tAPIKey128=d0c8d8a8-9cff-4b00-b891-e081b41953a2\tStartDate=2017-10-12\t" +
            "UUID=99dd8f1afd46529dfdc316e13d881c32\tDeviceID=5B5E6CD9-B009-4635-89CA-A5558A3A3B6F\t" +
            "OriginalDeviceID=6C03EC48-DCA0-46E6-9548-079D042866A6UUIDHash=737899387808438411\t" +
            "DeviceIDHash=16327040839588031404\tIFV=5062B6CA-83A8-4D69-B261-6706CE4E3DF8\tAppPlatform=iOS\t" +
            "AppFramework=NATIVE\tAppVersionName=512\tAppBuildNumber=1613\tAppDebuggable=false\t" +
            "KitVersion=292\tManufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\t" +
            "OriginalModel=iPhone8,1\tOSVersion=11.0\tOSApiLevel=11\tScreenWidth=667\tScreenHeight=375\t" +
            "ScreenDPI=326\tScaleFactor=2\tClientKitVersion=292\tKitBuildType=101\tKitBuildNumber=8707\t" +
            "SendTimestamp=1507802976\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507802976\tSessionID=1507802917\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=7123113273064847859\tStartTime=2017-10-12 13:08:37\t" +
            "StartTimestamp=1507802917\tStartTimeZone=10800\tRegionTimeZone=10800\tLocale=en_RU\t" +
            "LocationSource=LBS\tLatitude=55.733821868896484\tLongitude=37.58698272705078\t" +
            "LocationPrecision=140\tLocationEnabled=undefined\tWifi_Macs=['2CD02D6B0EB0']\t" +
            "Wifi_SignalsStrengths=[0]\tWifi_Ssids=['Yandex']\tWifi_AreConnected=[1]\t" +
            "WifiAccessPointState=UNKNOWN\tConnectionType=CONN_WIFI\tNetworkType=LTE\tCountryCode=250\t" +
            "OperatorID=11\tOperatorName=YOTA\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\t" +
            "Cells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[11]\t" +
            "Cells_OperatorsNames=['YOTA']\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\t" +
            "Cells_PhysicalsCellsIDs=[0]\tDeviceType=PHONE\tEventID=16467856438443608079\tEventNumber=70\t" +
            "EventDate=2017-10-12\tEventDateTime=2017-10-12 13:09:35\tEventTimestamp=1507802975\t" +
            "EventTimeZone=10800\tEventTimeOffset=58\tEventType=EVENT_STATBOX\t" +
            "EventName=скорость > переход между экранами\t" +
            "EventValue={\"info\" : {\"fromPageId\" : \"touch:catalog\",\"toPageId\" : \"touch:product\"}," +
            "\"name\" : \"user-interaction-navigation-transition\"," +
            "\"startTime\" : 1507802650,\"startTimeInMs\" : 1507802650123," +
            "\"requestId\" : \"1507802975099/70DF58477F0440C48B20027DB0B040ED\",  " +
            "\"timestamp\" : 1507802657,\"timestampInMs\" : 1507802656653,  \"duration\" : 240}\t" +
            "RegionID=120542\t" +
            "AppID=ru.yandex.ymarket.inhouse\tClientIP=2a02:6b8:0:40c:cdf8:1438:8fe1:3552\tClientPort=58691\t" +
            "Sex=1\tAge=25\tAccountID=285830221\tAccountType=login\tDeduplicationEnabled=1\n";

        String[] keys = {"fromPageId", "toPageId"};
        String[] values = {"touch:catalog", "touch:product"};

        checker.check(
            line,
            new Date(1507802656653L),
            "1507802975099/70DF58477F0440C48B20027DB0B040ED", "market_front_touch", "iOS",
            120542, checker.getHost(), "user-interaction-navigation-transition", "",
            240, "", keys, values, "iPhone 6s", "512", "1613", 1507802656653L, 1507802650, 1507802650123L, "",
            "2a02:6b8:0:40c:cdf8:1438:8fe1:3552", "5B5E6CD9-B009-4635-89CA-A5558A3A3B6F", "", ""
        );
    }

    @Test
    public void parseAndroidAppNativeMetric() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 09:52:46\ttimezone=+0000\t" +
            "APIKey=23107\tAPIKey128=a996e97c-9bed-4157-b05f-ab6b42fa46b6\tStartDate=2017-10-12\t" +
            "UUID=31b72cdd9a671e34f6fd577280cac2d0\tDeviceID=56852bf8e80d0eec978d77e8b0334e8f\t" +
            "UUIDHash=2138176770510822151\tDeviceIDHash=17444363333799911657\tAppPlatform=android\t" +
            "AppFramework=NATIVE\tAppVersionName=5.11\tAppBuildNumber=1499\tAppDebuggable=undefined\t" +
            "KitVersion=262\tManufacturer=Samsung\tModel=Galaxy S8\tOriginalManufacturer=samsung\t" +
            "OriginalModel=SM-G950F\tOSVersion=7.0\tOSApiLevel=24\tScreenWidth=2960\tScreenHeight=1440\t" +
            "ScreenDPI=640\tScaleFactor=4\tAndroidID=fee65181b916abe9\t" +
            "ADVID=fef5d0d2-7068-4db2-b7ae-de859145fdfa\tClientKitVersion=262\tKitBuildType=202\t" +
            "KitBuildNumber=6484\tSendTimestamp=1507802022\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507801966\tSessionID=1507801973\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=6772906628173958217\tStartTime=2017-10-12 12:52:46\t" +
            "StartTimestamp=1507801966\tStartTimeZone=10800\tStartTimeCorrected=1\tRegionTimeZone=10800\t" +
            "Locale=en_US\tLocationSource=MISSING\tLocationEnabled=undefined\tWifiAccessPointSsid=AndroidAP\t" +
            "WifiAccessPointState=DISABLED\tConnectionType=CONN_WIFI\tCountryCode=250\tOperatorID=1\t" +
            "OperatorName=MTS RUS\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[-95]\tCells_Lacs=[0]\t" +
            "Cells_CountriesCodes=[250]\tCells_OperatorsIDs=[1]\tCells_OperatorsNames=['MTS RUS']\t" +
            "Cells_AreConnected=[1]Cells_Types=[DEFAULT]\tCells_PhysicalsCellsIDs=[0]\t" +
            "SimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[1]\tSimCards_OperatorsNames=['MTS RUS']\t" +
            "SimCards_AreRoaming=[0]\tSimCards_IccIDs=['']\tNetworksInterfaces_Names=['p2p0','wlan0']\t" +
            "NetworksInterfaces_Macs=['32074D603481','30074D603481']\tDeviceType=PHONE\t" +
            "EventID=1460827329979314175\tEventNumber=73\tEventDate=2017-10-12\t" +
            "EventDateTime=2017-10-12 12:53:33\tEventTimestamp=1507802013\tEventTimeZone=10800\t" +
            "EventTimeOffset=47\tEventType=EVENT_STATBOX\tEventValue={" +
            "\"requestId\":\"1507802021872/995d5e7aa8654f30b26ed4c3f864bb14\",\"startTime\":1507802016," +
            "\"name\":\"start-auth\",\"timestamp\":1507802017413,\"duration\":1422}\tRegionID=9999\t" +
            "AppID=ru.yandex.market\tClientIP=2a02:6b8:0:402:7cb0:eac6:fb1d:6fd8\tClientPort=38548\t" +
            "Age=25\tAccountID=154845656\tAccountType=login\tDeduplicationEnabled=1\n";

        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1507802017413L),
            "1507802021872/995d5e7aa8654f30b26ed4c3f864bb14", "market_front_touch", "android",
            9999, checker.getHost(), "start-auth", "",
            1422, "", keys, values, "Galaxy S8", "5.11", "1499", 1507802017413L, 1507802016, 1507802016000L, "",
            "2a02:6b8:0:402:7cb0:eac6:fb1d:6fd8", "56852bf8e80d0eec978d77e8b0334e8f", "", ""
        );
    }

    @Test
    public void iosSkipWrongEventType() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 10:09:36\ttimezone=+0000\t" +
            "APIKey=23104\tAPIKey128=d0c8d8a8-9cff-4b00-b891-e081b41953a2\tStartDate=2017-10-12\t" +
            "UUID=99dd8f1afd46529dfdc316e13d881c32\tDeviceID=5B5E6CD9-B009-4635-89CA-A5558A3A3B6F\t" +
            "OriginalDeviceID=6C03EC48-DCA0-46E6-9548-079D042866A6UUIDHash=737899387808438411\t" +
            "DeviceIDHash=16327040839588031404\tIFV=5062B6CA-83A8-4D69-B261-6706CE4E3DF8\tAppPlatform=iOS\t" +
            "AppFramework=NATIVE\tAppVersionName=512\tAppBuildNumber=1613\tAppDebuggable=false\t" +
            "KitVersion=292\tManufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\t" +
            "OriginalModel=iPhone8,1\tOSVersion=11.0\tOSApiLevel=11\tScreenWidth=667\tScreenHeight=375\t" +
            "ScreenDPI=326\tScaleFactor=2\tClientKitVersion=292\tKitBuildType=101\tKitBuildNumber=8707\t" +
            "SendTimestamp=1507802976\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507802976\tSessionID=1507802917\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=7123113273064847859\tStartTime=2017-10-12 13:08:37\t" +
            "StartTimestamp=1507802917\tStartTimeZone=10800\tRegionTimeZone=10800\tLocale=en_RU\t" +
            "LocationSource=LBS\tLatitude=55.733821868896484\tLongitude=37.58698272705078\t" +
            "LocationPrecision=140\tLocationEnabled=undefined\tWifi_Macs=['2CD02D6B0EB0']\t" +
            "Wifi_SignalsStrengths=[0]\tWifi_Ssids=['Yandex']\tWifi_AreConnected=[1]\t" +
            "WifiAccessPointState=UNKNOWN\tConnectionType=CONN_WIFI\tNetworkType=LTE\tCountryCode=250\t" +
            "OperatorID=11\tOperatorName=YOTA\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\t" +
            "Cells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[11]\t" +
            "Cells_OperatorsNames=['YOTA']\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\t" +
            "Cells_PhysicalsCellsIDs=[0]\tDeviceType=PHONE\tEventID=16467856438443608079\tEventNumber=70\t" +
            "EventDate=2017-10-12\tEventDateTime=2017-10-12 13:09:35\tEventTimestamp=1507802975\t" +
            "EventTimeZone=10800\tEventTimeOffset=58\tEventType=EVENT_OLOLO\t" +
            "EventName=ololo\t" +
            "EventValue={\"info\" : {\"fromPageId\" : \"touch:catalog\",\"toPageId\" : \"touch:product\"}," +
            "\"name\" : \"user-interaction-navigation-transition\"," +
            "\"startTime\" : 1507802650,\"startTimeInMs\" : 1507802650123," +
            "\"requestId\" : \"1507802975099/70DF58477F0440C48B20027DB0B040ED\",  " +
            "\"timestamp\" : 1507802657,\"timestampInMs\" : 1507802656653,  \"duration\" : 240}\t" +
            "RegionID=120542\t" +
            "AppID=ru.yandex.ymarket.inhouse\tClientIP=2a02:6b8:0:40c:cdf8:1438:8fe1:3552\tClientPort=58691\t" +
            "Sex=1\tAge=25\tAccountID=285830221\tAccountType=login\tDeduplicationEnabled=1\n";

        checker.checkEmpty(line);
    }

    @Test
    public void appSkipNoApiKey() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 10:09:36\ttimezone=+0000\t" +
            "APIKey128=d0c8d8a8-9cff-4b00-b891-e081b41953a2\tStartDate=2017-10-12\t" +
            "UUID=99dd8f1afd46529dfdc316e13d881c32\tDeviceID=5B5E6CD9-B009-4635-89CA-A5558A3A3B6F\t" +
            "OriginalDeviceID=6C03EC48-DCA0-46E6-9548-079D042866A6UUIDHash=737899387808438411\t" +
            "DeviceIDHash=16327040839588031404\tIFV=5062B6CA-83A8-4D69-B261-6706CE4E3DF8\tAppPlatform=iOS\t" +
            "AppFramework=NATIVE\tAppVersionName=512\tAppBuildNumber=1613\tAppDebuggable=false\t" +
            "KitVersion=292\tManufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\t" +
            "OriginalModel=iPhone8,1\tOSVersion=11.0\tOSApiLevel=11\tScreenWidth=667\tScreenHeight=375\t" +
            "ScreenDPI=326\tScaleFactor=2\tClientKitVersion=292\tKitBuildType=101\tKitBuildNumber=8707\t" +
            "SendTimestamp=1507802976\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507802976\tSessionID=1507802917\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=7123113273064847859\tStartTime=2017-10-12 13:08:37\t" +
            "StartTimestamp=1507802917\tStartTimeZone=10800\tRegionTimeZone=10800\tLocale=en_RU\t" +
            "LocationSource=LBS\tLatitude=55.733821868896484\tLongitude=37.58698272705078\t" +
            "LocationPrecision=140\tLocationEnabled=undefined\tWifi_Macs=['2CD02D6B0EB0']\t" +
            "Wifi_SignalsStrengths=[0]\tWifi_Ssids=['Yandex']\tWifi_AreConnected=[1]\t" +
            "WifiAccessPointState=UNKNOWN\tConnectionType=CONN_WIFI\tNetworkType=LTE\tCountryCode=250\t" +
            "OperatorID=11\tOperatorName=YOTA\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\t" +
            "Cells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[11]\t" +
            "Cells_OperatorsNames=['YOTA']\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\t" +
            "Cells_PhysicalsCellsIDs=[0]\tDeviceType=PHONE\tEventID=16467856438443608079\tEventNumber=70\t" +
            "EventDate=2017-10-12\tEventDateTime=2017-10-12 13:09:35\tEventTimestamp=1507802975\t" +
            "EventTimeZone=10800\tEventTimeOffset=58\tEventType=EVENT_STATBOX\t" +
            "EventName=скорость > переход между экранами\t" +
            "EventValue={\"info\" : {\"fromPageId\" : \"touch:catalog\",\"toPageId\" : \"touch:product\"}," +
            "\"name\" : \"user-interaction-navigation-transition\",\"startTime\" : 1507802650," +
            "\"requestId\" : \"1507802975099/70DF58477F0440C48B20027DB0B040ED\",  " +
            "\"timestamp\" : 1507802656653,  \"duration\" : 240}\tRegionID=120542\t" +
            "AppID=ru.yandex.ymarket.inhouse\tClientIP=2a02:6b8:0:40c:cdf8:1438:8fe1:3552\tClientPort=58691\t" +
            "Sex=1\tAge=25\tAccountID=285830221\tAccountType=login\tDeduplicationEnabled=1\n";

        checker.checkEmpty(line);
    }

    @Test
    public void appSkipUnknownApiKey() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 10:09:36\ttimezone=+0000\t" +
            "APIKey=123456\tAPIKey128=d0c8d8a8-9cff-4b00-b891-e081b41953a2\tStartDate=2017-10-12\t" +
            "UUID=99dd8f1afd46529dfdc316e13d881c32\tDeviceID=5B5E6CD9-B009-4635-89CA-A5558A3A3B6F\t" +
            "OriginalDeviceID=6C03EC48-DCA0-46E6-9548-079D042866A6UUIDHash=737899387808438411\t" +
            "DeviceIDHash=16327040839588031404\tIFV=5062B6CA-83A8-4D69-B261-6706CE4E3DF8\tAppPlatform=iOS\t" +
            "AppFramework=NATIVE\tAppVersionName=512\tAppBuildNumber=1613\tAppDebuggable=false\t" +
            "KitVersion=292\tManufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\t" +
            "OriginalModel=iPhone8,1\tOSVersion=11.0\tOSApiLevel=11\tScreenWidth=667\tScreenHeight=375\t" +
            "ScreenDPI=326\tScaleFactor=2\tClientKitVersion=292\tKitBuildType=101\tKitBuildNumber=8707\t" +
            "SendTimestamp=1507802976\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507802976\tSessionID=1507802917\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=7123113273064847859\tStartTime=2017-10-12 13:08:37\t" +
            "StartTimestamp=1507802917\tStartTimeZone=10800\tRegionTimeZone=10800\tLocale=en_RU\t" +
            "LocationSource=LBS\tLatitude=55.733821868896484\tLongitude=37.58698272705078\t" +
            "LocationPrecision=140\tLocationEnabled=undefined\tWifi_Macs=['2CD02D6B0EB0']\t" +
            "Wifi_SignalsStrengths=[0]\tWifi_Ssids=['Yandex']\tWifi_AreConnected=[1]\t" +
            "WifiAccessPointState=UNKNOWN\tConnectionType=CONN_WIFI\tNetworkType=LTE\tCountryCode=250\t" +
            "OperatorID=11\tOperatorName=YOTA\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\t" +
            "Cells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[11]\t" +
            "Cells_OperatorsNames=['YOTA']\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\t" +
            "Cells_PhysicalsCellsIDs=[0]\tDeviceType=PHONE\tEventID=16467856438443608079\tEventNumber=70\t" +
            "EventDate=2017-10-12\tEventDateTime=2017-10-12 13:09:35\tEventTimestamp=1507802975\t" +
            "EventTimeZone=10800\tEventTimeOffset=58\tEventType=EVENT_STATBOX\t" +
            "EventName=скорость > переход между экранами\t" +
            "EventValue={\"info\" : {\"fromPageId\" : \"touch:catalog\",\"toPageId\" : \"touch:product\"}," +
            "\"name\" : \"user-interaction-navigation-transition\",\"startTime\" : 1507802650," +
            "\"requestId\" : \"1507802975099/70DF58477F0440C48B20027DB0B040ED\",  " +
            "\"timestamp\" : 1507802656653,  \"duration\" : 240}\tRegionID=120542\t" +
            "AppID=ru.yandex.ymarket.inhouse\tClientIP=2a02:6b8:0:40c:cdf8:1438:8fe1:3552\tClientPort=58691\t" +
            "Sex=1\tAge=25\tAccountID=285830221\tAccountType=login\tDeduplicationEnabled=1\n";

        checker.checkEmpty(line);
    }

    @Test
    public void noEventTypeFilterOnEmptyParam() throws Exception {
        checker.setParam("allowedAppEvents", null);
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 10:09:36\ttimezone=+0000\t" +
            "APIKey=23104\tAPIKey128=d0c8d8a8-9cff-4b00-b891-e081b41953a2\tStartDate=2017-10-12\t" +
            "UUID=99dd8f1afd46529dfdc316e13d881c32\tDeviceID=5B5E6CD9-B009-4635-89CA-A5558A3A3B6F\t" +
            "OriginalDeviceID=6C03EC48-DCA0-46E6-9548-079D042866A6UUIDHash=737899387808438411\t" +
            "DeviceIDHash=16327040839588031404\tIFV=5062B6CA-83A8-4D69-B261-6706CE4E3DF8\tAppPlatform=iOS\t" +
            "AppFramework=NATIVE\tAppVersionName=512\tAppBuildNumber=1613\tAppDebuggable=false\t" +
            "KitVersion=292\tManufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\t" +
            "OriginalModel=iPhone8,1\tOSVersion=11.0\tOSApiLevel=11\tScreenWidth=667\tScreenHeight=375\t" +
            "ScreenDPI=326\tScaleFactor=2\tClientKitVersion=292\tKitBuildType=101\tKitBuildNumber=8707\t" +
            "SendTimestamp=1507802976\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507802976\tSessionID=1507802917\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=7123113273064847859\tStartTime=2017-10-12 13:08:37\t" +
            "StartTimestamp=1507802917\tStartTimeZone=10800\tRegionTimeZone=10800\tLocale=en_RU\t" +
            "LocationSource=LBS\tLatitude=55.733821868896484\tLongitude=37.58698272705078\t" +
            "LocationPrecision=140\tLocationEnabled=undefined\tWifi_Macs=['2CD02D6B0EB0']\t" +
            "Wifi_SignalsStrengths=[0]\tWifi_Ssids=['Yandex']\tWifi_AreConnected=[1]\t" +
            "WifiAccessPointState=UNKNOWN\tConnectionType=CONN_WIFI\tNetworkType=LTE\tCountryCode=250\t" +
            "OperatorID=11\tOperatorName=YOTA\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\t" +
            "Cells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[11]\t" +
            "Cells_OperatorsNames=['YOTA']\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\t" +
            "Cells_PhysicalsCellsIDs=[0]\tDeviceType=PHONE\tEventID=16467856438443608079\tEventNumber=70\t" +
            "EventDate=2017-10-12\tEventDateTime=2017-10-12 13:09:35\tEventTimestamp=1507802975\t" +
            "EventTimeZone=10800\tEventTimeOffset=58\tEventType=EVENT_OLOLO\t" +
            "EventName=ololo\t" +
            "EventValue={\"info\" : {\"fromPageId\" : \"touch:catalog\",\"toPageId\" : \"touch:product\"}," +
            "\"name\" : \"user-interaction-navigation-transition\"," +
            "\"startTime\" : 1507802650,\"startTimeInMs\" : 1507802650123," +
            "\"requestId\" : \"1507802975099/70DF58477F0440C48B20027DB0B040ED\",  " +
            "\"timestamp\" : 1507802657,\"timestampInMs\" : 1507802656653,  \"duration\" : 240}\t" +
            "RegionID=120542\t" +
            "AppID=ru.yandex.ymarket.inhouse\tClientIP=2a02:6b8:0:40c:cdf8:1438:8fe1:3552\tClientPort=58691\t" +
            "Sex=1\tAge=25\tAccountID=285830221\tAccountType=login\tDeduplicationEnabled=1\n";

        checker.checkEmpty(line);
    }

    @Test
    public void parseAndroidAppProperTimersMetric() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 09:52:46\ttimezone=+0000\t" +
            "APIKey=23107\tAPIKey128=a996e97c-9bed-4157-b05f-ab6b42fa46b6\tStartDate=2017-10-12\t" +
            "UUID=31b72cdd9a671e34f6fd577280cac2d0\tDeviceID=56852bf8e80d0eec978d77e8b0334e8f\t" +
            "UUIDHash=2138176770510822151\tDeviceIDHash=17444363333799911657\tAppPlatform=android\t" +
            "AppFramework=NATIVE\tAppVersionName=5.11\tAppBuildNumber=1499\tAppDebuggable=undefined\t" +
            "KitVersion=262\tManufacturer=Samsung\tModel=Galaxy S8\tOriginalManufacturer=samsung\t" +
            "OriginalModel=SM-G950F\tOSVersion=7.0\tOSApiLevel=24\tScreenWidth=2960\tScreenHeight=1440\t" +
            "ScreenDPI=640\tScaleFactor=4\tAndroidID=fee65181b916abe9\t" +
            "ADVID=fef5d0d2-7068-4db2-b7ae-de859145fdfa\tClientKitVersion=262\tKitBuildType=202\t" +
            "KitBuildNumber=6484\tSendTimestamp=1507802022\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507801966\tSessionID=1507801973\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=6772906628173958217\tStartTime=2017-10-12 12:52:46\t" +
            "StartTimestamp=1507801966\tStartTimeZone=10800\tStartTimeCorrected=1\tRegionTimeZone=10800\t" +
            "Locale=en_US\tLocationSource=MISSING\tLocationEnabled=undefined\tWifiAccessPointSsid=AndroidAP\t" +
            "WifiAccessPointState=DISABLED\tConnectionType=CONN_WIFI\tCountryCode=250\tOperatorID=1\t" +
            "OperatorName=MTS RUS\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[-95]\tCells_Lacs=[0]\t" +
            "Cells_CountriesCodes=[250]\tCells_OperatorsIDs=[1]\tCells_OperatorsNames=['MTS RUS']\t" +
            "Cells_AreConnected=[1]Cells_Types=[DEFAULT]\tCells_PhysicalsCellsIDs=[0]\t" +
            "SimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[1]\tSimCards_OperatorsNames=['MTS RUS']\t" +
            "SimCards_AreRoaming=[0]\tSimCards_IccIDs=['']\tNetworksInterfaces_Names=['p2p0','wlan0']\t" +
            "NetworksInterfaces_Macs=['32074D603481','30074D603481']\tDeviceType=PHONE\t" +
            "EventID=1460827329979314175\tEventNumber=73\tEventDate=2017-10-12\t" +
            "EventDateTime=2017-10-12 12:53:33\tEventTimestamp=1507802013\tEventTimeZone=10800\t" +
            "EventTimeOffset=47\tEventType=EVENT_STATBOX\tEventValue={" +
            "\"requestId\":\"1507802021872/995d5e7aa8654f30b26ed4c3f864bb14\"," +
            "\"startTime\":1507802016,\"startTimeInMs\":1507802016123," +
            "\"name\":\"start-auth\",\"timestamp\":1507802018,\"timestampInMs\":1507802017123," +
            "\"duration\":1422}\tRegionID=9999\t" +
            "AppID=ru.yandex.market\tClientIP=2a02:6b8:0:402:7cb0:eac6:fb1d:6fd8\tClientPort=38548\t" +
            "Age=25\tAccountID=154845656\tAccountType=login\tDeduplicationEnabled=1\n";

        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1507802017123L),
            "1507802021872/995d5e7aa8654f30b26ed4c3f864bb14", "market_front_touch", "android",
            9999, checker.getHost(), "start-auth", "",
            1422, "", keys, values, "Galaxy S8", "5.11", "1499", 1507802017123L, 1507802016, 1507802016123L, "",
            "2a02:6b8:0:402:7cb0:eac6:fb1d:6fd8", "56852bf8e80d0eec978d77e8b0334e8f", "", ""
        );
    }

    @Test
    public void parseWebWithServicePageId() throws Exception {
        String line = "_logfeller_index_bucket=//home/logfeller/index/yabs-rt/bs-watch-log/900-1800/" +
            "1508948700/1508949000\t_stbx=rt3.man--yabs-rt--bs-watch-log:156@@18781776@@base64:sdl8JFBrWKpwU" +
            "KuHxf5rPg@@1508949020526@@1508949021@@bs-watch-log@@516509525\tantivirusyes=0\t" +
            "browserinfo=ti:7:s:320x568x32:sk:2:adb:2:fpr:216613626101:cn:1:w:320x497:z:180:i:20171025133233:et" +
            ":1508927553:en:utf-8:v:900:c:1:la:ru-ru:ar:1:ls:7221921141:rqn:3680:rn:106687610:hid:228801046:ds:" +
            ",,,,,,,,,,,,:rqnl:2:st:1508949020:u:1493650301540700277:t:«бу шуруповерт метабо» — Результаты " +
            "поиска — Яндекс.Маркет\tclientip=128.68.216.114\tclientip6=::ffff:128.68.216.114\t" +
            "clientport=34238\tcookiegid=213\tcookiegpauto=55_807804:37_580764:65:1:1508948934\t" +
            "cookiel=\tcookieys=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH\t" +
            "counterclass=0\tcounterid=722867\tdomainzone=yandex.ru\teventtime=1508949020\t" +
            "funiqid=6415179191375575620\theaderargs=293: yabs-sid\\=1642096531508910470; sc_1508949015322" +
            "\\=%D0%B3%D0%B4%D0%B5%20%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%BF%D0%B8%D1%81%D1%82%D0%BE%" +
            "D0%BB%D0%B5%D1%82%20%D0%BA%D1%80%D0%B0%D1%84%D1%82%D0%BE%D0%BB%20%D0%B4%D0%BB%D1%8F%20%D0%B3%D0%" +
            "B5%D1%80%D0%BC%D0%B5%D1%82%D0%B8%D0%BA%D0%B0:m.market.yandex.ru:%2Fsearch%2Ftouch%2F; " +
            "ys\\=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH; yp\\=2145916800.uuid.a4" +
            "76c80fa68d24b1bd7425211e455d3d#2145916800.did.F8B957C3-0173-4502-A5C4-0222E14CBB4C#1509418300.sz.667" +
            "x375x2#1523927007.szm.2%3A568x320%3A320x497#1510554827.ygu.1#1511541013.shlos.1#1508952534.gpauto." +
            "55_807804%3A37_580764%3A65%3A1%3A1508948934; _ym_isad\\=2; yabs-frequency\\=/4/001o0000003K5EzP/; " +
            "i\\=kTfvtJ8t24U490Ec55eWlrXl8SaPCnRj5/nMpAiA8yY+SC1XvpTq/pyMkzSkqZJtsA8qw+iGV7KNOljFGfEJqf8PG6k\\=" +
            "; yc\\=1508914877.ls.2136%3A353; my\\=YwA\\=; yandex_gid\\=213; mda\\=0; _ym_uid\\=1493650301540700" +
            "277; fuid01\\=59074b7c486b3a44.0WwRlfqutt6E8BETMz8g9XFRg73gu0WEPVO4PAdTkZdFHjO4PyGH_xeWjAt-nUpgxcd" +
            "FB0zruGEDMriBBynHtC1uUbIhPLuZaGovxsfYckFDhQyqnnGskLfiJ-xqzkGu; yandexuid\\=1005275251493650256\t" +
            "iso_eventtime=2017-10-25 19:30:20\tmetrikaexp=\tparams={\\\"requestId\\\":\\\"1508927549006/81fcf1" +
            "af7cbff9ba3bc84e8bd3766027\\\",\\\"info\\\":{\\\"pageId\\\":\\\"touch:search\\\"," +
            "\\\"nannyServiceId\\\":\\\"production_market_front_desktop_vla\\\"," +
            "\\\"serviceId\\\":\\\"market_front_touch\\\"},\\\"name\\\":" +
            "\\\"page\\\",\\\"startTime\\\":1508927547,\\\"startTimeInMs\\\":1508927547123," +
            "\\\"timestamp\\\":1508927549,\\\"timestampInMs\\\":1508927549123,\\\"duration\\\":3894," +
            "\\\"portion\\\":\\\"loaded\\\"}" +
            "\tprofile=\tprovider=\treferer=https://m.market.yandex.ru/sear" +
            "ch?text\\=%D0%B1%D1%83%20%D1%88%D1%83%D1%80%D1%83%D0%BF%D0%BE%D0%B2%D0%B5%D1%80%D1%82%20%D0%BC%D0" +
            "%B5%D1%82%D0%B0%D0%B1%D0%BE&clid\\=708&onstock\\=1&local-offers-first\\=0&page\\=1#list_12544517\t" +
            "regionid=213\tremoteip=127.0.0.1\tremoteip6=::ffff:127.0.0.1\t" +
            "requestid=00055C619257E60400000F8F67F33E49\tsearchquery=\tsessid=1642096531508910470\t" +
            "source_uri=prt://yabs-rt@2a02:6b8:0:872:0:0:3c:11;unknown\tsourcebit=0\tsubkey=\t" +
            "timestamp=1508949020\ttskv_format=bs-watch-log\tuniqid=1005275251493650256\t" +
            "unixtime=1508949020\tupdatetag=noindex\turl=goal://wat.market.yandex.ru/TIMERS\t" +
            "useragent=Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
            "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1\twapprofile=\t" +
            "watchid=6312065858767163279\txoperaminiphoneua=\txwapprofile=\n";

        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1508927549123L),
            "1508927549006/81fcf1af7cbff9ba3bc84e8bd3766027", "market_front_touch", "web",
            213, checker.getHost(), "page", "loaded",
            3894, "touch:search", keys, values, "", "", "", 1508927549123L, 1508927547, 1508927547123L,
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
                "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1",
            "128.68.216.114", "1005275251493650256", "wat.market.yandex.ru",
            "production_market_front_desktop_vla"
        );
    }

    @Test
    public void parseLegacyWebBulk() throws Exception {
        String line = "_logfeller_index_bucket=//home/logfeller/index/yabs-rt/bs-watch-log/900-1800/" +
            "1508948700/1508949000\t_stbx=rt3.man--yabs-rt--bs-watch-log:156@@18781776@@base64:sdl8JFBrWKpwU" +
            "KuHxf5rPg@@1508949020526@@1508949021@@bs-watch-log@@516509525\tantivirusyes=0\t" +
            "browserinfo=ti:7:s:320x568x32:sk:2:adb:2:fpr:216613626101:cn:1:w:320x497:z:180:i:20171025133233:et" +
            ":1508927553:en:utf-8:v:900:c:1:la:ru-ru:ar:1:ls:7221921141:rqn:3680:rn:106687610:hid:228801046:ds:" +
            ",,,,,,,,,,,,:rqnl:2:st:1508949020:u:1493650301540700277:t:«бу шуруповерт метабо» — Результаты " +
            "поиска — Яндекс.Маркет\tclientip=128.68.216.114\tclientip6=::ffff:128.68.216.114\t" +
            "clientport=34238\tcookiegid=213\tcookiegpauto=55_807804:37_580764:65:1:1508948934\t" +
            "cookiel=\tcookieys=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH\t" +
            "counterclass=0\tcounterid=722867\tdomainzone=yandex.ru\teventtime=1508949020\t" +
            "funiqid=6415179191375575620\theaderargs=293: yabs-sid\\=1642096531508910470; sc_1508949015322" +
            "\\=%D0%B3%D0%B4%D0%B5%20%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%BF%D0%B8%D1%81%D1%82%D0%BE%" +
            "D0%BB%D0%B5%D1%82%20%D0%BA%D1%80%D0%B0%D1%84%D1%82%D0%BE%D0%BB%20%D0%B4%D0%BB%D1%8F%20%D0%B3%D0%" +
            "B5%D1%80%D0%BC%D0%B5%D1%82%D0%B8%D0%BA%D0%B0:m.market.yandex.ru:%2Fsearch%2Ftouch%2F; " +
            "ys\\=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH; yp\\=2145916800.uuid.a4" +
            "76c80fa68d24b1bd7425211e455d3d#2145916800.did.F8B957C3-0173-4502-A5C4-0222E14CBB4C#1509418300.sz.667" +
            "x375x2#1523927007.szm.2%3A568x320%3A320x497#1510554827.ygu.1#1511541013.shlos.1#1508952534.gpauto." +
            "55_807804%3A37_580764%3A65%3A1%3A1508948934; _ym_isad\\=2; yabs-frequency\\=/4/001o0000003K5EzP/; " +
            "i\\=kTfvtJ8t24U490Ec55eWlrXl8SaPCnRj5/nMpAiA8yY+SC1XvpTq/pyMkzSkqZJtsA8qw+iGV7KNOljFGfEJqf8PG6k\\=" +
            "; yc\\=1508914877.ls.2136%3A353; my\\=YwA\\=; yandex_gid\\=213; mda\\=0; _ym_uid\\=1493650301540700" +
            "277; fuid01\\=59074b7c486b3a44.0WwRlfqutt6E8BETMz8g9XFRg73gu0WEPVO4PAdTkZdFHjO4PyGH_xeWjAt-nUpgxcd" +
            "FB0zruGEDMriBBynHtC1uUbIhPLuZaGovxsfYckFDhQyqnnGskLfiJ-xqzkGu; yandexuid\\=1005275251493650256\t" +
            "iso_eventtime=2017-10-25 19:30:20\tmetrikaexp=\tparams=[" +

            "{\\\"requestId\\\":\\\"1508927549006/81fcf1af7cbff9ba3bc84e8bd3766027\\\"," +
            "\\\"info\\\":{\\\"pageId\\\":\\\"touch:search\\\"," +
            "\\\"nannyServiceId\\\":\\\"production_market_front_desktop_vla\\\"," +
            "\\\"serviceId\\\":\\\"market_front_touch\\\"},\\\"name\\\":" +
            "\\\"page\\\",\\\"startTime\\\":1508927547,\\\"startTimeInMs\\\":1508927547123," +
            "\\\"timestamp\\\":1508927549,\\\"timestampInMs\\\":1508927549123,\\\"duration\\\":3894," +
            "\\\"portion\\\":\\\"loaded\\\"}," +

            "{\\\"requestId\\\":\\\"1508927549006/81fcf1af7cbff9ba3bc84e8bd3766028\\\"" +
            ",\\\"info\\\":{\\\"pageId\\\":\\\"touch:index\\\"," +
            "\\\"nannyServiceId\\\":\\\"production_market_front_desktop_vla\\\"," +
            "\\\"serviceId\\\":\\\"market_front_touch\\\"},\\\"name\\\":" +
            "\\\"page\\\",\\\"startTime\\\":1508927547,\\\"startTimeInMs\\\":1508927547223," +
            "\\\"timestamp\\\":1508927649,\\\"timestampInMs\\\":1508927549323,\\\"duration\\\":3894," +
            "\\\"portion\\\":\\\"loaded\\\"}," +

            "{\\\"requestId\\\":\\\"1508927549006/81fcf1af7cbff9ba3bc84e8bd3766029\\\"" +
            ",\\\"info\\\":{\\\"pageId\\\":\\\"touch:product\\\"," +
            "\\\"nannyServiceId\\\":\\\"production_market_front_desktop_vla\\\"," +
            "\\\"serviceId\\\":\\\"market_front_touch\\\"},\\\"name\\\":" +
            "\\\"page\\\",\\\"startTime\\\":1508927547,\\\"startTimeInMs\\\":1508927547323," +
            "\\\"timestamp\\\":1508927749,\\\"timestampInMs\\\":1508927549523,\\\"duration\\\":3894," +
            "\\\"portion\\\":\\\"loaded\\\"}" +

            "]\tprofile=\tprovider=\treferer=https://m.market.yandex.ru/sear" +
            "ch?text\\=%D0%B1%D1%83%20%D1%88%D1%83%D1%80%D1%83%D0%BF%D0%BE%D0%B2%D0%B5%D1%80%D1%82%20%D0%BC%D0" +
            "%B5%D1%82%D0%B0%D0%B1%D0%BE&clid\\=708&onstock\\=1&local-offers-first\\=0&page\\=1#list_12544517\t" +
            "regionid=213\tremoteip=127.0.0.1\tremoteip6=::ffff:127.0.0.1\t" +
            "requestid=00055C619257E60400000F8F67F33E49\tsearchquery=\tsessid=1642096531508910470\t" +
            "source_uri=prt://yabs-rt@2a02:6b8:0:872:0:0:3c:11;unknown\tsourcebit=0\tsubkey=\t" +
            "timestamp=1508949020\ttskv_format=bs-watch-log\tuniqid=1005275251493650256\t" +
            "unixtime=1508949020\tupdatetag=noindex\turl=goal://wat.market.yandex.ru/TIMERS\t" +
            "useragent=Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
            "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1\twapprofile=\t" +
            "watchid=6312065858767163279\txoperaminiphoneua=\txwapprofile=\n";

        String[] keys = {};
        String[] values = {};

        List<Date> expectedDateList = Arrays.asList(
            new Date(1508927549123L),
            new Date(1508927549323L),
            new Date(1508927549523L)
        );

        List<Object[]> expectedFieldsList = Arrays.asList(
            new Object[]{
                "1508927549006/81fcf1af7cbff9ba3bc84e8bd3766027", "market_front_touch", "web",
                213, checker.getHost(), "page", "loaded",
                3894, "touch:search", keys, values, "", "", "", 1508927549123L, 1508927547, 1508927547123L,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
                    "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1",
                "128.68.216.114", "1005275251493650256", "wat.market.yandex.ru",
                "production_market_front_desktop_vla"
            },

            new Object[]{
                "1508927549006/81fcf1af7cbff9ba3bc84e8bd3766028", "market_front_touch", "web",
                213, checker.getHost(), "page", "loaded",
                3894, "touch:index", keys, values, "", "", "", 1508927549323L, 1508927547, 1508927547223L,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
                    "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1",
                "128.68.216.114", "1005275251493650256", "wat.market.yandex.ru",
                "production_market_front_desktop_vla"
            },

            new Object[]{
                "1508927549006/81fcf1af7cbff9ba3bc84e8bd3766029", "market_front_touch", "web",
                213, checker.getHost(), "page", "loaded",
                3894, "touch:product", keys, values, "", "", "", 1508927549523L, 1508927547, 1508927547323L,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
                    "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1",
                "128.68.216.114", "1005275251493650256", "wat.market.yandex.ru",
                "production_market_front_desktop_vla"
            }
        );

        checker.check(line, expectedDateList, expectedFieldsList);
    }

    @Test
    public void parseWebBulk() throws Exception {
        String line = "_logfeller_index_bucket=//home/logfeller/index/yabs-rt/bs-watch-log/900-1800/" +
            "1508948700/1508949000\t_stbx=rt3.man--yabs-rt--bs-watch-log:156@@18781776@@base64:sdl8JFBrWKpwU" +
            "KuHxf5rPg@@1508949020526@@1508949021@@bs-watch-log@@516509525\tantivirusyes=0\t" +
            "browserinfo=ti:7:s:320x568x32:sk:2:adb:2:fpr:216613626101:cn:1:w:320x497:z:180:i:20171025133233:et" +
            ":1508927553:en:utf-8:v:900:c:1:la:ru-ru:ar:1:ls:7221921141:rqn:3680:rn:106687610:hid:228801046:ds:" +
            ",,,,,,,,,,,,:rqnl:2:st:1508949020:u:1493650301540700277:t:«бу шуруповерт метабо» — Результаты " +
            "поиска — Яндекс.Маркет\tclientip=128.68.216.114\tclientip6=::ffff:128.68.216.114\t" +
            "clientport=34238\tcookiegid=213\tcookiegpauto=55_807804:37_580764:65:1:1508948934\t" +
            "cookiel=\tcookieys=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH\t" +
            "counterclass=0\tcounterid=722867\tdomainzone=yandex.ru\teventtime=1508949020\t" +
            "funiqid=6415179191375575620\theaderargs=293: yabs-sid\\=1642096531508910470; sc_1508949015322" +
            "\\=%D0%B3%D0%B4%D0%B5%20%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%BF%D0%B8%D1%81%D1%82%D0%BE%" +
            "D0%BB%D0%B5%D1%82%20%D0%BA%D1%80%D0%B0%D1%84%D1%82%D0%BE%D0%BB%20%D0%B4%D0%BB%D1%8F%20%D0%B3%D0%" +
            "B5%D1%80%D0%BC%D0%B5%D1%82%D0%B8%D0%BA%D0%B0:m.market.yandex.ru:%2Fsearch%2Ftouch%2F; " +
            "ys\\=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH; yp\\=2145916800.uuid.a4" +
            "76c80fa68d24b1bd7425211e455d3d#2145916800.did.F8B957C3-0173-4502-A5C4-0222E14CBB4C#1509418300.sz.667" +
            "x375x2#1523927007.szm.2%3A568x320%3A320x497#1510554827.ygu.1#1511541013.shlos.1#1508952534.gpauto." +
            "55_807804%3A37_580764%3A65%3A1%3A1508948934; _ym_isad\\=2; yabs-frequency\\=/4/001o0000003K5EzP/; " +
            "i\\=kTfvtJ8t24U490Ec55eWlrXl8SaPCnRj5/nMpAiA8yY+SC1XvpTq/pyMkzSkqZJtsA8qw+iGV7KNOljFGfEJqf8PG6k\\=" +
            "; yc\\=1508914877.ls.2136%3A353; my\\=YwA\\=; yandex_gid\\=213; mda\\=0; _ym_uid\\=1493650301540700" +
            "277; fuid01\\=59074b7c486b3a44.0WwRlfqutt6E8BETMz8g9XFRg73gu0WEPVO4PAdTkZdFHjO4PyGH_xeWjAt-nUpgxcd" +
            "FB0zruGEDMriBBynHtC1uUbIhPLuZaGovxsfYckFDhQyqnnGskLfiJ-xqzkGu; yandexuid\\=1005275251493650256\t" +
            "iso_eventtime=2017-10-25 19:30:20\tmetrikaexp=\tparams={\\\"requestId\\\":\\\"1508927549006/81fcf1af" +
            "7cbff9ba3bc84e8bd3766027\\\",\\\"startTime\\\":1508927547,\\\"startTimeInMs\\\":1508927547223," +
            "\\\"info\\\"" +
            ":{\\\"pageId\\\":\\\"touch:search\\\"," +
            "\\\"nannyServiceId\\\":\\\"production_market_front_desktop_vla\\\"," +
            "\\\"serviceId\\\":\\\"market_front_touch\\\"," +
            "\\\"expFlags\\\":\\\"touch_delivery_description\\\"" +
            "},\\\"timers\\\":[" +

            "{\\\"name\\\":\\\"page\\\"," +
            "\\\"timestamp\\\":1508927549,\\\"timestampInMs\\\":1508927549123,\\\"duration\\\":3894," +
            "\\\"portion\\\":\\\"loaded\\\",\\\"info\\\"" +
            ":{\\\"foo\\\":\\\"bar\\\"," +
            "\\\"baz\\\":\\\"quux\\\"}}," +

            "{\\\"name\\\":\\\"page\\\"," +
            "\\\"timestamp\\\":1508927649,\\\"timestampInMs\\\":1508927549323,\\\"duration\\\":3894," +
            "\\\"portion\\\":\\\"loaded\\\",\\\"info\\\"" +
            ":{\\\"foo\\\":\\\"bar\\\"," +
            "\\\"baz\\\":\\\"quux\\\"}}," +

            "{\\\"name\\\":\\\"page\\\"," +
            "\\\"timestamp\\\":1508927749,\\\"timestampInMs\\\":1508927549523,\\\"duration\\\":3894," +
            "\\\"portion\\\":\\\"loaded\\\",\\\"info\\\"" +
            ":{\\\"foo\\\":\\\"bar\\\"," +
            "\\\"baz\\\":\\\"quux\\\"}}" +

            "]}\tprofile=\tprovider=\treferer=https://m.market.yandex.ru/sear" +
            "ch?text\\=%D0%B1%D1%83%20%D1%88%D1%83%D1%80%D1%83%D0%BF%D0%BE%D0%B2%D0%B5%D1%80%D1%82%20%D0%BC%D0" +
            "%B5%D1%82%D0%B0%D0%B1%D0%BE&clid\\=708&onstock\\=1&local-offers-first\\=0&page\\=1#list_12544517\t" +
            "regionid=213\tremoteip=127.0.0.1\tremoteip6=::ffff:127.0.0.1\t" +
            "requestid=00055C619257E60400000F8F67F33E49\tsearchquery=\tsessid=1642096531508910470\t" +
            "source_uri=prt://yabs-rt@2a02:6b8:0:872:0:0:3c:11;unknown\tsourcebit=0\tsubkey=\t" +
            "timestamp=1508949020\ttskv_format=bs-watch-log\tuniqid=1005275251493650256\t" +
            "unixtime=1508949020\tupdatetag=noindex\turl=goal://wat.market.yandex.ru/TIMERS\t" +
            "useragent=Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
            "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1\twapprofile=\t" +
            "watchid=6312065858767163279\txoperaminiphoneua=\txwapprofile=\n";

        String[] keys = {"expFlags", "foo", "baz"};
        String[] values = {"touch_delivery_description", "bar", "quux"};

        List<Date> expectedDateList = Arrays.asList(
            new Date(1508927549123L),
            new Date(1508927549323L),
            new Date(1508927549523L)
        );

        List<Object[]> expectedFieldsList = Arrays.asList(
            new Object[]{
                "1508927549006/81fcf1af7cbff9ba3bc84e8bd3766027", "market_front_touch", "web",
                213, checker.getHost(), "page", "loaded",
                3894, "touch:search", keys, values, "", "", "", 1508927549123L, 1508927547, 1508927547223L,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
                    "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1",
                "128.68.216.114", "1005275251493650256", "wat.market.yandex.ru",
                "production_market_front_desktop_vla"
            },

            new Object[]{
                "1508927549006/81fcf1af7cbff9ba3bc84e8bd3766027", "market_front_touch", "web",
                213, checker.getHost(), "page", "loaded",
                3894, "touch:search", keys, values, "", "", "", 1508927549323L, 1508927547, 1508927547223L,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
                    "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1",
                "128.68.216.114", "1005275251493650256", "wat.market.yandex.ru",
                "production_market_front_desktop_vla"
            },

            new Object[]{
                "1508927549006/81fcf1af7cbff9ba3bc84e8bd3766027", "market_front_touch", "web",
                213, checker.getHost(), "page", "loaded",
                3894, "touch:search", keys, values, "", "", "", 1508927549523L, 1508927547, 1508927547223L,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
                    "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1",
                "128.68.216.114", "1005275251493650256", "wat.market.yandex.ru",
                "production_market_front_desktop_vla"
            }
        );

        checker.check(line, expectedDateList, expectedFieldsList);
    }


    @Test
    public void parseAppBulk() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-26 10:25:49\t" +
            "timezone=+0000\tAPIKey=23107\tAPIKey128=a996e97c-9bed-4157-b05f-ab6b42fa46b6\tStartDate=2017-10-26\t" +
            "UUID=ce4642cd3123fb75fbf6c1abfeddd13e\tDeviceID=738ba4b7765246ae7f5f8b3992f4569f\t" +
            "UUIDHash=9045334822516910520\tDeviceIDHash=11487695674382257827\tAppPlatform=android\t" +
            "OperatingSystem=android\tAppFramework=NATIVE\tAppVersionName=5.11\tAppBuildNumber=1500\t" +
            "AppDebuggable=undefined\tKitVersion=262\tManufacturer=Samsung\tModel=Galaxy A7\t" +
            "OriginalManufacturer=samsung\tOriginalModel=SM-A700FD\tOSVersion=6.0.1\tOSApiLevel=23\t" +
            "ScreenWidth=1920\tScreenHeight=1080\tScreenDPI=480\tScaleFactor=3\tAndroidID=c7d47a16bb88e5b3\t" +
            "ADVID=71ec9bd6-39da-41e3-b050-c293ba337b5e\tClientKitVersion=262\tKitBuildType=202\t" +
            "KitBuildNumber=6484\tSendTimestamp=1509013550\tSendTimeZone=10800\tReceiveDate=2017-10-26\t" +
            "ReceiveTimestamp=1509013549\tSessionID=1509013541\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=17595071052827490338\t" +
            "StartTime=2017-10-26 13:25:41\tStartTimestamp=1509013541\t" +
            "StartTimeZone=10800\tRegionTimeZone=10800\tLocale=ru_RU\tLocationSource=LBS\t" +
            "Latitude=55.605796813964844\tLongitude=37.490509033203125\tLocationPrecision=324\t" +
            "LocationEnabled=undefined\tWifiAccessPointSsid=AndroidAP\tWifiAccessPointState=DISABLED\t" +
            "ConnectionType=CONN_CELL\tCountryCode=250\tOperatorID=99\t" +
            "Cells_CellsIDs=[197689353,2147483647,2147483647,2147483647,34415,3219,3220]\t" +
            "Cells_SignalsStrengths=[-78,-105,-103,-118,-81,-87,-97]\t" +
            "Cells_Lacs=[21677,2147483647,2147483647,2147483647,7753,7753,7753]\t" +
            "Cells_CountriesCodes=[250,2147483647,2147483647,2147483647,250,250,250]\t" +
            "Cells_OperatorsIDs=[99,2147483647,2147483647,2147483647,2,2,2]\t" +
            "Cells_OperatorsNames=['','','','','','','']\tCells_AreConnected=[1,0,0,0,1,0,0]\t" +
            "Cells_Types=[LTE,LTE,LTE,LTE,GSM,GSM,GSM]\tCells_PhysicalsCellsIDs=[50,414,240,325,0,0,0]\t" +
            "SimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[99]\tSimCards_OperatorsNames=['MegaFon']\t" +
            "SimCards_AreRoaming=[0]\tSimCards_IccIDs=['']\tNetworksInterfaces_Names=['wlan0','p2p0','dummy0']\t" +
            "NetworksInterfaces_Macs=['ACEE9E1017B3','AEEE9E1017B3','5E5B653FA1DB']\tDeviceType=PHONE\t" +
            "EventID=10057862631024411190\tEventNumber=24\tEventDate=2017-10-26\t" +
            "EventDateTime=2017-10-26 13:25:49\t" +
            "EventTimestamp=1509013549\tEventTimeZone=10800\tEventTimeOffset=8\t" +
            "EventType=EVENT_CLIENT\tEventName=TIMERS\t" +
            "EventValue=[" +

            "{\"requestId\":\"1509013543308/0afa9b77932cacd6f1ce99ac5229ecda\",\"info\":" +
            "{\"pageId\":\"touch:index\"},\"name\":\"page\"," +
            "\"startTime\":1509013541,\"startTimeInMs\":1509013541123," +
            "\"timestamp\":1509013543,\"timestampInMs\":1509013543123," +
            "\"duration\":5925,\"portion\":\"loaded\",\"yandex_uid\":\"5371758881499875586\"}," +

            "{\"requestId\":\"1509013543308/0afa9b77932cacd6f1ce99ac5229ecdb\",\"info\":" +
            "{\"pageId\":\"touch:product\"},\"name\":\"page\"," +
            "\"startTime\":1509013541,\"startTimeInMs\":1509013541223," +
            "\"timestamp\":1509013543,\"timestampInMs\":1509013543323," +
            "\"duration\":5925,\"portion\":\"loaded\",\"yandex_uid\":\"5371758881499875586\"}," +

            "{\"requestId\":\"1509013543308/0afa9b77932cacd6f1ce99ac5229ecdc\",\"info\":" +
            "{\"pageId\":\"touch:search\"},\"name\":\"page\"," +
            "\"startTime\":1509013541,\"startTimeInMs\":1509013541323," +
            "\"timestamp\":1509013543,\"timestampInMs\":1509013543523," +
            "\"duration\":5925,\"portion\":\"loaded\",\"yandex_uid\":\"5371758881499875586\"}" +

            "]\tParsedParams_Key1=['requestId','info','name','startTime','timestamp','duration','portion','yandex" +
            "_uid']\tParsedParams_Key2=['1509013543308/0afa9b77932cacd6f1ce99ac5229ecda','pageId','page','150901" +
            "3543','1509013543','5925','loaded','5371758881499875586']\tParsedParams_Key3=['','touch:inde" +
            "x','','','','','','']\tParsedParams_Key4=['','','','','','','','']\tParsedParams_Key5=['',''" +
            ",'','','','','','']\tParsedParams_Key6=['','','','','','','','']\tParsedParams_Key7=['','','','','" +
            "','','','']\tParsedParams_Key8=['','','','','','','','']\tParsedParams_Key9=['','','','','','',''" +
            ",'']\tParsedParams_Key10=['','','','','','','','']\tParsedParams_ValueDouble=[0,0,0,1509013543,150901" +
            "3543,5925,0,5371758881499875000]\tParsedParams_Quantity=[1,1,1,1,1,1,1,1]\tClickDate=1970-01-01\tRe" +
            "gionID=114619\tAppID=ru.yandex.market\tClientIP=::ffff:83.220.236.195\tClientPort=58375\tSex=1\t" +
            "Age=45\tDeduplicationEnabled=1\n";

        String[] keys = {};
        String[] values = {};

        List<Date> expectedDateList = Arrays.asList(
            new Date(1509013543123L),
            new Date(1509013543323L),
            new Date(1509013543523L)
        );

        List<Object[]> expectedFieldsList = Arrays.asList(
            new Object[]{
                "1509013543308/0afa9b77932cacd6f1ce99ac5229ecda", "market_front_touch", "android",
                114619, checker.getHost(), "page", "loaded",
                5925, "touch:index", keys, values, "Galaxy A7", "5.11", "1500",
                1509013543123L, 1509013541, 1509013541123L, "", "::ffff:83.220.236.195",
                "738ba4b7765246ae7f5f8b3992f4569f", "", ""
            },

            new Object[]{
                "1509013543308/0afa9b77932cacd6f1ce99ac5229ecdb", "market_front_touch", "android",
                114619, checker.getHost(), "page", "loaded",
                5925, "touch:product", keys, values, "Galaxy A7", "5.11", "1500",
                1509013543323L, 1509013541, 1509013541223L, "", "::ffff:83.220.236.195",
                "738ba4b7765246ae7f5f8b3992f4569f", "", ""
            },

            new Object[]{
                "1509013543308/0afa9b77932cacd6f1ce99ac5229ecdc", "market_front_touch", "android",
                114619, checker.getHost(), "page", "loaded",
                5925, "touch:search", keys, values, "Galaxy A7", "5.11", "1500",
                1509013543523L, 1509013541, 1509013541323L, "", "::ffff:83.220.236.195",
                "738ba4b7765246ae7f5f8b3992f4569f", "", ""
            }
        );

        checker.check(line, expectedDateList, expectedFieldsList);
    }

    @Test
    public void parseWebWithServicePageIdExpFlags() throws Exception {
        String line = "tskv\ttskv_format=bs-watch-log\tunixtime=1518027658\trequestid" +
            "=000564A35B5211A200000771F798EBC4" +
            "\twatchid=425003085551896433\teventtime=1518027658\tcounterid=722867\tclientip=31.173.83.77\t" +
            "clientip6=::ffff:31.173.83.77\tclientport=57330\tremoteip=0.0.0.0\tremoteip6=::1\tregionid=213" +
            "\tcookiegid=213\tuniqid=5590356461518010500\tsessid=712006511518010501\tcounterclass=0\tantivirusyes=0" +
            "\tbrowserinfo=cy:1:ti:7:s:375x667x32:sk:2:adb:2:fpr:216613626101:cn:1:w:375x553:z:180:i:" +
            "20180207212057:et:1518027658:en:utf-8:v:962:c:1:la:ru-ru:ar:1:ls:1343049629234" +
            ":rqn:7:rn:1037344467:hid:753866591:ds:,,,,,,,,,,,,:rqnl:1:st:1518027658:u:15180105011068821150:t" +
            ":«айфон 7» — Мобильные телефоны — купить на Яндекс.Маркете\t" +
            "params={\\\"requestId\\\":\\\"1518027650963/3aa21305ba22a4420074482e0c09fa33\\\"," +
            "\\\"info\\\":{\\\"pageId\\\":\\\"touch:list\\\",\\\"serviceId\\\":\\\"market_front_touch\\\"," +
            "\\\"expFlags\\\":\\\"touch_delivery_description\\\",\\\"widgetId\\\":\\\"/footer\\\"," +
            "\\\"widgetName\\\":\\\"w-footer\\\"},\\\"startTimeInMs\\\":1518027650964,\\\"startTime\\\":1518027651," +
            "\\\"timestampInMs\\\":1518027653863,\\\"timestamp\\\":1518027654,\\\"duration\\\":2426," +
            "\\\"name\\\":\\\"widget\\\",\\\"portion\\\":\\\"/footer\\\"}" +
            "\tuseragent=Mozilla/5.0 (iPhone; CPU iPhone OS 11_2_5 like Mac OS X) AppleWebKit/604.5.6 (KHTML, " +
            "like Gecko) Version/11.0 Mobile/15D60 Safari/604.1\turl=goal://m.market.yandex.ru/TIMERS\t" +
            "referer=https://m.market.yandex.ru/catalog/54726/list?was_redir\\=1&srnum\\=334&hid\\=91491&rt\\=9&text" +
            "\\=%D0%B0%D0%B9%D1%84%D0%BE%D0%BD%207&onstock\\=0&local-offers-first\\=0#\tsourcebit=0" +
            "\tsearchquery=\tfuniqid=0\txoperaminiphoneua=\txwapprofile=\twapprofile=\tprofile=" +
            "\tupdatetag=noindex\tcookieys=wprid.1518027620581247-261640060301702006442772-man1-5717-TCH" +
            "\tcookiel=\theaderargs=293: sc_1518027649647\\=%D0%B0%D0%B9%D1%84%D0%BE%D0%BD%207%20:" +
            "m.market.yandex.ru:%2Fsearch%2Ftouch%2F; __ym_zz_zz\\=1518010815587207613160375285579; " +
            "yabs-frequency\\=/4/00020000001hGtjQ/; ys\\=wprid" +
 ".1518027620581247-261640060301702006442772-man1-5717-TCH; " +
            "yp\\=1520602500.ygu.1#1533778503.szm.2%3A667x375%3A375x553#1520619622.shlos.1#1533792585.sz.667x375x2; " +
            "my\\=YwA\\=; _ym_isad\\=2; _ym_uid\\=15180105011068821150; yabs-sid\\=712006511518010501; " +
            "i\\=RdOPzpVXyn9aFrrmFOAqFrWlgWXM3Pk8lqUlP5LHb8VZUqPbhkTaNnUXv/AcgN3WAaAw577v5W1vsZw+OHC2f08pxYk\\=; " +
            "mda\\=0; yandex_gid\\=213; " +
            "yandexuid\\=5590356461518010500\tdomainzone=yandex.ru\tcookiegpauto=\tmetrikaexp=\tetag=0" +
            "\tcookiei=5590356461518010500\tdonottrack=1";

        String[] keys = {"expFlags", "widgetId", "widgetName"};
        String[] values = {"touch_delivery_description", "/footer", "w-footer"};

        checker.check(
            line,
            new Date(1518027653863L),
            "1518027650963/3aa21305ba22a4420074482e0c09fa33", "market_front_touch", "web",
            213, checker.getHost(), "widget", "/footer",
            2426, "touch:list", keys, values, "", "", "", 1518027653863L, 1518027651, 1518027650964L,
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_2_5 like Mac OS X) AppleWebKit/604.5.6 (KHTML, " +
                "like Gecko) Version/11.0 Mobile/15D60 Safari/604.1",
            "31.173.83.77", "5590356461518010500", "m.market.yandex.ru", ""
        );
    }

    @Test
    public void skipWebWithZeroTimestamp() throws Exception {
        String line = "tskv\ttskv_format=bs-watch-log\tunixtime=1518027658\trequestid" +
            "=000564A35B5211A200000771F798EBC4" +
            "\twatchid=425003085551896433\teventtime=1518027658\tcounterid=722867\tclientip=31.173.83.77\t" +
            "clientip6=::ffff:31.173.83.77\tclientport=57330\tremoteip=0.0.0.0\tremoteip6=::1\tregionid=213" +
            "\tcookiegid=213\tuniqid=5590356461518010500\tsessid=712006511518010501\tcounterclass=0\tantivirusyes=0" +
            "\tbrowserinfo=cy:1:ti:7:s:375x667x32:sk:2:adb:2:fpr:216613626101:cn:1:w:375x553:z:180:i:" +
            "20180207212057:et:1518027658:en:utf-8:v:962:c:1:la:ru-ru:ar:1:ls:1343049629234" +
            ":rqn:7:rn:1037344467:hid:753866591:ds:,,,,,,,,,,,,:rqnl:1:st:1518027658:u:15180105011068821150:t" +
            ":«айфон 7» — Мобильные телефоны — купить на Яндекс.Маркете\t" +
            "params={\\\"requestId\\\":\\\"1518027650963/3aa21305ba22a4420074482e0c09fa33\\\"," +
            "\\\"info\\\":{\\\"pageId\\\":\\\"touch:list\\\",\\\"serviceId\\\":\\\"market_front_touch\\\"," +
            "\\\"expFlags\\\":\\\"touch_delivery_description\\\",\\\"widgetId\\\":\\\"/footer\\\"," +
            "\\\"widgetName\\\":\\\"w-footer\\\"},\\\"startTimeInMs\\\":1518027650964,\\\"startTime\\\":1518027651," +
            "\\\"timestampInMs\\\":0,\\\"timestamp\\\":0,\\\"duration\\\":2426," +
            "\\\"name\\\":\\\"widget\\\",\\\"portion\\\":\\\"/footer\\\"}" +
            "\tuseragent=Mozilla/5.0 (iPhone; CPU iPhone OS 11_2_5 like Mac OS X) AppleWebKit/604.5.6 (KHTML, " +
            "like Gecko) Version/11.0 Mobile/15D60 Safari/604.1\turl=goal://m.market.yandex.ru/TIMERS\t" +
            "referer=https://m.market.yandex.ru/catalog/54726/list?was_redir\\=1&srnum\\=334&hid\\=91491&rt\\=9&text" +
            "\\=%D0%B0%D0%B9%D1%84%D0%BE%D0%BD%207&onstock\\=0&local-offers-first\\=0#\tsourcebit=0" +
            "\tsearchquery=\tfuniqid=0\txoperaminiphoneua=\txwapprofile=\twapprofile=\tprofile=" +
            "\tupdatetag=noindex\tcookieys=wprid.1518027620581247-261640060301702006442772-man1-5717-TCH" +
            "\tcookiel=\theaderargs=293: sc_1518027649647\\=%D0%B0%D0%B9%D1%84%D0%BE%D0%BD%207%20:" +
            "m.market.yandex.ru:%2Fsearch%2Ftouch%2F; __ym_zz_zz\\=1518010815587207613160375285579; " +
            "yabs-frequency\\=/4/00020000001hGtjQ/; ys\\=wprid" +
            ".1518027620581247-261640060301702006442772-man1-5717-TCH; " +
            "yp\\=1520602500.ygu.1#1533778503.szm.2%3A667x375%3A375x553#1520619622.shlos.1#1533792585.sz.667x375x2; " +
            "my\\=YwA\\=; _ym_isad\\=2; _ym_uid\\=15180105011068821150; yabs-sid\\=712006511518010501; " +
            "i\\=RdOPzpVXyn9aFrrmFOAqFrWlgWXM3Pk8lqUlP5LHb8VZUqPbhkTaNnUXv/AcgN3WAaAw577v5W1vsZw+OHC2f08pxYk\\=; " +
            "mda\\=0; yandex_gid\\=213; " +
            "yandexuid\\=5590356461518010500\tdomainzone=yandex.ru\tcookiegpauto=\tmetrikaexp=\tetag=0" +
            "\tcookiei=5590356461518010500\tdonottrack=1";

        checker.checkEmpty(line);
    }

    @Test
    public void skipWebWithFutureTimestamp() throws Exception {
        long tsMs = System.currentTimeMillis() + 3600000;
        long ts = tsMs / 1000;

        String line = "tskv\ttskv_format=bs-watch-log\tunixtime=1518027658\trequestid" +
 "=000564A35B5211A200000771F798EBC4" +
            "\twatchid=425003085551896433\teventtime=1518027658\tcounterid=722867\tclientip=31.173.83.77\t" +
            "clientip6=::ffff:31.173.83.77\tclientport=57330\tremoteip=0.0.0.0\tremoteip6=::1\tregionid=213" +
            "\tcookiegid=213\tuniqid=5590356461518010500\tsessid=712006511518010501\tcounterclass=0\tantivirusyes=0" +
            "\tbrowserinfo=cy:1:ti:7:s:375x667x32:sk:2:adb:2:fpr:216613626101:cn:1:w:375x553:z:180:i:" +
            "20180207212057:et:1518027658:en:utf-8:v:962:c:1:la:ru-ru:ar:1:ls:1343049629234" +
            ":rqn:7:rn:1037344467:hid:753866591:ds:,,,,,,,,,,,,:rqnl:1:st:1518027658:u:15180105011068821150:t" +
            ":«айфон 7» — Мобильные телефоны — купить на Яндекс.Маркете\t" +
            "params={\\\"requestId\\\":\\\"1518027650963/3aa21305ba22a4420074482e0c09fa33\\\"," +
            "\\\"info\\\":{\\\"pageId\\\":\\\"touch:list\\\",\\\"serviceId\\\":\\\"market_front_touch\\\"," +
            "\\\"expFlags\\\":\\\"touch_delivery_description\\\",\\\"widgetId\\\":\\\"/footer\\\"," +
            "\\\"widgetName\\\":\\\"w-footer\\\"},\\\"startTimeInMs\\\":1518027650964,\\\"startTime\\\":1518027651," +
            "\\\"timestampInMs\\\":" + tsMs + ",\\\"timestamp\\\":" + ts + ",\\\"duration\\\":2426," +
            "\\\"name\\\":\\\"widget\\\",\\\"portion\\\":\\\"/footer\\\"}" +
            "\tuseragent=Mozilla/5.0 (iPhone; CPU iPhone OS 11_2_5 like Mac OS X) AppleWebKit/604.5.6 (KHTML, " +
            "like Gecko) Version/11.0 Mobile/15D60 Safari/604.1\turl=goal://m.market.yandex.ru/TIMERS\t" +
            "referer=https://m.market.yandex.ru/catalog/54726/list?was_redir\\=1&srnum\\=334&hid\\=91491&rt\\=9&text" +
            "\\=%D0%B0%D0%B9%D1%84%D0%BE%D0%BD%207&onstock\\=0&local-offers-first\\=0#\tsourcebit=0" +
            "\tsearchquery=\tfuniqid=0\txoperaminiphoneua=\txwapprofile=\twapprofile=\tprofile=" +
            "\tupdatetag=noindex\tcookieys=wprid.1518027620581247-261640060301702006442772-man1-5717-TCH" +
            "\tcookiel=\theaderargs=293: sc_1518027649647\\=%D0%B0%D0%B9%D1%84%D0%BE%D0%BD%207%20:" +
            "m.market.yandex.ru:%2Fsearch%2Ftouch%2F; __ym_zz_zz\\=1518010815587207613160375285579; " +
            "yabs-frequency\\=/4/00020000001hGtjQ/; ys\\=wprid" +
             ".1518027620581247-261640060301702006442772-man1-5717-TCH; " +
            "yp\\=1520602500.ygu.1#1533778503.szm.2%3A667x375%3A375x553#1520619622.shlos.1#1533792585.sz.667x375x2; " +
            "my\\=YwA\\=; _ym_isad\\=2; _ym_uid\\=15180105011068821150; yabs-sid\\=712006511518010501; " +
            "i\\=RdOPzpVXyn9aFrrmFOAqFrWlgWXM3Pk8lqUlP5LHb8VZUqPbhkTaNnUXv/AcgN3WAaAw577v5W1vsZw+OHC2f08pxYk\\=; " +
            "mda\\=0; yandex_gid\\=213; " +
            "yandexuid\\=5590356461518010500\tdomainzone=yandex.ru\tcookiegpauto=\tmetrikaexp=\tetag=0" +
            "\tcookiei=5590356461518010500\tdonottrack=1";

        checker.checkEmpty(line);
    }

    @Test
    public void skipWebNonPrimitiveInfoEntries() throws Exception {
        String line = "tskv\ttskv_format=bs-watch-log\tunixtime=1518027658\trequestid" +
         "=000564A35B5211A200000771F798EBC4" +
            "\twatchid=425003085551896433\teventtime=1518027658\tcounterid=722867\tclientip=31.173.83.77\t" +
            "clientip6=::ffff:31.173.83.77\tclientport=57330\tremoteip=0.0.0.0\tremoteip6=::1\tregionid=213" +
            "\tcookiegid=213\tuniqid=5590356461518010500\tsessid=712006511518010501\tcounterclass=0\tantivirusyes=0" +
            "\tbrowserinfo=cy:1:ti:7:s:375x667x32:sk:2:adb:2:fpr:216613626101:cn:1:w:375x553:z:180:i:" +
            "20180207212057:et:1518027658:en:utf-8:v:962:c:1:la:ru-ru:ar:1:ls:1343049629234" +
            ":rqn:7:rn:1037344467:hid:753866591:ds:,,,,,,,,,,,,:rqnl:1:st:1518027658:u:15180105011068821150:t" +
            ":«айфон 7» — Мобильные телефоны — купить на Яндекс.Маркете\t" +
            "params={\\\"requestId\\\":\\\"1518027650963/3aa21305ba22a4420074482e0c09fa33\\\"," +
            "\\\"info\\\":{\\\"pageId\\\":\\\"touch:list\\\",\\\"serviceId\\\":\\\"market_front_touch\\\"," +
            "\\\"expFlags\\\":[\\\"touch_delivery_description\\\"],\\\"widgetId\\\":\\\"/footer\\\"," +
            "\\\"widgetName\\\":\\\"w-footer\\\"},\\\"startTimeInMs\\\":1518027650964,\\\"startTime\\\":1518027651," +
            "\\\"timestampInMs\\\":1518027653863,\\\"timestamp\\\":1518027654,\\\"duration\\\":2426," +
            "\\\"name\\\":\\\"widget\\\",\\\"portion\\\":\\\"/footer\\\"}" +
            "\tuseragent=Mozilla/5.0 (iPhone; CPU iPhone OS 11_2_5 like Mac OS X) AppleWebKit/604.5.6 (KHTML, " +
            "like Gecko) Version/11.0 Mobile/15D60 Safari/604.1\turl=goal://m.market.yandex.ru/TIMERS\t" +
            "referer=https://m.market.yandex.ru/catalog/54726/list?was_redir\\=1&srnum\\=334&hid\\=91491&rt\\=9&text" +
            "\\=%D0%B0%D0%B9%D1%84%D0%BE%D0%BD%207&onstock\\=0&local-offers-first\\=0#\tsourcebit=0" +
            "\tsearchquery=\tfuniqid=0\txoperaminiphoneua=\txwapprofile=\twapprofile=\tprofile=" +
            "\tupdatetag=noindex\tcookieys=wprid.1518027620581247-261640060301702006442772-man1-5717-TCH" +
            "\tcookiel=\theaderargs=293: sc_1518027649647\\=%D0%B0%D0%B9%D1%84%D0%BE%D0%BD%207%20:" +
            "m.market.yandex.ru:%2Fsearch%2Ftouch%2F; __ym_zz_zz\\=1518010815587207613160375285579; " +
            "yabs-frequency\\=/4/00020000001hGtjQ/; ys\\=wprid" +
             ".1518027620581247-261640060301702006442772-man1-5717-TCH; " +
            "yp\\=1520602500.ygu.1#1533778503.szm.2%3A667x375%3A375x553#1520619622.shlos.1#1533792585.sz.667x375x2; " +
            "my\\=YwA\\=; _ym_isad\\=2; _ym_uid\\=15180105011068821150; yabs-sid\\=712006511518010501; " +
            "i\\=RdOPzpVXyn9aFrrmFOAqFrWlgWXM3Pk8lqUlP5LHb8VZUqPbhkTaNnUXv/AcgN3WAaAw577v5W1vsZw+OHC2f08pxYk\\=; " +
            "mda\\=0; yandex_gid\\=213; " +
            "yandexuid\\=5590356461518010500\tdomainzone=yandex.ru\tcookiegpauto=\tmetrikaexp=\tetag=0" +
            "\tcookiei=5590356461518010500\tdonottrack=1";

        String[] keys = {"widgetId", "widgetName"};
        String[] values = {"/footer", "w-footer"};

        checker.check(
            line,
            new Date(1518027653863L),
            "1518027650963/3aa21305ba22a4420074482e0c09fa33", "market_front_touch", "web",
            213, checker.getHost(), "widget", "/footer",
            2426, "touch:list", keys, values, "", "", "", 1518027653863L, 1518027651, 1518027650964L,
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_2_5 like Mac OS X) AppleWebKit/604.5.6 (KHTML, " +
                "like Gecko) Version/11.0 Mobile/15D60 Safari/604.1",
            "31.173.83.77", "5590356461518010500", "m.market.yandex.ru", ""
        );
    }

    @Test
    public void tskvSkipOnAbsentJsonRequiredFields() throws Exception {
        String[] lines = {
            "tskv\ttskv_format=metrika-mobile-log\tEventValue={\"name\":\"test\",\"duration\":123,\"startTime\":123}",
            "tskv\ttskv_format=metrika-mobile-log\tEventValue={\"name\":\"test\",\"duration\":123," +
             "\"requestId\":\"test\"}",
            "tskv\ttskv_format=metrika-mobile-log\tEventValue={\"name\":\"test\",\"startTime\":123," +
             "\"requestId\":\"test\"}",
            "tskv\ttskv_format=metrika-mobile-log\tEventValue={\"duration\":123,\"startTime\":123," +
             "\"requestId\":\"test\"}",
        };

        for (String line : lines) {
            checker.checkEmpty(line);
        }
    }

    @Test
    public void tskvSkipOnMalformedJson() throws Exception {
        String line =
            "tskv\ttskv_format=metrika-mobile-log\tEventValue=%{\"name\":\"test\",\"duration\":123,\"startTime\":123}";

        checker.checkEmpty(line);
    }

    @Test
    public void bigSkipOnMalformedJson() throws Exception {
        String line = "tskv_format=bs-watch-log\tparams=%{}";

        checker.checkEmpty(line);
    }

    @Test
    public void webSkipOnAbsentJsonRequiredFields() throws Exception {
        String[] lines = {
            "tskv_format=bs-watch-log\tparams={\\\"name\\\":\\\"test\\\",\\\"portion\\\":\\\"test\\\"," +
             "\\\"duration\\\":123,\\\"startTime\\\":123}",
            "tskv_format=bs-watch-log\tparams={\\\"name\\\":\\\"test\\\",\\\"portion\\\":\\\"test\\\"," +
                "\\\"duration\\\":123,\\\"requestId\\\":\\\"test\\\"}",
            "tskv_format=bs-watch-log\tparams={\\\"name\\\":\\\"test\\\",\\\"portion\\\":\\\"test\\\"," +
             "\\\"startTime\\\":123,\\\"requestId\\\":\\\"test\\\"}",
            "tskv_format=bs-watch-log\tparams={\\\"name\\\":\\\"test\\\",\\\"duration\\\":123,\\\"startTime\\\":123," +
                "\\\"requestId\\\":\\\"test\\\"}",
            "tskv_format=bs-watch-log\tparams={\\\"portion\\\":\\\"test\\\",\\\"duration\\\":123," +
                "\\\"startTime\\\":123,\\\"requestId\\\":\\\"test\\\"}"
        };

        for (String line : lines) {
            checker.checkEmpty(line);
        }
    }

    @Test
    public void appAcceptStartTimeInSeconds() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 10:09:36\ttimezone=+0000\t" +
            "APIKey=23104\tAPIKey128=d0c8d8a8-9cff-4b00-b891-e081b41953a2\tStartDate=2017-10-12\t" +
            "UUID=99dd8f1afd46529dfdc316e13d881c32\tDeviceID=5B5E6CD9-B009-4635-89CA-A5558A3A3B6F\t" +
            "OriginalDeviceID=6C03EC48-DCA0-46E6-9548-079D042866A6UUIDHash=737899387808438411\t" +
            "DeviceIDHash=16327040839588031404\tIFV=5062B6CA-83A8-4D69-B261-6706CE4E3DF8\tAppPlatform=iOS\t" +
            "AppFramework=NATIVE\tAppVersionName=512\tAppBuildNumber=1613\tAppDebuggable=false\t" +
            "KitVersion=292\tManufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\t" +
            "OriginalModel=iPhone8,1\tOSVersion=11.0\tOSApiLevel=11\tScreenWidth=667\tScreenHeight=375\t" +
            "ScreenDPI=326\tScaleFactor=2\tClientKitVersion=292\tKitBuildType=101\tKitBuildNumber=8707\t" +
            "SendTimestamp=1507802976\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507802976\tSessionID=1507802917\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=7123113273064847859\tStartTime=2017-10-12 13:08:37\t" +
            "StartTimestamp=1507802917\tStartTimeZone=10800\tRegionTimeZone=10800\tLocale=en_RU\t" +
            "LocationSource=LBS\tLatitude=55.733821868896484\tLongitude=37.58698272705078\t" +
            "LocationPrecision=140\tLocationEnabled=undefined\tWifi_Macs=['2CD02D6B0EB0']\t" +
            "Wifi_SignalsStrengths=[0]\tWifi_Ssids=['Yandex']\tWifi_AreConnected=[1]\t" +
            "WifiAccessPointState=UNKNOWN\tConnectionType=CONN_WIFI\tNetworkType=LTE\tCountryCode=250\t" +
            "OperatorID=11\tOperatorName=YOTA\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\t" +
            "Cells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[11]\t" +
            "Cells_OperatorsNames=['YOTA']\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\t" +
            "Cells_PhysicalsCellsIDs=[0]\tDeviceType=PHONE\tEventID=16467856438443608079\tEventNumber=70\t" +
            "EventDate=2017-10-12\tEventDateTime=2017-10-12 13:09:35\tEventTimestamp=1507802975\t" +
            "EventTimeZone=10800\tEventTimeOffset=58\tEventType=EVENT_STATBOX\t" +
            "EventName=скорость > переход между экранами\t" +
            "EventValue={\"info\" : {\"fromPageId\" : \"touch:catalog\",\"toPageId\" : \"touch:product\"}," +
            "\"name\" : \"user-interaction-navigation-transition\",\"startTime\" : 1510842479," +
            "\"requestId\" : \"1507802975099/70DF58477F0440C48B20027DB0B040ED\",  " +
            "\"timestamp\" : 1507802656653,  \"duration\" : 240}\tRegionID=120542\t" +
            "AppID=ru.yandex.ymarket.inhouse\tClientIP=2a02:6b8:0:40c:cdf8:1438:8fe1:3552\tClientPort=58691\t" +
            "Sex=1\tAge=25\tAccountID=285830221\tAccountType=login\tDeduplicationEnabled=1\n";

        String[] keys = {"fromPageId", "toPageId"};
        String[] values = {"touch:catalog", "touch:product"};

        checker.check(
            line,
            new Date(1507802656653L),
            "1507802975099/70DF58477F0440C48B20027DB0B040ED", "market_front_touch", "iOS",
            120542, checker.getHost(), "user-interaction-navigation-transition", "",
            240, "", keys, values, "iPhone 6s", "512", "1613", 1507802656653L, 1510842479, 1510842479000L, "",
            "2a02:6b8:0:40c:cdf8:1438:8fe1:3552", "5B5E6CD9-B009-4635-89CA-A5558A3A3B6F", "", ""
        );
    }

    @Test
    public void appAcceptStartTimeInMilliseconds() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 10:09:36\ttimezone=+0000\t" +
            "APIKey=23104\tAPIKey128=d0c8d8a8-9cff-4b00-b891-e081b41953a2\tStartDate=2017-10-12\t" +
            "UUID=99dd8f1afd46529dfdc316e13d881c32\tDeviceID=5B5E6CD9-B009-4635-89CA-A5558A3A3B6F\t" +
            "OriginalDeviceID=6C03EC48-DCA0-46E6-9548-079D042866A6UUIDHash=737899387808438411\t" +
            "DeviceIDHash=16327040839588031404\tIFV=5062B6CA-83A8-4D69-B261-6706CE4E3DF8\tAppPlatform=iOS\t" +
            "AppFramework=NATIVE\tAppVersionName=512\tAppBuildNumber=1613\tAppDebuggable=false\t" +
            "KitVersion=292\tManufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\t" +
            "OriginalModel=iPhone8,1\tOSVersion=11.0\tOSApiLevel=11\tScreenWidth=667\tScreenHeight=375\t" +
            "ScreenDPI=326\tScaleFactor=2\tClientKitVersion=292\tKitBuildType=101\tKitBuildNumber=8707\t" +
            "SendTimestamp=1507802976\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507802976\tSessionID=1507802917\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=7123113273064847859\tStartTime=2017-10-12 13:08:37\t" +
            "StartTimestamp=1507802917\tStartTimeZone=10800\tRegionTimeZone=10800\tLocale=en_RU\t" +
            "LocationSource=LBS\tLatitude=55.733821868896484\tLongitude=37.58698272705078\t" +
            "LocationPrecision=140\tLocationEnabled=undefined\tWifi_Macs=['2CD02D6B0EB0']\t" +
            "Wifi_SignalsStrengths=[0]\tWifi_Ssids=['Yandex']\tWifi_AreConnected=[1]\t" +
            "WifiAccessPointState=UNKNOWN\tConnectionType=CONN_WIFI\tNetworkType=LTE\tCountryCode=250\t" +
            "OperatorID=11\tOperatorName=YOTA\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\t" +
            "Cells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[11]\t" +
            "Cells_OperatorsNames=['YOTA']\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\t" +
            "Cells_PhysicalsCellsIDs=[0]\tDeviceType=PHONE\tEventID=16467856438443608079\tEventNumber=70\t" +
            "EventDate=2017-10-12\tEventDateTime=2017-10-12 13:09:35\tEventTimestamp=1507802975\t" +
            "EventTimeZone=10800\tEventTimeOffset=58\tEventType=EVENT_STATBOX\t" +
            "EventName=скорость > переход между экранами\t" +
            "EventValue={\"info\" : {\"fromPageId\" : \"touch:catalog\",\"toPageId\" : \"touch:product\"}," +
            "\"name\" : \"user-interaction-navigation-transition\",\"startTime\" : 1510842479123," +
            "\"requestId\" : \"1507802975099/70DF58477F0440C48B20027DB0B040ED\",  " +
            "\"timestamp\" : 1507802656653,  \"duration\" : 240}\tRegionID=120542\t" +
            "AppID=ru.yandex.ymarket.inhouse\tClientIP=2a02:6b8:0:40c:cdf8:1438:8fe1:3552\tClientPort=58691\t" +
            "Sex=1\tAge=25\tAccountID=285830221\tAccountType=login\tDeduplicationEnabled=1\n";

        String[] keys = {"fromPageId", "toPageId"};
        String[] values = {"touch:catalog", "touch:product"};

        checker.check(
            line,
            new Date(1507802656653L),
            "1507802975099/70DF58477F0440C48B20027DB0B040ED", "market_front_touch", "iOS",
            120542, checker.getHost(), "user-interaction-navigation-transition", "",
            240, "", keys, values, "iPhone 6s", "512", "1613", 1507802656653L, 1510842479, 1510842479123L, "",
            "2a02:6b8:0:40c:cdf8:1438:8fe1:3552", "5B5E6CD9-B009-4635-89CA-A5558A3A3B6F", "", ""
        );
    }


    @Test
    public void appSkipWithZeroTimestamp() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 10:09:36\ttimezone=+0000\t" +
            "APIKey=23104\tAPIKey128=d0c8d8a8-9cff-4b00-b891-e081b41953a2\tStartDate=2017-10-12\t" +
            "UUID=99dd8f1afd46529dfdc316e13d881c32\tDeviceID=5B5E6CD9-B009-4635-89CA-A5558A3A3B6F\t" +
            "OriginalDeviceID=6C03EC48-DCA0-46E6-9548-079D042866A6UUIDHash=737899387808438411\t" +
            "DeviceIDHash=16327040839588031404\tIFV=5062B6CA-83A8-4D69-B261-6706CE4E3DF8\tAppPlatform=iOS\t" +
            "AppFramework=NATIVE\tAppVersionName=512\tAppBuildNumber=1613\tAppDebuggable=false\t" +
            "KitVersion=292\tManufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\t" +
            "OriginalModel=iPhone8,1\tOSVersion=11.0\tOSApiLevel=11\tScreenWidth=667\tScreenHeight=375\t" +
            "ScreenDPI=326\tScaleFactor=2\tClientKitVersion=292\tKitBuildType=101\tKitBuildNumber=8707\t" +
            "SendTimestamp=1507802976\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507802976\tSessionID=1507802917\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=7123113273064847859\tStartTime=2017-10-12 13:08:37\t" +
            "StartTimestamp=1507802917\tStartTimeZone=10800\tRegionTimeZone=10800\tLocale=en_RU\t" +
            "LocationSource=LBS\tLatitude=55.733821868896484\tLongitude=37.58698272705078\t" +
            "LocationPrecision=140\tLocationEnabled=undefined\tWifi_Macs=['2CD02D6B0EB0']\t" +
            "Wifi_SignalsStrengths=[0]\tWifi_Ssids=['Yandex']\tWifi_AreConnected=[1]\t" +
            "WifiAccessPointState=UNKNOWN\tConnectionType=CONN_WIFI\tNetworkType=LTE\tCountryCode=250\t" +
            "OperatorID=11\tOperatorName=YOTA\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\t" +
            "Cells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[11]\t" +
            "Cells_OperatorsNames=['YOTA']\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\t" +
            "Cells_PhysicalsCellsIDs=[0]\tDeviceType=PHONE\tEventID=16467856438443608079\tEventNumber=70\t" +
            "EventDate=2017-10-12\tEventDateTime=2017-10-12 13:09:35\tEventTimestamp=1507802975\t" +
            "EventTimeZone=10800\tEventTimeOffset=58\tEventType=EVENT_STATBOX\t" +
            "EventName=скорость > переход между экранами\t" +
            "EventValue={\"info\" : {\"fromPageId\" : \"touch:catalog\",\"toPageId\" : \"touch:product\"}," +
            "\"name\" : \"user-interaction-navigation-transition\",\"startTime\" : 1510842479123," +
            "\"requestId\" : \"1507802975099/70DF58477F0440C48B20027DB0B040ED\",  " +
            "\"timestamp\" : 0,  \"duration\" : 240}\tRegionID=120542\t" +
            "AppID=ru.yandex.ymarket.inhouse\tClientIP=2a02:6b8:0:40c:cdf8:1438:8fe1:3552\tClientPort=58691\t" +
            "Sex=1\tAge=25\tAccountID=285830221\tAccountType=login\tDeduplicationEnabled=1\n";

        checker.checkEmpty(line);
    }


    @Test
    public void appSkipWithFutureTimestamp() throws Exception {
        long tsMs = System.currentTimeMillis() + 3600000;
        long ts = tsMs / 1000;

        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 10:09:36\ttimezone=+0000\t" +
            "APIKey=23104\tAPIKey128=d0c8d8a8-9cff-4b00-b891-e081b41953a2\tStartDate=2017-10-12\t" +
            "UUID=99dd8f1afd46529dfdc316e13d881c32\tDeviceID=5B5E6CD9-B009-4635-89CA-A5558A3A3B6F\t" +
            "OriginalDeviceID=6C03EC48-DCA0-46E6-9548-079D042866A6UUIDHash=737899387808438411\t" +
            "DeviceIDHash=16327040839588031404\tIFV=5062B6CA-83A8-4D69-B261-6706CE4E3DF8\tAppPlatform=iOS\t" +
            "AppFramework=NATIVE\tAppVersionName=512\tAppBuildNumber=1613\tAppDebuggable=false\t" +
            "KitVersion=292\tManufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\t" +
            "OriginalModel=iPhone8,1\tOSVersion=11.0\tOSApiLevel=11\tScreenWidth=667\tScreenHeight=375\t" +
            "ScreenDPI=326\tScaleFactor=2\tClientKitVersion=292\tKitBuildType=101\tKitBuildNumber=8707\t" +
            "SendTimestamp=1507802976\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507802976\tSessionID=1507802917\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=7123113273064847859\tStartTime=2017-10-12 13:08:37\t" +
            "StartTimestamp=1507802917\tStartTimeZone=10800\tRegionTimeZone=10800\tLocale=en_RU\t" +
            "LocationSource=LBS\tLatitude=55.733821868896484\tLongitude=37.58698272705078\t" +
            "LocationPrecision=140\tLocationEnabled=undefined\tWifi_Macs=['2CD02D6B0EB0']\t" +
            "Wifi_SignalsStrengths=[0]\tWifi_Ssids=['Yandex']\tWifi_AreConnected=[1]\t" +
            "WifiAccessPointState=UNKNOWN\tConnectionType=CONN_WIFI\tNetworkType=LTE\tCountryCode=250\t" +
            "OperatorID=11\tOperatorName=YOTA\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\t" +
            "Cells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[11]\t" +
            "Cells_OperatorsNames=['YOTA']\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\t" +
            "Cells_PhysicalsCellsIDs=[0]\tDeviceType=PHONE\tEventID=16467856438443608079\tEventNumber=70\t" +
            "EventDate=2017-10-12\tEventDateTime=2017-10-12 13:09:35\tEventTimestamp=1507802975\t" +
            "EventTimeZone=10800\tEventTimeOffset=58\tEventType=EVENT_STATBOX\t" +
            "EventName=скорость > переход между экранами\t" +
            "EventValue={\"info\" : {\"fromPageId\" : \"touch:catalog\",\"toPageId\" : \"touch:product\"}," +
            "\"name\" : \"user-interaction-navigation-transition\",\"startTime\" : 1510842479," +
            "\"requestId\" : \"1507802975099/70DF58477F0440C48B20027DB0B040ED\",  " +
            "\"timestamp\" : " + ts + ",  \"duration\" : 240}\tRegionID=120542\t" +
            "AppID=ru.yandex.ymarket.inhouse\tClientIP=2a02:6b8:0:40c:cdf8:1438:8fe1:3552\tClientPort=58691\t" +
            "Sex=1\tAge=25\tAccountID=285830221\tAccountType=login\tDeduplicationEnabled=1\n";

        checker.checkEmpty(line);
    }

    @Test
    public void appSkipWithFutureTimestampMs() throws Exception {
        long tsMs = System.currentTimeMillis() + 3600000;
        long ts = tsMs / 1000;

        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-12 10:09:36\ttimezone=+0000\t" +
            "APIKey=23104\tAPIKey128=d0c8d8a8-9cff-4b00-b891-e081b41953a2\tStartDate=2017-10-12\t" +
            "UUID=99dd8f1afd46529dfdc316e13d881c32\tDeviceID=5B5E6CD9-B009-4635-89CA-A5558A3A3B6F\t" +
            "OriginalDeviceID=6C03EC48-DCA0-46E6-9548-079D042866A6UUIDHash=737899387808438411\t" +
            "DeviceIDHash=16327040839588031404\tIFV=5062B6CA-83A8-4D69-B261-6706CE4E3DF8\tAppPlatform=iOS\t" +
            "AppFramework=NATIVE\tAppVersionName=512\tAppBuildNumber=1613\tAppDebuggable=false\t" +
            "KitVersion=292\tManufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\t" +
            "OriginalModel=iPhone8,1\tOSVersion=11.0\tOSApiLevel=11\tScreenWidth=667\tScreenHeight=375\t" +
            "ScreenDPI=326\tScaleFactor=2\tClientKitVersion=292\tKitBuildType=101\tKitBuildNumber=8707\t" +
            "SendTimestamp=1507802976\tSendTimeZone=10800\tReceiveDate=2017-10-12\t" +
            "ReceiveTimestamp=1507802976\tSessionID=1507802917\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=7123113273064847859\tStartTime=2017-10-12 13:08:37\t" +
            "StartTimestamp=1507802917\tStartTimeZone=10800\tRegionTimeZone=10800\tLocale=en_RU\t" +
            "LocationSource=LBS\tLatitude=55.733821868896484\tLongitude=37.58698272705078\t" +
            "LocationPrecision=140\tLocationEnabled=undefined\tWifi_Macs=['2CD02D6B0EB0']\t" +
            "Wifi_SignalsStrengths=[0]\tWifi_Ssids=['Yandex']\tWifi_AreConnected=[1]\t" +
            "WifiAccessPointState=UNKNOWN\tConnectionType=CONN_WIFI\tNetworkType=LTE\tCountryCode=250\t" +
            "OperatorID=11\tOperatorName=YOTA\tCells_CellsIDs=[0]\tCells_SignalsStrengths=[0]\t" +
            "Cells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[11]\t" +
            "Cells_OperatorsNames=['YOTA']\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\t" +
            "Cells_PhysicalsCellsIDs=[0]\tDeviceType=PHONE\tEventID=16467856438443608079\tEventNumber=70\t" +
            "EventDate=2017-10-12\tEventDateTime=2017-10-12 13:09:35\tEventTimestamp=1507802975\t" +
            "EventTimeZone=10800\tEventTimeOffset=58\tEventType=EVENT_STATBOX\t" +
            "EventName=скорость > переход между экранами\t" +
            "EventValue={\"info\" : {\"fromPageId\" : \"touch:catalog\",\"toPageId\" : \"touch:product\"}," +
            "\"name\" : \"user-interaction-navigation-transition\",\"startTime\" : 1510842479," +
            "\"startTimeInMs\" : 1510842479123, \"timestampInMs\": " + tsMs + "," +
            "\"requestId\" : \"1507802975099/70DF58477F0440C48B20027DB0B040ED\",  " +
            "\"timestamp\" : " + ts + ",  \"duration\" : 240}\tRegionID=120542\t" +
            "AppID=ru.yandex.ymarket.inhouse\tClientIP=2a02:6b8:0:40c:cdf8:1438:8fe1:3552\tClientPort=58691\t" +
            "Sex=1\tAge=25\tAccountID=285830221\tAccountType=login\tDeduplicationEnabled=1\n";

        checker.checkEmpty(line);
    }

    @Test
    public void webPreferIp4Over6() throws Exception {
        String line = "_logfeller_index_bucket=//home/logfeller/index/yabs-rt/bs-watch-log/900-1800/" +
            "1508948700/1508949000\t_stbx=rt3.man--yabs-rt--bs-watch-log:156@@18781776@@base64:sdl8JFBrWKpwU" +
            "KuHxf5rPg@@1508949020526@@1508949021@@bs-watch-log@@516509525\tantivirusyes=0\t" +
            "browserinfo=ti:7:s:320x568x32:sk:2:adb:2:fpr:216613626101:cn:1:w:320x497:z:180:i:20171025133233:et" +
            ":1508927553:en:utf-8:v:900:c:1:la:ru-ru:ar:1:ls:7221921141:rqn:3680:rn:106687610:hid:228801046:ds:" +
            ",,,,,,,,,,,,:rqnl:2:st:1508949020:u:1493650301540700277:t:«бу шуруповерт метабо» — Результаты " +
            "поиска — Яндекс.Маркет\tclientip=128.68.216.114\tclientip6=::ffff:128.68.216.114\t" +
            "clientport=34238\tcookiegid=213\tcookiegpauto=55_807804:37_580764:65:1:1508948934\t" +
            "cookiel=\tcookieys=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH\t" +
            "counterclass=0\tcounterid=722867\tdomainzone=yandex.ru\teventtime=1508949020\t" +
            "funiqid=6415179191375575620\theaderargs=293: yabs-sid\\=1642096531508910470; sc_1508949015322" +
            "\\=%D0%B3%D0%B4%D0%B5%20%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%BF%D0%B8%D1%81%D1%82%D0%BE%" +
            "D0%BB%D0%B5%D1%82%20%D0%BA%D1%80%D0%B0%D1%84%D1%82%D0%BE%D0%BB%20%D0%B4%D0%BB%D1%8F%20%D0%B3%D0%" +
            "B5%D1%80%D0%BC%D0%B5%D1%82%D0%B8%D0%BA%D0%B0:m.market.yandex.ru:%2Fsearch%2Ftouch%2F; " +
            "ys\\=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH; yp\\=2145916800.uuid.a4" +
            "76c80fa68d24b1bd7425211e455d3d#2145916800.did.F8B957C3-0173-4502-A5C4-0222E14CBB4C#1509418300.sz.667" +
            "x375x2#1523927007.szm.2%3A568x320%3A320x497#1510554827.ygu.1#1511541013.shlos.1#1508952534.gpauto." +
            "55_807804%3A37_580764%3A65%3A1%3A1508948934; _ym_isad\\=2; yabs-frequency\\=/4/001o0000003K5EzP/; " +
            "i\\=kTfvtJ8t24U490Ec55eWlrXl8SaPCnRj5/nMpAiA8yY+SC1XvpTq/pyMkzSkqZJtsA8qw+iGV7KNOljFGfEJqf8PG6k\\=" +
            "; yc\\=1508914877.ls.2136%3A353; my\\=YwA\\=; yandex_gid\\=213; mda\\=0; _ym_uid\\=1493650301540700" +
            "277; fuid01\\=59074b7c486b3a44.0WwRlfqutt6E8BETMz8g9XFRg73gu0WEPVO4PAdTkZdFHjO4PyGH_xeWjAt-nUpgxcd" +
            "FB0zruGEDMriBBynHtC1uUbIhPLuZaGovxsfYckFDhQyqnnGskLfiJ-xqzkGu; yandexuid\\=1005275251493650256\t" +
            "iso_eventtime=2017-10-25 19:30:20\tmetrikaexp=\tparams={\\\"requestId\\\":\\\"1508927549006/81fcf1" +
            "af7cbff9ba3bc84e8bd3766027\\\",\\\"info\\\":{\\\"pageId\\\":\\\"touch:search\\\"," +
            "\\\"serviceId\\\":\\\"market_front_touch\\\"},\\\"name\\\":" +
            "\\\"page\\\",\\\"startTime\\\":1508927547,\\\"startTimeInMs\\\":1508927547123," +
            "\\\"timestamp\\\":1508927549,\\\"timestampInMs\\\":1508927549123,\\\"duration\\\":3894," +
            "\\\"portion\\\":\\\"loaded\\\"}" +
            "\tprofile=\tprovider=\treferer=https://m.market.yandex.ru/sear" +
            "ch?text\\=%D0%B1%D1%83%20%D1%88%D1%83%D1%80%D1%83%D0%BF%D0%BE%D0%B2%D0%B5%D1%80%D1%82%20%D0%BC%D0" +
            "%B5%D1%82%D0%B0%D0%B1%D0%BE&clid\\=708&onstock\\=1&local-offers-first\\=0&page\\=1#list_12544517\t" +
            "regionid=213\tremoteip=127.0.0.1\tremoteip6=::ffff:127.0.0.1\t" +
            "requestid=00055C619257E60400000F8F67F33E49\tsearchquery=\tsessid=1642096531508910470\t" +
            "source_uri=prt://yabs-rt@2a02:6b8:0:872:0:0:3c:11;unknown\tsourcebit=0\tsubkey=\t" +
            "timestamp=1508949020\ttskv_format=bs-watch-log\tuniqid=1005275251493650256\t" +
            "unixtime=1508949020\tupdatetag=noindex\turl=goal://m.market.yandex.ru/TIMERS\t" +
            "useragent=Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
            "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1\twapprofile=\t" +
            "watchid=6312065858767163279\txoperaminiphoneua=\txwapprofile=\n";

        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1508927549123L),
            "1508927549006/81fcf1af7cbff9ba3bc84e8bd3766027", "market_front_touch", "web",
            213, checker.getHost(), "page", "loaded",
            3894, "touch:search", keys, values, "", "", "", 1508927549123L, 1508927547, 1508927547123L,
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
                "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1",
            "128.68.216.114", "1005275251493650256", "m.market.yandex.ru", ""
        );
    }

    @Test
    public void webSetIp6IfNoIp4() throws Exception {
        String line = "_logfeller_index_bucket=//home/logfeller/index/yabs-rt/bs-watch-log/900-1800/" +
            "1508948700/1508949000\t_stbx=rt3.man--yabs-rt--bs-watch-log:156@@18781776@@base64:sdl8JFBrWKpwU" +
            "KuHxf5rPg@@1508949020526@@1508949021@@bs-watch-log@@516509525\tantivirusyes=0\t" +
            "browserinfo=ti:7:s:320x568x32:sk:2:adb:2:fpr:216613626101:cn:1:w:320x497:z:180:i:20171025133233:et" +
            ":1508927553:en:utf-8:v:900:c:1:la:ru-ru:ar:1:ls:7221921141:rqn:3680:rn:106687610:hid:228801046:ds:" +
            ",,,,,,,,,,,,:rqnl:2:st:1508949020:u:1493650301540700277:t:«бу шуруповерт метабо» — Результаты " +
            "поиска — Яндекс.Маркет\tclientip6=::ffff:128.68.216.114\t" +
            "clientport=34238\tcookiegid=213\tcookiegpauto=55_807804:37_580764:65:1:1508948934\t" +
            "cookiel=\tcookieys=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH\t" +
            "counterclass=0\tcounterid=722867\tdomainzone=yandex.ru\teventtime=1508949020\t" +
            "funiqid=6415179191375575620\theaderargs=293: yabs-sid\\=1642096531508910470; sc_1508949015322" +
            "\\=%D0%B3%D0%B4%D0%B5%20%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%BF%D0%B8%D1%81%D1%82%D0%BE%" +
            "D0%BB%D0%B5%D1%82%20%D0%BA%D1%80%D0%B0%D1%84%D1%82%D0%BE%D0%BB%20%D0%B4%D0%BB%D1%8F%20%D0%B3%D0%" +
            "B5%D1%80%D0%BC%D0%B5%D1%82%D0%B8%D0%BA%D0%B0:m.market.yandex.ru:%2Fsearch%2Ftouch%2F; " +
            "ys\\=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH; yp\\=2145916800.uuid.a4" +
            "76c80fa68d24b1bd7425211e455d3d#2145916800.did.F8B957C3-0173-4502-A5C4-0222E14CBB4C#1509418300.sz.667" +
            "x375x2#1523927007.szm.2%3A568x320%3A320x497#1510554827.ygu.1#1511541013.shlos.1#1508952534.gpauto." +
            "55_807804%3A37_580764%3A65%3A1%3A1508948934; _ym_isad\\=2; yabs-frequency\\=/4/001o0000003K5EzP/; " +
            "i\\=kTfvtJ8t24U490Ec55eWlrXl8SaPCnRj5/nMpAiA8yY+SC1XvpTq/pyMkzSkqZJtsA8qw+iGV7KNOljFGfEJqf8PG6k\\=" +
            "; yc\\=1508914877.ls.2136%3A353; my\\=YwA\\=; yandex_gid\\=213; mda\\=0; _ym_uid\\=1493650301540700" +
            "277; fuid01\\=59074b7c486b3a44.0WwRlfqutt6E8BETMz8g9XFRg73gu0WEPVO4PAdTkZdFHjO4PyGH_xeWjAt-nUpgxcd" +
            "FB0zruGEDMriBBynHtC1uUbIhPLuZaGovxsfYckFDhQyqnnGskLfiJ-xqzkGu; yandexuid\\=1005275251493650256\t" +
            "iso_eventtime=2017-10-25 19:30:20\tmetrikaexp=\tparams={\\\"requestId\\\":\\\"1508927549006/81fcf1" +
            "af7cbff9ba3bc84e8bd3766027\\\",\\\"info\\\":{\\\"pageId\\\":\\\"touch:search\\\"," +
            "\\\"serviceId\\\":\\\"market_front_touch\\\"},\\\"name\\\":" +
            "\\\"page\\\",\\\"startTime\\\":1508927547,\\\"startTimeInMs\\\":1508927547123," +
            "\\\"timestamp\\\":1508927549,\\\"timestampInMs\\\":1508927549123,\\\"duration\\\":3894," +
            "\\\"portion\\\":\\\"loaded\\\",\\\"serviceId\\\":\\\"market_front_touch\\\"}" +
            "\tprofile=\tprovider=\treferer=https://m.market.yandex.ru/sear" +
            "ch?text\\=%D0%B1%D1%83%20%D1%88%D1%83%D1%80%D1%83%D0%BF%D0%BE%D0%B2%D0%B5%D1%80%D1%82%20%D0%BC%D0" +
            "%B5%D1%82%D0%B0%D0%B1%D0%BE&clid\\=708&onstock\\=1&local-offers-first\\=0&page\\=1#list_12544517\t" +
            "regionid=213\tremoteip=127.0.0.1\tremoteip6=::ffff:127.0.0.1\t" +
            "requestid=00055C619257E60400000F8F67F33E49\tsearchquery=\tsessid=1642096531508910470\t" +
            "source_uri=prt://yabs-rt@2a02:6b8:0:872:0:0:3c:11;unknown\tsourcebit=0\tsubkey=\t" +
            "timestamp=1508949020\ttskv_format=bs-watch-log\tuniqid=1005275251493650256\t" +
            "unixtime=1508949020\tupdatetag=noindex\turl=goal://m.market.yandex.ru/TIMERS\t" +
            "useragent=Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
            "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1\twapprofile=\t" +
            "watchid=6312065858767163279\txoperaminiphoneua=\txwapprofile=\n";

        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1508927549123L),
            "1508927549006/81fcf1af7cbff9ba3bc84e8bd3766027", "market_front_touch", "web",
            213, checker.getHost(), "page", "loaded",
            3894, "touch:search", keys, values, "", "", "", 1508927549123L, 1508927547, 1508927547123L,
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
                "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1",
            "::ffff:128.68.216.114", "1005275251493650256", "m.market.yandex.ru", ""
        );
    }

    @Test
    public void webSkipNonTimersGoal() throws Exception {
        String line = "_logfeller_index_bucket=//home/logfeller/index/yabs-rt/bs-watch-log/900-1800/" +
            "1508948700/1508949000\t_stbx=rt3.man--yabs-rt--bs-watch-log:156@@18781776@@base64:sdl8JFBrWKpwU" +
            "KuHxf5rPg@@1508949020526@@1508949021@@bs-watch-log@@516509525\tantivirusyes=0\t" +
            "browserinfo=ti:7:s:320x568x32:sk:2:adb:2:fpr:216613626101:cn:1:w:320x497:z:180:i:20171025133233:et" +
            ":1508927553:en:utf-8:v:900:c:1:la:ru-ru:ar:1:ls:7221921141:rqn:3680:rn:106687610:hid:228801046:ds:" +
            ",,,,,,,,,,,,:rqnl:2:st:1508949020:u:1493650301540700277:t:«бу шуруповерт метабо» — Результаты " +
            "поиска — Яндекс.Маркет\tclientip6=::ffff:128.68.216.114\t" +
            "clientport=34238\tcookiegid=213\tcookiegpauto=55_807804:37_580764:65:1:1508948934\t" +
            "cookiel=\tcookieys=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH\t" +
            "counterclass=0\tcounterid=722867\tdomainzone=yandex.ru\teventtime=1508949020\t" +
            "funiqid=6415179191375575620\theaderargs=293: yabs-sid\\=1642096531508910470; sc_1508949015322" +
            "\\=%D0%B3%D0%B4%D0%B5%20%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%BF%D0%B8%D1%81%D1%82%D0%BE%" +
            "D0%BB%D0%B5%D1%82%20%D0%BA%D1%80%D0%B0%D1%84%D1%82%D0%BE%D0%BB%20%D0%B4%D0%BB%D1%8F%20%D0%B3%D0%" +
            "B5%D1%80%D0%BC%D0%B5%D1%82%D0%B8%D0%BA%D0%B0:m.market.yandex.ru:%2Fsearch%2Ftouch%2F; " +
            "ys\\=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH; yp\\=2145916800.uuid.a4" +
            "76c80fa68d24b1bd7425211e455d3d#2145916800.did.F8B957C3-0173-4502-A5C4-0222E14CBB4C#1509418300.sz.667" +
            "x375x2#1523927007.szm.2%3A568x320%3A320x497#1510554827.ygu.1#1511541013.shlos.1#1508952534.gpauto." +
            "55_807804%3A37_580764%3A65%3A1%3A1508948934; _ym_isad\\=2; yabs-frequency\\=/4/001o0000003K5EzP/; " +
            "i\\=kTfvtJ8t24U490Ec55eWlrXl8SaPCnRj5/nMpAiA8yY+SC1XvpTq/pyMkzSkqZJtsA8qw+iGV7KNOljFGfEJqf8PG6k\\=" +
            "; yc\\=1508914877.ls.2136%3A353; my\\=YwA\\=; yandex_gid\\=213; mda\\=0; _ym_uid\\=1493650301540700" +
            "277; fuid01\\=59074b7c486b3a44.0WwRlfqutt6E8BETMz8g9XFRg73gu0WEPVO4PAdTkZdFHjO4PyGH_xeWjAt-nUpgxcd" +
            "FB0zruGEDMriBBynHtC1uUbIhPLuZaGovxsfYckFDhQyqnnGskLfiJ-xqzkGu; yandexuid\\=1005275251493650256\t" +
            "iso_eventtime=2017-10-25 19:30:20\tmetrikaexp=\tparams={\\\"requestId\\\":\\\"1508927549006/81fcf1" +
            "af7cbff9ba3bc84e8bd3766027\\\",\\\"info\\\":{\\\"pageId\\\":\\\"touch:search\\\"},\\\"name\\\":" +
            "\\\"page\\\",\\\"startTime\\\":1508927547,\\\"startTimeInMs\\\":1508927547123," +
            "\\\"timestamp\\\":1508927549,\\\"timestampInMs\\\":1508927549123,\\\"duration\\\":3894," +
            "\\\"portion\\\":\\\"loaded\\\",\\\"serviceId\\\":\\\"market_front_touch\\\"}" +
            "\tprofile=\tprovider=\treferer=https://m.market.yandex.ru/sear" +
            "ch?text\\=%D0%B1%D1%83%20%D1%88%D1%83%D1%80%D1%83%D0%BF%D0%BE%D0%B2%D0%B5%D1%80%D1%82%20%D0%BC%D0" +
            "%B5%D1%82%D0%B0%D0%B1%D0%BE&clid\\=708&onstock\\=1&local-offers-first\\=0&page\\=1#list_12544517\t" +
            "regionid=213\tremoteip=127.0.0.1\tremoteip6=::ffff:127.0.0.1\t" +
            "requestid=00055C619257E60400000F8F67F33E49\tsearchquery=\tsessid=1642096531508910470\t" +
            "source_uri=prt://yabs-rt@2a02:6b8:0:872:0:0:3c:11;unknown\tsourcebit=0\tsubkey=\t" +
            "timestamp=1508949020\ttskv_format=bs-watch-log\tuniqid=1005275251493650256\t" +
            "unixtime=1508949020\tupdatetag=noindex\turl=goal://example.com/OLOLO\t" +
            "useragent=Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
            "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1\twapprofile=\t" +
            "watchid=6312065858767163279\txoperaminiphoneua=\txwapprofile=\n";

        checker.checkEmpty(line);
    }

    @Test
    public void webSkipOlderMetrics() throws Exception {
        checker.setParam("maxAgeDays", "1");
        long ts = System.currentTimeMillis() - 129600000L;

        String line = "_logfeller_index_bucket=//home/logfeller/index/yabs-rt/bs-watch-log/900-1800/" +
            "1508948700/1508949000\t_stbx=rt3.man--yabs-rt--bs-watch-log:156@@18781776@@base64:sdl8JFBrWKpwU" +
            "KuHxf5rPg@@1508949020526@@1508949021@@bs-watch-log@@516509525\tantivirusyes=0\t" +
            "browserinfo=ti:7:s:320x568x32:sk:2:adb:2:fpr:216613626101:cn:1:w:320x497:z:180:i:20171025133233:et" +
            ":1508927553:en:utf-8:v:900:c:1:la:ru-ru:ar:1:ls:7221921141:rqn:3680:rn:106687610:hid:228801046:ds:" +
            ",,,,,,,,,,,,:rqnl:2:st:1508949020:u:1493650301540700277:t:«бу шуруповерт метабо» — Результаты " +
            "поиска — Яндекс.Маркет\tclientip=128.68.216.114\tclientip6=::ffff:128.68.216.114\t" +
            "clientport=34238\tcookiegid=213\tcookiegpauto=55_807804:37_580764:65:1:1508948934\t" +
            "cookiel=\tcookieys=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH\t" +
            "counterclass=0\tcounterid=722867\tdomainzone=yandex.ru\teventtime=1508949020\t" +
            "funiqid=6415179191375575620\theaderargs=293: yabs-sid\\=1642096531508910470; sc_1508949015322" +
            "\\=%D0%B3%D0%B4%D0%B5%20%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%BF%D0%B8%D1%81%D1%82%D0%BE%" +
            "D0%BB%D0%B5%D1%82%20%D0%BA%D1%80%D0%B0%D1%84%D1%82%D0%BE%D0%BB%20%D0%B4%D0%BB%D1%8F%20%D0%B3%D0%" +
            "B5%D1%80%D0%BC%D0%B5%D1%82%D0%B8%D0%BA%D0%B0:m.market.yandex.ru:%2Fsearch%2Ftouch%2F; " +
            "ys\\=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH; yp\\=2145916800.uuid.a4" +
            "76c80fa68d24b1bd7425211e455d3d#2145916800.did.F8B957C3-0173-4502-A5C4-0222E14CBB4C#1509418300.sz.667" +
            "x375x2#1523927007.szm.2%3A568x320%3A320x497#1510554827.ygu.1#1511541013.shlos.1#1508952534.gpauto." +
            "55_807804%3A37_580764%3A65%3A1%3A1508948934; _ym_isad\\=2; yabs-frequency\\=/4/001o0000003K5EzP/; " +
            "i\\=kTfvtJ8t24U490Ec55eWlrXl8SaPCnRj5/nMpAiA8yY+SC1XvpTq/pyMkzSkqZJtsA8qw+iGV7KNOljFGfEJqf8PG6k\\=" +
            "; yc\\=1508914877.ls.2136%3A353; my\\=YwA\\=; yandex_gid\\=213; mda\\=0; _ym_uid\\=1493650301540700" +
            "277; fuid01\\=59074b7c486b3a44.0WwRlfqutt6E8BETMz8g9XFRg73gu0WEPVO4PAdTkZdFHjO4PyGH_xeWjAt-nUpgxcd" +
            "FB0zruGEDMriBBynHtC1uUbIhPLuZaGovxsfYckFDhQyqnnGskLfiJ-xqzkGu; yandexuid\\=1005275251493650256\t" +
            "iso_eventtime=2017-10-25 19:30:20\tmetrikaexp=\tparams={\\\"requestId\\\":\\\"1508927549006/81fcf1" +
            "af7cbff9ba3bc84e8bd3766027\\\",\\\"info\\\":{\\\"pageId\\\":\\\"touch:search\\\"," +
            "\\\"serviceId\\\":\\\"market_front_touch\\\"},\\\"name\\\":" +
            "\\\"page\\\",\\\"startTime\\\":1508927547,\\\"startTimeInMs\\\":1508927547123," +
            "\\\"timestamp\\\":" + ts / 1000 + ",\\\"timestampInMs\\\":" + ts + ",\\\"duration\\\":3894," +
            "\\\"portion\\\":\\\"loaded\\\"}" +
            "\tprofile=\tprovider=\treferer=https://m.market.yandex.ru/sear" +
            "ch?text\\=%D0%B1%D1%83%20%D1%88%D1%83%D1%80%D1%83%D0%BF%D0%BE%D0%B2%D0%B5%D1%80%D1%82%20%D0%BC%D0" +
            "%B5%D1%82%D0%B0%D0%B1%D0%BE&clid\\=708&onstock\\=1&local-offers-first\\=0&page\\=1#list_12544517\t" +
            "regionid=213\tremoteip=127.0.0.1\tremoteip6=::ffff:127.0.0.1\t" +
            "requestid=00055C619257E60400000F8F67F33E49\tsearchquery=\tsessid=1642096531508910470\t" +
            "source_uri=prt://yabs-rt@2a02:6b8:0:872:0:0:3c:11;unknown\tsourcebit=0\tsubkey=\t" +
            "timestamp=" + ts / 1000 + "\ttskv_format=bs-watch-log\tuniqid=1005275251493650256\t" +
            "unixtime=" + ts / 1000 + "\tupdatetag=noindex\turl=goal://m.market.yandex.ru/TIMERS\t" +
            "useragent=Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
            "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1\twapprofile=\t" +
            "watchid=6312065858767163279\txoperaminiphoneua=\txwapprofile=\n";

        checker.checkEmpty(line);
    }

    @Test
    public void webDontSkipRecentMetrics() throws Exception {
        checker.setParam("maxAgeDays", "1");
        long tsMs = System.currentTimeMillis() - 3600000;
        long ts = tsMs / 1000;

        String line = "_logfeller_index_bucket=//home/logfeller/index/yabs-rt/bs-watch-log/900-1800/" +
            "1508948700/1508949000\t_stbx=rt3.man--yabs-rt--bs-watch-log:156@@18781776@@base64:sdl8JFBrWKpwU" +
            "KuHxf5rPg@@1508949020526@@1508949021@@bs-watch-log@@516509525\tantivirusyes=0\t" +
            "browserinfo=ti:7:s:320x568x32:sk:2:adb:2:fpr:216613626101:cn:1:w:320x497:z:180:i:20171025133233:et" +
            ":1508927553:en:utf-8:v:900:c:1:la:ru-ru:ar:1:ls:7221921141:rqn:3680:rn:106687610:hid:228801046:ds:" +
            ",,,,,,,,,,,,:rqnl:2:st:1508949020:u:1493650301540700277:t:«бу шуруповерт метабо» — Результаты " +
            "поиска — Яндекс.Маркет\tclientip=128.68.216.114\tclientip6=::ffff:128.68.216.114\t" +
            "clientport=34238\tcookiegid=213\tcookiegpauto=55_807804:37_580764:65:1:1508948934\t" +
            "cookiel=\tcookieys=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH\t" +
            "counterclass=0\tcounterid=722867\tdomainzone=yandex.ru\teventtime=1508949020\t" +
            "funiqid=6415179191375575620\theaderargs=293: yabs-sid\\=1642096531508910470; sc_1508949015322" +
            "\\=%D0%B3%D0%B4%D0%B5%20%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C%20%D0%BF%D0%B8%D1%81%D1%82%D0%BE%" +
            "D0%BB%D0%B5%D1%82%20%D0%BA%D1%80%D0%B0%D1%84%D1%82%D0%BE%D0%BB%20%D0%B4%D0%BB%D1%8F%20%D0%B3%D0%" +
            "B5%D1%80%D0%BC%D0%B5%D1%82%D0%B8%D0%BA%D0%B0:m.market.yandex.ru:%2Fsearch%2Ftouch%2F; " +
            "ys\\=wprid.1508949009561786-213342257331853388384817-vla1-2880-TCH; yp\\=2145916800.uuid.a4" +
            "76c80fa68d24b1bd7425211e455d3d#2145916800.did.F8B957C3-0173-4502-A5C4-0222E14CBB4C#1509418300.sz.667" +
            "x375x2#1523927007.szm.2%3A568x320%3A320x497#1510554827.ygu.1#1511541013.shlos.1#1508952534.gpauto." +
            "55_807804%3A37_580764%3A65%3A1%3A1508948934; _ym_isad\\=2; yabs-frequency\\=/4/001o0000003K5EzP/; " +
            "i\\=kTfvtJ8t24U490Ec55eWlrXl8SaPCnRj5/nMpAiA8yY+SC1XvpTq/pyMkzSkqZJtsA8qw+iGV7KNOljFGfEJqf8PG6k\\=" +
            "; yc\\=1508914877.ls.2136%3A353; my\\=YwA\\=; yandex_gid\\=213; mda\\=0; _ym_uid\\=1493650301540700" +
            "277; fuid01\\=59074b7c486b3a44.0WwRlfqutt6E8BETMz8g9XFRg73gu0WEPVO4PAdTkZdFHjO4PyGH_xeWjAt-nUpgxcd" +
            "FB0zruGEDMriBBynHtC1uUbIhPLuZaGovxsfYckFDhQyqnnGskLfiJ-xqzkGu; yandexuid\\=1005275251493650256\t" +
            "iso_eventtime=2017-10-25 19:30:20\tmetrikaexp=\tparams={\\\"requestId\\\":\\\"1508927549006/81fcf1" +
            "af7cbff9ba3bc84e8bd3766027\\\",\\\"info\\\":{\\\"pageId\\\":\\\"touch:search\\\"," +
            "\\\"serviceId\\\":\\\"market_front_touch\\\"},\\\"name\\\":" +
            "\\\"page\\\",\\\"startTime\\\":1508927547,\\\"startTimeInMs\\\":1508927547123," +
            "\\\"timestamp\\\":" + ts + ",\\\"timestampInMs\\\":" + tsMs + ",\\\"duration\\\":3894," +
            "\\\"portion\\\":\\\"loaded\\\"}" +
            "\tprofile=\tprovider=\treferer=https://m.market.yandex.ru/sear" +
            "ch?text\\=%D0%B1%D1%83%20%D1%88%D1%83%D1%80%D1%83%D0%BF%D0%BE%D0%B2%D0%B5%D1%80%D1%82%20%D0%BC%D0" +
            "%B5%D1%82%D0%B0%D0%B1%D0%BE&clid\\=708&onstock\\=1&local-offers-first\\=0&page\\=1#list_12544517\t" +
            "regionid=213\tremoteip=127.0.0.1\tremoteip6=::ffff:127.0.0.1\t" +
            "requestid=00055C619257E60400000F8F67F33E49\tsearchquery=\tsessid=1642096531508910470\t" +
            "source_uri=prt://yabs-rt@2a02:6b8:0:872:0:0:3c:11;unknown\tsourcebit=0\tsubkey=\t" +
            "timestamp=" + ts + "\ttskv_format=bs-watch-log\tuniqid=1005275251493650256\t" +
            "unixtime=" + ts + "\tupdatetag=noindex\turl=goal://m.market.yandex.ru/TIMERS\t" +
            "useragent=Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
            "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1\twapprofile=\t" +
            "watchid=6312065858767163279\txoperaminiphoneua=\txwapprofile=\n";

        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(tsMs),
            "1508927549006/81fcf1af7cbff9ba3bc84e8bd3766027", "market_front_touch", "web",
            213, checker.getHost(), "page", "loaded",
            3894, "touch:search", keys, values, "", "", "", tsMs, 1508927547, 1508927547123L,
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.1.30 " +
                "(KHTML, like Gecko) Version/10.0 YaBrowser/17.10.0.1970.10 Mobile/14G60 Safari/602.1",
            "128.68.216.114", "1005275251493650256", "m.market.yandex.ru", ""
        );
    }


    @Test
    public void skipAndroidAppOlderMetric() throws Exception {
        checker.setParam("maxAgeDays", "0.5");
        long tsMs = System.currentTimeMillis() - 64800000L;
        long ts = tsMs / 1000;

        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-26 10:25:49\t" +
            "timezone=+0000\tAPIKey=23107\tAPIKey128=a996e97c-9bed-4157-b05f-ab6b42fa46b6\tStartDate=2017-10-26\t" +
            "UUID=ce4642cd3123fb75fbf6c1abfeddd13e\tDeviceID=738ba4b7765246ae7f5f8b3992f4569f\t" +
            "UUIDHash=9045334822516910520\tDeviceIDHash=11487695674382257827\tAppPlatform=android\t" +
            "OperatingSystem=android\tAppFramework=NATIVE\tAppVersionName=5.11\tAppBuildNumber=1500\t" +
            "AppDebuggable=undefined\tKitVersion=262\tManufacturer=Samsung\tModel=Galaxy A7\t" +
            "OriginalManufacturer=samsung\tOriginalModel=SM-A700FD\tOSVersion=6.0.1\tOSApiLevel=23\t" +
            "ScreenWidth=1920\tScreenHeight=1080\tScreenDPI=480\tScaleFactor=3\tAndroidID=c7d47a16bb88e5b3\t" +
            "ADVID=71ec9bd6-39da-41e3-b050-c293ba337b5e\tClientKitVersion=262\tKitBuildType=202\t" +
            "KitBuildNumber=6484\tSendTimestamp=" + ts + "\tSendTimeZone=10800\tReceiveDate=2017-10-26\t" +
            "ReceiveTimestamp=" + ts + "\tSessionID=1509013541\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=17595071052827490338\t" +
            "StartTime=2017-10-26 13:25:41\tStartTimestamp=1509013541\t" +
            "StartTimeZone=10800\tRegionTimeZone=10800\tLocale=ru_RU\tLocationSource=LBS\t" +
            "Latitude=55.605796813964844\tLongitude=37.490509033203125\tLocationPrecision=324\t" +
            "LocationEnabled=undefined\tWifiAccessPointSsid=AndroidAP\tWifiAccessPointState=DISABLED\t" +
            "ConnectionType=CONN_CELL\tCountryCode=250\tOperatorID=99\t" +
            "Cells_CellsIDs=[197689353,2147483647,2147483647,2147483647,34415,3219,3220]\t" +
            "Cells_SignalsStrengths=[-78,-105,-103,-118,-81,-87,-97]\t" +
            "Cells_Lacs=[21677,2147483647,2147483647,2147483647,7753,7753,7753]\t" +
            "Cells_CountriesCodes=[250,2147483647,2147483647,2147483647,250,250,250]\t" +
            "Cells_OperatorsIDs=[99,2147483647,2147483647,2147483647,2,2,2]\t" +
            "Cells_OperatorsNames=['','','','','','','']\tCells_AreConnected=[1,0,0,0,1,0,0]\t" +
            "Cells_Types=[LTE,LTE,LTE,LTE,GSM,GSM,GSM]\tCells_PhysicalsCellsIDs=[50,414,240,325,0,0,0]\t" +
            "SimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[99]\tSimCards_OperatorsNames=['MegaFon']\t" +
            "SimCards_AreRoaming=[0]\tSimCards_IccIDs=['']\tNetworksInterfaces_Names=['wlan0','p2p0','dummy0']\t" +
            "NetworksInterfaces_Macs=['ACEE9E1017B3','AEEE9E1017B3','5E5B653FA1DB']\tDeviceType=PHONE\t" +
            "EventID=10057862631024411190\tEventNumber=24\tEventDate=2017-10-26\t" +
            "EventDateTime=2017-10-26 13:25:49\t" +
            "EventTimestamp=" + ts + "\tEventTimeZone=10800\tEventTimeOffset=8\t" +
            "EventType=EVENT_CLIENT\tEventName=TIMERS\t" +
            "EventValue={\"requestId\":\"1509013543308/0afa9b77932cacd6f1ce99ac5229ecda\",\"info\":" +
            "{\"pageId\":\"touch:index\"},\"name\":\"page\"," +
            "\"startTime\":1509013541,\"startTimeInMs\":1509013541123," +
            "\"timestamp\":" + ts + ",\"timestampInMs\":" + tsMs + "," +
            "\"duration\":5925,\"portion\":\"loaded\",\"yandex_uid\":\"5371758881499875586\"}\t" +
            "EventValueJsonReference={0:\"0\",1:{1:\"1\"},2:\"2\",3:3,4:3,5:5,6:\"6\",7:\"7\"}\tPars" +
            "edParams_Key1=['requestId','info','name','startTime','timestamp','duration','portion','yandex" +
            "_uid']\tParsedParams_Key2=['1509013543308/0afa9b77932cacd6f1ce99ac5229ecda','pageId','page','150901" +
            "3543','1509013543','5925','loaded','5371758881499875586']\tParsedParams_Key3=['','touch:inde" +
            "x','','','','','','']\tParsedParams_Key4=['','','','','','','','']\tParsedParams_Key5=['',''" +
            ",'','','','','','']\tParsedParams_Key6=['','','','','','','','']\tParsedParams_Key7=['','','','','" +
            "','','','']\tParsedParams_Key8=['','','','','','','','']\tParsedParams_Key9=['','','','','','',''" +
            ",'']\tParsedParams_Key10=['','','','','','','','']\tParsedParams_ValueDouble=[0,0,0,1509013543,150901" +
            "3543,5925,0,5371758881499875000]\tParsedParams_Quantity=[1,1,1,1,1,1,1,1]\tClickDate=1970-01-01\tRe" +
            "gionID=114619\tAppID=ru.yandex.market\tClientIP=::ffff:83.220.236.195\tClientPort=58375\tSex=1\t" +
            "Age=45\tDeduplicationEnabled=1\n";

        checker.checkEmpty(line);
    }

    @Test
    public void dontSkipAndroidAppRecentMetric() throws Exception {
        checker.setParam("maxAgeDays", "1");
        long tsMs = System.currentTimeMillis() - 3600000;
        long ts = tsMs / 1000;

        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-26 10:25:49\t" +
            "timezone=+0000\tAPIKey=23107\tAPIKey128=a996e97c-9bed-4157-b05f-ab6b42fa46b6\tStartDate=2017-10-26\t" +
            "UUID=ce4642cd3123fb75fbf6c1abfeddd13e\tDeviceID=738ba4b7765246ae7f5f8b3992f4569f\t" +
            "UUIDHash=9045334822516910520\tDeviceIDHash=11487695674382257827\tAppPlatform=android\t" +
            "OperatingSystem=android\tAppFramework=NATIVE\tAppVersionName=5.11\tAppBuildNumber=1500\t" +
            "AppDebuggable=undefined\tKitVersion=262\tManufacturer=Samsung\tModel=Galaxy A7\t" +
            "OriginalManufacturer=samsung\tOriginalModel=SM-A700FD\tOSVersion=6.0.1\tOSApiLevel=23\t" +
            "ScreenWidth=1920\tScreenHeight=1080\tScreenDPI=480\tScaleFactor=3\tAndroidID=c7d47a16bb88e5b3\t" +
            "ADVID=71ec9bd6-39da-41e3-b050-c293ba337b5e\tClientKitVersion=262\tKitBuildType=202\t" +
            "KitBuildNumber=6484\tSendTimestamp=" + ts + "\tSendTimeZone=10800\tReceiveDate=2017-10-26\t" +
            "ReceiveTimestamp=" + ts + "\tSessionID=1509013541\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=17595071052827490338\t" +
            "StartTime=2017-10-26 13:25:41\tStartTimestamp=1509013541\t" +
            "StartTimeZone=10800\tRegionTimeZone=10800\tLocale=ru_RU\tLocationSource=LBS\t" +
            "Latitude=55.605796813964844\tLongitude=37.490509033203125\tLocationPrecision=324\t" +
            "LocationEnabled=undefined\tWifiAccessPointSsid=AndroidAP\tWifiAccessPointState=DISABLED\t" +
            "ConnectionType=CONN_CELL\tCountryCode=250\tOperatorID=99\t" +
            "Cells_CellsIDs=[197689353,2147483647,2147483647,2147483647,34415,3219,3220]\t" +
            "Cells_SignalsStrengths=[-78,-105,-103,-118,-81,-87,-97]\t" +
            "Cells_Lacs=[21677,2147483647,2147483647,2147483647,7753,7753,7753]\t" +
            "Cells_CountriesCodes=[250,2147483647,2147483647,2147483647,250,250,250]\t" +
            "Cells_OperatorsIDs=[99,2147483647,2147483647,2147483647,2,2,2]\t" +
            "Cells_OperatorsNames=['','','','','','','']\tCells_AreConnected=[1,0,0,0,1,0,0]\t" +
            "Cells_Types=[LTE,LTE,LTE,LTE,GSM,GSM,GSM]\tCells_PhysicalsCellsIDs=[50,414,240,325,0,0,0]\t" +
            "SimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[99]\tSimCards_OperatorsNames=['MegaFon']\t" +
            "SimCards_AreRoaming=[0]\tSimCards_IccIDs=['']\tNetworksInterfaces_Names=['wlan0','p2p0','dummy0']\t" +
            "NetworksInterfaces_Macs=['ACEE9E1017B3','AEEE9E1017B3','5E5B653FA1DB']\tDeviceType=PHONE\t" +
            "EventID=10057862631024411190\tEventNumber=24\tEventDate=2017-10-26\t" +
            "EventDateTime=2017-10-26 13:25:49\t" +
            "EventTimestamp=" + ts + "\tEventTimeZone=10800\tEventTimeOffset=8\t" +
            "EventType=EVENT_CLIENT\tEventName=TIMERS\t" +
            "EventValue={\"requestId\":\"1509013543308/0afa9b77932cacd6f1ce99ac5229ecda\",\"info\":" +
            "{\"pageId\":\"touch:index\"},\"name\":\"page\"," +
            "\"startTime\":1509013541,\"startTimeInMs\":1509013541123," +
            "\"timestamp\":" + ts + ",\"timestampInMs\":" + tsMs + "," +
            "\"duration\":5925,\"portion\":\"loaded\",\"yandex_uid\":\"5371758881499875586\"}\t" +
            "EventValueJsonReference={0:\"0\",1:{1:\"1\"},2:\"2\",3:3,4:3,5:5,6:\"6\",7:\"7\"}\tPars" +
            "edParams_Key1=['requestId','info','name','startTime','timestamp','duration','portion','yandex" +
            "_uid']\tParsedParams_Key2=['1509013543308/0afa9b77932cacd6f1ce99ac5229ecda','pageId','page','150901" +
            "3543','1509013543','5925','loaded','5371758881499875586']\tParsedParams_Key3=['','touch:inde" +
            "x','','','','','','']\tParsedParams_Key4=['','','','','','','','']\tParsedParams_Key5=['',''" +
            ",'','','','','','']\tParsedParams_Key6=['','','','','','','','']\tParsedParams_Key7=['','','','','" +
            "','','','']\tParsedParams_Key8=['','','','','','','','']\tParsedParams_Key9=['','','','','','',''" +
            ",'']\tParsedParams_Key10=['','','','','','','','']\tParsedParams_ValueDouble=[0,0,0,1509013543,150901" +
            "3543,5925,0,5371758881499875000]\tParsedParams_Quantity=[1,1,1,1,1,1,1,1]\tClickDate=1970-01-01\tRe" +
            "gionID=114619\tAppID=ru.yandex.market\tClientIP=::ffff:83.220.236.195\tClientPort=58375\tSex=1\t" +
            "Age=45\tDeduplicationEnabled=1\n";

        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(tsMs),
            "1509013543308/0afa9b77932cacd6f1ce99ac5229ecda", "market_front_touch", "android",
            114619, checker.getHost(), "page", "loaded",
            5925, "touch:index", keys, values, "Galaxy A7", "5.11", "1500",
            tsMs, 1509013541, 1509013541123L, "", "::ffff:83.220.236.195",
            "738ba4b7765246ae7f5f8b3992f4569f", "", ""
        );
    }

    @Test
    public void dontParseEventValueWithOtherType() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-26 10:25:49\t" +
            "timezone=+0000\tAPIKey=23107\tAPIKey128=a996e97c-9bed-4157-b05f-ab6b42fa46b6\tStartDate=2017-10-26\t" +
            "UUID=ce4642cd3123fb75fbf6c1abfeddd13e\tDeviceID=738ba4b7765246ae7f5f8b3992f4569f\t" +
            "UUIDHash=9045334822516910520\tDeviceIDHash=11487695674382257827\tAppPlatform=android\t" +
            "OperatingSystem=android\tAppFramework=NATIVE\tAppVersionName=5.11\tAppBuildNumber=1500\t" +
            "AppDebuggable=undefined\tKitVersion=262\tManufacturer=Samsung\tModel=Galaxy A7\t" +
            "OriginalManufacturer=samsung\tOriginalModel=SM-A700FD\tOSVersion=6.0.1\tOSApiLevel=23\t" +
            "ScreenWidth=1920\tScreenHeight=1080\tScreenDPI=480\tScaleFactor=3\tAndroidID=c7d47a16bb88e5b3\t" +
            "ADVID=71ec9bd6-39da-41e3-b050-c293ba337b5e\tClientKitVersion=262\tKitBuildType=202\t" +
            "KitBuildNumber=6484\tSendTimestamp=1509013550\tSendTimeZone=10800\tReceiveDate=2017-10-26\t" +
            "ReceiveTimestamp=1509013549\tSessionID=1509013541\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=17595071052827490338\t" +
            "StartTime=2017-10-26 13:25:41\tStartTimestamp=1509013541\t" +
            "StartTimeZone=10800\tRegionTimeZone=10800\tLocale=ru_RU\tLocationSource=LBS\t" +
            "Latitude=55.605796813964844\tLongitude=37.490509033203125\tLocationPrecision=324\t" +
            "LocationEnabled=undefined\tWifiAccessPointSsid=AndroidAP\tWifiAccessPointState=DISABLED\t" +
            "ConnectionType=CONN_CELL\tCountryCode=250\tOperatorID=99\t" +
            "Cells_CellsIDs=[197689353,2147483647,2147483647,2147483647,34415,3219,3220]\t" +
            "Cells_SignalsStrengths=[-78,-105,-103,-118,-81,-87,-97]\t" +
            "Cells_Lacs=[21677,2147483647,2147483647,2147483647,7753,7753,7753]\t" +
            "Cells_CountriesCodes=[250,2147483647,2147483647,2147483647,250,250,250]\t" +
            "Cells_OperatorsIDs=[99,2147483647,2147483647,2147483647,2,2,2]\t" +
            "Cells_OperatorsNames=['','','','','','','']\tCells_AreConnected=[1,0,0,0,1,0,0]\t" +
            "Cells_Types=[LTE,LTE,LTE,LTE,GSM,GSM,GSM]\tCells_PhysicalsCellsIDs=[50,414,240,325,0,0,0]\t" +
            "SimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[99]\tSimCards_OperatorsNames=['MegaFon']\t" +
            "SimCards_AreRoaming=[0]\tSimCards_IccIDs=['']\tNetworksInterfaces_Names=['wlan0','p2p0','dummy0']\t" +
            "NetworksInterfaces_Macs=['ACEE9E1017B3','AEEE9E1017B3','5E5B653FA1DB']\tDeviceType=PHONE\t" +
            "EventID=10057862631024411190\tEventNumber=24\tEventDate=2017-10-26\t" +
            "EventDateTime=2017-10-26 13:25:49\t" +
            "EventTimestamp=1509013549\tEventTimeZone=10800\tEventTimeOffset=8\t" +
            "EventType=EVENT_CLIENT\tEventName=TIMERS\t" +
            "EventValue={\"type\":\"LOG\",\"requestId\":\"1509013543308/0afa9b77932cacd6f1ce99ac5229ecda\",\"info\":" +
            "{\"pageId\":\"touch:index\"},\"name\":\"page\"," +
            "\"startTime\":1509013541,\"startTimeInMs\":1509013541123," +
            "\"timestamp\":1509013543,\"timestampInMs\":1509013543123," +
            "\"duration\":5925,\"portion\":\"loaded\",\"yandex_uid\":\"5371758881499875586\"}\t" +
            "EventValueJsonReference={0:\"0\",1:{1:\"1\"},2:\"2\",3:3,4:3,5:5,6:\"6\",7:\"7\"}\tPars" +
            "edParams_Key1=['requestId','info','name','startTime','timestamp','duration','portion','yandex" +
            "_uid']\tParsedParams_Key2=['1509013543308/0afa9b77932cacd6f1ce99ac5229ecda','pageId','page','150901" +
            "3543','1509013543','5925','loaded','5371758881499875586']\tParsedParams_Key3=['','touch:inde" +
            "x','','','','','','']\tParsedParams_Key4=['','','','','','','','']\tParsedParams_Key5=['',''" +
            ",'','','','','','']\tParsedParams_Key6=['','','','','','','','']\tParsedParams_Key7=['','','','','" +
            "','','','']\tParsedParams_Key8=['','','','','','','','']\tParsedParams_Key9=['','','','','','',''" +
            ",'']\tParsedParams_Key10=['','','','','','','','']\tParsedParams_ValueDouble=[0,0,0,1509013543,150901" +
            "3543,5925,0,5371758881499875000]\tParsedParams_Quantity=[1,1,1,1,1,1,1,1]\tClickDate=1970-01-01\tRe" +
            "gionID=114619\tAppID=ru.yandex.market\tClientIP=::ffff:83.220.236.195\tClientPort=58375\tSex=1\t" +
            "Age=45\tDeduplicationEnabled=1\n";

        checker.checkEmpty(line);
    }

    @Test
    public void parseEventValueWithMetricType() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-26 10:25:49\t" +
            "timezone=+0000\tAPIKey=23107\tAPIKey128=a996e97c-9bed-4157-b05f-ab6b42fa46b6\tStartDate=2017-10-26\t" +
            "UUID=ce4642cd3123fb75fbf6c1abfeddd13e\tDeviceID=738ba4b7765246ae7f5f8b3992f4569f\t" +
            "UUIDHash=9045334822516910520\tDeviceIDHash=11487695674382257827\tAppPlatform=android\t" +
            "OperatingSystem=android\tAppFramework=NATIVE\tAppVersionName=5.11\tAppBuildNumber=1500\t" +
            "AppDebuggable=undefined\tKitVersion=262\tManufacturer=Samsung\tModel=Galaxy A7\t" +
            "OriginalManufacturer=samsung\tOriginalModel=SM-A700FD\tOSVersion=6.0.1\tOSApiLevel=23\t" +
            "ScreenWidth=1920\tScreenHeight=1080\tScreenDPI=480\tScaleFactor=3\tAndroidID=c7d47a16bb88e5b3\t" +
            "ADVID=71ec9bd6-39da-41e3-b050-c293ba337b5e\tClientKitVersion=262\tKitBuildType=202\t" +
            "KitBuildNumber=6484\tSendTimestamp=1509013550\tSendTimeZone=10800\tReceiveDate=2017-10-26\t" +
            "ReceiveTimestamp=1509013549\tSessionID=1509013541\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=17595071052827490338\t" +
            "StartTime=2017-10-26 13:25:41\tStartTimestamp=1509013541\t" +
            "StartTimeZone=10800\tRegionTimeZone=10800\tLocale=ru_RU\tLocationSource=LBS\t" +
            "Latitude=55.605796813964844\tLongitude=37.490509033203125\tLocationPrecision=324\t" +
            "LocationEnabled=undefined\tWifiAccessPointSsid=AndroidAP\tWifiAccessPointState=DISABLED\t" +
            "ConnectionType=CONN_CELL\tCountryCode=250\tOperatorID=99\t" +
            "Cells_CellsIDs=[197689353,2147483647,2147483647,2147483647,34415,3219,3220]\t" +
            "Cells_SignalsStrengths=[-78,-105,-103,-118,-81,-87,-97]\t" +
            "Cells_Lacs=[21677,2147483647,2147483647,2147483647,7753,7753,7753]\t" +
            "Cells_CountriesCodes=[250,2147483647,2147483647,2147483647,250,250,250]\t" +
            "Cells_OperatorsIDs=[99,2147483647,2147483647,2147483647,2,2,2]\t" +
            "Cells_OperatorsNames=['','','','','','','']\tCells_AreConnected=[1,0,0,0,1,0,0]\t" +
            "Cells_Types=[LTE,LTE,LTE,LTE,GSM,GSM,GSM]\tCells_PhysicalsCellsIDs=[50,414,240,325,0,0,0]\t" +
            "SimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[99]\tSimCards_OperatorsNames=['MegaFon']\t" +
            "SimCards_AreRoaming=[0]\tSimCards_IccIDs=['']\tNetworksInterfaces_Names=['wlan0','p2p0','dummy0']\t" +
            "NetworksInterfaces_Macs=['ACEE9E1017B3','AEEE9E1017B3','5E5B653FA1DB']\tDeviceType=PHONE\t" +
            "EventID=10057862631024411190\tEventNumber=24\tEventDate=2017-10-26\t" +
            "EventDateTime=2017-10-26 13:25:49\t" +
            "EventTimestamp=1509013549\tEventTimeZone=10800\tEventTimeOffset=8\t" +
            "EventType=EVENT_CLIENT\tEventName=TIMERS\t" +
            "EventValue={\"type\":\"METRIKA\",\"requestId\":\"1509013543308/0afa9b77932cacd6f1ce99ac5229ecda\"," +
            "\"info\":" +
            "{\"pageId\":\"touch:index\"},\"name\":\"page\"," +
            "\"startTime\":1509013541,\"startTimeInMs\":1509013541123," +
            "\"timestamp\":1509013543,\"timestampInMs\":1509013543123," +
            "\"duration\":5925,\"portion\":\"loaded\",\"yandex_uid\":\"5371758881499875586\"}\t" +
            "EventValueJsonReference={0:\"0\",1:{1:\"1\"},2:\"2\",3:3,4:3,5:5,6:\"6\",7:\"7\"}\tPars" +
            "edParams_Key1=['requestId','info','name','startTime','timestamp','duration','portion','yandex" +
            "_uid']\tParsedParams_Key2=['1509013543308/0afa9b77932cacd6f1ce99ac5229ecda','pageId','page','150901" +
            "3543','1509013543','5925','loaded','5371758881499875586']\tParsedParams_Key3=['','touch:inde" +
            "x','','','','','','']\tParsedParams_Key4=['','','','','','','','']\tParsedParams_Key5=['',''" +
            ",'','','','','','']\tParsedParams_Key6=['','','','','','','','']\tParsedParams_Key7=['','','','','" +
            "','','','']\tParsedParams_Key8=['','','','','','','','']\tParsedParams_Key9=['','','','','','',''" +
            ",'']\tParsedParams_Key10=['','','','','','','','']\tParsedParams_ValueDouble=[0,0,0,1509013543,150901" +
            "3543,5925,0,5371758881499875000]\tParsedParams_Quantity=[1,1,1,1,1,1,1,1]\tClickDate=1970-01-01\tRe" +
            "gionID=114619\tAppID=ru.yandex.market\tClientIP=::ffff:83.220.236.195\tClientPort=58375\tSex=1\t" +
            "Age=45\tDeduplicationEnabled=1\n";

        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1509013543123L),
            "1509013543308/0afa9b77932cacd6f1ce99ac5229ecda", "market_front_touch", "android",
            114619, checker.getHost(), "page", "loaded",
            5925, "touch:index", keys, values, "Galaxy A7", "5.11", "1500",
            1509013543123L, 1509013541, 1509013541123L, "", "::ffff:83.220.236.195",
            "738ba4b7765246ae7f5f8b3992f4569f", "", ""
        );
    }

    @Test
    public void parseEventValueWithoutType() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2017-10-26 10:25:49\t" +
            "timezone=+0000\tAPIKey=23107\tAPIKey128=a996e97c-9bed-4157-b05f-ab6b42fa46b6\tStartDate=2017-10-26\t" +
            "UUID=ce4642cd3123fb75fbf6c1abfeddd13e\tDeviceID=738ba4b7765246ae7f5f8b3992f4569f\t" +
            "UUIDHash=9045334822516910520\tDeviceIDHash=11487695674382257827\tAppPlatform=android\t" +
            "OperatingSystem=android\tAppFramework=NATIVE\tAppVersionName=5.11\tAppBuildNumber=1500\t" +
            "AppDebuggable=undefined\tKitVersion=262\tManufacturer=Samsung\tModel=Galaxy A7\t" +
            "OriginalManufacturer=samsung\tOriginalModel=SM-A700FD\tOSVersion=6.0.1\tOSApiLevel=23\t" +
            "ScreenWidth=1920\tScreenHeight=1080\tScreenDPI=480\tScaleFactor=3\tAndroidID=c7d47a16bb88e5b3\t" +
            "ADVID=71ec9bd6-39da-41e3-b050-c293ba337b5e\tClientKitVersion=262\tKitBuildType=202\t" +
            "KitBuildNumber=6484\tSendTimestamp=1509013550\tSendTimeZone=10800\tReceiveDate=2017-10-26\t" +
            "ReceiveTimestamp=1509013549\tSessionID=1509013541\tSessionType=SESSION_FOREGROUND\t" +
            "DeviceIDSessionIDHash=17595071052827490338\t" +
            "StartTime=2017-10-26 13:25:41\tStartTimestamp=1509013541\t" +
            "StartTimeZone=10800\tRegionTimeZone=10800\tLocale=ru_RU\tLocationSource=LBS\t" +
            "Latitude=55.605796813964844\tLongitude=37.490509033203125\tLocationPrecision=324\t" +
            "LocationEnabled=undefined\tWifiAccessPointSsid=AndroidAP\tWifiAccessPointState=DISABLED\t" +
            "ConnectionType=CONN_CELL\tCountryCode=250\tOperatorID=99\t" +
            "Cells_CellsIDs=[197689353,2147483647,2147483647,2147483647,34415,3219,3220]\t" +
            "Cells_SignalsStrengths=[-78,-105,-103,-118,-81,-87,-97]\t" +
            "Cells_Lacs=[21677,2147483647,2147483647,2147483647,7753,7753,7753]\t" +
            "Cells_CountriesCodes=[250,2147483647,2147483647,2147483647,250,250,250]\t" +
            "Cells_OperatorsIDs=[99,2147483647,2147483647,2147483647,2,2,2]\t" +
            "Cells_OperatorsNames=['','','','','','','']\tCells_AreConnected=[1,0,0,0,1,0,0]\t" +
            "Cells_Types=[LTE,LTE,LTE,LTE,GSM,GSM,GSM]\tCells_PhysicalsCellsIDs=[50,414,240,325,0,0,0]\t" +
            "SimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[99]\tSimCards_OperatorsNames=['MegaFon']\t" +
            "SimCards_AreRoaming=[0]\tSimCards_IccIDs=['']\tNetworksInterfaces_Names=['wlan0','p2p0','dummy0']\t" +
            "NetworksInterfaces_Macs=['ACEE9E1017B3','AEEE9E1017B3','5E5B653FA1DB']\tDeviceType=PHONE\t" +
            "EventID=10057862631024411190\tEventNumber=24\tEventDate=2017-10-26\t" +
            "EventDateTime=2017-10-26 13:25:49\t" +
            "EventTimestamp=1509013549\tEventTimeZone=10800\tEventTimeOffset=8\t" +
            "EventType=EVENT_CLIENT\tEventName=TIMERS\t" +
            "EventValue={\"requestId\":\"1509013543308/0afa9b77932cacd6f1ce99ac5229ecda\",\"info\":" +
            "{\"pageId\":\"touch:index\"},\"name\":\"page\"," +
            "\"startTime\":1509013541,\"startTimeInMs\":1509013541123," +
            "\"timestamp\":1509013543,\"timestampInMs\":1509013543123," +
            "\"duration\":5925,\"portion\":\"loaded\",\"yandex_uid\":\"5371758881499875586\"}\t" +
            "EventValueJsonReference={0:\"0\",1:{1:\"1\"},2:\"2\",3:3,4:3,5:5,6:\"6\",7:\"7\"}\tPars" +
            "edParams_Key1=['requestId','info','name','startTime','timestamp','duration','portion','yandex" +
            "_uid']\tParsedParams_Key2=['1509013543308/0afa9b77932cacd6f1ce99ac5229ecda','pageId','page','150901" +
            "3543','1509013543','5925','loaded','5371758881499875586']\tParsedParams_Key3=['','touch:inde" +
            "x','','','','','','']\tParsedParams_Key4=['','','','','','','','']\tParsedParams_Key5=['',''" +
            ",'','','','','','']\tParsedParams_Key6=['','','','','','','','']\tParsedParams_Key7=['','','','','" +
            "','','','']\tParsedParams_Key8=['','','','','','','','']\tParsedParams_Key9=['','','','','','',''" +
            ",'']\tParsedParams_Key10=['','','','','','','','']\tParsedParams_ValueDouble=[0,0,0,1509013543,150901" +
            "3543,5925,0,5371758881499875000]\tParsedParams_Quantity=[1,1,1,1,1,1,1,1]\tClickDate=1970-01-01\tRe" +
            "gionID=114619\tAppID=ru.yandex.market\tClientIP=::ffff:83.220.236.195\tClientPort=58375\tSex=1\t" +
            "Age=45\tDeduplicationEnabled=1\n";

        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1509013543123L),
            "1509013543308/0afa9b77932cacd6f1ce99ac5229ecda", "market_front_touch", "android",
            114619, checker.getHost(), "page", "loaded",
            5925, "touch:index", keys, values, "Galaxy A7", "5.11", "1500",
            1509013543123L, 1509013541, 1509013541123L, "", "::ffff:83.220.236.195",
            "738ba4b7765246ae7f5f8b3992f4569f", "", ""
        );
    }
}
