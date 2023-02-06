package ru.yandex.direct.web.entity.mobilecontent.controller;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.mobileapp.model.MobileAppStoreType;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilegoals.model.AppmetrikaInternalEvent;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileApp;
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileEvent;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.mobilecontent.model.MobileAppCreateResponse;
import ru.yandex.direct.web.validation.model.ValidationResponse;

import static com.google.common.collect.Sets.cartesianProduct;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.mobileapp.model.MobileAppStoreType.GOOGLEPLAYSTORE;
import static ru.yandex.direct.core.testing.data.TestGroups.getDefaultStoreHref;
import static ru.yandex.direct.core.testing.data.TestGroups.getIosStoreHref;
import static ru.yandex.direct.core.testing.data.TestMobileContents.androidMobileContent;
import static ru.yandex.direct.core.testing.data.TestMobileContents.iosMobileContent;
import static ru.yandex.direct.utils.CommonUtils.nvl;

@DirectWebTest
@RunWith(Parameterized.class)
public class MobileAppControllerAppMetrikaEventsCreateTest extends MobileAppControllerBaseTest {
    public static final String CLIENT_EVENT   = "my event";
    public static final String CLIENT_EVENT_2 = "my event2";

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public List<WebMobileEvent> events;

    @Parameterized.Parameter(2)
    public boolean isValid;

    @Parameterized.Parameter(3)
    public MobileAppStoreType mobileAppStoreType;

    @Parameterized.Parameters(name = "{3} {0}")
    public static Collection<Object[]> params() {
        return cartesianProduct(
                ImmutableSet.of(
                        new Object[] {"null", null, true},
                        new Object[] {"Пустой список", emptyList(), true},
                        new Object[] {"Пользовательское событие без наименования", singletonList(
                        new WebMobileEvent()
                                .withEventName(CLIENT_EVENT)
                                .withCustomEventName("")
                                .withIsInternal(false)),
                        true
                },
                        new Object[] {"Пользовательское событие с наименованием", singletonList(
                        new WebMobileEvent()
                                .withEventName(CLIENT_EVENT)
                                .withCustomEventName("name1")
                                .withIsInternal(false)),
                        true
                },
                        new Object[] {"Внутреннее событие START", singletonList(
                        new WebMobileEvent()
                                .withEventName(AppmetrikaInternalEvent.START.name())
                                .withCustomEventName("name1")
                                .withIsInternal(true)),
                        true
                },
                        new Object[] {"Внутреннее событие ECOMMERCE_SHOW_SCREEN", singletonList(
                        new WebMobileEvent()
                                .withEventName(AppmetrikaInternalEvent.ECOMMERCE_SHOW_SCREEN.name())
                                .withCustomEventName("name1")
                                .withIsInternal(true)),
                        true
                },
                        new Object[] {"Несколько событий", List.of(
                        new WebMobileEvent()
                                .withEventName(AppmetrikaInternalEvent.REVENUE.name())
                                .withCustomEventName("name1")
                                .withIsInternal(true),
                        new WebMobileEvent()
                                .withEventName(AppmetrikaInternalEvent.ECOMMERCE_SHOW_PRODUCT_CARD.name())
                                .withCustomEventName("")
                                .withIsInternal(true),
                        new WebMobileEvent()
                                .withEventName(CLIENT_EVENT)
                                .withCustomEventName("")
                                .withIsInternal(false),
                        new WebMobileEvent()
                                .withEventName(CLIENT_EVENT_2)
                                .withCustomEventName("name2")
                                .withIsInternal(false)),
                        true
                },
                        new Object[] {"Ошибочный элемент - неправильное внутреннее событие", singletonList(
                        new WebMobileEvent()
                                .withEventName(CLIENT_EVENT)
                                .withCustomEventName("name1")
                                .withIsInternal(true)),
                        false
                }
                        ),
                ImmutableSet.of(
                        new Object[]{MobileAppStoreType.APPLEAPPSTORE},
                        new Object[]{MobileAppStoreType.GOOGLEPLAYSTORE}
                )).stream().map(e -> ArrayUtils.addAll(e.get(0), e.get(1))).collect(toList());
    }

    @Before
    public void before() {
        super.before();
    }

    @Test
    public void testCreate() {
        MobileContentInfo mobileContentInfo = new MobileContentInfo()
                .withClientInfo(clientInfo)
                .withMobileContent(mobileAppStoreType == GOOGLEPLAYSTORE ? androidMobileContent() : iosMobileContent());
        MobileContent mobileContent = steps.mobileContentSteps()
                .createMobileContent(mobileContentInfo)
                .getMobileContent();
        String storeHref = mobileAppStoreType == GOOGLEPLAYSTORE
                ? getDefaultStoreHref(mobileContent.getStoreContentId())
                : getIosStoreHref(mobileContent.getStoreContentId());

        long appMetrikaApplicationId = RandomUtils.nextLong(1, 100_000_000_000L);
        WebMobileApp webMobileApp = defaultMobileApp()
                .withAppMetrikaApplicationId(appMetrikaApplicationId)
                .withStoreHref(storeHref)
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
