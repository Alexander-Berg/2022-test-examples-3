package ru.yandex.market.logshatter.parser.front;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

import static ru.yandex.market.logshatter.parser.trace.Environment.UNKNOWN;


public class AppMetrikaEventLogParserTest {
    private LogParserChecker checker;

    @Before
    public void setUp() {
        checker = new LogParserChecker(new AppMetrikaEventLogParser());
        checker.setOrigin("market-health-dev");
        checker.setParam("allowedAppEvents", "EVENT_STATBOX,EVENT_CLIENT");
        checker.setParam(
            "apiKeysToServiceNames",
            "23107:market_mobile,23104:market_front_touch,1389598:market_front_bluetouch"
        );
    }

    @Test
    public void parseAndroidWithStackTrace() throws Exception {
        String line = "ADVID=91c4b0b2-e03b-4899-9540-b96e04e086f1\tAPIKey=1389598\tAPIKey128=36ef1512-b352-4415-87bd-" +
            "cf45c4f7e46b\tAge=25\tAndroidID=e8f9cd59a322e40b\tAppBuildNumber=1600\tAppDebuggable=false\tAppFramewor" +
            "k=NATIVE\tAppID=ru.beru.android\tAppPlatform=android\tAppVersionName=1.55\tAttributionID=1\tAttributionI" +
            "DUUIDHash=7447668078913031990\tCells_AreConnected=[1]\tCells_CellsIDs=[0]\tCells_CountriesCodes=[0]\tCel" +
            "ls_Lacs=[0]\tCells_OperatorsIDs=[0]\tCells_OperatorsNames=[\\\"\\\"]\tCells_PhysicalsCellsIDs=[0]\tCell" +
            "s_SignalsStrengths=[0]\tCells_Types=[DEFAULT]\tClickDate=1970-01-01\tClientIP=::ffff:2.62.145.45\tClien" +
            "tIPHash=10050381772417513344\tClientKitVersion=3005002\tClientPort=18410\tConnectionType=CONN_WIFI\tCou" +
            "ntryCode=\tDeduplicationEnabled=1\tDeviceID=0de1ef8844b082009e6eb57e10aa7e77\tDeviceIDHash=147051650718" +
            "99854556\tDeviceIDSessionIDHash=10635599058492357844\tDeviceType=TABLET\tEventDate=2019-05-24\tEventDa" +
            "teTime=2019-05-24 18:56:50\tEventFirstOccurrence=undefined\tEventID=16385773888387851755\tEventName=HE" +
            "ALTH_EVENT_STATBOX\tEventNumber=76\tEventSource=sdk\tEventTimeOffset=263\tEventTimeZone=10800\tEventTi" +
            "mestamp=1558713410\tEventType=EVENT_STATBOX\tEventValue={\"level\":\"error\",\"name\":\"ADD_CART_ITEM" +
            "_ERROR\",\"portion\":\"SEARCH_RESULT_SCREEN\",\"type\":\"LOG\",\"duration\":0,\"requestId\":\"1558713" +
            "411440/468c1be92fe146ccb9b59298474434ae\",\"info\":{\"offer_id\":\"yDpJekrrgZH6Ra11ieiXri4vCeFRTe5RN" +
            "TKWMEHyprChGQ8uNUV6iPKCVkIQuWJ1zxpCiC9OE6FvJo9y2MpYOfjb73dDKMvEMMLeaFCGlb88Wgs4HCCLlmKcYlIADLeEoyguDvY" +
            "r7AWCTzoTFuOFAGQxp0J20t0HIU6NlDRAiDUBcHsZhpPWm3C-dBxSWwShyP9ZnCN3a-g87Y3GdWPdZNplnyDD0k9J_jOoXLpnfqkln" +
            "EF7p7OR81JRqxIp92ZD2iFR5n4fq9A_bzpDUbsSFW9v67L-Aqac\",\"message\":\"Error on adding item to cart\",\"st" +
            "ackTrace\":\"CommunicationException[SERVICE_ERROR(500)]\\n\\tat ru.yandex.market.net.http.RequestExecut" +
            "or.executeRequest(RequestExecutor.java:55)\\n\\tat ru.yandex.market.net.http.RequestExecutor.executeRe" +
            "quest(RequestExecutor.java:38)\\n\\tat ru.yandex.market.net.http.ContentApiClient.addCartItem(ContentA" +
            "piClient.java:469)\\n\\tat ru.yandex.market.db.CartOnlineRepository.addCartItemFromContentApi(CartOnli" +
            "neRepository.kt:157)\\n\\tat ru.yandex.market.db.CartOnlineRepository.access$addCartItemFromContentApi" +
            "(CartOnlineRepository.kt:41)\\n\\tat ru.yandex.market.db.CartOnlineRepository$addItemToCart$uploadCart" +
            "Item$1.apply(CartOnlineRepository.kt:134)\\n\\tat ru.yandex.market.db.CartOnlineRepository$addItemToCa" +
            "rt$uploadCartItem$1.apply(CartOnlineRepository.kt:41)\\n\\tat io.reactivex.internal.operators.single.S" +
            "ingleMap$MapSingleObserver.onSuccess(SingleMap.java:57)\\n\\tat io.reactivex.internal.operators.single." +
            "SingleFromCallable.subscribeActual(SingleFromCallable.java:56)\\n\\tat io.reactivex.Single.subscribe(S" +
            "ingle.java:3603)\\n\\tat io.reactivex.internal.operators.single.SingleMap.subscribeActual(SingleMap.ja" +
            "va:34)\\n\\tat io.reactivex.Single.subscribe(Single.java:3603)\\n\\tat io.reactivex.internal.operators" +
            ".single.SingleSubscribeOn$SubscribeOnObserver.run(SingleSubscribeOn.java:89)\\n\\tat io.reactivex.Sche" +
            "duler$DisposeTask.run(Scheduler.java:578)\\n\\tat ru.yandex.market.rx.schedulers.RxSchedulersFactory$A" +
            "ctionWrapper.run(RxSchedulersFactory.java:106)\\n\\tat io.reactivex.internal.schedulers.ScheduledRunna" +
            "ble.run(ScheduledRunnable.java:66)\\n\\tat io.reactivex.internal.schedulers.ScheduledRunnable.call(S" +
            "cheduledRunnable.java:57)\\n\\tat java.util.concurrent.FutureTask.run(FutureTask.java:237)\\n\\tat ja" +
            "va.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.ja" +
            "va:272)\\n\\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1133)\\n\\t" +
            "at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:607)\\n\\tat java.lang." +
            "Thread.run(Thread.java:761)\\n\",\"uid\":\"8357013411558604071\",\"muid\":\"1152921504671574927:UR0hJ" +
            "wfCScvLkQALzuDS4vjJJS7ew4Wb\"},\"startTime\":1558713411,\"timestamp\":1558713411}\tEventValueJsonRefe" +
            "rence=\tIsRevenueVerified=undefined\tKitBuildNumber=16697\tKitBuildType=202\tKitVersion=3005002\tLati" +
            "tude=\tLimitAdTracking=false\tLocale=ru_RU\tLocationEnabled=true\tLocationPrecision=\tLocationSource=" +
            "MISSING\tLongitude=\tManufacturer=DEXP\tModel=DEXP Ursus S169\tNetworkType=\tNetworksInterfaces_Macs=" +
            "[\\\"FA6856402D61\\\",\\\"EE1CB04B822C\\\",\\\"5E8D16445FA9\\\",\\\"4045DA19AEB8\\\",\\\"4045DA19AEB" +
            "8\\\"]\tNetworksInterfaces_Names=[\\\"seth_w0\\\",\\\"seth_w1\\\",\\\"seth_w2\\\",\\\"wlan0\\\",\\\"p" +
            "2p0\\\"]\tOSApiLevel=24\tOSVersion=7.0\tOperatingSystem=android\tOperatorID=\tOperatorName=\tOrigina" +
            "lManufacturer=DEXP\tOriginalModel=S169\tParsedParams_Key1=\tParsedParams_Key10=\tParsedParams_Key" +
            "2=\tParsedParams_Key3=\tParsedParams_Key4=\tParsedParams_Key5=\tParsedParams_Key6=\tParsedParams_Ke" +
            "y7=\tParsedParams_Key8=\tParsedParams_Key9=\tParsedParams_Quantity=\tParsedParams_ValueDouble=\tPro" +
            "fileAttributeVersion=26150867149258828\tReceiveDate=2019-05-24\tReceiveTimestamp=1558713558\tRegion" +
            "ID=11293\tRegionTimeZone=25200\tRevenueOrderIdSource=autogenerated\tScaleFactor=1\tScreenDPI=160\tS" +
            "creenHeight=600\tScreenWidth=1024\tSendTimestamp=1558713558\tSessionID=10000000005\tSessionType=SES" +
            "SION_FOREGROUND\tSex=1\tSimCards_AreRoaming=[0]\tSimCards_CountriesCodes=[0]\tSimCards_IccI" +
            "Ds=[\\\"\\\"]\tSimCards_OperatorsIDs=[0]\tSimCards_OperatorsNames=[\\\"\\\"]\tStartDate=2019-05-24\tS" +
            "tartTime=2019-05-24 18:52:27\tStartTimeZone=10800\tStartTimestamp=1558713147\tUUID=c3e60b9c826f411dbf5" +
            "282f76c7c583f\tUUIDHash=6422661156785156246\tWifiAccessPointState=DISABLED\tWifi_AreConnected=[1]\tWif" +
            "i_LastVisibleTimeOffset=[0]\tWifi_Macs=[\\\"18A6F77397DF\\\"]\tWifi_SignalsStrengths=[-63]\tWifi_Ssids" +
            "=[\\\"TP-LINK_7397DF\\\"]\t_logfeller_index_bucket=//home/logfeller/index/logfeller-topic-splitter/mar" +
            "ket-apps-metrika-mobile-log/3600-86400/1558714200/1558645200\t_logfeller_timestamp=1558713558\t_stbx=r" +
            "t3.man--logfeller-topic-splitter--market-apps-metrika-mobile-log:0@@1225180579@@pqout_market_apps_metr" +
            "ika_mobile_log:d25c3d02-be080231-dd82d590-1794-18040@@1558713648849@@1558713649@@market-apps-metrika-m" +
            "obile-log@@1558713648683617@@1558713648971\tiso_eventtime=2019-05-24 18:59:18\tsource_uri=prt://logfel" +
            "ler-topic-splitter@2a02:6b8:c01:723:0:1504:b2a9:923a;unknown_path\ttimestamp=2019-05-24 15:59:18\ttime" +
            "zone=+0000\ttskv_format=metrika-mobile-log\n";

        String[] tags = {};
        String[] keys = {"offer_id", "uid", "muid", "portion"};
        String[] values = {"yDpJekrrgZH6Ra11ieiXri4vCeFRTe5RNTKWMEHyprChGQ8uNUV6iPKCVkIQuWJ1zxpCiC9OE6FvJo9y2MpYOfjb" +
            "73dDKMvEMMLeaFCGlb88Wgs4HCCLlmKcYlIADLeEoyguDvYr7AWCTzoTFuOFAGQxp0J20t0HIU6NlDRAiDUBcHsZhpPWm3C-dBxSWwS" +
            "hyP9ZnCN3a-g87Y3GdWPdZNplnyDD0k9J_jOoXLpnfqklnEF7p7OR81JRqxIp92ZD2iFR5n4fq9A_bzpDUbsSFW9v67L-Aqac",
            "8357013411558604071", "1152921504671574927:UR0hJwfCScvLkQALzuDS4vjJJS7ew4Wb", "SEARCH_RESULT_SCREEN"};

        checker.check(
            line,
            new Date(1558713410000L),
            "market_front_bluetouch", checker.getHost(), "1558713411440/468c1be92fe146ccb9b59" +
                "298474434ae", "ADD_CART_ITEM_ERROR", "error", "Error on adding item to cart", "CommunicationException[SERVICE_ERROR(500)]\n" +
                "\tat ru.yandex.market.net.http.RequestExecutor.executeRequest(RequestExecutor.java:55)\n" +
                "\tat ru.yandex.market.net.http.RequestExecutor.executeRequest(RequestExecutor.java:38)\n" +
                "\tat ru.yandex.market.net.http.ContentApiClient.addCartItem(ContentApiClient.java:469)\n" +
                "\tat ru.yandex.market.db.CartOnlineRepository.addCartItemFromContentApi(CartOnlineRepository.kt:157)\n" +
                "\tat ru.yandex.market.db.CartOnlineRepository.access$addCartItemFromContentApi(CartOnlineRepository.kt:41)\n" +
                "\tat ru.yandex.market.db.CartOnlineRepository$addItemToCart$uploadCartItem$1.apply(CartOnlineRepository.kt:134)\n" +
                "\tat ru.yandex.market.db.CartOnlineRepository$addItemToCart$uploadCartItem$1.apply(CartOnlineRepository.kt:41)\n" +
                "\tat io.reactivex.internal.operators.single.SingleMap$MapSingleObserver.onSuccess(SingleMap.java:57)\n" +
                "\tat io.reactivex.internal.operators.single.SingleFromCallable.subscribeActual(SingleFromCallable.java:56)\n" +
                "\tat io.reactivex.Single.subscribe(Single.java:3603)\n" +
                "\tat io.reactivex.internal.operators.single.SingleMap.subscribeActual(SingleMap.java:34)\n" +
                "\tat io.reactivex.Single.subscribe(Single.java:3603)\n" +
                "\tat io.reactivex.internal.operators.single.SingleSubscribeOn$SubscribeOnObserver.run(SingleSubscribeOn.java:89)\n" +
                "\tat io.reactivex.Scheduler$DisposeTask.run(Scheduler.java:578)\n" +
                "\tat ru.yandex.market.rx.schedulers.RxSchedulersFactory$ActionWrapper.run(RxSchedulersFactory.java:106)\n" +
                "\tat io.reactivex.internal.schedulers.ScheduledRunnable.run(ScheduledRunnable.java:66)\n" +
                "\tat io.reactivex.internal.schedulers.ScheduledRunnable.call(ScheduledRunnable.java:57)\n" +
                "\tat java.util.concurrent.FutureTask.run(FutureTask.java:237)\n" +
                "\tat java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:272)\n" +
                "\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1133)\n" +
                "\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:607)\n" +
                "\tat java.lang.Thread.run(Thread.java:761)\n",
            "14924397360597504922", "RequestExecutor.java", 55, tags, keys, values, "1.55", UNKNOWN, "android"
        );
    }

