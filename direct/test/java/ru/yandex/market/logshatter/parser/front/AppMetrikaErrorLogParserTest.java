package ru.yandex.market.logshatter.parser.front;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

import static ru.yandex.market.logshatter.parser.trace.Environment.UNKNOWN;


public class AppMetrikaErrorLogParserTest {
    private LogParserChecker checker;

    @Before
    public void setUp() {
        checker = new LogParserChecker(new AppMetrikaErrorLogParser());
        checker.setOrigin("market-health-dev");
        checker.setParam("allowedAppEvents", "EVENT_CRASH");
        checker.setParam(
            "apiKeysToServiceNames",
            "23107:market_mobile,23104:market_front_touch,1389598:market_front_bluetouch"
        );
    }

    @Test
    public void parseWithoutErrorMessage() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2018-12-07 14:45:48\ttimezone=+0000\tAPIK" +
                    "ey=23107\tAttributionIDUUIDHash=15433599176222037151\tStartDate=2018-12-07\tUUID=fbfa908f5" +
                    "d3d9546ecb27db703b97821\tDeviceID=eaa1a4e6138a06351784852e4446d943\tUUIDHash=1443916746230" +
                    "3939563\tDeviceIDHash=11070894046081033018\tAppPlatform=android\tOperatingSystem=android\t" +
                    "AppFramework=NATIVE\tAppVersionName=2.20\tAppBuildNumber=703\tAppDebuggable=false\tKitVers" +
                    "ion=271\tManufacturer=Irbis\tModel=TX24\tOriginalManufacturer=IRBIS\tOriginalModel=TX24\tO" +
                    "SVersion=4.4.2\tOSApiLevel=19\tScreenWidth=1024\tScreenHeight=600\tScreenDPI=160\tScaleFac" +
                    "tor=1\tAndroidID=2bf9bc4a217456c4\tADVID=621734e7-95ef-4629-bc25-f08cbce2bae0\tLimitAdTrac" +
                    "king=false\tClientKitVersion=120\tKitBuildType=202\tKitBuildNumber=7828\tSendTimestamp=154" +
                    "4190308\tSendTimeZone=14400\tReceiveDate=2018-12-07\tReceiveTimestamp=1544193948\tSessionI" +
                    "D=1544190308\tSessionType=SESSION_BACKGROUND\tDeviceIDSessionIDHash=9997575031146670950\tS" +
                    "tartTime=2018-12-07 17:45:48\tStartTimestamp=1544193948\tStartTimeZone=14400\tRegionTimeZo" +
                    "ne=10800\tLocale=ru_RU\tLocationSource=LBS\tLatitude=56.122520446777344\tLongitude=47.2738" +
                    "41857910156\tLocationPrecision=1600\tLocationEnabled=undefined\tWifiAccessPointSsid=IRBIS " +
                    "TX24\tWifiAccessPointState=DISABLED\tConnectionType=CONN_CELL\tNetworkType=HSPA+\tCountryC" +
                    "ode=250\tOperatorID=20\tOperatorName=Tele2\tCells_CellsIDs=[6456852]\tCells_SignalsStrengt" +
                    "hs=[-91]\tCells_Lacs=[20317]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[20]\tCells_O" +
                    "peratorsNames=[\"Tele2\"]\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\tCells_PhysicalsC" +
                    "ellsIDs=[0]\tSimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[20]\tSimCards_Operators" +
                    "Names=[\"Tele2\"]\tSimCards_AreRoaming=[0]\tSimCards_IccIDs=[\"\"]\tNetworksInterfaces_Nam" +
                    "es=[\"ifb0\",\"ifb1\",\"ccmni0\",\"ccmni1\",\"ccmni2\"]\tNetworksInterfaces_Macs=[\"527F99" +
                    "3276E1\",\"4AB405FFD8B6\",\"6EC3A2168BBD\",\"8EC0BF09082A\",\"7AAB3D474C55\"]\tDeviceType=" +
                    "TABLET\tEventID=12951947817559953868\tEventDate=2018-12-07\tEventDateTime=2018-12-07 17:45" +
                    ":48\tEventTimestamp=1544193948\tEventTimeZone=14400\tEventType=EVENT_CRASH\tEventSource=sd" +
                    "k\tEventFirstOccurrence=undefined\tEventValueCrash=java.lang.NullPointerException\\n\\tat " +
                    "com.yandex.auth.sync.AccountProviderHelper.yandexAccountsFromCursor(AccountProviderHelper." +
                    "java:74)\\n\\tat com.yandex.auth.sync.AccountProviderHelper.getAccounts(AccountProviderHel" +
                    "per.java:39)\\n\\tat com.yandex.auth.sync.BackupLogic.backup(BackupLogic.java:44)\\n\\tat " +
                    "com.yandex.auth.sync.BackupLogic.perform(BackupLogic.java:36)\\n\\tat com.yandex.auth.sync" +
                    ".BackupAccountsService.runBackupAction(BackupAccountsService.java:167)\\n\\tat com.yandex." +
                    "auth.sync.BackupAccountsService.onHandleIntent(BackupAccountsService.java:54)\\n\\tat andr" +
                    "oid.app.IntentService$ServiceHandler.handleMessage(IntentService.java:65)\\n\\tat android." +
                    "os.Handler.dispatchMessage(Handler.java:110)\\n\\tat android.os.Looper.loop(Looper.java:19" +
                    "3)\\n\\tat android.os.HandlerThread.run(HandlerThread.java:61)\\n\tCrashID=561948237755694" +
                    "5081\tCrashGroupID=5871795208066209891\tClickDate=1970-01-01\tRegionID=45\tAppID=ru.yandex" +
                    ".market\tClientIP=::ffff:176.59.97.169\tClientIPHash=3925492953153793702\tClientPort=3242" +
                    "2\tDeduplicationEnabled=1\tProfileAttributeVersion=25907275411488768\tCustomAttribute_Type" +
                    "s=[3]\tCustomAttribute_Ids=[48]\tCustomAttribute_IdSetFlags=[1]\tCustomAttribute_StringVal" +
                    "ues=[\"\"]\tCustomAttribute_NumberValues=[1]\tCustomAttribute_BoolValues=[0]\tCustomAttrib" +
                    "ute_ResetFlags=[0]\tCustomAttribute_Versions=[25907275411488768]";

