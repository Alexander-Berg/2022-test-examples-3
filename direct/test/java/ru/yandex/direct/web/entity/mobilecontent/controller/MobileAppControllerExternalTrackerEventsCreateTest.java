package ru.yandex.direct.web.entity.mobilecontent.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileApp;
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileEvent;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.mobilecontent.model.MobileAppCreateResponse;
import ru.yandex.direct.web.validation.model.ValidationResponse;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.mobileapp.model.ExternalTrackerEventName.APP_LAUNCHED;
import static ru.yandex.direct.core.entity.mobileapp.model.ExternalTrackerEventName.COMPLETED_REGISTRATION;
import static ru.yandex.direct.core.testing.data.TestGroups.getDefaultStoreHref;
import static ru.yandex.direct.utils.CommonUtils.nvl;

@DirectWebTest
@RunWith(Parameterized.class)
public class MobileAppControllerExternalTrackerEventsCreateTest extends MobileAppControllerBaseTest {

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public List<WebMobileEvent> events;

    @Parameterized.Parameter(2)
    public boolean isValid;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"null", null, true},
                {"Пустой список", emptyList(), true},
                {"Один элемент без наименования", singletonList(
                        new WebMobileEvent()
                                .withEventName(COMPLETED_REGISTRATION.name())
                                .withCustomEventName("")
                                .withIsInternal(true)),
                        true
                },
                {"Один элемент с наименованием", singletonList(
                        new WebMobileEvent()
                                .withEventName(COMPLETED_REGISTRATION.name())
                                .withCustomEventName("name1")
                                .withIsInternal(true)),
                        true
                },
                {"Два элемента", List.of(
                        new WebMobileEvent()
                                .withEventName(COMPLETED_REGISTRATION.name())
                                .withCustomEventName("name1")
                                .withIsInternal(true),
                        new WebMobileEvent()
                                .withEventName(APP_LAUNCHED.name())
                                .withCustomEventName("name2")
                                .withIsInternal(true)),
                        true
                },
                {"Ошибочный элемент", singletonList(
                        new WebMobileEvent()
                                .withEventName("test")
                                .withCustomEventName("name1")
                                .withIsInternal(true)),
                        false
                },
        });
    }

    @Before
    public void before() {
        super.before();
    }

    @Test
    public void testCreate() {
        MobileContent mobileContent = steps.mobileContentSteps().createDefaultMobileContent(clientInfo)
                .getMobileContent();

        WebMobileApp webMobileApp = defaultMobileApp()
                .withStoreHref(getDefaultStoreHref(mobileContent.getStoreContentId()))
                .withMobileEvents(events);
        WebResponse webResponse = mobileAppController.create(webMobileApp, user.getLogin());

        if (isValid) {
            //В ответе вернулись те же события, что были в запросе
            MobileAppCreateResponse webResponseMobileAppCreateResponse = (MobileAppCreateResponse) webResponse;
            assertEquals(new HashSet<>(nvl(events, emptyList())),
                    new HashSet<>(nvl(webResponseMobileAppCreateResponse.getResult().getMobileEvents(), emptyList())));
        } else {
            assertEquals(((ValidationResponse) webResponse).validationResult().getErrors().get(0).getPath(),
                    "mobileApps[0].mobileEvents[0].eventName");
        }
    }
}