    @Test
    public void parseAndroidWithoutInfo() throws Exception {
        String line = "ADVID=91c4b0b2-e03b-4899-9540-b96e04e086f1\tAPIKey=1389598\tAPIKey128=36ef1512-b352-4415-87bd-" +
            "cf45c4f7e46b\tAge=25\tAndroidID=e8f9cd59a322e40b\tAppBuildNumber=1600\tAppDebuggable=false\tAppFramewor" +
            "k=NATIVE\tAppID=ru.beru.android\tAppPlatform=android\tAppVersionName=1.55\tAttributionID=1\tAttributionI" +
            "DUUIDHash=7447668078913031990\tCells_AreConnected=[1]\tCells_CellsIDs=[0]\tCells_CountriesCodes=[0]\tCel" +
            "ls_Lacs=[0]\tCells_OperatorsIDs=[0]\tCells_OperatorsNames=[\\\"\\\"]\tCells_PhysicalsCellsIDs=[0]\tCell" +
            "s_SignalsStrengths=[0]\tCells_Types=[DEFAULT]\tClickDate=1970-01-01\tClientIP=::ffff:2.62.145.45\tClien" +
            "tIPHash=10050381772417513344\tClientKitVersion=3005002\tClientPort=18410\tConnectionType=CONN_WIFI\tCou" +
            "ntryCode=\tDeduplicationEnabled=1\tDeviceID=0de1ef8844b082009e6eb57e10aa7e77\tDeviceIDHash=147051650718" +
            "99854556\tDeviceIDSessionIDHash=10635599058492357844\tDeviceType=TABLET\tEventDate=2019-05-24\tEventDa" +
            "teTime=2019-05-24 18:56:50\tEventFirstOccurrence=undefined\tEventID=16385773888387851755\tEventName=HE" +
            "ALTH_EVENT_STATBOX\tEventNumber=76\tEventSource=sdk\tEventTimeOffset=263\tEventTimeZone=10800\tEventTi" +
            "mestamp=1558713410\tEventType=EVENT_STATBOX\tEventValue={\"level\":\"info\",\"name\":\"SCREEN_OPEN" +
            "ED\",\"portion\":\"PROFILE_SCREEN\",\"type\":\"LOG\",\"duration\":0,\"requestId\":\"1557919508493/c" +
            "873f5720f044ecaab4b6c9ccbc82a47\",\"startTime\":1557919508,\"timestamp\":1557919508}\tEventValueJsonRefe" +
            "rence=\tIsRevenueVerified=undefined\tKitBuildNumber=16697\tKitBuildType=202\tKitVersion=3005002\tLati" +
            "tude=\tLimitAdTracking=false\tLocale=ru_RU\tLocationEnabled=true\tLocationPrecision=\tLocationSource=" +
            "MISSING\tLongitude=\tManufacturer=DEXP\tModel=DEXP Ursus S169\tNetworkType=\tNetworksInterfaces_Macs=" +
            "[\\\"FA6856402D61\\\",\\\"EE1CB04B822C\\\",\\\"5E8D16445FA9\\\",\\\"4045DA19AEB8\\\",\\\"4045DA19AEB" +
            "8\\\"]\tNetworksInterfaces_Names=[\\\"seth_w0\\\",\\\"seth_w1\\\",\\\"seth_w2\\\",\\\"wlan0\\\",\\\"p" +
            "2p0\\\"]\tOSApiLevel=24\tOSVersion=7.0\tOperatingSystem=android\tOperatorID=\tOperatorName=\tOrigina" +
            "lManufacturer=DEXP\tOriginalModel=S169\tParsedParams_Key1=\tParsedParams_Key10=\tParsedParams_Key" +
            "2=\tParsedParams_Key3=\tParsedParams_Key4=\tParsedParams_Key5=\tParsedParams_Key6=\tParsedParams_Ke" +
            "y7=\tParsedParams_Key8=\tParsedParams_Key9=\tParsedParams_Quantity=\tParsedParams_ValueDouble=\tPro" +
            "fileAttributeVersion=26150867149258828\tReceiveDate=2019-05-24\tReceiveTimestamp=1558713558\tRegion" +
            "ID=11293\tRegionTimeZone=25200\tRevenueOrderIdSource=autogenerated\tScaleFactor=1\tScreenDPI=160\tS" +
            "creenHeight=600\tScreenWidth=1024\tSendTimestamp=1558713558\tSessionID=10000000005\tSessionType=SES" +
            "SION_FOREGROUND\tSex=1\tSimCards_AreRoaming=[0]\tSimCards_CountriesCodes=[0]\tSimCards_IccI" +
            "Ds=[\\\"\\\"]\tSimCards_OperatorsIDs=[0]\tSimCards_OperatorsNames=[\\\"\\\"]\tStartDate=2019-05-24\tS" +
            "tartTime=2019-05-24 18:52:27\tStartTimeZone=10800\tStartTimestamp=1558713147\tUUID=c3e60b9c826f411dbf5" +
            "282f76c7c583f\tUUIDHash=6422661156785156246\tWifiAccessPointState=DISABLED\tWifi_AreConnected=[1]\tWif" +
            "i_LastVisibleTimeOffset=[0]\tWifi_Macs=[\\\"18A6F77397DF\\\"]\tWifi_SignalsStrengths=[-63]\tWifi_Ssids" +
            "=[\\\"TP-LINK_7397DF\\\"]\t_logfeller_index_bucket=//home/logfeller/index/logfeller-topic-splitter/mar" +
            "ket-apps-metrika-mobile-log/3600-86400/1558714200/1558645200\t_logfeller_timestamp=1558713558\t_stbx=r" +
            "t3.man--logfeller-topic-splitter--market-apps-metrika-mobile-log:0@@1225180579@@pqout_market_apps_metr" +
            "ika_mobile_log:d25c3d02-be080231-dd82d590-1794-18040@@1558713648849@@1558713649@@market-apps-metrika-m" +
            "obile-log@@1558713648683617@@1558713648971\tiso_eventtime=2019-05-24 18:59:18\tsource_uri=prt://logfel" +
            "ler-topic-splitter@2a02:6b8:c01:723:0:1504:b2a9:923a;unknown_path\ttimestamp=2019-05-24 15:59:18\ttime" +
            "zone=+0000\ttskv_format=metrika-mobile-log\n";

        String[] tags = {};
        String[] keys = {"portion"};
        String[] values = {"PROFILE_SCREEN"};

        checker.check(
            line,
            new Date(1558713410000L),
            "market_front_bluetouch", checker.getHost(), "1557919508493/c873f5720f044ecaab4b" +
                "6c9ccbc82a47", "SCREEN_OPENED", "info", "", "", "", "", 0, tags, keys, values, "1.55", UNKNOWN, "android"
        );
    }

