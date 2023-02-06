package ru.yandex.direct.web.entity.mobilecontent.converter;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.web.core.entity.mobilecontent.converter.MobileContentConverter;
import ru.yandex.direct.web.core.entity.mobilecontent.converter.TrackerConverter;
import ru.yandex.direct.web.entity.mobilecontent.model.WebUpdateMobileAppRequest;

public class WebMobileAppConverterTest {
    @SuppressWarnings("unused")
    @Mock
    private TrackerConverter trackerConverter;

    @SuppressWarnings("unused")
    @Mock
    private MobileContentConverter mobileContentConverter;

    @InjectMocks
    private WebMobileAppConverter webMobileAppConverter;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createMobileAppModelChanges_EmptyRequest() {
        ModelChanges<MobileApp> changes = webMobileAppConverter.createMobileAppModelChanges(
                new WebUpdateMobileAppRequest());
        SoftAssertions.assertSoftly(
                softly -> {
                    softly.assertThat(changes.isPropChanged(MobileApp.NAME))
                            .describedAs(MobileApp.NAME.name()).isFalse();
                    softly.assertThat(changes.isPropChanged(MobileApp.DOMAIN))
                            .describedAs(MobileApp.DOMAIN.name()).isFalse();
                    softly.assertThat(changes.isPropChanged(MobileApp.TRACKERS))
                            .describedAs(MobileApp.TRACKERS.name()).isTrue();
                    softly.assertThat(changes.isPropChanged(MobileApp.DISPLAYED_ATTRIBUTES))
                            .describedAs(MobileApp.DISPLAYED_ATTRIBUTES.name()).isTrue();
                    softly.assertThat(changes.isPropChanged(MobileApp.MINIMAL_OPERATING_SYSTEM_VERSION))
                            .describedAs(MobileApp.MINIMAL_OPERATING_SYSTEM_VERSION.name()).isTrue();
                    softly.assertThat(changes.isPropChanged(MobileApp.DEVICE_TYPE_TARGETING))
                            .describedAs(MobileApp.DEVICE_TYPE_TARGETING.name()).isTrue();
                    softly.assertThat(changes.isPropChanged(MobileApp.NETWORK_TARGETING))
                            .describedAs(MobileApp.NETWORK_TARGETING.name()).isTrue();
                    softly.assertThat(changes.getChangedProp(MobileApp.DISPLAYED_ATTRIBUTES))
                            .describedAs(MobileApp.DISPLAYED_ATTRIBUTES.name()).isNull();
                    softly.assertThat(changes.getChangedProp(MobileApp.MINIMAL_OPERATING_SYSTEM_VERSION))
                            .describedAs(MobileApp.MINIMAL_OPERATING_SYSTEM_VERSION.name()).isNull();
                    softly.assertThat(changes.getChangedProp(MobileApp.DEVICE_TYPE_TARGETING))
                            .describedAs(MobileApp.DEVICE_TYPE_TARGETING.name()).isNull();
                    softly.assertThat(changes.getChangedProp(MobileApp.NETWORK_TARGETING))
                            .describedAs(MobileApp.NETWORK_TARGETING.name()).isNull();
                });
    }

    @Test
    public void createMobileAppModelChanges_RequestWithDomain() {
        WebUpdateMobileAppRequest updateRequest = new WebUpdateMobileAppRequest();
        updateRequest.domain = "ya.ru";

        ModelChanges<MobileApp> changes = webMobileAppConverter.createMobileAppModelChanges(
                updateRequest);
        SoftAssertions.assertSoftly(
                softly -> {
                    softly.assertThat(changes.isPropChanged(MobileApp.DOMAIN))
                            .describedAs("changed").isTrue();
                    softly.assertThat(changes.getChangedProp(MobileApp.DOMAIN))
                            .describedAs("value").isEqualTo("ya.ru");
                });
    }
}
