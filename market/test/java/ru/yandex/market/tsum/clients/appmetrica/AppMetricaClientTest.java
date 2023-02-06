package ru.yandex.market.tsum.clients.appmetrica;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import org.asynchttpclient.RequestBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.request.netty.JsonNettyHttpClient;
import ru.yandex.market.tsum.clients.appmetrica.filters.AppVersionFilter;
import ru.yandex.market.tsum.clients.appmetrica.models.AppVersionDescription;
import ru.yandex.market.tsum.clients.appmetrica.models.Crash;
import ru.yandex.market.tsum.clients.appmetrica.models.CrashReport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AppMetricaClientTest {

    @InjectMocks
    private AppMetricaClient client;
    @Mock
    private JsonNettyHttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        when(httpClient.createRequest(any(), anyString())).thenReturn(new RequestBuilder());
    }

    @Test
    public void testAppVersions() throws IOException {
        String response = "{\"query\":{\"ids\":[1389598],\"dimensions\":[\"ym:t:appVersionAndOS\"," +
            "\"ym:t:buildNumber\"],\"metrics\":[\"ym:t:devices\"],\"sort\":[\"-ym:t:appVersionAndOS\"]," +
            "\"humanized_filter\":\"Операционная система = 'Android'\",\"date1\":\"2019-05-30\"," +
            "\"date2\":\"2019-06-13\",\"filters\":\"operatingSystemInfo=='android'\",\"limit\":50,\"offset\":1," +
            "\"group\":\"Week\",\"pca_integer_intervals\":\"false\",\"auto_group_size\":\"1\"," +
            "\"pca_intervals_length\":\"0\",\"quantile\":\"50\",\"from\":\"0\",\"currency\":\"XXX\",\"to\":\"0\"," +
            "\"profile_attribute_id\":\"0\"},\"data\":[{\"dimensions\":[{\"name\":\"1.63 (Android)\"}," +
            "{\"name\":\"1625\"}],\"metrics\":[7.0]},{\"dimensions\":[{\"name\":\"1.62 (Android)\"}," +
            "{\"name\":\"1623\"}],\"metrics\":[143.0]},{\"dimensions\":[{\"name\":\"1.61 (Android)\"}," +
            "{\"name\":\"1624\"}],\"metrics\":[19263.0]}],\"total_rows\":99,\"total_rows_rounded\":false," +
            "\"sampled\":true,\"sample_share\":0.26496,\"sample_size\":154737159,\"sample_space\":584001960," +
            "\"data_lag\":0,\"totals\":[862786.0],\"min\":[3.0],\"max\":[407876.0]}";

        when(httpClient.executeRequest(any(RequestBuilder.class), any(Function.class))).thenReturn(response);

        List<AppVersionDescription> versions = client.getVersions(
            "1",
            LocalDateTime.now().minusMonths(1),
            LocalDateTime.now(),
            AppVersionFilter.create(Platform.ANDROID, "1")
        );

        assertEquals(versions.get(0), new AppVersionDescription("1.63 (Android)", "1625", 7));
        assertEquals(versions.get(1), new AppVersionDescription("1.62 (Android)", "1623", 143));
        assertEquals(versions.get(2), new AppVersionDescription("1.61 (Android)", "1624", 19263));
    }

    @Test
    public void getCrashes() throws IOException {
        String response = "{\"query\":{\"ids\":[1389598],\"dimensions\":[\"ym:cr2:crashDumpObj\"]," +
            "\"metrics\":[\"ym:cr2:crashes\",\"ym:cr2:crashDevices\",\"ym:cr2:crashesDevicesPercentage\"]," +
            "\"sort\":[\"-ym:cr2:crashes\"],\"humanized_filter\":\"\",\"date1\":\"2019-05-09\"," +
            "\"date2\":\"2019-06-09\",\"filters\":\"appVersionDetails=='1.60 (Android)'\",\"limit\":50,\"offset\":1," +
            "\"group\":\"Week\",\"pca_integer_intervals\":\"false\",\"auto_group_size\":\"1\"," +
            "\"pca_intervals_length\":\"0\",\"quantile\":\"50\",\"from\":\"0\",\"currency\":\"XXX\",\"to\":\"0\"," +
            "\"profile_attribute_id\":\"0\"},\"data\":[{\"dimensions\":[{\"id\":\"4318384357574566178\"," +
            "\"comment\":null,\"name\":\"java.lang.IllegalStateException: Unable to toQueryParam layer for " +
            "LinearLayout, size 768x8064 max size 8192 color type 4 has context 1\\n\\tat android.os.MessageQueue" +
            ".nativePollOnce(Native Method)\\n\\tat android.os.MessageQueue.next(MessageQueue.java:326)\\n\\tat " +
            "android.os.Looper.loop(Looper.java:181)\\n\\tat android.app.ActivityThread.main(ActivityThread" +
            ".java:7097)\\n\\tat java.lang.reflect.Method.invoke(Native Method)\\n\\tat com.android.internal.os" +
            ".RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:494)\\n\\tat com.android.internal.os.ZygoteInit" +
            ".main(ZygoteInit.java:975)\\n\",\"app_version\":\"1.60\"}],\"metrics\":[249.0,116.0,0.5]}," +
            "{\"dimensions\":[{\"id\":\"12498897976883462871\",\"comment\":\"креш на странице вишлиста\"," +
            "\"name\":\"java.lang.IllegalStateException: You should call setParentDelegate() before first onCreate()" +
            "\\n\\tat com.arellomobile.mvp.MvpDelegate.setParentDelegate(MvpDelegate.java:54)\\n\\tat ru.yandex" +
            ".market.clean.presentation.mvp.MvpItem.onAddedToAdapter(MvpItem.java:101)\\n\\tat ru.yandex.market.clean" +
            ".presentation.mvp.MvpFastAdapter.notifyAdapterItemRangeInserted(MvpFastAdapter.java:27)\\n\\tat ru" +
            ".yandex.market.util.MvpFastAdapterDiffUtil$FastAdapterListUpdateCallback.onInserted" +
            "(MvpFastAdapterDiffUtil.java:124)\\n\\tat androidx.recyclerview.widget.BatchingListUpdateCallback" +
            ".dispatchLastEvent(BatchingListUpdateCallback.java:61)\\n\\tat androidx.recyclerview.widget" +
            ".DiffUtil$DiffResult.dispatchUpdatesTo(DiffUtil.java:852)\\n\\tat ru.yandex.market.util" +
            ".MvpFastAdapterDiffUtil.set(MvpFastAdapterDiffUtil.java:60)\\n\\tat ru.yandex.market.util" +
            ".MvpFastAdapterDiffUtil.set(MvpFastAdapterDiffUtil.java:51)\\n\\tat ru.yandex.market.util" +
            ".MvpFastAdapterDiffUtil.set(MvpFastAdapterDiffUtil.java:39)\\n\\tat ru.yandex.market.util" +
            ".MvpFastAdapterDiffUtil.set(MvpFastAdapterDiffU\",\"app_version\":\"1.60\"}],\"metrics\":[158.0,154.0,0" +
            ".05469292]},{\"dimensions\":[{\"id\":\"11330973676803232660\",\"comment\":null,\"name\":\"java.lang" +
            ".RuntimeException: Canvas: trying to draw too large(169804856bytes) bitmap.\\n\\tat android.view" +
            ".DisplayListCanvas.throwIfCannotDraw(DisplayListCanvas.java:229)\\n\\tat android.view.RecordingCanvas" +
            ".drawBitmap(RecordingCanvas.java:98)\\n\\tat android.graphics.drawable.BitmapDrawable.draw" +
            "(BitmapDrawable.java:545)\\n\\tat android.widget.ImageView.onDraw(ImageView.java:1360)\\n\\tat android" +
            ".view.View.draw(View.java:20234)\\n\\tat android.view.View.updateDisplayListIfDirty(View.java:19109)" +
            "\\n\\tat android.view.View.draw(View.java:19962)\\n\\tat android.view.ViewGroup.drawChild(ViewGroup" +
            ".java:4337)\\n\\tat android.view.ViewGroup.dispatchDraw(ViewGroup.java:4114)\\n\\tat android.view.View" +
            ".updateDisplayListIfDirty(View.java:19100)\\n\\tat android.view.View.draw(View.java:19962)\\n\\tat " +
            "android.view.ViewGroup.drawChild(ViewGroup.java:4337)\\n\\tat android.view.ViewGroup.dispatchDraw" +
            "(ViewGroup.java:4114)\\n\\tat android.view.View.draw(View.java:20237)\\n\\tat androidx.viewpager.widget" +
            ".ViewPager.draw(ViewPager.java:2426)\\n\\tat android.view.View.updateDisplayListIfDirty(Vi\"," +
            "\"app_version\":\"1.60\"}],\"metrics\":[64.0,41.0,0.01467371]}," +
            "{\"dimensions\":[{\"id\":\"15217199570637428730\",\"comment\":null,\"name\":\"java.lang" +
            ".NoSuchMethodError: No direct method <init>(Lio/reactivex/SingleObserver;JLjava/lang/Object;)V in class " +
            "Lio/reactivex/internal/operators/observable/ObservableElementAtSingle$ElementAtObserver; or its super " +
            "classes (declaration of 'io.reactivex.internal.operators.observable" +
            ".ObservableElementAtSingle$ElementAtObserver' appears in /data/app/ru.beru" +
            ".android-4_cinlqW4evccqv37A6KWQ==/base.apk!classes2.dex)\\n\\tat io.reactivex.internal.operators" +
            ".observable.ObservableElementAtSingle.subscribeActual(ObservableElementAtSingle.java:37)\\n\\tat io" +
            ".reactivex.Single.subscribe(Single.java:3603)\\n\\tat io.reactivex.internal.operators.single" +
            ".SingleZipArray.subscribeActual(SingleZipArray.java:63)\\n\\tat io.reactivex.Single.subscribe(Single" +
            ".java:3603)\\n\\tat io.reactivex.internal.operators.single.SingleDefer.subscribeActual(SingleDefer" +
            ".java:43)\\n\\tat io.reactivex.Single.subscribe(Single.java:3603)\\n\\tat io.reactivex.internal" +
            ".operators.single.SingleFlatMap.subscribeActual(SingleFlatMap.java:36)\\n\\tat io.reactivex.Single" +
            ".subscribe(Single.java\",\"app_version\":\"1.60\"}],\"metrics\":[56.0,3.0,0.00133397]}," +
            "{\"dimensions\":[{\"id\":\"16822417360273281627\",\"comment\":null,\"name\":\"java.lang" +
            ".IllegalArgumentException: Expected value from range [0.0..0.155] but actual value is NaN!\\n\\tat ru" +
            ".yandex.market.util.MathUtilsKt.mapFromRangeToRange(Math.kt:22)\\n\\tat ru.yandex.market.clean" +
            ".presentation.feature.smartshopping.UserCoinsCollectionItem$SmartCoinsPageTransformer.calculateElevation" +
            "(UserCoinsCollectionItem.kt:101)\\n\\tat ru.yandex.market.clean.presentation.feature.smartshopping" +
            ".UserCoinsCollectionItem$SmartCoinsPageTransformer.transformPage(UserCoinsCollectionItem.kt:96)\\n\\tat " +
            "ru.yandex.market.clean.presentation.view.FixedTransformationViewPager.fixedTransformPage" +
            "(FixedTransformationViewPager.kt:38)\\n\\tat ru.yandex.market.clean.presentation.view" +
            ".FixedTransformationViewPager.onPageScrolled(FixedTransformationViewPager.kt:15)\\n\\tat androidx" +
            ".viewpager.widget.ViewPager.pageScrolled(ViewPager.java:1842)\\n\\tat androidx.viewpager.widget" +
            ".ViewPager.scrollToItem(ViewPager.java:694)\\n\\tat androidx.viewpager.widget.ViewPager.onLayout" +
            "(ViewPager.java:1786)\\n\\tat android.view.View.layout(View.java:22419)\\n\\tat android.view.\"," +
            "\"app_version\":\"1.60\"}],\"metrics\":[37.0,33.0,0.01200576]}],\"totals\":[909.0,584.0,0.20676591]}";
        when(httpClient.executeRequest(any(RequestBuilder.class), any(Function.class))).thenReturn(response);

        CrashReport crashReport = client.getCrashes(
            "1389598",
            LocalDateTime.now().minusMonths(1),
            LocalDateTime.now(),
            AppVersionFilter.create(Platform.ANDROID, "1")
        );

        assertEquals("1389598", crashReport.getAppId());
        assertEquals(584, crashReport.getTotalAffected());
        assertEquals(0.20676591, crashReport.getTotalAffectedPercent(), 0.000000001);

        List<Crash> crashes = crashReport.getCrashList();
        assertEquals(5, crashes.size());
        for (Crash crash : crashes) {
            assertFalse(crash.getId().isEmpty());
            assertFalse(crash.getTrace().isEmpty());
            assertFalse(crash.getLink().isEmpty());
            assertNotNull(crash.getAffectedUsersPercent());
            assertNotNull(crash.getTotalUserCount());
            assertNotNull(crash.getTotalCrashCount());
        }

        String appVersion = "1.60";
        String crashId = "4318384357574566178";
        assertEquals(new Crash(
                crashId,
                "java.lang.IllegalStateException: Unable to toQueryParam layer for LinearLayout, size 768x8064 max " +
                    "size 8192 color type 4 has context 1\n\tat android.os.MessageQueue.nativePollOnce(Native Method)" +
                    "\n\tat android.os.MessageQueue.next(MessageQueue.java:326)\n\tat android.os.Looper.loop(Looper" +
                    ".java:181)\n\tat android.app.ActivityThread.main(ActivityThread.java:7097)\n\tat java.lang" +
                    ".reflect.Method.invoke(Native Method)\n\tat com.android.internal.os" +
                    ".RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:494)\n\tat com.android.internal.os" +
                    ".ZygoteInit.main(ZygoteInit.java:975)\n",
                appVersion,
                249,
                116,
                0.5,
                String.format("https://appmetrica.yandex.ru/statistic?group=hour&appId=%s&report=crash-dump&crashId" +
                    "=%s&version=%s&metrics=ym_cr2_crashes&sampling=0.26496", "1389598", crashId, appVersion)
            ), crashes.get(0)
        );
    }
}