    @Test
    public void parseIosWithoutStackTrace() throws Exception {
        String line = "ADVID=\tAPIKey=1389598\tAPIKey128=36ef1512-b352-4415-87bd-cf45c4f7e46b\tAccountID=\tAccountTy" +
            "pe=\tAge=25\tAndroidID=\tAppBuildNumber=1\tAppDebuggable=true\tAppFramework=NATIVE\tAppID=ru.yandex.bl" +
            "ue.market\tAppPlatform=iOS\tAppVersionName=223\tAttributionIDUUIDHash=6330336754387851607\tCells_Are" +
            "Connected=\tCells_CellsIDs=\tCells_CountriesCodes=\tCells_Lacs=\tCells_OperatorsIDs=\tCells_Operators" +
            "Names=\tCells_PhysicalsCellsIDs=\tCells_SignalsStrengths=\tCells_Types=\tClickDate=1970-01-01\tClientI" +
            "P=2a02:6b8:0:419:6968:31e9:34ed:2289\tClientIPHash=5080544421494218057\tClientKitVersion=3005000\tClie" +
            "ntPort=65170\tConnectionType=CONN_WIFI\tCountryCode=\tDeduplicationEnabled=1\tDeviceID=DD7AF6FC-DAEA-4" +
            "760-8C87-0E7F0318974F\tDeviceIDHash=14245746932424403545\tDeviceIDSessionIDHash=6901266150818643632\tDe" +
            "viceType=PHONE\tEventDate=2019-05-15\tEventDateTime=2019-05-15 17:05:24\tEventFirstOccurrence=undefine" +
            "d\tEventID=7725579940267362833\tEventNumber=53\tEventSource=sdk\tEventTimeOffset=19\tEventTimeZone=1080" +
            "0\tEventTimestamp=1557929124\tEventType=EVENT_STATBOX\tEventValue={  \"type\" : \"LOG\",  \"timestam" +
            "p\" : 1557929124,  \"startTime\" : 1557929124,  \"requestId\" : \"1557929124783/55051A64507C40CA932B2" +
            "587B99BB899\",  \"name\" : \"SCREEN_OPENED\",  \"portion\" : \"CART_SCREEN\",  \"level\" : \"INFO\", " +
            " \"duration\" : 0,  \"info\" : {    \"uid\" : \"a9fc48beec6943aa9715876fe7baef3e\",    \"message\" " +
            ": \"SCREEN_OPENED\",    \"muid\" : \"1152921504669614276:UR0hJwfCScsxQYj+q7xOzUggVdF/BS5l\"  }}\tIsR" +
            "evenueVerified=undefined\tKitBuildNumber=13847\tKitBuildType=101\tKitVersion=3005000\tLatitude=\tLimi" +
            "tAdTracking=false\tLocale=en_US\tLocationEnabled=true\tLocationPrecision=\tLocationSource=MISSING\tLoc" +
            "ationTimestamp=\tLongitude=\tManufacturer=Apple\tModel=x86_64\tNetworkType=\tNetworksInterfaces_Mac" +
            "s=\tNetworksInterfaces_Names=\tOSApiLevel=12\tOSVersion=12.1\tOperatingSystem=ios\tOperatorID=\tOri" +
            "ginalManufacturer=Apple\tOriginalModel=x86_64\tProfileAttributeVersion=26137713107271733\tReceiveD" +
            "ate=2019-05-15\tReceiveTimestamp=1557929210\tRegionID=9999\tRegionTimeZone=10800\tRevenueOrderIdSo" +
            "urce=autogenerated\tScaleFactor=3\tScreenDPI=326\tScreenHeight=375\tScreenWidth=812\tSendTimeZon" +
            "e=\tSendTimestamp=1557929209\tSessionID=10000000778\tSessionType=SESSION_FOREGROUND\tSex=1\tSimCar" +
            "ds_AreRoaming=\tSimCards_CountriesCodes=\tSimCards_IccIDs=\tSimCards_OperatorsIDs=\tSimCards_Operat" +
            "orsNames=\tStartDate=2019-05-15\tStartTime=2019-05-15 17:05:05\tStartTimeZone=10800\tStartTimestam" +
            "p=1557929105\tUUID=a9fc48beec6943aa9715876fe7baef3e\tUUIDHash=14516739211785122096\tWifiAccessPoi" +
            "ntSsid=\tWifiAccessPointState=UNKNOWN\t_logfeller_index_bucket=//home/logfeller/index/logfeller-t" +
            "opic-splitter/market-apps-metrika-mobile-log/3600-86400/1557932400/1557867600\t_logfeller_timestam" +
            "p=1557929210\t_stbx=rt3.man--logfeller-topic-splitter--market-apps-metrika-mobile-log:0@@11665935" +
            "13@@pqout_market_apps_metrika_mobile_log:a1404ed0-3769e613-6ca8a2ff-2d05-18031@@1557929589101@@15" +
            "57929592@@market-apps-metrika-mobile-log@@1557929589002212@@1557929589459\tiso_eventtime=2019-05-15" +
            " 17:06:50\tsource_uri=prt://logfeller-topic-splitter@2a02:6b8:c01:837:0:1504:5cf6:94b;unknown_pa" +
            "th\ttimestamp=2019-05-15 14:06:50\ttimezone=+0000\ttskv_format=metrika-mobile-log";

        String[] tags = {};
        String[] keys = {"uid", "muid", "portion"};
        String[] values = {"a9fc48beec6943aa9715876fe7baef3e", "1152921504669614276:UR0hJwfCScsxQYj+q7xOzUggVdF/BS5l", "CART_SCREEN"};

        checker.check(
            line,
            new Date(1557929124000L),
            "market_front_bluetouch", checker.getHost(), "1557929124783/55051A64507C40CA932B2587B99BB" +
                "899", "SCREEN_OPENED", "info", "SCREEN_OPENED", "", "", "", 0, tags, keys, values, "223", UNKNOWN, "iOS"
        );
    }