        String[] tags = {};
        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1544193948000L),
            "market_mobile", checker.getHost(), "", "java.lang.NullPointerException",
            "critical", "", "java.lang.NullPointerException\\n\\tat com.yandex.auth.sync.AccountProviderHelper.yandexA" +
            "ccountsFromCursor(AccountProviderHelper.java:74)\\n\\tat com.yandex.auth.sync.AccountProvi" +
            "derHelper.getAccounts(AccountProviderHelper.java:39)\\n\\tat com.yandex.auth.sync.BackupLo" +
            "gic.backup(BackupLogic.java:44)\\n\\tat com.yandex.auth.sync.BackupLogic.perform(BackupLog" +
            "ic.java:36)\\n\\tat com.yandex.auth.sync.BackupAccountsService.runBackupAction(BackupAccou" +
            "ntsService.java:167)\\n\\tat com.yandex.auth.sync.BackupAccountsService.onHandleIntent(Bac" +
            "kupAccountsService.java:54)\\n\\tat android.app.IntentService$ServiceHandler.handleMessage" +
            "(IntentService.java:65)\\n\\tat android.os.Handler.dispatchMessage(Handler.java:110)\\n\\t" +
            "at android.os.Looper.loop(Looper.java:193)\\n\\tat android.os.HandlerThread.run(HandlerThr" +
            "ead.java:61)\\n",
            "2958285933004561399", "AccountProviderHelper.java", 74, tags, keys, values, "2.20", UNKNOWN, "android"
        );
    }

    @Test
    public void parseWithErrorMessage() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2018-12-11 13:33:14\ttimezone=+0000\tAPIKe" +
                    "y=23107\tAPIKey128=36ef1512-b352-4415-87bd-cf45c4f7e46b\tAttributionID=1\tAttributionIDUUI" +
                    "DHash=14959878514340525980\tStartDate=2018-12-11\tUUID=1cb40f2d731d9ba2bc6bc43d0faf3333\tD" +
                    "eviceID=ee6f5c3b1a7694d70f99b73dc940d5f5\tUUIDHash=6070073441431965067\tDeviceIDHash=16316" +
                    "734494101148939\tAppPlatform=android\tOperatingSystem=android\tAppFramework=NATIVE\tAppVer" +
                    "sionName=1.27\tAppBuildNumber=1497\tAppDebuggable=false\tKitVersion=3002002\tManufacturer=" +
                    "Asus\tModel=ZC554KL\tOriginalManufacturer=asus\tOriginalModel=ZC554KL\tOSVersion=8.1.0\tOS" +
                    "ApiLevel=27\tScreenWidth=1280\tScreenHeight=720\tScreenDPI=320\tScaleFactor=2\tAndroidID=0" +
                    "1e1776f71c9208d\tADVID=68ef6067-6ef7-4e6a-8535-79e7791c16b8\tLimitAdTracking=false\tClient" +
                    "KitVersion=3002002\tKitBuildType=202\tKitBuildNumber=11391\tSendTimestamp=1544535183\tRece" +
                    "iveDate=2018-12-11\tReceiveTimestamp=1544535194\tSessionID=10000000002\tSessionType=SESSIO" +
                    "N_BACKGROUND\tDeviceIDSessionIDHash=10005528074688712569\tStartTime=2018-12-11 16:33:03\tS" +
                    "tartTimestamp=1544535183\tStartTimeZone=10800\tRegionTimeZone=18000\tLocale=ru_RU\tLocatio" +
                    "nSource=MISSING\tLocationEnabled=true\tWifiAccessPointState=UNKNOWNConnectionType=CONN_CEL" +
                    "L\tNetworkType=HSPA+\tCountryCode=250\tOperatorID=2\tOperatorName=MegaFon\tCells_CellsIDs=" +
                    "[0]\tCells_SignalsStrengths=[-95]\tCells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_Opera" +
                    "torsIDs=[2]\tCells_OperatorsNames=[\"MegaFon\"]\tCells_AreConnected=[1]\tCells_Types=[DEFA" +
                    "ULT]\tCells_PhysicalsCellsIDs=[0]\tSimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[2" +
                    "]\tSimCards_OperatorsNames=[\"MegaFon\"]\tSimCards_AreRoaming=[0]\tSimCards_IccIDs=[\"\"]" +
                    "\tNetworksInterfaces_Names=[\"dummy0\",\"p2p0\",\"wlan0\"]\tNetworksInterfaces_Macs=[\"1A24" +
                    "638C9E24\",\"127B4486FB5A\",\"107B4486FB5A\"]\tDeviceType=PHONE\tEventID=52252958152399680" +
                    "60\tEventNumber=1\tEventDate=2018-12-11\tEventDateTime=2018-12-11 16:33:03\tEventTimestamp" +
                    "=1544535183\tEventTimeZone=10800\tEventType=EVENT_CRASH\tEventName=java.lang.IllegalStateE" +
                    "xception\tEventSource=sdk\tEventFirstOccurrence=undefined\tEventValueCrash=java.lang.Illeg" +
                    "alStateException: Two different ViewHolders have the same stable ID. Stable IDs in your ad" +
                    "apter MUST BE unique and SHOULD NOT change.\\n ViewHolder 1:ViewHolder{8287fab position=0 " +
                    "id=0, oldPos=-1, pLpos:-1} \\n View Holder 2:ViewHolder{6d6b87 position=1 id=0, oldPos=-1," +
                    " pLpos:-1} android.support.v7.widget.RecyclerView{d2b3923 VFED..... ......ID 0,36-720,463 " +
                    "#7f0900cd app:id/carouselWidgetItems}, adapter:com.mikepenz.fastadapter.commons.adapters.F" +
                    "astItemAdapter@ae22e20, layout:android.support.v7.widget.LinearLayoutManager@91b93d9, cont" +
                    "ext:ru.yandex.market.activity.main.MainActivity@51d52c2\\n\\tat android.support.v7.widget." +
                    "RecyclerView.handleMissingPreInfoForChangeError(RecyclerView.java:4058)\\n\\tat android.su" +
                    "pport.v7.widget.RecyclerView.dispatchLayoutStep3(RecyclerView.java:3982)\\n\\tat android.s" +
                    "upport.v7.widget.RecyclerView.dispatchLayout(RecyclerView.java:3652)\\n\\tat android.suppo" +
                    "rt.v7.widget.RecyclerView.onLayout(RecyclerView.java:4194)\\n\\tat android.view.View.layou" +
                    "t(View.java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat andr" +
                    "oid.widget.FrameLayout.layoutChildren(FrameLayout.java:323)\\n\\tat android.widget.FrameLa" +
                    "yout.onLayout(FrameLayout.java:261)\\n\\tat android.view.View.layout(View.java:19681)\\n\\" +
                    "tat android.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.widget.LinearLayout" +
                    ".setChildFrame(LinearLayout.java:1791)\\n\\tat android.widget.LinearLayout.layoutVertical(" +
                    "LinearLayout.java:1635)\\n\\tat android.widget.LinearLayout.onLayout(LinearLayout.java:154" +
                    "4)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layout" +
                    "(ViewGroup.java:6075)\\n\\tat android.widget.FrameLayout.layoutChildren(FrameLayout.java:3" +
                    "23)\\n\\tat android.widget.FrameLayout.onLayout(FrameLayout.java:261)\\n\\tat android.view" +
                    ".View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGroup.java:6075)\\" +
                    "n\\tat android.widget.LinearLayout.setChildFrame(LinearLayout.java:1791)\\n\\tat android.w" +
                    "idget.LinearLayout.layoutVertical(LinearLayout.java:1635)\\n\\tat android.widget.LinearLay" +
                    "out.onLayout(LinearLayout.java:1544)\\n\\tat android.view.View.layout(View.java:19681)\\" +
                    "n\\tat android.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.widget.FrameLayo" +
                    "ut.layoutChildren(FrameLayout.java:323)\\n\\tat android.widget.FrameLayout.onLayout(FrameL" +
                    "ayout.java:261)\\n\\tat android.support.v4.widget.NestedScrollView.onLayout(NestedScrollVi" +
                    "ew.java:1733)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat android.view.ViewG" +
                    "roup.layout(ViewGroup.java:6075)\\n\\tat android.widget.FrameLayout.layoutChildren(FrameLa" +
                    "yout.java:323)\\n\\tat android.widget.FrameLayout.onLayout(FrameLayout.java:261)\\n\\tat a" +
                    "ndroid.view.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGroup.j" +
                    "ava:6075)\\n\\tat android.support.design.widget.CoordinatorLayout.layoutChild(CoordinatorL" +
                    "ayout.java:1183)\\n\\tat android.support.design.widget.CoordinatorLayout.onLayoutChild(Coo" +
                    "rdinatorLayout.java:870)\\n\\tat android.support.design.widget.CoordinatorLayout.onLayout(" +
                    "CoordinatorLayout.java:889)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat andr" +
                    "oid.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.widget.FrameLayout.layoutCh" +
                    "ildren(FrameLayout.java:323)\\n\\tat android.widget.FrameLayout.onLayout(FrameLayout.java:" +
                    "261)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layo" +
                    "ut(ViewGroup.java:6075)\\n\\tat android.widget.FrameLayout.layoutChildren(FrameLayout.java" +
                    ":323)\\n\\tat android.widget.FrameLayout.onLayout(FrameLayout.java:261)\\n\\tat android.vi" +
                    "ew.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGroup.java:6075" +
                    ")\\n\\tat android.widget.LinearLayout.setChildFrame(LinearLayout.java:1791)\\n\\tat androi" +
                    "d.widget.LinearLayout.layoutVertical(LinearLayout.java:1635)\\n\\tat android.widget.Linear" +
                    "Layout.onLayout(LinearLayout.java:1544)\\n\\tat android.view.View.layout(View.java:19681" +
                    ")\\n\\tat android.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.widget.FrameL" +
                    "ayout.layoutChildren(FrameLayout.java:323)\\n\\tat android.widget.FrameLayout.onLayout(Fra" +
                    "meLayout.java:261)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat android.view." +
                    "ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.widget.FrameLayout.layoutChildren(Fr" +
                    "ameLayout.java:323)\\n\\tat android.widget.FrameLayout.onLayout(FrameLayout.java:261)\\n\\" +
                    "tat android.view.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGr" +
                    "oup.java:6075)\\n\\tat android.widget.FrameLayout.layoutChildren(FrameLayout.java:323)\\" +
                    "n\\tat android.widget.FrameLayout.onLayout(FrameLayout.java:261)\\n\\tat android.view.View" +
                    ".layout(View.java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\ta" +
                    "t android.widget.LinearLayout.setChildFrame(LinearLayout.java:1791)\\n\\tat android.widget" +
                    ".LinearLayout.layoutVertical(LinearLayout.java:1635)\\n\\tat android.widget.LinearLayout.o" +
                    "nLayout(LinearLayout.java:1544)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat " +
                    "android.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.widget.FrameLayout.layo" +
                    "utChildren(FrameLayout.java:323)\\n\\tat android.widget.FrameLayout.onLayout(FrameLayout.j" +
                    "ava:261)\\n\\tat com.android.internal.policy.DecorView.onLayout(DecorView.java:772)\\n\\ta" +
                    "t android.view.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGrou" +
                    "p.java:6075)\\n\\tat android.view.ViewRootImpl.performLayout(ViewRootImpl.java:2507)\\n\\t" +
                    "at android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:2223)\\n\\tat android.vie" +
                    "w.ViewRootImpl.doTraversal(ViewRootImpl.java:1403)\\n\\tat android.view.ViewRootImpl$Trave" +
                    "rsalRunnable.run(ViewRootImpl.java:6804)\\n\\tat android.view.Choreographer$CallbackRecord" +
                    ".run(Choreographer.java:911)\\n\\tat android.view.Choreographer.doCallbacks(Choreographer." +
                    "java:723)\\n\\tat android.view.Choreographer.doFrame(Choreographer.java:658)\\n\\tat andro" +
                    "id.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:897)\\n\\tat androi" +
                    "d.os.Handler.handleCallback(Handler.java:790)\\n\\tat android.os.Handler.dispatchMessage(H" +
                    "andler.java:99)\\n\\tat android.os.Looper.loop(Looper.java:169)\\n\\tat android.app.Activi" +
                    "tyThread.main(ActivityThread.java:6521)\\n\\tat java.lang.reflect.Method.invoke(Native Met" +
                    "hod)\\n\\tat com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:" +
                    "438)\\n\\tat com.android.internal.os.ZygoteInit.main(ZygoteInit.java:807)\\n\tCrashID=1531" +
                    "8074709981486426\tCrashGroupID=18293030491426648851\tClickDate=1970-01-01\tRegionID=52\tAp" +
                    "pID=ru.beru.android\tClientIP=::ffff:31.173.101.97\tClientIPHash=7672558688229485\tClientP" +
                    "ort=22524\tSex=1\tAge=18\tDeduplicationEnabled=1\tProfileAttributeVersion=2591300038479052" +
                    "9\tCustomAttribute_Types=[3]\tCustomAttribute_Ids=[48]\tCustomAttribute_IdSetFlags=[1]\tCu" +
                    "stomAttribute_StringValues=[\"\"]\tCustomAttribute_NumberValues=[1]\tCustomAttribute_BoolV" +
                    "alues=[0]\tCustomAttribute_ResetFlags=[0]\tCustomAttribute_Versions=[25913000384790529]";

        String[] tags = {};
        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1544535183000L),
            "market_mobile", checker.getHost(), "", "java.lang.IllegalStateException",
            "critical", "Two different ViewHolders have the same stable ID. Stable IDs in your adapter MUST BE uni" +
            "que and SHOULD NOT change.\\n ViewHolder 1:ViewHolder{8287fab position=0 id=0, oldPos=-1, " +
            "pLpos:-1} \\n View Holder 2:ViewHolder{6d6b87 position=1 id=0, oldPos=-1, pLpos:-1} androi" +
            "d.support.v7.widget.RecyclerView{d2b3923 VFED..... ......ID 0,36-720,463 #7f0900cd app:id/" +
            "carouselWidgetItems}, adapter:com.mikepenz.fastadapter.commons.adapters.FastItemAdapter@ae" +
            "22e20, layout:android.support.v7.widget.LinearLayoutManager@91b93d9, context:ru.yandex.mar" +
            "ket.activity.main.MainActivity@51d52c2",
                        "java.lang.IllegalStateException: Two different ViewHolders have the same stable ID. Stabl" +
            "e IDs in your adapter MUST BE unique and SHOULD NOT change.\\n ViewHolder 1:ViewHolder{828" +
            "7fab position=0 id=0, oldPos=-1, pLpos:-1} \\n View Holder 2:ViewHolder{6d6b87 position=1 " +
            "id=0, oldPos=-1, pLpos:-1} android.support.v7.widget.RecyclerView{d2b3923 VFED..... ......" +
            "ID 0,36-720,463 #7f0900cd app:id/carouselWidgetItems}, adapter:com.mikepenz.fastadapter.co" +
            "mmons.adapters.FastItemAdapter@ae22e20, layout:android.support.v7.widget.LinearLayoutManag" +
            "er@91b93d9, context:ru.yandex.market.activity.main.MainActivity@51d52c2\\n\\tat android.su" +
            "pport.v7.widget.RecyclerView.handleMissingPreInfoForChangeError(RecyclerView.java:4058)\\" +
            "n\\tat android.support.v7.widget.RecyclerView.dispatchLayoutStep3(RecyclerView.java:3982)\\" +
            "n\\tat android.support.v7.widget.RecyclerView.dispatchLayout(RecyclerView.java:3652)\\n\\t" +
            "at android.support.v7.widget.RecyclerView.onLayout(RecyclerView.java:4194)\\n\\tat android" +
            ".view.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGroup.java:60" +
            "75)\\n\\tat android.widget.FrameLayout.layoutChildren(FrameLayout.java:323)\\n\\tat androi" +
            "d.widget.FrameLayout.onLayout(FrameLayout.java:261)\\n\\tat android.view.View.layout(View." +
            "java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.wid" +
            "get.LinearLayout.setChildFrame(LinearLayout.java:1791)\\n\\tat android.widget.LinearLayout" +
            ".layoutVertical(LinearLayout.java:1635)\\n\\tat android.widget.LinearLayout.onLayout(Linea" +
            "rLayout.java:1544)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat android.view." +
            "ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.widget.FrameLayout.layoutChildren(Fr" +
            "ameLayout.java:323)\\n\\tat android.widget.FrameLayout.onLayout(FrameLayout.java:261)\\n\\" +
            "tat android.view.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGr" +
            "oup.java:6075)\\n\\tat android.widget.LinearLayout.setChildFrame(LinearLayout.java:1791)\\" +
            "n\\tat android.widget.LinearLayout.layoutVertical(LinearLayout.java:1635)\\n\\tat android." +
            "widget.LinearLayout.onLayout(LinearLayout.java:1544)\\n\\tat android.view.View.layout(View" +
            ".java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.wi" +
            "dget.FrameLayout.layoutChildren(FrameLayout.java:323)\\n\\tat android.widget.FrameLayout.o" +
            "nLayout(FrameLayout.java:261)\\n\\tat android.support.v4.widget.NestedScrollView.onLayout(" +
            "NestedScrollView.java:1733)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat andr" +
            "oid.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.widget.FrameLayout.layoutCh" +
            "ildren(FrameLayout.java:323)\\n\\tat android.widget.FrameLayout.onLayout(FrameLayout.java:" +
            "261)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layo" +
            "ut(ViewGroup.java:6075)\\n\\tat android.support.design.widget.CoordinatorLayout.layoutChil" +
            "d(CoordinatorLayout.java:1183)\\n\\tat android.support.design.widget.CoordinatorLayout.onL" +
            "ayoutChild(CoordinatorLayout.java:870)\\n\\tat android.support.design.widget.CoordinatorLa" +
            "yout.onLayout(CoordinatorLayout.java:889)\\n\\tat android.view.View.layout(View.java:19681" +
            ")\\n\\tat android.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.widget.FrameL" +
            "ayout.layoutChildren(FrameLayout.java:323)\\n\\tat android.widget.FrameLayout.onLayout(Fra" +
            "meLayout.java:261)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat android.view." +
            "ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.widget.FrameLayout.layoutChildren(Fr" +
            "ameLayout.java:323)\\n\\tat android.widget.FrameLayout.onLayout(FrameLayout.java:261)\\n\\" +
            "tat android.view.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGr" +
            "oup.java:6075)\\n\\tat android.widget.LinearLayout.setChildFrame(LinearLayout.java:1791)\\" +
            "n\\tat android.widget.LinearLayout.layoutVertical(LinearLayout.java:1635)\\n\\tat android." +
            "widget.LinearLayout.onLayout(LinearLayout.java:1544)\\n\\tat android.view.View.layout(View" +
            ".java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.wi" +
            "dget.FrameLayout.layoutChildren(FrameLayout.java:323)\\n\\tat android.widget.FrameLayout.o" +
            "nLayout(FrameLayout.java:261)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat an" +
            "droid.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.widget.FrameLayout.layout" +
            "Children(FrameLayout.java:323)\\n\\tat android.widget.FrameLayout.onLayout(FrameLayout.jav" +
            "a:261)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.la" +
            "yout(ViewGroup.java:6075)\\n\\tat android.widget.FrameLayout.layoutChildren(FrameLayout.ja" +
            "va:323)\\n\\tat android.widget.FrameLayout.onLayout(FrameLayout.java:261)\\n\\tat android." +
            "view.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layout(ViewGroup.java:607" +
            "5)\\n\\tat android.widget.LinearLayout.setChildFrame(LinearLayout.java:1791)\\n\\tat andro" +
            "id.widget.LinearLayout.layoutVertical(LinearLayout.java:1635)\\n\\tat android.widget.Linea" +
            "rLayout.onLayout(LinearLayout.java:1544)\\n\\tat android.view.View.layout(View.java:19681" +
            ")\\n\\tat android.view.ViewGroup.layout(ViewGroup.java:6075)\\n\\tat android.widget.FrameL" +
            "ayout.layoutChildren(FrameLayout.java:323)\\n\\tat android.widget.FrameLayout.onLayout(Fra" +
            "meLayout.java:261)\\n\\tat com.android.internal.policy.DecorView.onLayout(DecorView.java:7" +
            "72)\\n\\tat android.view.View.layout(View.java:19681)\\n\\tat android.view.ViewGroup.layou" +
            "t(ViewGroup.java:6075)\\n\\tat android.view.ViewRootImpl.performLayout(ViewRootImpl.java:2" +
            "507)\\n\\tat android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:2223)\\n\\tat a" +
            "ndroid.view.ViewRootImpl.doTraversal(ViewRootImpl.java:1403)\\n\\tat android.view.ViewRoot" +
            "Impl$TraversalRunnable.run(ViewRootImpl.java:6804)\\n\\tat android.view.Choreographer$Call" +
            "backRecord.run(Choreographer.java:911)\\n\\tat android.view.Choreographer.doCallbacks(Chor" +
            "eographer.java:723)\\n\\tat android.view.Choreographer.doFrame(Choreographer.java:658)\\n\\" +
            "tat android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:897)\\n\\t" +
            "at android.os.Handler.handleCallback(Handler.java:790)\\n\\tat android.os.Handler.dispatch" +
            "Message(Handler.java:99)\\n\\tat android.os.Looper.loop(Looper.java:169)\\n\\tat android.a" +
            "pp.ActivityThread.main(ActivityThread.java:6521)\\n\\tat java.lang.reflect.Method.invoke(N" +
            "ative Method)\\n\\tat com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeI" +
            "nit.java:438)\\n\\tat com.android.internal.os.ZygoteInit.main(ZygoteInit.java:807)\\n",
            "7435241013474846247", "RecyclerView.java", 4058, tags, keys, values, "1.27", UNKNOWN, "android"
        );
    }

    @Test
    public void parseWithDoubleErrorMessage() throws Exception {
        String line = "tskv_format=metrika-mobile-log\ttimestamp=2018-12-19 20:29:12\ttimezone=+0000\tAPIKey=1389" +
                    "598\tAPIKey128=36ef1512-b352-4415-87bd-cf45c4f7e46b\tAttributionID=1\tAttributionIDUUIDHas" +
                    "h=9257515450591655498\tStartDate=2018-12-19\tUUID=6bd35f0ddc1f97e99b8b5f4662540dd7\tDevice" +
                    "ID=bd07ec6739f82bbf847ee915939e2083\tUUIDHash=6731484360958769059\tDeviceIDHash=1095891045" +
                    "5461080956\tAppPlatform=android\tOperatingSystem=android\tAppFramework=NATIVE\tAppVersionN" +
                    "ame=1.28\tAppBuildNumber=1500\tAppDebuggable=false\tKitVersion=3002002\tManufacturer=Noki" +
                    "a\tModel=Nokia 5\tOriginalManufacturer=HMD Global\tOriginalModel=TA-1024\tOSVersion=7.1.1" +
                    "\tOSApiLevel=25\tScreenWidth=1280\tScreenHeight=720\tScreenDPI=320\tScaleFactor=2\tAndroidI" +
                    "D=5571107241e99132\tADVID=4ff941d2-9d30-4b43-8746-4428a25291cc\tLimitAdTracking=false\tCli" +
                    "entKitVersion=3002002\tKitBuildType=202\tKitBuildNumber=11391\tSendTimestamp=1545251351\tR" +
                    "eceiveDate=2018-12-19\tReceiveTimestamp=1545251352\tSessionID=10000000035\tSessionType=SES" +
                    "SION_BACKGROUND\tDeviceIDSessionIDHash=16410841302757672991\tStartTime=2018-12-19 23:29:0" +
                    "9\tStartTimestamp=1545251349\tStartTimeZone=10800\tRegionTimeZone=10800\tLocale=ru_RU\tLoc" +
                    "ationSource=MISSING\tLocationEnabled=true\tWifiAccessPointState=UNKNOWNConnectionType=CONN" +
                    "_CELL\tNetworkType=LTE\tCountryCode=250\tOperatorID=1\tOperatorName=MTS RUS\tCells_CellsID" +
                    "s=[0]\tCells_SignalsStrengths=[0]\tCells_Lacs=[0]\tCells_CountriesCodes=[250]\tCells_Opera" +
                    "torsIDs=[1]\tCells_OperatorsNames=[\"MTS RUS\"]\tCells_AreConnected=[1]\tCells_Types=[DEFA" +
                    "ULT]\tCells_PhysicalsCellsIDs=[0]\tSimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[1" +
                    "]\tSimCards_OperatorsNames=[\"MTS RUS\"]\tSimCards_AreRoaming=[0]\tSimCards_IccIDs=[\"\"]" +
                    "\tNetworksInterfaces_Names=[\"wlan0\",\"p2p0\",\"dummy0\"]\tNetworksInterfaces_Macs=[\"58C9" +
                    "35306B5A\",\"5AC935306B5A\",\"7E997BAF9342\"]\tDeviceType=PHONE\tEventID=79346761566427952" +
                    "51\tEventNumber=1\tEventDate=2018-12-19\tEventDateTime=2018-12-19 23:29:09\tEventTimestamp" +
                    "=1545251349\tEventTimeZone=10800\tEventType=EVENT_CRASH\tEventName=java.lang.RuntimeExcept" +
                    "ion\tEventSource=sdk\tEventFirstOccurrence=undefined\tEventValueCrash=java.lang.RuntimeExc" +
                    "eption: Unable to destroy activity {ru.beru.android/ru.yandex.market.activity.main.MainAct" +
                    "ivity}: java.lang.IllegalArgumentException: You cannot start a load for a destroyed activi" +
                    "ty\\n\\tat android.app.ActivityThread.performDestroyActivity(ActivityThread.java:4203)\\" +
                    "n\\tat android.app.ActivityThread.handleDestroyActivity(ActivityThread.java:4221)\\n\\tat " +
                    "android.app.ActivityThread.-wrap6(ActivityThread.java)\\n\\tat android.app.ActivityThread$" +
                    "H.handleMessage(ActivityThread.java:1538)\\n\\tat android.os.Handler.dispatchMessage(Handl" +
                    "er.java:102)\\n\\tat android.os.Looper.loop(Looper.java:154)\\n\\tat android.app.ActivityT" +
                    "hread.main(ActivityThread.java:6119)\\n\\tat java.lang.reflect.Method.invoke(Native Method" +
                    ")\\n\\tat com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:886" +
                    ")\\n\\tat com.android.internal.os.ZygoteInit.main(ZygoteInit.java:776)\\nCaused by: java.l" +
                    "ang.IllegalArgumentException: You cannot start a load for a destroyed activity\\n\\tat com" +
                    ".bumptech.glide.manager.RequestManagerRetriever.assertNotDestroyed(RequestManagerRetriever" +
                    ".java:134)\\n\\tat com.bumptech.glide.manager.RequestManagerRetriever.get(RequestManagerRe" +
                    "triever.java:102)\\n\\tat com.bumptech.glide.manager.RequestManagerRetriever.get(RequestMa" +
                    "nagerRetriever.java:87)\\n\\tat com.bumptech.glide.Glide.with(Glide.java:629)\\n\\tat ru.y" +
                    "andex.market.util.GlideWrapper.loadImage(GlideWrapper.java:52)\\n\\tat ru.yandex.market.ad" +
                    "apter.ModelGalleryAdapter$ImageController.loadImage(ModelGalleryAdapter.java:209)\\n\\tat " +
                    "ru.yandex.market.adapter.ModelGalleryAdapter.instantiateItem(ModelGalleryAdapter.java:99" +
                    ")\\n\\tat android.support.v4.view.ViewPager.addNewItem(ViewPager.java:1010)\\n\\tat androi" +
                    "d.support.v4.view.ViewPager.populate(ViewPager.java:1158)\\n\\tat android.support.v4.view." +
                    "ViewPager.setCurrentItemInternal(ViewPager.java:669)\\n\\tat android.support.v4.view.ViewP" +
                    "ager.setCurrentItemInternal(ViewPager.java:631)\\n\\tat android.support.v4.view.ViewPager." +
                    "dataSetChanged(ViewPager.java:1086)\\n\\tat android.support.v4.view.ViewPager$PagerObserve" +
                    "r.onChanged(ViewPager.java:3097)\\n\\tat android.support.v4.view.PagerAdapter.notifyDataSe" +
                    "tChanged(PagerAdapter.java:291)\\n\\tat ru.yandex.market.adapter.ModelGalleryAdapter.setUr" +
                    "ls(ModelGalleryAdapter.java:75)\\n\\tat ru.yandex.market.activity.model.SkuFragment.onDest" +
                    "royView(SkuFragment.java:292)\\n\\tat android.support.v4.app.Fragment.performDestroyView(F" +
                    "ragment.java:2678)\\n\\tat android.support.v4.app.FragmentManagerImpl.moveToState(Fragment" +
                    "Manager.java:1533)\\n\\tat android.support.v4.app.FragmentManagerImpl.moveFragmentToExpect" +
                    "edState(FragmentManager.java:1784)\\n\\tat android.support.v4.app.FragmentManagerImpl.move" +
                    "ToState(FragmentManager.java:1852)\\n\\tat android.support.v4.app.FragmentManagerImpl.disp" +
                    "atchStateChange(FragmentManager.java:3269)\\n\\tat android.support.v4.app.FragmentManagerI" +
                    "mpl.dispatchDestroyView(FragmentManager.java:3254)\\n\\tat android.support.v4.app.Fragment" +
                    ".performDestroyView(Fragment.java:2674)\\n\\tat android.support.v4.app.FragmentManagerImpl" +
                    ".moveToState(FragmentManager.java:1533)\\n\\tat android.support.v4.app.FragmentManagerImpl" +
                    ".moveFragmentToExpectedState(FragmentManager.java:1784)\\n\\tat android.support.v4.app.Fra" +
                    "gmentManagerImpl.moveToState(FragmentManager.java:1852)\\n\\tat android.support.v4.app.Fra" +
                    "gmentManagerImpl.dispatchStateChange(FragmentManager.java:3269)\\n\\tat android.support.v4" +
                    ".app.FragmentManagerImpl.dispatchDestroy(FragmentManager.java:3260)\\n\\tat android.suppor" +
                    "t.v4.app.FragmentController.dispatchDestroy(FragmentController.java:274)\\n\\tat android.s" +
                    "upport.v4.app.FragmentActivity.onDestroy(FragmentActivity.java:419)\\n\\tat android.suppor" +
                    "t.v7.app.AppCompatActivity.onDestroy(AppCompatActivity.java:210)\\n\\tat ru.yandex.market." +
                    "internal.containers.BaseActivity.onDestroy(BaseActivity.java:145)\\n\\tat ru.yandex.market" +
                    ".activity.main.MainActivity.onDestroy(MainActivity.java:317)\\n\\tat android.app.Activity." +
                    "performDestroy(Activity.java:6922)\\n\\tat android.app.Instrumentation.callActivityOnDestr" +
                    "oy(Instrumentation.java:1154)\\n\\tat android.app.ActivityThread.performDestroyActivity(Ac" +
                    "tivityThread.java:4190)\\n\\t... 9 more\\n\tCrashID=5024449155327736856\tCrashGroupID=9179" +
                    "94071270636385\tClickDate=1970-01-01\tRegionID=35\tAppID=ru.beru.android\tClientIP=::ffff:" +
                    "95.153.135.79\tClientIPHash=489688703947514735\tClientPort=4434\tSex=1\tAge=25\tDeduplicat" +
                    "ionEnabled=1\tProfileAttributeVersion=25925015656464385\tCustomAttribute_Types=[3]\tCustom" +
                    "Attribute_Ids=[48]\tCustomAttribute_IdSetFlags=[1]\tCustomAttribute_StringValues=[\"\"]\tC" +
                    "ustomAttribute_NumberValues=[1]\tCustomAttribute_BoolValues=[0]\tCustomAttribute_ResetFlag" +
                    "s=[0]\tCustomAttribute_Versions=[25925015656464385]";

        String[] tags = {};
        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1545251349000L),
            "market_front_bluetouch", checker.getHost(), "", "java.lang.IllegalArgumentException",
            "critical", "You cannot start a load for a destroyed activity",
            "java.lang.RuntimeException: Unable to destroy activity {ru.beru.android/ru.yandex.market." +
            "activity.main.MainActivity}: java.lang.IllegalArgumentException: You cannot start a load f" +
            "or a destroyed activity\\n\\tat android.app.ActivityThread.performDestroyActivity(Activity" +
            "Thread.java:4203)\\n\\tat android.app.ActivityThread.handleDestroyActivity(ActivityThread." +
            "java:4221)\\n\\tat android.app.ActivityThread.-wrap6(ActivityThread.java)\\n\\tat android." +
            "app.ActivityThread$H.handleMessage(ActivityThread.java:1538)\\n\\tat android.os.Handler.di" +
            "spatchMessage(Handler.java:102)\\n\\tat android.os.Looper.loop(Looper.java:154)\\n\\tat an" +
            "droid.app.ActivityThread.main(ActivityThread.java:6119)\\n\\tat java.lang.reflect.Method.i" +
            "nvoke(Native Method)\\n\\tat com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(Zy" +
            "goteInit.java:886)\\n\\tat com.android.internal.os.ZygoteInit.main(ZygoteInit.java:776)\\n" +
            "Caused by: java.lang.IllegalArgumentException: You cannot start a load for a destroyed act" +
            "ivity\\n\\tat com.bumptech.glide.manager.RequestManagerRetriever.assertNotDestroyed(Reques" +
            "tManagerRetriever.java:134)\\n\\tat com.bumptech.glide.manager.RequestManagerRetriever.get" +
            "(RequestManagerRetriever.java:102)\\n\\tat com.bumptech.glide.manager.RequestManagerRetrie" +
            "ver.get(RequestManagerRetriever.java:87)\\n\\tat com.bumptech.glide.Glide.with(Glide.java:" +
            "629)\\n\\tat ru.yandex.market.util.GlideWrapper.loadImage(GlideWrapper.java:52)\\n\\tat ru" +
            ".yandex.market.adapter.ModelGalleryAdapter$ImageController.loadImage(ModelGalleryAdapter.j" +
            "ava:209)\\n\\tat ru.yandex.market.adapter.ModelGalleryAdapter.instantiateItem(ModelGallery" +
            "Adapter.java:99)\\n\\tat android.support.v4.view.ViewPager.addNewItem(ViewPager.java:1010" +
            ")\\n\\tat android.support.v4.view.ViewPager.populate(ViewPager.java:1158)\\n\\tat android." +
            "support.v4.view.ViewPager.setCurrentItemInternal(ViewPager.java:669)\\n\\tat android.suppo" +
            "rt.v4.view.ViewPager.setCurrentItemInternal(ViewPager.java:631)\\n\\tat android.support.v4" +
            ".view.ViewPager.dataSetChanged(ViewPager.java:1086)\\n\\tat android.support.v4.view.ViewPa" +
            "ger$PagerObserver.onChanged(ViewPager.java:3097)\\n\\tat android.support.v4.view.PagerAdap" +
            "ter.notifyDataSetChanged(PagerAdapter.java:291)\\n\\tat ru.yandex.market.adapter.ModelGall" +
            "eryAdapter.setUrls(ModelGalleryAdapter.java:75)\\n\\tat ru.yandex.market.activity.model.Sk" +
            "uFragment.onDestroyView(SkuFragment.java:292)\\n\\tat android.support.v4.app.Fragment.perf" +
            "ormDestroyView(Fragment.java:2678)\\n\\tat android.support.v4.app.FragmentManagerImpl.move" +
            "ToState(FragmentManager.java:1533)\\n\\tat android.support.v4.app.FragmentManagerImpl.move" +
            "FragmentToExpectedState(FragmentManager.java:1784)\\n\\tat android.support.v4.app.Fragment" +
            "ManagerImpl.moveToState(FragmentManager.java:1852)\\n\\tat android.support.v4.app.Fragment" +
            "ManagerImpl.dispatchStateChange(FragmentManager.java:3269)\\n\\tat android.support.v4.app." +
            "FragmentManagerImpl.dispatchDestroyView(FragmentManager.java:3254)\\n\\tat android.support" +
            ".v4.app.Fragment.performDestroyView(Fragment.java:2674)\\n\\tat android.support.v4.app.Fra" +
            "gmentManagerImpl.moveToState(FragmentManager.java:1533)\\n\\tat android.support.v4.app.Fra" +
            "gmentManagerImpl.moveFragmentToExpectedState(FragmentManager.java:1784)\\n\\tat android.su" +
            "pport.v4.app.FragmentManagerImpl.moveToState(FragmentManager.java:1852)\\n\\tat android.su" +
            "pport.v4.app.FragmentManagerImpl.dispatchStateChange(FragmentManager.java:3269)\\n\\tat an" +
            "droid.support.v4.app.FragmentManagerImpl.dispatchDestroy(FragmentManager.java:3260)\\n\\ta" +
            "t android.support.v4.app.FragmentController.dispatchDestroy(FragmentController.java:274)\\" +
            "n\\tat android.support.v4.app.FragmentActivity.onDestroy(FragmentActivity.java:419)\\n\\ta" +
            "t android.support.v7.app.AppCompatActivity.onDestroy(AppCompatActivity.java:210)\\n\\tat r" +
            "u.yandex.market.internal.containers.BaseActivity.onDestroy(BaseActivity.java:145)\\n\\tat " +
            "ru.yandex.market.activity.main.MainActivity.onDestroy(MainActivity.java:317)\\n\\tat andro" +
            "id.app.Activity.performDestroy(Activity.java:6922)\\n\\tat android.app.Instrumentation.cal" +
            "lActivityOnDestroy(Instrumentation.java:1154)\\n\\tat android.app.ActivityThread.performDe" +
            "stroyActivity(ActivityThread.java:4190)\\n\\t... 9 more\\n",
            "11568297339348775782", "ActivityThread.java", 4203, tags, keys, values, "1.28", UNKNOWN, "android"
        );
    }

    @Test
    public void parseWithAndroidShortErrorMessage() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2018-12-19 21:01:24\ttimezone=+0000\tAPIKe" +
                    "y=1389598\tAPIKey128=36ef1512-b352-4415-87bd-cf45c4f7e46b\tAttributionID=1\tAttributionIDU" +
                    "UIDHash=16456991729672471399\tStartDate=2018-12-20\tUUID=cb45b32b995e1f8f027b1ebe0a48e9b2" +
                    "\tDeviceID=bdb8d2e75494603aa3e31f41314d35d9\tUUIDHash=4444523966327805198\tDeviceIDHash=969" +
                    "3420710960777059\tAppPlatform=android\tOperatingSystem=android\tAppFramework=NATIVE\tAppVe" +
                    "rsionName=1.27\tAppBuildNumber=1497\tAppDebuggable=false\tKitVersion=3002002\tManufacturer" +
                    "=Lenovo\tModel=TAB4 10 Plus\tOriginalManufacturer=LENOVO\tOriginalModel=Lenovo TB-X704L\tO" +
                    "SVersion=7.1.1\tOSApiLevel=25\tScreenWidth=1920\tScreenHeight=1200\tScreenDPI=280\tScaleFa" +
                    "ctor=1.75\tAndroidID=c82cdd4119b68c34\tADVID=d6b4d998-4731-4217-b4cf-a114450a9d02\tLimitAd" +
                    "Tracking=false\tClientKitVersion=3002002\tKitBuildType=202\tKitBuildNumber=11391\tSendTime" +
                    "stamp=1545253284\tReceiveDate=2018-12-20\tReceiveTimestamp=1545253284\tSessionID=100000003" +
                    "76\tSessionType=SESSION_BACKGROUND\tDeviceIDSessionIDHash=5234547411175181806\tStartTime=2" +
                    "018-12-20 00:00:52\tStartTimestamp=1545253252\tStartTimeZone=10800\tRegionTimeZone=10800\t" +
                    "Locale=ru_RU\tLocationSource=LBS\tLatitude=55.80995559692383\tLongitude=37.72992706298828" +
                    "\tLocationPrecision=140\tLocationEnabled=true\tWifi_Macs=[\"384C4F6C5F88\"]\tWifi_SignalsSt" +
                    "rengths=[-49]\tWifi_Ssids=[\"11038811\"]\tWifi_AreConnected=[1]\tWifiAccessPointState=UNKN" +
                    "OWN\tConnectionType=CONN_WIFI\tNetworksInterfaces_Names=[\"p2p0\",\"dummy0\",\"wlan0\"]\tN" +
                    "etworksInterfaces_Macs=[\"D2F88CA23EDC\",\"C66A616F0031\",\"D0F88CA23EDC\"]\tDeviceType=TA" +
                    "BLET\tEventID=8272082399897274278\tEventNumber=1\tEventDate=2018-12-20\tEventDateTime=2018" +
                    "-12-20 00:00:52\tEventTimestamp=1545253252\tEventTimeZone=10800\tEventType=EVENT_CRASH\tEv" +
                    "entName=java.lang.OutOfMemoryError\tEventSource=sdk\tEventFirstOccurrence=undefined\tEvent" +
                    "ValueCrash=java.lang.OutOfMemoryError: OutOfMemoryError thrown while trying to throw OutOf" +
                    "MemoryError; no stack trace available\\n\tCrashID=17195427211382060370\tCrashGroupID=15113" +
                    "618746461067532\tClickDate=1970-01-01\tRegionID=116980\tAppID=ru.beru.android\tClientIP=::" +
                    "ffff:109.252.50.156\tClientIPHash=8413107664649086293\tClientPort=19308\tSex=2\tAge=25\tDe" +
                    "duplicationEnabled=1\tProfileAttributeVersion=25925047583506433\tCustomAttribute_Types=[3" +
                    "]\tCustomAttribute_Ids=[48]\tCustomAttribute_IdSetFlags=[1]\tCustomAttribute_StringValues=" +
                    "[\"\"]\tCustomAttribute_NumberValues=[1]\tCustomAttribute_BoolValues=[0]\tCustomAttribute_" +
                    "ResetFlags=[0]\tCustomAttribute_Versions=[25925047583506433]";

        String[] tags = {};
        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1545253252000L),
            "market_front_bluetouch", checker.getHost(), "", "java.lang.OutOfMemoryError",
            "critical", "OutOfMemoryError thrown while trying to throw OutOfMemoryError",
            "java.lang.OutOfMemoryError: OutOfMemoryError thrown while trying to throw OutOfMemoryErro" +
            "r; no stack trace available\\n",
            "11435680755645645390", "", 0, tags, keys, values, "1.27", UNKNOWN, "android"
        );
    }

    @Test
    public void parseErrorMessage() throws Exception {
        String line = "ADVID=0e7b3bc8-c6aa-4359-a967-9cf61c18c534\tAPIKey=1389598\tAPIKey128=36ef1512-b352-4415" +
            "-87bd-cf45c4f7e46b\tAndroidID=cbb1523dc01e5b4b\tAppBuildNumber=1586\tAppDebuggable=false\tAppFrame" +
            "work=NATIVE\tAppID=ru.beru.android\tAppPlatform=android\tAppVersionName=1.52\tAttributionID=1\tAtt" +
            "ributionIDUUIDHash=7318918502422928125\tCells_AreConnected=[1]\tCells_CellsIDs=[0]\tCells_Countrie" +
            "sCodes=[250]\tCells_Lacs=[0]\tCells_OperatorsIDs=[2]\tCells_OperatorsNames=[\\\"MegaFon\\\"]\tCell" +
            "s_PhysicalsCellsIDs=[0]\tCells_SignalsStrengths=[-91]\tCells_Types=[DEFAULT]\tClickDate=1970-01-01" +
            "\tClientIP=::ffff:85.26.233.122\tClientIPHash=14190143581350876356\tClientKitVersion=3005002\tClie" +
            "ntPort=4966\tConnectionType=CONN_CELL\tCountryCode=250\tDeduplicationEnabled=1\tDeviceID=125288bca" +
            "970581f0c463515f81cf1ef\tDeviceIDHash=13949263166050660369\tDeviceIDSessionIDHash=3642673062210216" +
            "740\tDeviceType=PHONE\tEventDate=2019-04-24\tEventDateTime=2019-04-24 18:59:56\tEventFirstOccurren" +
            "ce=undefined\tEventID=9760154410970374107\tEventName=java.lang.RuntimeException\tEventNumber=850\t" +
            "EventSource=sdk\tEventTimeOffset=807\tEventTimeZone=18000\tEventTimestamp=1556121596\tEventType=EV" +
            "ENT_CRASH\tEventValue=\tEventValueJsonReference=\tImei=\tIsRevenueVerified=undefined\tKitBuildNumb" +
            "er=16697\tKitBuildType=202\tKitVersion=3005002\tLatitude=\tLimitAdTracking=false\tLocale=ru_RU\tLo" +
            "cationAltitude=\tLocationEnabled=true\tLocationPrecision=\tLocationSource=MISSING\tLocationTimesta" +
            "mp=\tLongitude=\tManufacturer=Samsung\tModel=Galaxy Note9\tNetworkType=HSUPA\tNetworksInterfaces_M" +
            "acs=[\\\"00904CF5A919\\\",\\\"02904CF5A919\\\",\\\"00904C332211\\\"]\tNetworksInterfaces_Names=[\\\"wl" +
            "an0\\\",\\\"swlan0\\\",\\\"p2p0\\\"]\tOSApiLevel=28\tOSVersion=9\tOperatingSystem=android\tOperat" +
            "orID=2\tOperatorName=MegaFon\tOriginalManufacturer=samsung\tOriginalModel=SM-N960F\tParsedParams_Ke" +
            "y1=\tParsedParams_Key10=\tParsedParams_Key2=\tParsedParams_Key3=\tParsedParams_Key4=\tParsedParams_K" +
            "ey5=\tParsedParams_Key6=\tParsedParams_Key7=\tParsedParams_Key8=\tParsedParams_Key9=\tParsedParams_Q" +
            "uantity=\tParsedParams_ValueDouble=\tProfileAttributeVersion=26107374599144274\tReceiveDate=2019-04-" +
            "24\tReceiveTimestamp=1556121623\tRegionID=51\tRegionTimeZone=14400\tRevenueOrderIdSource=autogenera" +
            "ted\tScaleFactor=3.5\tScreenDPI=560\tScreenHeight=1440\tScreenWidth=2960\tSendTimestamp=15561215" +
            "96\tSessionID=10000000171\tSessionType=SESSION_FOREGROUND\tSex=1\tSimCards_AreRoaming=[0]\tSimCard" +
            "s_CountriesCodes=[250]\tSimCards_IccIDs=[\\\"\\\"]\tSimCards_OperatorsIDs=[2]\tSimCards_OperatorsNa" +
            "mes=[\\\"MegaFon\\\"]\tStartDate=2019-04-24\tStartTime=2019-04-24 18:46:29\tStartTimeZone=18000\tSt" +
            "artTimestamp=1556120789\tUUID=d119c92cf11f4cf2bcf8d96dc86169c0\tUUIDHash=10189134413446969278\tWifi" +
            "AccessPointState=UNKNOWN\tWifi_AreConnected=\tWifi_LastVisibleTimeOffset=\tWifi_Macs=\tWifi_Signals" +
            "Strengths=\tWifi_Ssids=\t_logfeller_index_bucket=//home/logfeller/index/logfeller-topic-splitter/mar" +
            "ket-apps-metrika-mobile-log/3600-86400/1556123400/1556053200\t_logfeller_timestamp=1556121623\t_stbx=" +
            "rt3.vla--logfeller-topic-splitter--market-apps-metrika-mobile-log:0@@1435422513@@pqout_market_apps_me" +
            "trika_mobile_log:6bb656dd-4261c56d-293905df-3f4-18010@@1556122486198@@1556122513@@market-apps-metrika-" +
            "mobile-log@@1556122486080917@@1556122486468\tiso_eventtime=2019-04-24 19:00:23\tsource_uri=prt://logf" +
            "eller-topic-splitter@2a02:6b8:c0e:376:0:1504:4b15:dcc0;unknown_path\ttimestamp=2019-04-24 16:00:23\tt" +
            "imezone=+0000\ttskv_format=metrika-mobile-log";

        String[] tags = {};
        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1556121596000L),
            "market_front_bluetouch", checker.getHost(), "", "java.lang.RuntimeException",
            "critical", "", "", "2202906307356721367", "", 0, tags, keys, values, "1.52", UNKNOWN, "android"
        );
    }

    @Test
    public void noEventTypeFilterOnEmptyParam() throws Exception {
        String line = "tskv\ttskv_format=metrika-mobile-log\ttimestamp=2018-12-07 14:45:48\ttimezone=+0000\tAPIKe" +
                    "y=23107\tAttributionIDUUIDHash=15433599176222037151\tStartDate=2018-12-07\tUUID=fbfa908f5d" +
                    "3d9546ecb27db703b97821\tDeviceID=eaa1a4e6138a06351784852e4446d943\tUUIDHash=14439167462303" +
                    "939563\tDeviceIDHash=11070894046081033018\tAppPlatform=android\tOperatingSystem=android\tA" +
                    "ppFramework=NATIVE\tAppVersionName=2.20\tAppBuildNumber=703\tAppDebuggable=false\tKitVersi" +
                    "on=271\tManufacturer=Irbis\tModel=TX24\tOriginalManufacturer=IRBIS\tOriginalModel=TX24\tOS" +
                    "Version=4.4.2\tOSApiLevel=19\tScreenWidth=1024\tScreenHeight=600\tScreenDPI=160\tScaleFact" +
                    "or=1\tAndroidID=2bf9bc4a217456c4\tADVID=621734e7-95ef-4629-bc25-f08cbce2bae0\tLimitAdTrack" +
                    "ing=false\tClientKitVersion=120\tKitBuildType=202\tKitBuildNumber=7828\tSendTimestamp=1544" +
                    "190308\tSendTimeZone=14400\tReceiveDate=2018-12-07\tReceiveTimestamp=1544193948\tSessionID" +
                    "=1544190308\tSessionType=SESSION_BACKGROUND\tDeviceIDSessionIDHash=9997575031146670950\tSt" +
                    "artTime=2018-12-07 17:45:48\tStartTimestamp=1544193948\tStartTimeZone=14400\tRegionTimeZon" +
                    "e=10800\tLocale=ru_RU\tLocationSource=LBS\tLatitude=56.122520446777344\tLongitude=47.27384" +
                    "1857910156\tLocationPrecision=1600\tLocationEnabled=undefined\tWifiAccessPointSsid=IRBIS T" +
                    "X24\tWifiAccessPointState=DISABLED\tConnectionType=CONN_CELL\tNetworkType=HSPA+\tCountryCo" +
                    "de=250\tOperatorID=20\tOperatorName=Tele2\tCells_CellsIDs=[6456852]\tCells_SignalsStrength" +
                    "s=[-91]\tCells_Lacs=[20317]\tCells_CountriesCodes=[250]\tCells_OperatorsIDs=[20]\tCells_Op" +
                    "eratorsNames=[\"Tele2\"]\tCells_AreConnected=[1]\tCells_Types=[DEFAULT]\tCells_PhysicalsCe" +
                    "llsIDs=[0]\tSimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[20]\tSimCards_OperatorsN" +
                    "ames=[\"Tele2\"]\tSimCards_AreRoaming=[0]\tSimCards_IccIDs=[\"\"]\tNetworksInterfaces_Name" +
                    "s=[\"ifb0\",\"ifb1\",\"ccmni0\",\"ccmni1\",\"ccmni2\"]\tNetworksInterfaces_Macs=[\"527F993" +
                    "276E1\",\"4AB405FFD8B6\",\"6EC3A2168BBD\",\"8EC0BF09082A\",\"7AAB3D474C55\"]\tDeviceType=T" +
                    "ABLET\tEventID=12951947817559953868\tEventDate=2018-12-07\tEventDateTime=2018-12-07 17:45:" +
                    "48\tEventTimestamp=1544193948\tEventTimeZone=14400\tEventType=ololo\tEventSource=sdk\tEven" +
                    "tFirstOccurrence=undefined\tEventValueCrash=java.lang.NullPointerException\\n\\tat com.yan" +
                    "dex.auth.sync.AccountProviderHelper.yandexAccountsFromCursor(AccountProviderHelper.java:74" +
                    ")\\n\\tat com.yandex.auth.sync.AccountProviderHelper.getAccounts(AccountProviderHelper.jav" +
                    "a:39)\\n\\tat com.yandex.auth.sync.BackupLogic.backup(BackupLogic.java:44)\\n\\tat com.yan" +
                    "dex.auth.sync.BackupLogic.perform(BackupLogic.java:36)\\n\\tat com.yandex.auth.sync.Backup" +
                    "AccountsService.runBackupAction(BackupAccountsService.java:167)\\n\\tat com.yandex.auth.sy" +
                    "nc.BackupAccountsService.onHandleIntent(BackupAccountsService.java:54)\\n\\tat android.app" +
                    ".IntentService$ServiceHandler.handleMessage(IntentService.java:65)\\n\\tat android.os.Hand" +
                    "ler.dispatchMessage(Handler.java:110)\\n\\tat android.os.Looper.loop(Looper.java:193)\\n\\" +
                    "tat android.os.HandlerThread.run(HandlerThread.java:61)\\n\tCrashID=5619482377556945081\tC" +
                    "rashGroupID=5871795208066209891\tClickDate=1970-01-01\tRegionID=45\tAppID=ru.yandex.marke" +
                    "t\tClientIP=::ffff:176.59.97.169\tClientIPHash=3925492953153793702\tClientPort=32422\tDedu" +
                    "plicationEnabled=1\tProfileAttributeVersion=25907275411488768\tCustomAttribute_Types=[3]\t" +
                    "CustomAttribute_Ids=[48]\tCustomAttribute_IdSetFlags=[1]\tCustomAttribute_StringValues=[" +
                    "\"\"]\tCustomAttribute_NumberValues=[1]\tCustomAttribute_BoolValues=[0]\tCustomAttribute_Re" +
                    "setFlags=[0]\tCustomAttribute_Versions=[25907275411488768]";

        checker.checkEmpty(line);
    }

    @Test
    public void parseWithIOsErrorMessage() throws Exception {
        String line = "tskv_format=metrika-mobile-log\ttimestamp=2018-12-19 22:14:27\ttimezone=+00" +
                    "00\tAPIKey=1389598\tAPIKey128=36ef1512-b352-4415-87bd-cf45c4f7e46b\tAttributionID=1\tAttri" +
                    "butionIDUUIDHash=3999058486263797332\tStartDate=2018-12-20\tUUID=f7dfd6449a541befa7e7839b0" +
                    "0623016\tDeviceID=F834E974-5D2D-4F0D-A15F-2E714C415636\tOriginalDeviceID=B0BC5AF1-829D-4C2" +
                    "C-9047-39CDD32E2138\tUUIDHash=11911350331370179513\tDeviceIDHash=5982430173149784503\tIFV=" +
                    "F834E974-5D2D-4F0D-A15F-2E714C415636\tAppPlatform=iOS\tOperatingSystem=ios\tAppFramework=N" +
                    "ATIVE\tAppVersionName=204\tAppBuildNumber=1523\tAppDebuggable=false\tKitVersion=3003000\tM" +
                    "anufacturer=Apple\tModel=iPhone 6s\tOriginalManufacturer=Apple\tOriginalModel=iPhone8,1\tO" +
                    "SVersion=12.1\tOSApiLevel=12\tScreenWidth=667\tScreenHeight=375\tScreenDPI=326\tScaleFacto" +
                    "r=2\tLimitAdTracking=false\tClientKitVersion=3003000\tKitBuildType=101\tKitBuildNumber=122" +
                    "85\tSendTimestamp=1545257667\tReceiveDate=2018-12-20\tReceiveTimestamp=1545257667\tSession" +
                    "ID=10000000003\tSessionType=SESSION_BACKGROUND\tDeviceIDSessionIDHash=9186804862522693737" +
                    "\tStartTime=2018-12-20 01:14:20\tStartTimestamp=1545257660\tStartTimeZone=10800\tRegionTime" +
                    "Zone=10800\tLocale=ru_RU\tLocationSource=MISSING\tLocationEnabled=true\tWifiAccessPointSta" +
                    "te=UNKNOWN\tConnectionType=CONN_WIFI\tNetworkType=LTE\tCountryCode=250\tOperatorID=99\tOpe" +
                    "ratorName=Beeline\tSimCards_CountriesCodes=[250]\tSimCards_OperatorsIDs=[99]\tSimCards_Ope" +
                    "ratorsNames=[\"Beeline\"]\tSimCards_AreRoaming=[0]\tSimCards_IccIDs=[\"\"]\tDeviceType=PHO" +
                    "NE\tEventID=335753216948518725\tEventNumber=1\tEventDate=2018-12-20\tEventDateTime=2018-12" +
                    "-20 01:14:20\tEventTimestamp=1545257660\tEventTimeZone=10800\tEventType=EVENT_CRASH\tEvent" +
                    "Source=sdk\tEventFirstOccurrence=undefined\tEventValueCrash=Incident Identifier: E3211827-" +
                    "5D2C-4DB6-B6F5-86907CE0C60A\\nCrashReporter Key:   ddac2b0066200e2f3105d5d84aeab1c85847e86" +
                    "d\\nHardware Model:      iPhone8,1\\nProcess:         Беру [1669]\\nPath:            /var/" +
                    "containers/Bundle/Application/0755B820-FF9E-42D9-9DF8-9BAA76FDDD9F/Беру.app/Беру\\nIdentif" +
                    "ier:      ru.yandex.blue.market\\nVersion:         1523 (204)\\nCode Type:       ARM-64\\n" +
                    "Parent Process:  ? [1]\\n\\nDate/Time:       2018-12-20 01:14:20.000 +0300\\nOS Version:  " +
                    "    iOS 12.1 (16B92)\\nReport Version:  104\\n\\nException Type:  EXC_CRASH (SIGABRT)\\nEx" +
                    "ception Codes: 0x00000000 at 0x0000000000000000\\nCrashed Thread:  0\\n\\nApplication Spec" +
                    "ific Information:\\n*** Terminating app due to uncaught exception \\'NSRangeException\\', " +
                    "reason: \\'*** -[__NSArray0 objectAtIndex:]: index 2 beyond bounds for empty NSArray\\'\\" +
                    "n\\nThread 0 Crashed:\\n0   CoreFoundation                  0x000000018d5ffea0 0x18d4e4000" +
                    " + 1162912 (<redacted> + 228)\\n1   libobjc.A.dylib                 0x000000018c7d1a40 0x1" +
                    "8c7cb000 + 27200 (objc_exception_throw + 56)\\n2   CoreFoundation                  0x00000" +
                    "0018d511a90 0x18d4e4000 + 187024 (<redacted> + 108)\\n3   –ë–µ—Ä—É                        " +
                    "0x00000001048f30ac 0x10469c000 + 2453676\\n4   UIKitCore                       0x00000001b" +
                    "a8ac540 0x1b9d9d000 + 11597120 (<redacted> + 684)\\n5   UIKitCore                       0x" +
                    "00000001ba8aca88 0x1b9d9d000 + 11598472 (<redacted> + 80)\\n6   UIKitCore                 " +
                    "      0x00000001ba878160 0x1b9d9d000 + 11383136 (<redacted> + 2256)\\n7   UIKitCore       " +
                    "                0x00000001ba876684 0x1b9d9d000 + 11376260 (<redacted> + 224)\\n8   UIKitCo" +
                    "re                       0x00000001ba8921cc 0x1b9d9d000 + 11489740 (<redacted> + 432)\\n9 " +
                    "  –ë–µ—Ä—É                        0x00000001048f6650 0x10469c000 + 2467408\\n10  UIKitCore" +
                    "                       0x00000001bab1e8b4 0x1b9d9d000 + 14162100 (<redacted> + 608)\\n11  " +
                    "UIKitCore                       0x00000001bab1eed0 0x1b9d9d000 + 14163664 (<redacted> + 60" +
                    ")\\n12  –ë–µ—Ä—É                        0x00000001048f64f0 0x10469c000 + 2467056\\n13  –ë–" +
                    "µ—Ä—É                        0x00000001048f562c 0x10469c000 + 2463276\\n14  –ë–µ—Ä—É      " +
                    "                  0x00000001046a7f50 0x10469c000 + 48976\\n15  –ë–µ—Ä—É                   " +
                    "     0x00000001049637a0 0x10469c000 + 2914208\\n16  libdispatch.dylib               0x0000" +
                    "00018d0396c8 0x18cfd9000 + 394952 (<redacted> + 24)\\n17  libdispatch.dylib               " +
                    "0x000000018d03a484 0x18cfd9000 + 398468 (<redacted> + 16)\\n18  libdispatch.dylib         " +
                    "      0x000000018cfe69b4 0x18cfd9000 + 55732 (<redacted> + 1068)\\n19  CoreFoundation     " +
                    "             0x000000018d58fdd0 0x18d4e4000 + 703952 (<redacted> + 12)\\n20  CoreFoundatio" +
                    "n                  0x000000018d58ac98 0x18d4e4000 + 683160 (<redacted> + 1964)\\n21  CoreF" +
                    "oundation                  0x000000018d58a1cc 0x18d4e4000 + 680396 (CFRunLoopRunSpecific +" +
                    " 436)\\n22  GraphicsServices                0x000000018f801584 0x18f7f6000 + 46468 (GSEven" +
                    "tRunModal + 100)\\n23  UIKitCore                       0x00000001ba685054 0x1b9d9d000 + 93" +
                    "38964 (UIApplicationMain + 212)\\n24  –ë–µ—Ä—É                        0x0000000104a563f8 0" +
                    "x10469c000 + 3908600\\n25  libdyld.dylib                   0x000000018d04abb4 0x18d04a000 " +
                    "+ 2996 (<redacted> + 4)\\n\\nThread 1:\\n0   libsystem_kernel.dylib          0x000000018d1" +
                    "97428 0x18d174000 + 144424 (__semwait_signal + 8)\\n1   libsystem_c.dylib               0x" +
                    "000000018d10c5d0 0x18d097000 + 480720 (nanosleep + 212)\\n2   libsystem_c.dylib           " +
                    "    0x000000018d10c3cc 0x18d097000 + 480204 (sleep + 44)\\n3   –ë–µ—Ä—É                   " +
                    "     0x00000001056c8708 0x10469c000 + 16959240 (_yandex_impl___ZN5boost13serialization16si" +
                    "ngleton_module8get_lockEv + 6189008)\\n4   libsystem_pthread.dylib         0x000000018d21b" +
                    "2ac 0x18d210000 + 45740 (<redacted> + 128)\\n5   libsystem_pthread.dylib         0x0000000" +
                    "18d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThread 2 name:  KSCrash Exception" +
                    " Handler (Secondary)\\nThread 2:\\n0   libsystem_kernel.dylib          0x000000018d18bed0 " +
                    "0x18d174000 + 98000 (mach_msg_trap + 8)\\n1   libsystem_kernel.dylib          0x000000018d" +
                    "18b3a8 0x18d174000 + 95144 (mach_msg + 72)\\n2   libsystem_kernel.dylib          0x0000000" +
                    "18d1876b8 0x18d174000 + 79544 (thread_suspend + 84)\\n3   –ë–µ—Ä—É                        " +
                    "0x00000001056ccfe8 0x10469c000 + 16977896 (_yandex_impl___ZN5boost13serialization16singlet" +
                    "on_module8get_lockEv + 6207664)\\n4   libsystem_pthread.dylib         0x000000018d21b2ac 0" +
                    "x18d210000 + 45740 (<redacted> + 128)\\n5   libsystem_pthread.dylib         0x000000018d21" +
                    "b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThread 3 name:  KSCrash Exception Hand" +
                    "ler (Primary)\\nThread 3:\\n0   libsystem_kernel.dylib          0x000000018d18bed0 0x18d17" +
                    "4000 + 98000 (mach_msg_trap + 8)\\n1   libsystem_kernel.dylib          0x000000018d18b3a8 " +
                    "0x18d174000 + 95144 (mach_msg + 72)\\n2   –ë–µ—Ä—É                        0x00000001056cd0" +
                    "14 0x10469c000 + 16977940 (_yandex_impl___ZN5boost13serialization16singleton_module8get_lo" +
                    "ckEv + 6207708)\\n3   libsystem_pthread.dylib         0x000000018d21b2ac 0x18d210000 + 457" +
                    "40 (<redacted> + 128)\\n4   libsystem_pthread.dylib         0x000000018d21b20c 0x18d210000" +
                    " + 45580 (_pthread_start + 48)\\n\\nThread 4 name:  com.apple.NSURLConnectionLoader\\nThre" +
                    "ad 4:\\n0   libsystem_kernel.dylib          0x000000018d18bed0 0x18d174000 + 98000 (mach_m" +
                    "sg_trap + 8)\\n1   libsystem_kernel.dylib          0x000000018d18b3a8 0x18d174000 + 95144 " +
                    "(mach_msg + 72)\\n2   CoreFoundation                  0x000000018d58fbc4 0x18d4e4000 + 703" +
                    "428 (<redacted> + 236)\\n3   CoreFoundation                  0x000000018d58aa60 0x18d4e400" +
                    "0 + 682592 (<redacted> + 1396)\\n4   CoreFoundation                  0x000000018d58a1cc 0x" +
                    "18d4e4000 + 680396 (CFRunLoopRunSpecific + 436)\\n5   CFNetwork                       0x00" +
                    "0000018dbab834 0x18dbaa000 + 6196 (<redacted> + 212)\\n6   Foundation                     " +
                    " 0x000000018e0b21ac 0x18df77000 + 1290668 (<redacted> + 1040)\\n7   libsystem_pthread.dyli" +
                    "b         0x000000018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n8   libsystem_pthrea" +
                    "d.dylib         0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThread 5" +
                    " name:  com.apple.uikit.eventfetch-thread\\nThread 5:\\n0   libsystem_kernel.dylib        " +
                    "  0x000000018d18bed0 0x18d174000 + 98000 (mach_msg_trap + 8)\\n1   libsystem_kernel.dylib " +
                    "         0x000000018d18b3a8 0x18d174000 + 95144 (mach_msg + 72)\\n2   CoreFoundation      " +
                    "            0x000000018d58fbc4 0x18d4e4000 + 703428 (<redacted> + 236)\\n3   CoreFoundatio" +
                    "n                  0x000000018d58aa60 0x18d4e4000 + 682592 (<redacted> + 1396)\\n4   CoreF" +
                    "oundation                  0x000000018d58a1cc 0x18d4e4000 + 680396 (CFRunLoopRunSpecific +" +
                    " 436)\\n5   Foundation                      0x000000018df7f404 0x18df77000 + 33796 (<redac" +
                    "ted> + 300)\\n6   Foundation                      0x000000018df7f2b0 0x18df77000 + 33456 (" +
                    "<redacted> + 148)\\n7   UIKitCore                       0x00000001ba772808 0x1b9d9d000 + 1" +
                    "0311688 (<redacted> + 136)\\n8   Foundation                      0x000000018e0b21ac 0x18df" +
                    "77000 + 1290668 (<redacted> + 1040)\\n9   libsystem_pthread.dylib         0x000000018d21b2" +
                    "ac 0x18d210000 + 45740 (<redacted> + 128)\\n10  libsystem_pthread.dylib         0x00000001" +
                    "8d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThread 6 name:  JavaScriptCore bma" +
                    "lloc scavenger\\nThread 6:\\n0   libsystem_kernel.dylib          0x000000018d196f0c 0x18d1" +
                    "74000 + 143116 (__psynch_cvwait + 8)\\n1   libsystem_pthread.dylib         0x000000018d213" +
                    "cd8 0x18d210000 + 15576 (<redacted> + 636)\\n2   libc++.1.dylib                  0x0000000" +
                    "18c7644d0 0x18c75c000 + 34000 (std::__1::condition_variable::wait(std::__1::unique_lock<st" +
                    "d::__1::mutex>&) + 24)\\n3   JavaScriptCore                  0x00000001949329b8 0x1948d600" +
                    "0 + 379320 (<redacted> + 104)\\n4   JavaScriptCore                  0x0000000194936aac 0x1" +
                    "948d6000 + 395948 (<redacted> + 176)\\n5   JavaScriptCore                  0x0000000194936" +
                    "1e0 0x1948d6000 + 393696 (<redacted> + 12)\\n6   JavaScriptCore                  0x0000000" +
                    "194937c8c 0x1948d6000 + 400524 (<redacted> + 40)\\n7   libsystem_pthread.dylib         0x0" +
                    "00000018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n8   libsystem_pthread.dylib      " +
                    "   0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThread 7 name:  WebTh" +
                    "read\\nThread 7:\\n0   libsystem_kernel.dylib          0x000000018d18bed0 0x18d174000 + 98" +
                    "000 (mach_msg_trap + 8)\\n1   libsystem_kernel.dylib          0x000000018d18b3a8 0x18d1740" +
                    "00 + 95144 (mach_msg + 72)\\n2   CoreFoundation                  0x000000018d58fbc4 0x18d4" +
                    "e4000 + 703428 (<redacted> + 236)\\n3   CoreFoundation                  0x000000018d58aa60" +
                    " 0x18d4e4000 + 682592 (<redacted> + 1396)\\n4   CoreFoundation                  0x00000001" +
                    "8d58a1cc 0x18d4e4000 + 680396 (CFRunLoopRunSpecific + 436)\\n5   WebCore                  " +
                    "       0x00000001963eb52c 0x195f3b000 + 4916524 (<redacted> + 592)\\n6   libsystem_pthread" +
                    ".dylib         0x000000018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n7   libsystem_p" +
                    "thread.dylib         0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThr" +
                    "ead 8 name:  0#BgLow\\nThread 8:\\n0   libsystem_kernel.dylib          0x000000018d196f0c " +
                    "0x18d174000 + 143116 (__psynch_cvwait + 8)\\n1   libsystem_pthread.dylib         0x0000000" +
                    "18d213cd8 0x18d210000 + 15576 (<redacted> + 636)\\n2   libc++.1.dylib                  0x0" +
                    "00000018c7644d0 0x18c75c000 + 34000 (std::__1::condition_variable::wait(std::__1::unique_l" +
                    "ock<std::__1::mutex>&) + 24)\\n3   –ë–µ—Ä—É                        0x00000001056e90e8 0x10" +
                    "469c000 + 17092840 (_yandex_impl___ZN5boost13serialization16singleton_module8get_lockEv + " +
                    "6322608)\\n4   –ë–µ—Ä—É                        0x00000001056e8ef4 0x10469c000 + 17092340 (" +
                    "_yandex_impl___ZN5boost13serialization16singleton_module8get_lockEv + 6322108)\\n5   –ë–µ—" +
                    "Ä—É                        0x00000001056ead7c 0x10469c000 + 17100156 (_yandex_impl___ZN5bo" +
                    "ost13serialization16singleton_module8get_lockEv + 6329924)\\n6   libsystem_pthread.dylib  " +
                    "vateFrameworks/EmailCore.framework/EmailCore\\n       0x1b1e71000 -        0x1b1e82fff  li" +
                    "bGSFontCache.dylib arm64  <85d5e1f6818e3cb789232339838e1ee9> /System/Library/PrivateFramew" +
                    "orks/FontServices.framework/libGSFontCache.dylib\\n       0x1b1e83000 -        0x1b1eb5fff" +
                    "  libTrueTypeScaler.dylib arm64  <5ce42bac7a6e366a8122f2b64931e4c7> /System/Library/Privat" +
                    "eFrameworks/FontServices.framework/libTrueTypeScaler.dylib\\n       0x1b28e1000 -        0" +
                    "x1b28e1fff  libmetal_timestamp.dylib arm64  <449f125aff6c3eb88930a13d4a4aee08> /System/Lib" +
                    "rary/PrivateFrameworks/GPUCompiler.framework/Libraries/libmetal_timestamp.dylib\\n       0" +
                    "x1b39b8000 -        0x1b39bcfff  InternationalSupport arm64  <7a90f1cc4432370a817487085008" +
                    "012b> /System/Library/PrivateFrameworks/InternationalSupport.framework/InternationalSuppor" +
                    "t\\n       0x1b4d8a000 -        0x1b4d96fff  PersonaUI arm64  <c5f00611a222383a8d5f6a4036b" +
                    "78060> /System/Library/PrivateFrameworks/PersonaUI.framework/PersonaUI\\n       0x1b51d200" +
                    "0 -        0x1b51dcfff  SignpostCollection arm64  <e8f8d054030e3960867be808d4a02543> /Syst" +
                    "em/Library/PrivateFrameworks/SignpostCollection.framework/SignpostCollection\\n       0x1b" +
                    "585a000 -        0x1b5860fff  TextInputUI arm64  <dab1f33343533cb9a4910f6f4506abf5> /Syste" +
                    "m/Library/PrivateFrameworks/TextInputUI.framework/TextInputUI\\n       0x1b5d8e000 -      " +
                    "  0x1b5d91fff  XCTTargetBootstrap arm64  <37a7b5cb51f43833b73284aa6da2b00c> /System/Librar" +
                    "y/PrivateFrameworks/XCTTargetBootstrap.framework/XCTTargetBootstrap\\n       0x1b5dd4000 -" +
                    "        0x1b5de6fff  libEDR arm64  <0e484330f1ea3c9aa653387fdd8e7eea> /System/Library/Priv" +
                    "ateFrameworks/libEDR.framework/libEDR\\n       0x1b6839000 -        0x1b6846fff  libMobile" +
                    "GestaltExtensions.dylib arm64  <7bb4ccf8882d3955aa725357af0cee21> /usr/lib/libMobileGestal" +
                    "tExtensions.dylib\\n       0x1b6958000 -        0x1b6958fff  libcharset.1.dylib arm64  <ea" +
                    "ce303743a83b0883cd2da45d81bb89> /usr/lib/libcharset.1.dylib\\n       0x1b7426000 -        " +
                    "0x1b7427fff  libsandbox.1.dylib arm64  <3954b72ed6543ce6a716f260d91c03b5> /usr/lib/libsand" +
                    "box.1.dylib\\n       0x1b7466000 -        0x1b7467fff  liblog_network.dylib arm64  <da638a" +
                    "55600c3bf59eb90fe80bcc5f2d> /usr/lib/log/liblog_network.dylib\\n       0x1b7555000 -      " +
                    "  0x1b755ffff  AuthenticationServices arm64  <59a53ebd094532d3a895238a5143a57f> /System/Li" +
                    "brary/Frameworks/AuthenticationServices.framework/AuthenticationServices\\n       0x1b75e7" +
                    "000 -        0x1b773bfff  CoreServices arm64  <59408d675a4733f1952055efefb431cb> /System/L" +
                    "ibrary/Frameworks/CoreServices.framework/CoreServices\\n       0x1b7768000 -        0x1b77" +
                    "81fff  MPSRayIntersector arm64  <45591bc292513bde96378423e15be33d> /System/Library/Framewo" +
                    "rks/MetalPerformanceShaders.framework/Frameworks/MPSRayIntersector.framework/MPSRayInterse" +
                    "ctor\\n       0x1b77b1000 -        0x1b78ecfff  Network arm64  <676dec9679353807ba5fffaee0" +
                    "721745> /System/Library/Frameworks/Network.framework/Network\\n       0x1b78f8000 -       " +
                    " 0x1b7906fff  ANEServices arm64  <dba54c743b0b336babeb9b33ff02a00b> /System/Library/Privat" +
                    "eFrameworks/ANEServices.framework/ANEServices\\n       0x1b790b000 -        0x1b790ffff  A" +
                    "SEProcessing arm64  <6beee62c25433808848e9f61e22f04f0> /System/Library/PrivateFrameworks/A" +
                    "SEProcessing.framework/ASEProcessing\\n       0x1b7910000 -        0x1b791bfff  AXCoreUtil" +
                    "ities arm64  <b333d651debb313ab4c880e8acf5ffb4> /System/Library/PrivateFrameworks/AXCoreUt" +
                    "ilities.framework/AXCoreUtilities\\n       0x1b7b95000 -        0x1b7cd2fff  AppleMediaSer" +
                    "vices arm64  <87437326183836c19acfcb50f4700bcf> /System/Library/PrivateFrameworks/AppleMed" +
                    "iaServices.framework/AppleMediaServices\\n       0x1b7cd3000 -        0x1b7ce2fff  AppleNe" +
                    "uralEngine arm64  <6916f5470c053c5baeda7d648f898e60> /System/Library/PrivateFrameworks/App" +
                    "leNeuralEngine.framework/AppleNeuralEngine\\n       0x1b7e6c000 -        0x1b7ea2fff  C2 a" +
                    "rm64  <8098f48338fc316abd9f88fafda6a7e2> /System/Library/PrivateFrameworks/C2.framework/C" +
                    "2\\n       0x1b8031000 -        0x1b803cfff  Categories arm64  <fd2162e5f1b13e0c9686fa2500" +
                    "b1d48c> /System/Library/PrivateFrameworks/Categories.framework/Categories\\n       0x1b804" +
                    "4000 -        0x1b8165fff  ConfigurationEngineModel arm64  <9e9dc40d171737589f6c3822375b57" +
                    "75> /System/Library/PrivateFrameworks/ConfigurationEngineModel.framework/ConfigurationEngi" +
                    "neModel\\n       0x1b8284000 -        0x1b829ffff  DoNotDisturb arm64  <f48569dc921d37378e" +
                    "da8254d026b633> /System/Library/PrivateFrameworks/DoNotDisturb.framework/DoNotDisturb\\n  " +
                    "     0x1b83d7000 -        0x1b842ffff  DocumentManager arm64  <07a9044f8ef33f6faaf3630b367" +
                    "01fdd> /System/Library/PrivateFrameworks/DocumentManager.framework/DocumentManager\\n     " +
                    "  0x1b8512000 -        0x1b8516fff  IdleTimerServices arm64  <96d09b21c501360bb8af4749d990" +
                    "e1da> /System/Library/PrivateFrameworks/IdleTimerServices.framework/IdleTimerServices\\n  " +
                    "     0x1b8552000 -        0x1b858efff  LocalAuthenticationPrivateUI arm64  <547985e62fc532" +
                    "22b8c4c173608ac430> /System/Library/PrivateFrameworks/LocalAuthenticationPrivateUI.framewo" +
                    "rk/LocalAuthenticationPrivateUI\\n       0x1b858f000 -        0x1b85bafff  MetadataUtiliti" +
                    "es arm64  <34c6506e802b37769d4a1a038547e721> /System/Library/PrivateFrameworks/MetadataUti" +
                    "lities.framework/MetadataUtilities\\n       0x1b8b2b000 -        0x1b8b3efff  NewsAnalytic" +
                    "sUpload arm64  <8a25548276bd3e42ba38115a51afc844> /System/Library/PrivateFrameworks/NewsAn" +
                    "alyticsUpload.framework/NewsAnalyticsUpload\\n       0x1b8b41000 -        0x1b8b92fff  OTS" +
                    "VG arm64  <8f37fe4148de377e8a58c91ccc356dd3> /System/Library/PrivateFrameworks/OTSVG.frame" +
                    "work/OTSVG\\n       0x1b8b93000 -        0x1b8bb5fff  OnBoardingKit arm64  <ac96657973dc3e" +
                    "7eaeb1241a00087982> /System/Library/PrivateFrameworks/OnBoardingKit.framework/OnBoardingKi" +
                    "t\\n       0x1b8c8e000 -        0x1b8cedfff  PhotoFoundation arm64  <d652b829d1e93ead9256f" +
                    "e518277ef8e> /System/Library/PrivateFrameworks/PhotoFoundation.framework/PhotoFoundation\\" +
                    "n       0x1b8d41000 -        0x1b8d86fff  PhotosImagingFoundation arm64  <b80937ab5b493a72" +
                    "86dba70152bea6d5> /System/Library/PrivateFrameworks/PhotosImagingFoundation.framework/Phot" +
                    "osImagingFoundation\\n       0x1b8dbc000 -        0x1b8dc5fff  PrototypeToolsUI arm64  <8a" +
                    "d3a202ba4632eebfa535f1f95b9382> /System/Library/PrivateFrameworks/PrototypeToolsUI.framewo" +
                    "rk/PrototypeToolsUI\\n       0x1b8dc6000 -        0x1b8dd9fff  QuickLookSupport arm64  <6e" +
                    "e115a760143d45b89bdfdc88e741a8> /System/Library/PrivateFrameworks/QuickLookSupport.framewo" +
                    "rk/QuickLookSupport\\n       0x1b8ddc000 -        0x1b8e3cfff  ROCKit arm64  <41c33276d267" +
                    "315f92b97e0aa9c744a4> /System/Library/PrivateFrameworks/ROCKit.framework/ROCKit\\n       0" +
                    "x1b8fae000 -        0x1b8fdffff  RemoteConfiguration arm64  <53b23751fe773357a723ba588c39b" +
                    "3d0> /System/Library/PrivateFrameworks/RemoteConfiguration.framework/RemoteConfiguration\\" +
                    "n       0x1b8fef000 -        0x1b904bfff  RemoteManagement arm64  <cd0a48d857cd3aef861de10" +
                    "16c170702> /System/Library/PrivateFrameworks/RemoteManagement.framework/RemoteManagement\\" +
                    "n       0x1b904c000 -        0x1b905efff  RemoteTextInput arm64  <21a7f85c5e6a3896991ac641" +
                    "6c5c74dd> /System/Library/PrivateFrameworks/RemoteTextInput.framework/RemoteTextInput\\n  " +
                    "     0x1b9087000 -        0x1b9121fff  SampleAnalysis arm64  <8a12e6cd83fb3b5c81e7b52fae6e" +
                    "a396> /System/Library/PrivateFrameworks/SampleAnalysis.framework/SampleAnalysis\\n       0" +
                    "x1b91fc000 -        0x1b91fcfff  SignpostNotification arm64  <5e421f577ebf3de6a7987b67a434" +
                    "0117> /System/Library/PrivateFrameworks/SignpostNotification.framework/SignpostNotificatio" +
                    "n\\n       0x1b926b000 -        0x1b9273fff  StatsKit arm64  <676c03e847ff36248d82878812f6" +
                    "2688> /System/Library/PrivateFrameworks/StatsKit.framework/StatsKit\\n       0x1b9d9d000 -" +
                    "        0x1baecdfff  UIKitCore arm64  <ab782a031ee43c979f4c15cb9751acb3> /System/Library/P" +
                    "rivateFrameworks/UIKitCore.framework/UIKitCore\\n       0x1baece000 -        0x1baed9fff  " +
                    "UIKitServices arm64  <7f48e5cb504b386892b7dee2480d9d75> /System/Library/PrivateFrameworks/" +
                    "UIKitServices.framework/UIKitServices\\n       0x1baeda000 -        0x1baee1fff  URLFormat" +
                    "ting arm64  <3a3381bad32a33d3bd9164ab72dcbcad> /System/Library/PrivateFrameworks/URLFormat" +
                    "ting.framework/URLFormatting\\n       0x1baee2000 -        0x1baf04fff  UsageTracking arm6" +
                    "4  <7689edf41080312b8b870d4a495aaea5> /System/Library/PrivateFrameworks/UsageTracking.fram" +
                    "ework/UsageTracking\\n\\nExtra Information:\\n\\nApplication Stats:\\n{\\n    \"active_tim" +
                    "e_since_last_crash\": 2225.62,\\n    \"active_time_since_launch\": 2225.62,\\n    \"applic" +
                    "ation_active\": true,\\n    \"application_in_foreground\": true,\\n    \"background_time_s" +
                    "ince_last_crash\": 27.1647,\\n    \"background_time_since_launch\": 27.1647,\\n    \"launc" +
                    "hes_since_last_crash\": 1,\\n    \"sessions_since_last_crash\": 4,\\n    \"sessions_since_" +
                    "launch\": 4\\n}\\n\\nCrashDoctor Diagnosis: Application threw exception NSRangeException: " +
                    "*** -[__NSArray0 objectAtIndex:]: index 2 beyond bounds for empty NSArray\\n\tCrashID=1532" +
                    "3359407173727777\tCrashGroupID=1710958146103909148\tClickDate=1970-01-01\tRegionID=43\tApp" +
                    "ID=ru.yandex.blue.market\tClientIP=::ffff:46.191.225.6\tClientIPHash=16807348798278655505" +
                    "\tClientPort=60242\tSex=2\tAge=45\tAccountID=305155963\tAccountType=login\tDeduplicationEna" +
                    "bled=1\tProfileAttributeVersion=25925121537474561\tCustomAttribute_Types=[3]\tCustomAttrib" +
                    "ute_Ids=[48]\tCustomAttribute_IdSetFlags=[1]\tCustomAttribute_StringValues=[\"\"]\tCustomA" +
                    "ttribute_NumberValues=[1]\tCustomAttribute_BoolValues=[0]\tCustomAttribute_ResetFlags=[0]" +
                    "\tCustomAttribute_Versions=[25925121537474561]";

        String[] tags = {};
        String[] keys = {};
        String[] values = {};

        checker.check(
            line,
            new Date(1545257660000L),
            "market_front_bluetouch", checker.getHost(), "", "EXC_CRASH (SIGABRT)",
            "critical", "Application threw exception NSRangeException: *** -[__NSArray0 objectAtIndex:]: index 2 beyond " +
                "bounds for empty NSArray",
                    "Incident Identifier: E3211827-5D2C-4DB6-B6F5-86907CE0C60A\\nCrashReporter Key:   ddac2b00" +
                    "66200e2f3105d5d84aeab1c85847e86d\\nHardware Model:      iPhone8,1\\nProcess:         Беру " +
                    "[1669]\\nPath:            /var/containers/Bundle/Application/0755B820-FF9E-42D9-9DF8-9BAA7" +
                    "6FDDD9F/Беру.app/Беру\\nIdentifier:      ru.yandex.blue.market\\nVersion:         1523 (20" +
                    "4)\\nCode Type:       ARM-64\\nParent Process:  ? [1]\\n\\nDate/Time:       2018-12-20 01:" +
                    "14:20.000 +0300\\nOS Version:      iOS 12.1 (16B92)\\nReport Version:  104\\n\\nException " +
                    "Type:  EXC_CRASH (SIGABRT)\\nException Codes: 0x00000000 at 0x0000000000000000\\nCrashed T" +
                    "hread:  0\\n\\nApplication Specific Information:\\n*** Terminating app due to uncaught exc" +
                    "eption \\'NSRangeException\\', reason: \\'*** -[__NSArray0 objectAtIndex:]: index 2 beyond" +
                    " bounds for empty NSArray\\'\\n\\nThread 0 Crashed:\\n0   CoreFoundation                  " +
                    "0x000000018d5ffea0 0x18d4e4000 + 1162912 (<redacted> + 228)\\n1   libobjc.A.dylib         " +
                    "        0x000000018c7d1a40 0x18c7cb000 + 27200 (objc_exception_throw + 56)\\n2   CoreFound" +
                    "ation                  0x000000018d511a90 0x18d4e4000 + 187024 (<redacted> + 108)\\n3   –ë" +
                    "–µ—Ä—É                        0x00000001048f30ac 0x10469c000 + 2453676\\n4   UIKitCore    " +
                    "                   0x00000001ba8ac540 0x1b9d9d000 + 11597120 (<redacted> + 684)\\n5   UIKi" +
                    "tCore                       0x00000001ba8aca88 0x1b9d9d000 + 11598472 (<redacted> + 80)\\n" +
                    "6   UIKitCore                       0x00000001ba878160 0x1b9d9d000 + 11383136 (<redacted> " +
                    "+ 2256)\\n7   UIKitCore                       0x00000001ba876684 0x1b9d9d000 + 11376260 (<" +
                    "redacted> + 224)\\n8   UIKitCore                       0x00000001ba8921cc 0x1b9d9d000 + 11" +
                    "489740 (<redacted> + 432)\\n9   –ë–µ—Ä—É                        0x00000001048f6650 0x10469" +
                    "c000 + 2467408\\n10  UIKitCore                       0x00000001bab1e8b4 0x1b9d9d000 + 1416" +
                    "2100 (<redacted> + 608)\\n11  UIKitCore                       0x00000001bab1eed0 0x1b9d9d0" +
                    "00 + 14163664 (<redacted> + 60)\\n12  –ë–µ—Ä—É                        0x00000001048f64f0 0" +
                    "x10469c000 + 2467056\\n13  –ë–µ—Ä—É                        0x00000001048f562c 0x10469c000 " +
                    "+ 2463276\\n14  –ë–µ—Ä—É                        0x00000001046a7f50 0x10469c000 + 48976\\n1" +
                    "5  –ë–µ—Ä—É                        0x00000001049637a0 0x10469c000 + 2914208\\n16  libdispa" +
                    "tch.dylib               0x000000018d0396c8 0x18cfd9000 + 394952 (<redacted> + 24)\\n17  li" +
                    "bdispatch.dylib               0x000000018d03a484 0x18cfd9000 + 398468 (<redacted> + 16)\\n" +
                    "18  libdispatch.dylib               0x000000018cfe69b4 0x18cfd9000 + 55732 (<redacted> + 1" +
                    "068)\\n19  CoreFoundation                  0x000000018d58fdd0 0x18d4e4000 + 703952 (<redac" +
                    "ted> + 12)\\n20  CoreFoundation                  0x000000018d58ac98 0x18d4e4000 + 683160 (" +
                    "<redacted> + 1964)\\n21  CoreFoundation                  0x000000018d58a1cc 0x18d4e4000 + " +
                    "680396 (CFRunLoopRunSpecific + 436)\\n22  GraphicsServices                0x000000018f8015" +
                    "84 0x18f7f6000 + 46468 (GSEventRunModal + 100)\\n23  UIKitCore                       0x000" +
                    "00001ba685054 0x1b9d9d000 + 9338964 (UIApplicationMain + 212)\\n24  –ë–µ—Ä—É              " +
                    "          0x0000000104a563f8 0x10469c000 + 3908600\\n25  libdyld.dylib                   0" +
                    "x000000018d04abb4 0x18d04a000 + 2996 (<redacted> + 4)\\n\\nThread 1:\\n0   libsystem_kerne" +
                    "l.dylib          0x000000018d197428 0x18d174000 + 144424 (__semwait_signal + 8)\\n1   libs" +
                    "ystem_c.dylib               0x000000018d10c5d0 0x18d097000 + 480720 (nanosleep + 212)\\n2 " +
                    "  libsystem_c.dylib               0x000000018d10c3cc 0x18d097000 + 480204 (sleep + 44)\\n3" +
                    "   –ë–µ—Ä—É                        0x00000001056c8708 0x10469c000 + 16959240 (_yandex_impl" +
                    "___ZN5boost13serialization16singleton_module8get_lockEv + 6189008)\\n4   libsystem_pthread" +
                    ".dylib         0x000000018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n5   libsystem_p" +
                    "thread.dylib         0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThr" +
                    "ead 2 name:  KSCrash Exception Handler (Secondary)\\nThread 2:\\n0   libsystem_kernel.dyli" +
                    "b          0x000000018d18bed0 0x18d174000 + 98000 (mach_msg_trap + 8)\\n1   libsystem_kern" +
                    "el.dylib          0x000000018d18b3a8 0x18d174000 + 95144 (mach_msg + 72)\\n2   libsystem_k" +
                    "ernel.dylib          0x000000018d1876b8 0x18d174000 + 79544 (thread_suspend + 84)\\n3   –ë" +
                    "–µ—Ä—É                        0x00000001056ccfe8 0x10469c000 + 16977896 (_yandex_impl___ZN" +
                    "5boost13serialization16singleton_module8get_lockEv + 6207664)\\n4   libsystem_pthread.dyli" +
                    "b         0x000000018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n5   libsystem_pthrea" +
                    "d.dylib         0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThread 3" +
                    " name:  KSCrash Exception Handler (Primary)\\nThread 3:\\n0   libsystem_kernel.dylib      " +
                    "    0x000000018d18bed0 0x18d174000 + 98000 (mach_msg_trap + 8)\\n1   libsystem_kernel.dyli" +
                    "b          0x000000018d18b3a8 0x18d174000 + 95144 (mach_msg + 72)\\n2   –ë–µ—Ä—É          " +
                    "              0x00000001056cd014 0x10469c000 + 16977940 (_yandex_impl___ZN5boost13serializ" +
                    "ation16singleton_module8get_lockEv + 6207708)\\n3   libsystem_pthread.dylib         0x0000" +
                    "00018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n4   libsystem_pthread.dylib         " +
                    "0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThread 4 name:  com.appl" +
                    "e.NSURLConnectionLoader\\nThread 4:\\n0   libsystem_kernel.dylib          0x000000018d18be" +
                    "d0 0x18d174000 + 98000 (mach_msg_trap + 8)\\n1   libsystem_kernel.dylib          0x0000000" +
                    "18d18b3a8 0x18d174000 + 95144 (mach_msg + 72)\\n2   CoreFoundation                  0x0000" +
                    "00018d58fbc4 0x18d4e4000 + 703428 (<redacted> + 236)\\n3   CoreFoundation                 " +
                    " 0x000000018d58aa60 0x18d4e4000 + 682592 (<redacted> + 1396)\\n4   CoreFoundation         " +
                    "         0x000000018d58a1cc 0x18d4e4000 + 680396 (CFRunLoopRunSpecific + 436)\\n5   CFNetw" +
                    "ork                       0x000000018dbab834 0x18dbaa000 + 6196 (<redacted> + 212)\\n6   F" +
                    "oundation                      0x000000018e0b21ac 0x18df77000 + 1290668 (<redacted> + 1040" +
                    ")\\n7   libsystem_pthread.dylib         0x000000018d21b2ac 0x18d210000 + 45740 (<redacted>" +
                    " + 128)\\n8   libsystem_pthread.dylib         0x000000018d21b20c 0x18d210000 + 45580 (_pth" +
                    "read_start + 48)\\n\\nThread 5 name:  com.apple.uikit.eventfetch-thread\\nThread 5:\\n0   " +
                    "libsystem_kernel.dylib          0x000000018d18bed0 0x18d174000 + 98000 (mach_msg_trap + 8" +
                    ")\\n1   libsystem_kernel.dylib          0x000000018d18b3a8 0x18d174000 + 95144 (mach_msg +" +
                    " 72)\\n2   CoreFoundation                  0x000000018d58fbc4 0x18d4e4000 + 703428 (<redac" +
                    "ted> + 236)\\n3   CoreFoundation                  0x000000018d58aa60 0x18d4e4000 + 682592 " +
                    "(<redacted> + 1396)\\n4   CoreFoundation                  0x000000018d58a1cc 0x18d4e4000 +" +
                    " 680396 (CFRunLoopRunSpecific + 436)\\n5   Foundation                      0x000000018df7f" +
                    "404 0x18df77000 + 33796 (<redacted> + 300)\\n6   Foundation                      0x0000000" +
                    "18df7f2b0 0x18df77000 + 33456 (<redacted> + 148)\\n7   UIKitCore                       0x0" +
                    "0000001ba772808 0x1b9d9d000 + 10311688 (<redacted> + 136)\\n8   Foundation                " +
                    "      0x000000018e0b21ac 0x18df77000 + 1290668 (<redacted> + 1040)\\n9   libsystem_pthread" +
                    ".dylib         0x000000018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n10  libsystem_p" +
                    "thread.dylib         0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start + 48)\\n\\nThr" +
                    "ead 6 name:  JavaScriptCore bmalloc scavenger\\nThread 6:\\n0   libsystem_kernel.dylib    " +
                    "      0x000000018d196f0c 0x18d174000 + 143116 (__psynch_cvwait + 8)\\n1   libsystem_pthrea" +
                    "d.dylib         0x000000018d213cd8 0x18d210000 + 15576 (<redacted> + 636)\\n2   libc++.1.d" +
                    "ylib                  0x000000018c7644d0 0x18c75c000 + 34000 (std::__1::condition_variable" +
                    "::wait(std::__1::unique_lock<std::__1::mutex>&) + 24)\\n3   JavaScriptCore                " +
                    "  0x00000001949329b8 0x1948d6000 + 379320 (<redacted> + 104)\\n4   JavaScriptCore         " +
                    "         0x0000000194936aac 0x1948d6000 + 395948 (<redacted> + 176)\\n5   JavaScriptCore  " +
                    "                0x00000001949361e0 0x1948d6000 + 393696 (<redacted> + 12)\\n6   JavaScript" +
                    "Core                  0x0000000194937c8c 0x1948d6000 + 400524 (<redacted> + 40)\\n7   libs" +
                    "ystem_pthread.dylib         0x000000018d21b2ac 0x18d210000 + 45740 (<redacted> + 128)\\n8 " +
                    "  libsystem_pthread.dylib         0x000000018d21b20c 0x18d210000 + 45580 (_pthread_start +" +
                    " 48)\\n\\nThread 7 name:  WebThread\\nThread 7:\\n0   libsystem_kernel.dylib          0x00" +
                    "0000018d18bed0 0x18d174000 + 98000 (mach_msg_trap + 8)\\n1   libsystem_kernel.dylib       " +
                    "   0x000000018d18b3a8 0x18d174000 + 95144 (mach_msg + 72)\\n2   CoreFoundation            " +
                    "      0x000000018d58fbc4 0x18d4e4000 + 703428 (<redacted> + 236)\\n3   CoreFoundation     " +
                    "             0x000000018d58aa60 0x18d4e4000 + 682592 (<redacted> + 1396)\\n4   CoreFoundat" +
                    "ion                  0x000000018d58a1cc 0x18d4e4000 + 680396 (CFRunLoopRunSpecific + 436)\\" +
                    "n5   WebCore                         0x00000001963eb52c 0x195f3b000 + 4916524 (<redacted> " +
                    "+ 592)\\n6   libsystem_pthread.dylib         0x000000018d21b2ac 0x18d210000 + 45740 (<reda" +
                    "cted> + 128)\\n7   libsystem_pthread.dylib         0x000000018d21b20c 0x18d210000 + 45580 " +
                    "(_pthread_start + 48)\\n\\nThread 8 name:  0#BgLow\\nThread 8:\\n0   libsystem_kernel.dyli" +
                    "b          0x000000018d196f0c 0x18d174000 + 143116 (__psynch_cvwait + 8)\\n1   libsystem_p" +
                    "thread.dylib         0x000000018d213cd8 0x18d210000 + 15576 (<redacted> + 636)\\n2   libc+" +
                    "+.1.dylib                  0x000000018c7644d0 0x18c75c000 + 34000 (std::__1::condition_var" +
                    "iable::wait(std::__1::unique_lock<std::__1::mutex>&) + 24)\\n3   –ë–µ—Ä—É                 " +
                    "       0x00000001056e90e8 0x10469c000 + 17092840 (_yandex_impl___ZN5boost13serialization16" +
                    "singleton_module8get_lockEv + 6322608)\\n4   –ë–µ—Ä—É                        0x00000001056" +
                    "e8ef4 0x10469c000 + 17092340 (_yandex_impl___ZN5boost13serialization16singleton_module8get" +
                    "_lockEv + 6322108)\\n5   –ë–µ—Ä—É                        0x00000001056ead7c 0x10469c000 + " +
                    "17100156 (_yandex_impl___ZN5boost13serialization16singleton_module8get_lockEv + 6329924)\\" +
                    "n6   libsystem_pthread.dylib  vateFrameworks/EmailCore.framework/EmailCore\\n       0x1b1e" +
                    "71000 -        0x1b1e82fff  libGSFontCache.dylib arm64  <85d5e1f6818e3cb789232339838e1ee9>" +
                    " /System/Library/PrivateFrameworks/FontServices.framework/libGSFontCache.dylib\\n       0x" +
                    "1b1e83000 -        0x1b1eb5fff  libTrueTypeScaler.dylib arm64  <5ce42bac7a6e366a8122f2b649" +
                    "31e4c7> /System/Library/PrivateFrameworks/FontServices.framework/libTrueTypeScaler.dylib\\" +
                    "n       0x1b28e1000 -        0x1b28e1fff  libmetal_timestamp.dylib arm64  <449f125aff6c3eb" +
                    "88930a13d4a4aee08> /System/Library/PrivateFrameworks/GPUCompiler.framework/Libraries/libme" +
                    "tal_timestamp.dylib\\n       0x1b39b8000 -        0x1b39bcfff  InternationalSupport arm64 " +
                    " <7a90f1cc4432370a817487085008012b> /System/Library/PrivateFrameworks/InternationalSupport" +
                    ".framework/InternationalSupport\\n       0x1b4d8a000 -        0x1b4d96fff  PersonaUI arm64" +
                    "  <c5f00611a222383a8d5f6a4036b78060> /System/Library/PrivateFrameworks/PersonaUI.framework" +
                    "/PersonaUI\\n       0x1b51d2000 -        0x1b51dcfff  SignpostCollection arm64  <e8f8d0540" +
                    "30e3960867be808d4a02543> /System/Library/PrivateFrameworks/SignpostCollection.framework/Si" +
                    "gnpostCollection\\n       0x1b585a000 -        0x1b5860fff  TextInputUI arm64  <dab1f33343" +
                    "533cb9a4910f6f4506abf5> /System/Library/PrivateFrameworks/TextInputUI.framework/TextInputU" +
                    "I\\n       0x1b5d8e000 -        0x1b5d91fff  XCTTargetBootstrap arm64  <37a7b5cb51f43833b7" +
                    "3284aa6da2b00c> /System/Library/PrivateFrameworks/XCTTargetBootstrap.framework/XCTTargetBo" +
                    "otstrap\\n       0x1b5dd4000 -        0x1b5de6fff  libEDR arm64  <0e484330f1ea3c9aa653387f" +
                    "dd8e7eea> /System/Library/PrivateFrameworks/libEDR.framework/libEDR\\n       0x1b6839000 -" +
                    "        0x1b6846fff  libMobileGestaltExtensions.dylib arm64  <7bb4ccf8882d3955aa725357af0c" +
                    "ee21> /usr/lib/libMobileGestaltExtensions.dylib\\n       0x1b6958000 -        0x1b6958fff " +
                    " libcharset.1.dylib arm64  <eace303743a83b0883cd2da45d81bb89> /usr/lib/libcharset.1.dylib\\" +
                    "n       0x1b7426000 -        0x1b7427fff  libsandbox.1.dylib arm64  <3954b72ed6543ce6a716f" +
                    "260d91c03b5> /usr/lib/libsandbox.1.dylib\\n       0x1b7466000 -        0x1b7467fff  liblog" +
                    "_network.dylib arm64  <da638a55600c3bf59eb90fe80bcc5f2d> /usr/lib/log/liblog_network.dyli" +
                    "b\\n       0x1b7555000 -        0x1b755ffff  AuthenticationServices arm64  <59a53ebd094532" +
                    "d3a895238a5143a57f> /System/Library/Frameworks/AuthenticationServices.framework/Authentica" +
                    "tionServices\\n       0x1b75e7000 -        0x1b773bfff  CoreServices arm64  <59408d675a473" +
                    "3f1952055efefb431cb> /System/Library/Frameworks/CoreServices.framework/CoreServices\\n    " +
                    "   0x1b7768000 -        0x1b7781fff  MPSRayIntersector arm64  <45591bc292513bde96378423e15" +
                    "be33d> /System/Library/Frameworks/MetalPerformanceShaders.framework/Frameworks/MPSRayInter" +
                    "sector.framework/MPSRayIntersector\\n       0x1b77b1000 -        0x1b78ecfff  Network arm6" +
                    "4  <676dec9679353807ba5fffaee0721745> /System/Library/Frameworks/Network.framework/Networ" +
                    "k\\n       0x1b78f8000 -        0x1b7906fff  ANEServices arm64  <dba54c743b0b336babeb9b33f" +
                    "f02a00b> /System/Library/PrivateFrameworks/ANEServices.framework/ANEServices\\n       0x1b" +
                    "790b000 -        0x1b790ffff  ASEProcessing arm64  <6beee62c25433808848e9f61e22f04f0> /Sys" +
                    "tem/Library/PrivateFrameworks/ASEProcessing.framework/ASEProcessing\\n       0x1b7910000 -" +
                    "        0x1b791bfff  AXCoreUtilities arm64  <b333d651debb313ab4c880e8acf5ffb4> /System/Lib" +
                    "rary/PrivateFrameworks/AXCoreUtilities.framework/AXCoreUtilities\\n       0x1b7b95000 -   " +
                    "     0x1b7cd2fff  AppleMediaServices arm64  <87437326183836c19acfcb50f4700bcf> /System/Lib" +
                    "rary/PrivateFrameworks/AppleMediaServices.framework/AppleMediaServices\\n       0x1b7cd300" +
                    "0 -        0x1b7ce2fff  AppleNeuralEngine arm64  <6916f5470c053c5baeda7d648f898e60> /Syste" +
                    "m/Library/PrivateFrameworks/AppleNeuralEngine.framework/AppleNeuralEngine\\n       0x1b7e6" +
                    "c000 -        0x1b7ea2fff  C2 arm64  <8098f48338fc316abd9f88fafda6a7e2> /System/Library/Pr" +
                    "ivateFrameworks/C2.framework/C2\\n       0x1b8031000 -        0x1b803cfff  Categories arm6" +
                    "4  <fd2162e5f1b13e0c9686fa2500b1d48c> /System/Library/PrivateFrameworks/Categories.framewo" +
                    "rk/Categories\\n       0x1b8044000 -        0x1b8165fff  ConfigurationEngineModel arm64  <" +
                    "9e9dc40d171737589f6c3822375b5775> /System/Library/PrivateFrameworks/ConfigurationEngineMod" +
                    "el.framework/ConfigurationEngineModel\\n       0x1b8284000 -        0x1b829ffff  DoNotDist" +
                    "urb arm64  <f48569dc921d37378eda8254d026b633> /System/Library/PrivateFrameworks/DoNotDistu" +
                    "rb.framework/DoNotDisturb\\n       0x1b83d7000 -        0x1b842ffff  DocumentManager arm64" +
                    "  <07a9044f8ef33f6faaf3630b36701fdd> /System/Library/PrivateFrameworks/DocumentManager.fra" +
                    "mework/DocumentManager\\n       0x1b8512000 -        0x1b8516fff  IdleTimerServices arm64 " +
                    " <96d09b21c501360bb8af4749d990e1da> /System/Library/PrivateFrameworks/IdleTimerServices.fr" +
                    "amework/IdleTimerServices\\n       0x1b8552000 -        0x1b858efff  LocalAuthenticationPr" +
                    "ivateUI arm64  <547985e62fc53222b8c4c173608ac430> /System/Library/PrivateFrameworks/LocalA" +
                    "uthenticationPrivateUI.framework/LocalAuthenticationPrivateUI\\n       0x1b858f000 -      " +
                    "  0x1b85bafff  MetadataUtilities arm64  <34c6506e802b37769d4a1a038547e721> /System/Library" +
                    "/PrivateFrameworks/MetadataUtilities.framework/MetadataUtilities\\n       0x1b8b2b000 -   " +
                    "     0x1b8b3efff  NewsAnalyticsUpload arm64  <8a25548276bd3e42ba38115a51afc844> /System/Li" +
                    "brary/PrivateFrameworks/NewsAnalyticsUpload.framework/NewsAnalyticsUpload\\n       0x1b8b4" +
                    "1000 -        0x1b8b92fff  OTSVG arm64  <8f37fe4148de377e8a58c91ccc356dd3> /System/Library" +
                    "/PrivateFrameworks/OTSVG.framework/OTSVG\\n       0x1b8b93000 -        0x1b8bb5fff  OnBoar" +
                    "dingKit arm64  <ac96657973dc3e7eaeb1241a00087982> /System/Library/PrivateFrameworks/OnBoar" +
                    "dingKit.framework/OnBoardingKit\\n       0x1b8c8e000 -        0x1b8cedfff  PhotoFoundation" +
                    " arm64  <d652b829d1e93ead9256fe518277ef8e> /System/Library/PrivateFrameworks/PhotoFoundati" +
                    "on.framework/PhotoFoundation\\n       0x1b8d41000 -        0x1b8d86fff  PhotosImagingFound" +
                    "ation arm64  <b80937ab5b493a7286dba70152bea6d5> /System/Library/PrivateFrameworks/PhotosIm" +
                    "agingFoundation.framework/PhotosImagingFoundation\\n       0x1b8dbc000 -        0x1b8dc5ff" +
                    "f  PrototypeToolsUI arm64  <8ad3a202ba4632eebfa535f1f95b9382> /System/Library/PrivateFrame" +
                    "works/PrototypeToolsUI.framework/PrototypeToolsUI\\n       0x1b8dc6000 -        0x1b8dd9ff" +
                    "f  QuickLookSupport arm64  <6ee115a760143d45b89bdfdc88e741a8> /System/Library/PrivateFrame" +
                    "works/QuickLookSupport.framework/QuickLookSupport\\n       0x1b8ddc000 -        0x1b8e3cff" +
                    "f  ROCKit arm64  <41c33276d267315f92b97e0aa9c744a4> /System/Library/PrivateFrameworks/ROCK" +
                    "it.framework/ROCKit\\n       0x1b8fae000 -        0x1b8fdffff  RemoteConfiguration arm64  " +
                    "<53b23751fe773357a723ba588c39b3d0> /System/Library/PrivateFrameworks/RemoteConfiguration.f" +
                    "ramework/RemoteConfiguration\\n       0x1b8fef000 -        0x1b904bfff  RemoteManagement a" +
                    "rm64  <cd0a48d857cd3aef861de1016c170702> /System/Library/PrivateFrameworks/RemoteManagemen" +
                    "t.framework/RemoteManagement\\n       0x1b904c000 -        0x1b905efff  RemoteTextInput ar" +
                    "m64  <21a7f85c5e6a3896991ac6416c5c74dd> /System/Library/PrivateFrameworks/RemoteTextInput." +
                    "framework/RemoteTextInput\\n       0x1b9087000 -        0x1b9121fff  SampleAnalysis arm64 " +
                    " <8a12e6cd83fb3b5c81e7b52fae6ea396> /System/Library/PrivateFrameworks/SampleAnalysis.frame" +
                    "work/SampleAnalysis\\n       0x1b91fc000 -        0x1b91fcfff  SignpostNotification arm64 " +
                    " <5e421f577ebf3de6a7987b67a4340117> /System/Library/PrivateFrameworks/SignpostNotification" +
                    ".framework/SignpostNotification\\n       0x1b926b000 -        0x1b9273fff  StatsKit arm64 " +
                    " <676c03e847ff36248d82878812f62688> /System/Library/PrivateFrameworks/StatsKit.framework/S" +
                    "tatsKit\\n       0x1b9d9d000 -        0x1baecdfff  UIKitCore arm64  <ab782a031ee43c979f4c1" +
                    "5cb9751acb3> /System/Library/PrivateFrameworks/UIKitCore.framework/UIKitCore\\n       0x1b" +
                    "aece000 -        0x1baed9fff  UIKitServices arm64  <7f48e5cb504b386892b7dee2480d9d75> /Sys" +
                    "tem/Library/PrivateFrameworks/UIKitServices.framework/UIKitServices\\n       0x1baeda000 -" +
                    "        0x1baee1fff  URLFormatting arm64  <3a3381bad32a33d3bd9164ab72dcbcad> /System/Libra" +
                    "ry/PrivateFrameworks/URLFormatting.framework/URLFormatting\\n       0x1baee2000 -        0" +
                    "x1baf04fff  UsageTracking arm64  <7689edf41080312b8b870d4a495aaea5> /System/Library/Privat" +
                    "eFrameworks/UsageTracking.framework/UsageTracking\\n\\nExtra Information:\\n\\nApplication" +
                    " Stats:\\n{\\n    \"active_time_since_last_crash\": 2225.62,\\n    \"active_time_since_lau" +
                    "nch\": 2225.62,\\n    \"application_active\": true,\\n    \"application_in_foreground\": t" +
                    "rue,\\n    \"background_time_since_last_crash\": 27.1647,\\n    \"background_time_since_la" +
                    "unch\": 27.1647,\\n    \"launches_since_last_crash\": 1,\\n    \"sessions_since_last_cras" +
                    "h\": 4,\\n    \"sessions_since_launch\": 4\\n}\\n\\nCrashDoctor Diagnosis: Application thr" +
                    "ew exception NSRangeException: *** -[__NSArray0 objectAtIndex:]: index 2 beyond bounds for" +
                    " empty NSArray\\n",
            "13529748302504801978", "", 0, tags, keys, values, "204", UNKNOWN, "iOS"
        );
    }
}
