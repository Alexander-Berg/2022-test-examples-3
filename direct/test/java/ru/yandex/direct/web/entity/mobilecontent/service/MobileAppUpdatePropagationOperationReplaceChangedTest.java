package ru.yandex.direct.web.entity.mobilecontent.service;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute;
import ru.yandex.direct.web.configuration.DirectWebTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.web.entity.mobilecontent.model.PropagationMode.APPLY_TO_BANNERS_WITH_SAME_TRACKING_URL_AND_REPLACE_CHANGED;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileAppUpdatePropagationOperationReplaceChangedTest extends MobileAppUpdatePropagationOperationApplyBase {
    private static final String TRACKING_URL_ON_BANNER2_EXPECTED = "http://app.adjust.com/newnewnew?aaa=111";
    private static final String IMPRESSION_URL_ON_BANNER2_EXPECTED = "http://view.adjust.com/impression/newnewnew?aaa=111";

    @Before
    public void before() {
        init(APPLY_TO_BANNERS_WITH_SAME_TRACKING_URL_AND_REPLACE_CHANGED);
    }

    @Test
    public void bannerInAnotherCompanyHasOldTrackingUrl() {
        assertThat("ссылка имеет правильное значение", actualBanner4.getHref(), equalTo(TRACKING_URL_ON_APP));
    }


    @Test
    public void bannerInAnotherCompanyHasOldAttrs() {
        assertThat("аттрибуты имеют правильное значение", actualBanner4.getReflectedAttributes(),
                equalTo(REFLECTED_ATTRIBUTE4));
    }

    @Test
    public void banner1OldTrackingUrl() {
        assertThat("ссылка имеет правильное значение", actualBanner1.getHref(), nullValue());
    }

    @Test
    public void banner1HasOldAttrs() {
        assertThat("аттрибуты имеют правильное значение", actualBanner1.getReflectedAttributes(),
                equalTo(REFLECTED_ATTRIBUTE1));
    }

    @Test
    public void banner1HasOldImpressionUrl() {
        assertThat("аттрибуты имеют правильное значение", actualBanner1.getImpressionUrl(), nullValue());
    }

    @Test
    public void banner2HasNewTrackingUrl() {
        assertThat("ссылка имеет правильное значение", actualBanner2.getHref(),
                equalTo(TRACKING_URL_ON_BANNER2_EXPECTED));
    }

    @Test
    public void banner2HasNewAttrs() {
        assertThat("аттрибуты имеют правильное значение", actualBanner2.getReflectedAttributes(),
                equalTo(ImmutableMap.of(
                        NewReflectedAttribute.RATING, true,
                        NewReflectedAttribute.PRICE, true,
                        NewReflectedAttribute.ICON, true,
                        NewReflectedAttribute.RATING_VOTES, false
                )));
    }

    @Test
    public void banner2HasNewImpressionUrl() {
        assertThat("ссылка имеет правильное значение", actualBanner2.getImpressionUrl(),
                equalTo(IMPRESSION_URL_ON_BANNER2_EXPECTED));
    }

    @Test
    public void banner3OldTrackingUrl() {
        assertThat("ссылка имеет правильное значение", actualBanner3.getHref(),
                equalTo(TRACKING_URL_ON_BANNER3));
    }

    @Test
    public void banner3HasOldAttrs() {
        assertThat("аттрибуты имеют правильное значение", actualBanner3.getReflectedAttributes(),
                equalTo(REFLECTED_ATTRIBUTE3));
    }

    @Test
    public void banner3HasOldImpressionUrl() {
        assertThat("ссылка имеет правильное значение", actualBanner3.getImpressionUrl(),
                equalTo(IMPRESSION_URL_ON_BANNER3));
    }
}