    @Test
    public void dontParseNonFlatObjectsInInfo() throws Exception {
        String line = "ADVID=91c4b0b2-e03b-4899-9540-b96e04e086f1\tAPIKey=1389598\tAPIKey128=36ef1512-b352-4415-87bd-" +
            "cf45c4f7e46b\tAge=25\tAndroidID=e8f9cd59a322e40b\tAppBuildNumber=1600\tAppDebuggable=false\tAppFramewor" +
            "k=NATIVE\tAppID=ru.beru.android\tAppPlatform=android\tAppVersionName=1.55\tAttributionID=1\tAttributionI" +
            "DUUIDHash=7447668078913031990\tCells_AreConnected=[1]\tCells_CellsIDs=[0]\tCells_CountriesCodes=[0]\tCel" +
            "ls_Lacs=[0]\tCells_OperatorsIDs=[0]\tCells_OperatorsNames=[\\\"\\\"]\tCells_PhysicalsCellsIDs=[0]\tCell" +
            "s_SignalsStrengths=[0]\tCells_Types=[DEFAULT]\tClickDate=1970-01-01\tClientIP=::ffff:2.62.145.45\tClien" +
            "tIPHash=10050381772417513344\tClientKitVersion=3005002\tClientPort=18410\tConnectionType=CONN_WIFI\tCou" +
            "ntryCode=\tDeduplicationEnabled=1\tDeviceID=0de1ef8844b082009e6eb57e10aa7e77\tDeviceIDHash=147051650718" +
            "99854556\tDeviceIDSessionIDHash=10635599058492357844\tDeviceType=TABLET\tEventDate=2019-05-24\tEventDa" +
            "teTime=2019-05-24 18:56:50\tEventFirstOccurrence=undefined\tEventID=16385773888387851755\tEventName=HE" +
            "ALTH_EVENT_STATBOX\tEventNumber=76\tEventSource=sdk\tEventTimeOffset=263\tEventTimeZone=10800\tEventTi" +
            "mestamp=1558713410\tEventType=EVENT_STATBOX\tEventValue={\"level\":\"error\",\"name\":\"ADD_CART_ITEM" +
            "_ERROR\",\"portion\":\"SEARCH_RESULT_SCREEN\",\"type\":\"LOG\",\"duration\":0,\"requestId\":\"1558713" +
            "411440/468c1be92fe146ccb9b59298474434ae\",\"info\":{\"offer_id\":{\"name\": \"yDpJekrrgZH6Ra11ieiXri4vCeFRTe5RN" +
            "TKWMEHyprChGQ8uNUV6iPKCVkIQuWJ1zxpCiC9OE6FvJo9y2MpYOfjb73dDKMvEMMLeaFCGlb88Wgs4HCCLlmKcYlIADLeEoyguDvY" +
            "r7AWCTzoTFuOFAGQxp0J20t0HIU6NlDRAiDUBcHsZhpPWm3C-dBxSWwShyP9ZnCN3a-g87Y3GdWPdZNplnyDD0k9J_jOoXLpnfqkln" +
            "EF7p7OR81JRqxIp92ZD2iFR5n4fq9A_bzpDUbsSFW9v67L-Aqac\"},\"message\":\"Error on adding item to cart\",\"st" +
            "ackTrace\":\"CommunicationException[SERVICE_ERROR(500)]\\n\\tat ru.yandex.market.net.http.RequestExecut" +
            "or.executeRequest(RequestExecutor.java:55)\\n\\tat ru.yandex.market.net.http.RequestExecutor.executeRe" +
            "quest(RequestExecutor.java:38)\\n\\tat ru.yandex.market.net.http.ContentApiClient.addCartItem(ContentA" +
            "piClient.java:469)\\n\\tat ru.yandex.market.db.CartOnlineRepository.addCartItemFromContentApi(CartOnli" +
            "neRepository.kt:157)\\n\\tat ru.yandex.market.db.CartOnlineRepository.access$addCartItemFromContentApi" +
            "(CartOnlineRepository.kt:41)\\n\\tat ru.yandex.market.db.CartOnlineRepository$addItemToCart$uploadCart" +
            "Item$1.apply(CartOnlineRepository.kt:134)\\n\\tat ru.yandex.market.db.CartOnlineRepository$addItemToCa" +
            "rt$uploadCartItem$1.apply(CartOnlineRepository.kt:41)\\n\\tat io.reactivex.internal.operators.single.S" +
            "ingleMap$MapSingleObserver.onSuccess(SingleMap.java:57)\\n\\tat io.reactivex.internal.operators.single." +
            "SingleFromCallable.subscribeActual(SingleFromCallable.java:56)\\n\\tat io.reactivex.Single.subscribe(S" +
            "ingle.java:3603)\\n\\tat io.reactivex.internal.operators.single.SingleMap.subscribeActual(SingleMap.ja" +
            "va:34)\\n\\tat io.reactivex.Single.subscribe(Single.java:3603)\\n\\tat io.reactivex.internal.operators" +
            ".single.SingleSubscribeOn$SubscribeOnObserver.run(SingleSubscribeOn.java:89)\\n\\tat io.reactivex.Sche" +
            "duler$DisposeTask.run(Scheduler.java:578)\\n\\tat ru.yandex.market.rx.schedulers.RxSchedulersFactory$A" +
            "ctionWrapper.run(RxSchedulersFactory.java:106)\\n\\tat io.reactivex.internal.schedulers.ScheduledRunna" +
            "ble.run(ScheduledRunnable.java:66)\\n\\tat io.reactivex.internal.schedulers.ScheduledRunnable.call(S" +
            "cheduledRunnable.java:57)\\n\\tat java.util.concurrent.FutureTask.run(FutureTask.java:237)\\n\\tat ja" +
            "va.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.ja" +
            "va:272)\\n\\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1133)\\n\\t" +
            "at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:607)\\n\\tat java.lang." +
            "Thread.run(Thread.java:761)\\n\",\"uid\":\"8357013411558604071\",\"muid\":\"1152921504671574927:UR0hJ" +
            "wfCScvLkQALzuDS4vjJJS7ew4Wb\"},\"startTime\":1558713411,\"timestamp\":1558713411}\tEventValueJsonRefe" +
            "rence=\tIsRevenueVerified=undefined\tKitBuildNumber=16697\tKitBuildType=202\tKitVersion=3005002\tLati" +
            "tude=\tLimitAdTracking=false\tLocale=ru_RU\tLocationEnabled=true\tLocationPrecision=\tLocationSource=" +
            "MISSING\tLongitude=\tManufacturer=DEXP\tModel=DEXP Ursus S169\tNetworkType=\tNetworksInterfaces_Macs=" +
            "[\\\"FA6856402D61\\\",\\\"EE1CB04B822C\\\",\\\"5E8D16445FA9\\\",\\\"4045DA19AEB8\\\",\\\"4045DA19AEB" +
            "8\\\"]\tNetworksInterfaces_Names=[\\\"seth_w0\\\",\\\"seth_w1\\\",\\\"seth_w2\\\",\\\"wlan0\\\",\\\"p" +
            "2p0\\\"]\tOSApiLevel=24\tOSVersion=7.0\tOperatingSystem=android\tOperatorID=\tOperatorName=\tOrigina" +
            "lManufacturer=DEXP\tOriginalModel=S169\tParsedParams_Key1=\tParsedParams_Key10=\tParsedParams_Key" +
            "2=\tParsedParams_Key3=\tParsedParams_Key4=\tParsedParams_Key5=\tParsedParams_Key6=\tParsedParams_Ke" +
            "y7=\tParsedParams_Key8=\tParsedParams_Key9=\tParsedParams_Quantity=\tParsedParams_ValueDouble=\tPro" +
            "fileAttributeVersion=26150867149258828\tReceiveDate=2019-05-24\tReceiveTimestamp=1558713558\tRegion" +
            "ID=11293\tRegionTimeZone=25200\tRevenueOrderIdSource=autogenerated\tScaleFactor=1\tScreenDPI=160\tS" +
            "creenHeight=600\tScreenWidth=1024\tSendTimestamp=1558713558\tSessionID=10000000005\tSessionType=SES" +
            "SION_FOREGROUND\tSex=1\tSimCards_AreRoaming=[0]\tSimCards_CountriesCodes=[0]\tSimCards_IccI" +
            "Ds=[\\\"\\\"]\tSimCards_OperatorsIDs=[0]\tSimCards_OperatorsNames=[\\\"\\\"]\tStartDate=2019-05-24\tS" +
            "tartTime=2019-05-24 18:52:27\tStartTimeZone=10800\tStartTimestamp=1558713147\tUUID=c3e60b9c826f411dbf5" +
            "282f76c7c583f\tUUIDHash=6422661156785156246\tWifiAccessPointState=DISABLED\tWifi_AreConnected=[1]\tWif" +
            "i_LastVisibleTimeOffset=[0]\tWifi_Macs=[\\\"18A6F77397DF\\\"]\tWifi_SignalsStrengths=[-63]\tWifi_Ssids" +
            "=[\\\"TP-LINK_7397DF\\\"]\t_logfeller_index_bucket=//home/logfeller/index/logfeller-topic-splitter/mar" +
            "ket-apps-metrika-mobile-log/3600-86400/1558714200/1558645200\t_logfeller_timestamp=1558713558\t_stbx=r" +
            "t3.man--logfeller-topic-splitter--market-apps-metrika-mobile-log:0@@1225180579@@pqout_market_apps_metr" +
            "ika_mobile_log:d25c3d02-be080231-dd82d590-1794-18040@@1558713648849@@1558713649@@market-apps-metrika-m" +
            "obile-log@@1558713648683617@@1558713648971\tiso_eventtime=2019-05-24 18:59:18\tsource_uri=prt://logfel" +
            "ler-topic-splitter@2a02:6b8:c01:723:0:1504:b2a9:923a;unknown_path\ttimestamp=2019-05-24 15:59:18\ttime" +
            "zone=+0000\ttskv_format=metrika-mobile-log\n";

        String[] tags = {};
        String[] keys = {"offer_id", "uid", "muid", "portion"};
        String[] values = {"", "8357013411558604071", "1152921504671574927:UR0hJwfCScvLkQALzuDS4vjJJS7ew4Wb", "SEARCH_RESULT_SCREEN"};

        checker.check(
            line,
            new Date(1558713410000L),
            "market_front_bluetouch", checker.getHost(), "1558713411440/468c1be92fe146ccb9b59" +
                "298474434ae", "ADD_CART_ITEM_ERROR", "error", "Error on adding item to cart", "CommunicationException[SERVICE_ERROR(500)]\n" +
                "\tat ru.yandex.market.net.http.RequestExecutor.executeRequest(RequestExecutor.java:55)\n" +
                "\tat ru.yandex.market.net.http.RequestExecutor.executeRequest(RequestExecutor.java:38)\n" +
                "\tat ru.yandex.market.net.http.ContentApiClient.addCartItem(ContentApiClient.java:469)\n" +
                "\tat ru.yandex.market.db.CartOnlineRepository.addCartItemFromContentApi(CartOnlineRepository.kt:157)\n" +
                "\tat ru.yandex.market.db.CartOnlineRepository.access$addCartItemFromContentApi(CartOnlineRepository.kt:41)\n" +
                "\tat ru.yandex.market.db.CartOnlineRepository$addItemToCart$uploadCartItem$1.apply(CartOnlineRepository.kt:134)\n" +
                "\tat ru.yandex.market.db.CartOnlineRepository$addItemToCart$uploadCartItem$1.apply(CartOnlineRepository.kt:41)\n" +
                "\tat io.reactivex.internal.operators.single.SingleMap$MapSingleObserver.onSuccess(SingleMap.java:57)\n" +
                "\tat io.reactivex.internal.operators.single.SingleFromCallable.subscribeActual(SingleFromCallable.java:56)\n" +
                "\tat io.reactivex.Single.subscribe(Single.java:3603)\n" +
                "\tat io.reactivex.internal.operators.single.SingleMap.subscribeActual(SingleMap.java:34)\n" +
                "\tat io.reactivex.Single.subscribe(Single.java:3603)\n" +
                "\tat io.reactivex.internal.operators.single.SingleSubscribeOn$SubscribeOnObserver.run(SingleSubscribeOn.java:89)\n" +
                "\tat io.reactivex.Scheduler$DisposeTask.run(Scheduler.java:578)\n" +
                "\tat ru.yandex.market.rx.schedulers.RxSchedulersFactory$ActionWrapper.run(RxSchedulersFactory.java:106)\n" +
                "\tat io.reactivex.internal.schedulers.ScheduledRunnable.run(ScheduledRunnable.java:66)\n" +
                "\tat io.reactivex.internal.schedulers.ScheduledRunnable.call(ScheduledRunnable.java:57)\n" +
                "\tat java.util.concurrent.FutureTask.run(FutureTask.java:237)\n" +
                "\tat java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:272)\n" +
                "\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1133)\n" +
                "\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:607)\n" +
                "\tat java.lang.Thread.run(Thread.java:761)\n",
            "14924397360597504922", "RequestExecutor.java", 55, tags, keys, values, "1.55", UNKNOWN, "android"
        );
    }

