package ru.yandex.direct.web.entity.mobilecontent.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.web.configuration.DirectWebTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.web.entity.mobilecontent.model.PropagationMode.APPLY_TO_ANY_RELATED_BANNERS_AND_REPLACE_ALL;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileAppUpdatePropagationOperationReplaceAllTest extends MobileAppUpdatePropagationOperationApplyBase {
    @Before
    public void before() {
        init(APPLY_TO_ANY_RELATED_BANNERS_AND_REPLACE_ALL);
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
    public void banner1HasNewTrackingUrl() {
        assertThat("ссылка имеет правильное значение", actualBanner1.getHref(), equalTo(TRACKING_URL_ON_REQ));
    }

    @Test
    public void banner1HasNewAttrs() {
        assertThat("аттрибуты имеют правильное значение", actualBanner1.getReflectedAttributes(), equalTo(NEW_ATTRIBUTES));
    }

    @Test
    public void banner1HasNewImpressionUrl() {
        assertThat("ссылка имеет правильное значение", actualBanner1.getImpressionUrl(), equalTo(IMPRESSION_URL_ON_REQ));
    }

    @Test
    public void banner2HasNewTrackingUrl() {
        assertThat("ссылка имеет правильное значение", actualBanner2.getHref(), equalTo(TRACKING_URL_ON_REQ));
    }

    @Test
    public void banner2HasNewAttrs() {
        assertThat("аттрибуты имеют правильное значение", actualBanner2.getReflectedAttributes(), equalTo(NEW_ATTRIBUTES));
    }

    @Test
    public void banner2HasNewImpressionUrl() {
        assertThat("ссылка имеет правильное значение", actualBanner2.getImpressionUrl(), equalTo(IMPRESSION_URL_ON_REQ));
    }

    @Test
    public void banner3HasNewTrackingUrl() {
        assertThat("ссылка имеет правильное значение", actualBanner3.getHref(), equalTo(TRACKING_URL_ON_REQ));
    }

    @Test
    public void banner3HasNewAttrs() {
        assertThat("аттрибуты имеют правильное значение", actualBanner3.getReflectedAttributes(), equalTo(NEW_ATTRIBUTES));
    }

    @Test
    public void banner3HasNewImpressionUrl() {
        assertThat("ссылка имеет правильное значение", actualBanner3.getImpressionUrl(), equalTo(IMPRESSION_URL_ON_REQ));
    }
}
