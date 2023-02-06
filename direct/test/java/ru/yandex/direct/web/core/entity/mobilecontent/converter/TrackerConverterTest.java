package ru.yandex.direct.web.core.entity.mobilecontent.converter;

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.mobileapp.Constants;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTracker;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTrackerTrackingSystem;
import ru.yandex.direct.web.core.entity.mobilecontent.model.TrackingSystem;
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileAppTracker;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


public class TrackerConverterTest {
    private static final String TRACKER_ID = "aaa-bbb";
    private static final String TRACKING_URL = "http://tracking-url/";
    private static final String TRACKING_IMPRESSION_URL = "http://tracking-impression-url/";
    private static final List<String> USER_PARAMS = singletonList("xxx");
    public static final String APPMETRICA_TRANSLATION = "apppppp mettttrica";

    private TrackerConverter trackerConverter;

    @Before
    public void before() {
        TranslationService translationService = mock(TranslationService.class);
        when(translationService.translate(Constants.TRACKER_NAMES.get(MobileAppTrackerTrackingSystem.APPMETRICA)))
                .thenReturn(APPMETRICA_TRANSLATION);
        trackerConverter = new TrackerConverter(translationService);
    }

    @Test
    public void convertTrackersToCore_EmptyInput_EmptyOutput() {
        assertThat(trackerConverter.convertTrackersToCore(Collections.emptyList())).isEmpty();
    }

    @Test
    public void convertTrackersToCore_OneInput_OneOutput() {
        List<MobileAppTracker> trackers =
                trackerConverter.convertTrackersToCore(singletonList(createFilledWebMobileAppTracker()));
        assertThat(trackers).hasSize(1);
    }

    @Test
    public void convertTrackersToCore_CheckOutput() {
        List<MobileAppTracker> trackers =
                trackerConverter.convertTrackersToCore(singletonList(createFilledWebMobileAppTracker()));
        assumeThat(trackers.size(), equalTo(1));

        MobileAppTracker tracker = trackers.get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(tracker.getTrackingSystem()).isEqualTo(MobileAppTrackerTrackingSystem.APPMETRICA);
            softly.assertThat(tracker.getTrackerId()).isEqualTo(TRACKER_ID);
            softly.assertThat(tracker.getUrl()).isEqualTo(TRACKING_URL);
            softly.assertThat(tracker.getImpressionUrl()).isEqualTo(TRACKING_IMPRESSION_URL);
            softly.assertThat(tracker.getUserParams()).isEqualTo(USER_PARAMS);
        });
    }

    @Test
    public void convertTrackersToCore_NullAtUserParams_EmptyUserParams() {
        List<MobileAppTracker> trackers = trackerConverter.convertTrackersToCore(
                singletonList(createFilledWebMobileAppTracker().withUserParams(null)));
        assumeThat(trackers.size(), equalTo(1));
        MobileAppTracker tracker = trackers.get(0);
        assertThat(tracker.getUserParams())
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void convertTrackersToCore_NullAtImpressionUrl_BlankImpressionUrl() {
        List<MobileAppTracker> trackers = trackerConverter.convertTrackersToCore(
                singletonList(createFilledWebMobileAppTracker().withImpressionUrl(null)));
        assumeThat(trackers.size(), equalTo(1));
        MobileAppTracker tracker = trackers.get(0);
        assertThat(tracker.getImpressionUrl())
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void convertTrackerToWeb_CheckOutput() {
        List<WebMobileAppTracker> trackers = trackerConverter.convertTrackersToWeb(singletonList(
                new MobileAppTracker()
                        .withTrackingSystem(MobileAppTrackerTrackingSystem.APPMETRICA)
                        .withTrackerId(TRACKER_ID)
                        .withUrl(TRACKING_URL)
                        .withImpressionUrl(TRACKING_IMPRESSION_URL)
                        .withUserParams(USER_PARAMS)
        ));
        WebMobileAppTracker tracker = trackers.get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(tracker.getTrackingSystem()).isEqualTo(TrackingSystem.APPMETRICA);
            softly.assertThat(tracker.getTrackingSystemName()).isEqualTo(APPMETRICA_TRANSLATION);
            softly.assertThat(tracker.getTrackerId()).isEqualTo(TRACKER_ID);
            softly.assertThat(tracker.getUrl()).isEqualTo(TRACKING_URL);
            softly.assertThat(tracker.getUserParams()).isEqualTo(USER_PARAMS);
        });
    }

    private WebMobileAppTracker createFilledWebMobileAppTracker() {
        return new WebMobileAppTracker()
                .withTrackingSystem(TrackingSystem.APPMETRICA)
                .withTrackerId(TRACKER_ID)
                .withUrl(TRACKING_URL)
                .withImpressionUrl(TRACKING_IMPRESSION_URL)
                .withUserParams(USER_PARAMS);
    }
}