    @Test
    public void dontAcceptOtherEventValueType () throws Exception {
        String line = "ADVID=91c4b0b2-e03b-4899-9540-b96e04e086f1\tAPIKey=1389598\tAPIKey128=36ef1512-b352-4415-87bd-" +
            "cf45c4f7e46b\tAge=25\tAndroidID=e8f9cd59a322e40b\tAppBuildNumber=1600\tAppDebuggable=false\tAppFramewor" +
            "k=NATIVE\tAppID=ru.beru.android\tAppPlatform=android\tAppVersionName=1.55\tAttributionID=1\tAttributionI" +
            "DUUIDHash=7447668078913031990\tCells_AreConnected=[1]\tCells_CellsIDs=[0]\tCells_CountriesCodes=[0]\tCel" +
            "ls_Lacs=[0]\tCells_OperatorsIDs=[0]\tCells_OperatorsNames=[\\\"\\\"]\tCells_PhysicalsCellsIDs=[0]\tCell" +
            "s_SignalsStrengths=[0]\tCells_Types=[DEFAULT]\tClickDate=1970-01-01\tClientIP=::ffff:2.62.145.45\tClien" +
            "tIPHash=10050381772417513344\tClientKitVersion=3005002\tClientPort=18410\tConnectionType=CONN_WIFI\tCou" +
            "ntryCode=\tDeduplicationEnabled=1\tDeviceID=0de1ef8844b082009e6eb57e10aa7e77\tDeviceIDHash=147051650718" +
            "99854556\tDeviceIDSessionIDHash=10635599058492357844\tDeviceType=TABLET\tEventDate=2019-05-24\tEventDa" +
            "teTime=2019-05-24 18:56:50\tEventFirstOccurrence=undefined\tEventID=16385773888387851755\tEventName=HE" +
            "ALTH_EVENT_STATBOX\tEventNumber=76\tEventSource=sdk\tEventTimeOffset=263\tEventTimeZone=10800\tEventTi" +
            "mestamp=1558713410\tEventType=EVENT_STATBOX\tEventValue={\"level\":\"error\",\"name\":\"ADD_CART_ITEM" +
            "_ERROR\",\"portion\":\"SEARCH_RESULT_SCREEN\",\"type\":\"METRIC\",\"duration\":0,\"requestId\":\"1558713" +
            "411440/468c1be92fe146ccb9b59298474434ae\",\"info\":{\"offer_id\":{\"name\": \"yDpJekrrgZH6Ra11ieiXri4vCeFRTe5RN" +
            "TKWMEHyprChGQ8uNUV6iPKCVkIQuWJ1zxpCiC9OE6FvJo9y2MpYOfjb73dDKMvEMMLeaFCGlb88Wgs4HCCLlmKcYlIADLeEoyguDvY" +
            "r7AWCTzoTFuOFAGQxp0J20t0HIU6NlDRAiDUBcHsZhpPWm3C-dBxSWwShyP9ZnCN3a-g87Y3GdWPdZNplnyDD0k9J_jOoXLpnfqkln" +
            "EF7p7OR81JRqxIp92ZD2iFR5n4fq9A_bzpDUbsSFW9v67L-Aqac\"},\"message\":\"Error on adding item to cart\",\"st" +
            "ackTrace\":\"CommunicationException[SERVICE_ERROR(500)]\\n\\tat ru.yandex.market.net.http.RequestExecut" +
            "or.executeRequest(RequestExecutor.java:55)\\n\\tat ru.yandex.market.net.http.RequestExecutor.executeRe" +
            "quest(RequestExecutor.java:38)\\n\\tat ru.yandex.market.net.http.ContentApiClient.addCartItem(ContentA" +
            "piClient.java:469)\\n\\tat ru.yandex.market.db.CartOnlineRepository.addCartItemFromContentApi(CartOnli" +
            "neRepository.kt:157)\\n\\tat ru.yandex.market.db.CartOnlineRepository.access$addCartItemFromContentApi" +
            "(CartOnlineRepository.kt:41)\\n\\tat ru.yandex.market.db.CartOnlineRepository$addItemToCart$uploadCart" +
            "Item$1.apply(CartOnlineRepository.kt:134)\\n\\tat ru.yandex.market.db.CartOnlineRepository$addItemToCa" +
            "rt$uploadCartItem$1.apply(CartOnlineRepository.kt:41)\\n\\tat io.reactivex.internal.operators.single.S" +
            "ingleMap$MapSingleObserver.onSuccess(SingleMap.java:57)\\n\\tat io.reactivex.internal.operators.single." +
            "SingleFromCallable.subscribeActual(SingleFromCallable.java:56)\\n\\tat io.reactivex.Single.subscribe(S" +
            "ingle.java:3603)\\n\\tat io.reactivex.internal.operators.single.SingleMap.subscribeActual(SingleMap.ja" +
            "va:34)\\n\\tat io.reactivex.Single.subscribe(Single.java:3603)\\n\\tat io.reactivex.internal.operators" +
            ".single.SingleSubscribeOn$SubscribeOnObserver.run(SingleSubscribeOn.java:89)\\n\\tat io.reactivex.Sche" +
            "duler$DisposeTask.run(Scheduler.java:578)\\n\\tat ru.yandex.market.rx.schedulers.RxSchedulersFactory$A" +
            "ctionWrapper.run(RxSchedulersFactory.java:106)\\n\\tat io.reactivex.internal.schedulers.ScheduledRunna" +
            "ble.run(ScheduledRunnable.java:66)\\n\\tat io.reactivex.internal.schedulers.ScheduledRunnable.call(S" +
            "cheduledRunnable.java:57)\\n\\tat java.util.concurrent.FutureTask.run(FutureTask.java:237)\\n\\tat ja" +
            "va.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.ja" +
            "va:272)\\n\\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1133)\\n\\t" +
            "at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:607)\\n\\tat java.lang." +
            "Thread.run(Thread.java:761)\\n\",\"uid\":\"8357013411558604071\",\"muid\":\"1152921504671574927:UR0hJ" +
            "wfCScvLkQALzuDS4vjJJS7ew4Wb\"},\"startTime\":1558713411,\"timestamp\":1558713411}\tEventValueJsonRefe" +
            "rence=\tIsRevenueVerified=undefined\tKitBuildNumber=16697\tKitBuildType=202\tKitVersion=3005002\tLati" +
            "tude=\tLimitAdTracking=false\tLocale=ru_RU\tLocationEnabled=true\tLocationPrecision=\tLocationSource=" +
            "MISSING\tLongitude=\tManufacturer=DEXP\tModel=DEXP Ursus S169\tNetworkType=\tNetworksInterfaces_Macs=" +
            "[\\\"FA6856402D61\\\",\\\"EE1CB04B822C\\\",\\\"5E8D16445FA9\\\",\\\"4045DA19AEB8\\\",\\\"4045DA19AEB" +
            "8\\\"]\tNetworksInterfaces_Names=[\\\"seth_w0\\\",\\\"seth_w1\\\",\\\"seth_w2\\\",\\\"wlan0\\\",\\\"p" +
            "2p0\\\"]\tOSApiLevel=24\tOSVersion=7.0\tOperatingSystem=android\tOperatorID=\tOperatorName=\tOrigina" +
            "lManufacturer=DEXP\tOriginalModel=S169\tParsedParams_Key1=\tParsedParams_Key10=\tParsedParams_Key" +
            "2=\tParsedParams_Key3=\tParsedParams_Key4=\tParsedParams_Key5=\tParsedParams_Key6=\tParsedParams_Ke" +
            "y7=\tParsedParams_Key8=\tParsedParams_Key9=\tParsedParams_Quantity=\tParsedParams_ValueDouble=\tPro" +
            "fileAttributeVersion=26150867149258828\tReceiveDate=2019-05-24\tReceiveTimestamp=1558713558\tRegion" +
            "ID=11293\tRegionTimeZone=25200\tRevenueOrderIdSource=autogenerated\tScaleFactor=1\tScreenDPI=160\tS" +
            "creenHeight=600\tScreenWidth=1024\tSendTimestamp=1558713558\tSessionID=10000000005\tSessionType=SES" +
            "SION_FOREGROUND\tSex=1\tSimCards_AreRoaming=[0]\tSimCards_CountriesCodes=[0]\tSimCards_IccI" +
            "Ds=[\\\"\\\"]\tSimCards_OperatorsIDs=[0]\tSimCards_OperatorsNames=[\\\"\\\"]\tStartDate=2019-05-24\tS" +
            "tartTime=2019-05-24 18:52:27\tStartTimeZone=10800\tStartTimestamp=1558713147\tUUID=c3e60b9c826f411dbf5" +
            "282f76c7c583f\tUUIDHash=6422661156785156246\tWifiAccessPointState=DISABLED\tWifi_AreConnected=[1]\tWif" +
            "i_LastVisibleTimeOffset=[0]\tWifi_Macs=[\\\"18A6F77397DF\\\"]\tWifi_SignalsStrengths=[-63]\tWifi_Ssids" +
            "=[\\\"TP-LINK_7397DF\\\"]\t_logfeller_index_bucket=//home/logfeller/index/logfeller-topic-splitter/mar" +
            "ket-apps-metrika-mobile-log/3600-86400/1558714200/1558645200\t_logfeller_timestamp=1558713558\t_stbx=r" +
            "t3.man--logfeller-topic-splitter--market-apps-metrika-mobile-log:0@@1225180579@@pqout_market_apps_metr" +
            "ika_mobile_log:d25c3d02-be080231-dd82d590-1794-18040@@1558713648849@@1558713649@@market-apps-metrika-m" +
            "obile-log@@1558713648683617@@1558713648971\tiso_eventtime=2019-05-24 18:59:18\tsource_uri=prt://logfel" +
            "ler-topic-splitter@2a02:6b8:c01:723:0:1504:b2a9:923a;unknown_path\ttimestamp=2019-05-24 15:59:18\ttime" +
            "zone=+0000\ttskv_format=metrika-mobile-log\n";

        checker.checkEmpty(line);
    }

    @Test
    public void dontAcceptEventValueWithoutType () throws Exception {
        String line = "ADVID=91c4b0b2-e03b-4899-9540-b96e04e086f1\tAPIKey=1389598\tAPIKey128=36ef1512-b352-4415-87bd-" +
            "cf45c4f7e46b\tAge=25\tAndroidID=e8f9cd59a322e40b\tAppBuildNumber=1600\tAppDebuggable=false\tAppFramewor" +
            "k=NATIVE\tAppID=ru.beru.android\tAppPlatform=android\tAppVersionName=1.55\tAttributionID=1\tAttributionI" +
            "DUUIDHash=7447668078913031990\tCells_AreConnected=[1]\tCells_CellsIDs=[0]\tCells_CountriesCodes=[0]\tCel" +
            "ls_Lacs=[0]\tCells_OperatorsIDs=[0]\tCells_OperatorsNames=[\\\"\\\"]\tCells_PhysicalsCellsIDs=[0]\tCell" +
            "s_SignalsStrengths=[0]\tCells_Types=[DEFAULT]\tClickDate=1970-01-01\tClientIP=::ffff:2.62.145.45\tClien" +
            "tIPHash=10050381772417513344\tClientKitVersion=3005002\tClientPort=18410\tConnectionType=CONN_WIFI\tCou" +
            "ntryCode=\tDeduplicationEnabled=1\tDeviceID=0de1ef8844b082009e6eb57e10aa7e77\tDeviceIDHash=147051650718" +
            "99854556\tDeviceIDSessionIDHash=10635599058492357844\tDeviceType=TABLET\tEventDate=2019-05-24\tEventDa" +
            "teTime=2019-05-24 18:56:50\tEventFirstOccurrence=undefined\tEventID=16385773888387851755\tEventName=HE" +
            "ALTH_EVENT_STATBOX\tEventNumber=76\tEventSource=sdk\tEventTimeOffset=263\tEventTimeZone=10800\tEventTi" +
            "mestamp=1558713410\tEventType=EVENT_STATBOX\tEventValue={\"level\":\"error\",\"name\":\"ADD_CART_ITEM" +
            "_ERROR\",\"portion\":\"SEARCH_RESULT_SCREEN\",\"duration\":0,\"requestId\":\"1558713" +
            "411440/468c1be92fe146ccb9b59298474434ae\",\"info\":{\"offer_id\":{\"name\": \"yDpJekrrgZH6Ra11ieiXri4vCeFRTe5RN" +
            "TKWMEHyprChGQ8uNUV6iPKCVkIQuWJ1zxpCiC9OE6FvJo9y2MpYOfjb73dDKMvEMMLeaFCGlb88Wgs4HCCLlmKcYlIADLeEoyguDvY" +
            "r7AWCTzoTFuOFAGQxp0J20t0HIU6NlDRAiDUBcHsZhpPWm3C-dBxSWwShyP9ZnCN3a-g87Y3GdWPdZNplnyDD0k9J_jOoXLpnfqkln" +
            "EF7p7OR81JRqxIp92ZD2iFR5n4fq9A_bzpDUbsSFW9v67L-Aqac\"},\"message\":\"Error on adding item to cart\",\"st" +
            "ackTrace\":\"CommunicationException[SERVICE_ERROR(500)]\\n\\tat ru.yandex.market.net.http.RequestExecut" +
            "or.executeRequest(RequestExecutor.java:55)\\n\\tat ru.yandex.market.net.http.RequestExecutor.executeRe" +
            "quest(RequestExecutor.java:38)\\n\\tat ru.yandex.market.net.http.ContentApiClient.addCartItem(ContentA" +
            "piClient.java:469)\\n\\tat ru.yandex.market.db.CartOnlineRepository.addCartItemFromContentApi(CartOnli" +
            "neRepository.kt:157)\\n\\tat ru.yandex.market.db.CartOnlineRepository.access$addCartItemFromContentApi" +
            "(CartOnlineRepository.kt:41)\\n\\tat ru.yandex.market.db.CartOnlineRepository$addItemToCart$uploadCart" +
            "Item$1.apply(CartOnlineRepository.kt:134)\\n\\tat ru.yandex.market.db.CartOnlineRepository$addItemToCa" +
            "rt$uploadCartItem$1.apply(CartOnlineRepository.kt:41)\\n\\tat io.reactivex.internal.operators.single.S" +
            "ingleMap$MapSingleObserver.onSuccess(SingleMap.java:57)\\n\\tat io.reactivex.internal.operators.single." +
            "SingleFromCallable.subscribeActual(SingleFromCallable.java:56)\\n\\tat io.reactivex.Single.subscribe(S" +
            "ingle.java:3603)\\n\\tat io.reactivex.internal.operators.single.SingleMap.subscribeActual(SingleMap.ja" +
            "va:34)\\n\\tat io.reactivex.Single.subscribe(Single.java:3603)\\n\\tat io.reactivex.internal.operators" +
            ".single.SingleSubscribeOn$SubscribeOnObserver.run(SingleSubscribeOn.java:89)\\n\\tat io.reactivex.Sche" +
            "duler$DisposeTask.run(Scheduler.java:578)\\n\\tat ru.yandex.market.rx.schedulers.RxSchedulersFactory$A" +
            "ctionWrapper.run(RxSchedulersFactory.java:106)\\n\\tat io.reactivex.internal.schedulers.ScheduledRunna" +
            "ble.run(ScheduledRunnable.java:66)\\n\\tat io.reactivex.internal.schedulers.ScheduledRunnable.call(S" +
            "cheduledRunnable.java:57)\\n\\tat java.util.concurrent.FutureTask.run(FutureTask.java:237)\\n\\tat ja" +
            "va.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.ja" +
            "va:272)\\n\\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1133)\\n\\t" +
            "at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:607)\\n\\tat java.lang." +
            "Thread.run(Thread.java:761)\\n\",\"uid\":\"8357013411558604071\",\"muid\":\"1152921504671574927:UR0hJ" +
            "wfCScvLkQALzuDS4vjJJS7ew4Wb\"},\"startTime\":1558713411,\"timestamp\":1558713411}\tEventValueJsonRefe" +
            "rence=\tIsRevenueVerified=undefined\tKitBuildNumber=16697\tKitBuildType=202\tKitVersion=3005002\tLati" +
            "tude=\tLimitAdTracking=false\tLocale=ru_RU\tLocationEnabled=true\tLocationPrecision=\tLocationSource=" +
            "MISSING\tLongitude=\tManufacturer=DEXP\tModel=DEXP Ursus S169\tNetworkType=\tNetworksInterfaces_Macs=" +
            "[\\\"FA6856402D61\\\",\\\"EE1CB04B822C\\\",\\\"5E8D16445FA9\\\",\\\"4045DA19AEB8\\\",\\\"4045DA19AEB" +
            "8\\\"]\tNetworksInterfaces_Names=[\\\"seth_w0\\\",\\\"seth_w1\\\",\\\"seth_w2\\\",\\\"wlan0\\\",\\\"p" +
            "2p0\\\"]\tOSApiLevel=24\tOSVersion=7.0\tOperatingSystem=android\tOperatorID=\tOperatorName=\tOrigina" +
            "lManufacturer=DEXP\tOriginalModel=S169\tParsedParams_Key1=\tParsedParams_Key10=\tParsedParams_Key" +
            "2=\tParsedParams_Key3=\tParsedParams_Key4=\tParsedParams_Key5=\tParsedParams_Key6=\tParsedParams_Ke" +
            "y7=\tParsedParams_Key8=\tParsedParams_Key9=\tParsedParams_Quantity=\tParsedParams_ValueDouble=\tPro" +
            "fileAttributeVersion=26150867149258828\tReceiveDate=2019-05-24\tReceiveTimestamp=1558713558\tRegion" +
            "ID=11293\tRegionTimeZone=25200\tRevenueOrderIdSource=autogenerated\tScaleFactor=1\tScreenDPI=160\tS" +
            "creenHeight=600\tScreenWidth=1024\tSendTimestamp=1558713558\tSessionID=10000000005\tSessionType=SES" +
            "SION_FOREGROUND\tSex=1\tSimCards_AreRoaming=[0]\tSimCards_CountriesCodes=[0]\tSimCards_IccI" +
            "Ds=[\\\"\\\"]\tSimCards_OperatorsIDs=[0]\tSimCards_OperatorsNames=[\\\"\\\"]\tStartDate=2019-05-24\tS" +
            "tartTime=2019-05-24 18:52:27\tStartTimeZone=10800\tStartTimestamp=1558713147\tUUID=c3e60b9c826f411dbf5" +
            "282f76c7c583f\tUUIDHash=6422661156785156246\tWifiAccessPointState=DISABLED\tWifi_AreConnected=[1]\tWif" +
            "i_LastVisibleTimeOffset=[0]\tWifi_Macs=[\\\"18A6F77397DF\\\"]\tWifi_SignalsStrengths=[-63]\tWifi_Ssids" +
            "=[\\\"TP-LINK_7397DF\\\"]\t_logfeller_index_bucket=//home/logfeller/index/logfeller-topic-splitter/mar" +
            "ket-apps-metrika-mobile-log/3600-86400/1558714200/1558645200\t_logfeller_timestamp=1558713558\t_stbx=r" +
            "t3.man--logfeller-topic-splitter--market-apps-metrika-mobile-log:0@@1225180579@@pqout_market_apps_metr" +
            "ika_mobile_log:d25c3d02-be080231-dd82d590-1794-18040@@1558713648849@@1558713649@@market-apps-metrika-m" +
            "obile-log@@1558713648683617@@1558713648971\tiso_eventtime=2019-05-24 18:59:18\tsource_uri=prt://logfel" +
            "ler-topic-splitter@2a02:6b8:c01:723:0:1504:b2a9:923a;unknown_path\ttimestamp=2019-05-24 15:59:18\ttime" +
            "zone=+0000\ttskv_format=metrika-mobile-log\n";

        checker.checkEmpty(line);
    }
}
