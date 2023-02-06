package ru.yandex.direct.web.entity.mobilecontent.controller;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.mobileapp.model.AppmetrikaEventSubtype;
import ru.yandex.direct.core.entity.mobileapp.model.AppmetrikaEventType;
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppMetrikaEvent;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppStoreType;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentService;
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsAppmetrikaRepository;
import ru.yandex.direct.core.entity.mobilegoals.model.AppmetrikaInternalEvent;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileEvent;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.mobilecontent.model.MobileAppUpdateResponse;
import ru.yandex.direct.web.entity.mobilecontent.model.WebUpdateMobileAppRequest;
import ru.yandex.direct.web.validation.model.ValidationResponse;

import static com.google.common.collect.Sets.cartesianProduct;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static junit.framework.TestCase.assertEquals;
import static ru.yandex.direct.core.entity.mobileapp.model.MobileAppStoreType.GOOGLEPLAYSTORE;
import static ru.yandex.direct.core.testing.data.TestGroups.getDefaultStoreHref;
import static ru.yandex.direct.core.testing.data.TestGroups.getIosStoreHref;
import static ru.yandex.direct.core.testing.data.TestMobileContents.androidMobileContent;
import static ru.yandex.direct.core.testing.data.TestMobileContents.iosMobileContent;
import static ru.yandex.direct.utils.CommonUtils.nvl;

@DirectWebTest
@RunWith(Parameterized.class)
public class MobileAppControllerAppmetrikaEventsUpdateTest extends MobileAppControllerBaseTest {
    @Autowired
    private MobileAppGoalsAppmetrikaRepository mobileAppGoalsAppmetrikaRepository;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public Set<MobileAppMetrikaEvent> initialEvents;

    @Parameterized.Parameter(2)
    public List<WebMobileEvent> updatedEvents;

    @Parameterized.Parameter(3)
    public Set<MobileAppMetrikaEvent> expectedEvents;

    @Parameterized.Parameter(4)
    public boolean isValid;

    @Parameterized.Parameter(5)
    public MobileAppStoreType mobileAppStoreType;

    @Parameterized.Parameters(name = "{5} {0}")
    public static Collection<Object[]> params() {
        return cartesianProduct(
                Set.of(
                        new Object[]{"Событий не было, ничего не передавали, ничего не сохранилось",
                                emptySet(),
                                emptyList(),
                                Set.of(),
                                true
                        },
                        new Object[]{"Событий не было, передали одно клиентское событие, оно сохранилось",
                                emptySet(),
                                singletonList(new WebMobileEvent()
                                        .withEventName("event name 1")
                                        .withCustomEventName("name 1")
                                        .withIsInternal(false)),
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.CLIENT)
                                        .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                        .withEventName("event name 1")
                                        .withCustomName("name 1")
                                        .withIsDeleted(false)),
                                true
                        },
                        new Object[]{"Было одно клиентское событие, передали его же с другим описанием, " +
                                "описание изменилось",
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.CLIENT)
                                        .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                        .withEventName("event name 1")
                                        .withCustomName("name 1")
                                        .withIsDeleted(false)),
                                singletonList(new WebMobileEvent()
                                        .withEventName("event name 1")
                                        .withCustomEventName("name 2")
                                        .withIsInternal(false)),
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.CLIENT)
                                        .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                        .withEventName("event name 1")
                                        .withCustomName("name 2")
                                        .withIsDeleted(false)),
                                true
                        },
                        new Object[]{"Было одно клиентское событие, помеченное удаленным передали передали его же, " +
                                "удаление снялось",
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.CLIENT)
                                        .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                        .withEventName("event name 1")
                                        .withCustomName("name 1")
                                        .withIsDeleted(true)),
                                singletonList(new WebMobileEvent()
                                        .withEventName("event name 1")
                                        .withCustomEventName("name 1")
                                        .withIsInternal(false)),
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.CLIENT)
                                        .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                        .withEventName("event name 1")
                                        .withCustomName("name 1")
                                        .withIsDeleted(false)),
                                true
                        },
                        new Object[]{"Было одно клиентское событие, передали пустой список, исходное пометилось " +
                                "удаленным",
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.CLIENT)
                                        .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                        .withEventName("event name 1")
                                        .withCustomName("name 1")
                                        .withIsDeleted(false)),
                                emptyList(),
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.CLIENT)
                                        .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                        .withEventName("event name 1")
                                        .withCustomName("name 1")
                                        .withIsDeleted(true)),
                                true
                        },
                        new Object[]{"Было одно клиентское событие, передали другое, первое пометилось удаленным, " +
                                "второе сохранилось",
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.CLIENT)
                                        .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                        .withEventName("event name 1")
                                        .withCustomName("name 1")
                                        .withIsDeleted(false)),
                                singletonList(new WebMobileEvent()
                                        .withEventName("event name 2")
                                        .withCustomEventName("name 2")
                                        .withIsInternal(false)),
                                Set.of(new MobileAppMetrikaEvent()
                                                .withEventType(AppmetrikaEventType.CLIENT)
                                                .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                                .withEventName("event name 1")
                                                .withCustomName("name 1")
                                                .withIsDeleted(true),
                                        new MobileAppMetrikaEvent()
                                                .withEventType(AppmetrikaEventType.CLIENT)
                                                .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                                .withEventName("event name 2")
                                                .withCustomName("name 2")
                                                .withIsDeleted(false)),
                                true
                        },
                        new Object[]{"Было одно внутреннее событие, передали передали его же с другим описанием, " +
                                "описание изменилось",
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.ECOMMERCE)
                                        .withEventSubtype(AppmetrikaEventSubtype.ADD_TO_CART)
                                        .withEventName("")
                                        .withCustomName("name 1")
                                        .withIsDeleted(false)),
                                singletonList(new WebMobileEvent()
                                        .withEventName(AppmetrikaInternalEvent.ECOMMERCE_ADD_TO_CART.name())
                                        .withCustomEventName("name 2")
                                        .withIsInternal(true)),
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.ECOMMERCE)
                                        .withEventSubtype(AppmetrikaEventSubtype.ADD_TO_CART)
                                        .withEventName("")
                                        .withCustomName("name 2")
                                        .withIsDeleted(false)),
                                true
                        },
                        new Object[]{"Было одно внутреннее событие, передали другое, первое пометилось удаленным, " +
                                "второе сохранилось",
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.REVENUE)
                                        .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                        .withEventName("")
                                        .withCustomName("name 1")
                                        .withIsDeleted(false)),
                                singletonList(new WebMobileEvent()
                                        .withEventName(AppmetrikaInternalEvent.ECOMMERCE_ADD_TO_CART.name())
                                        .withCustomEventName("name 2")
                                        .withIsInternal(true)),
                                Set.of(new MobileAppMetrikaEvent()
                                                .withEventType(AppmetrikaEventType.REVENUE)
                                                .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                                .withEventName("")
                                                .withCustomName("name 1")
                                                .withIsDeleted(true),
                                        new MobileAppMetrikaEvent()
                                                .withEventType(AppmetrikaEventType.ECOMMERCE)
                                                .withEventSubtype(AppmetrikaEventSubtype.ADD_TO_CART)
                                                .withEventName("")
                                                .withCustomName("name 2")
                                                .withIsDeleted(false)),
                                true
                        },
                        new Object[]{"Было одно внутреннее событие, передали клиентское, первое пометилось удаленным," +
                                " второе сохранилось",
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.REVENUE)
                                        .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                        .withEventName("")
                                        .withCustomName("name 1")
                                        .withIsDeleted(false)),
                                singletonList(new WebMobileEvent()
                                        .withEventName("event name 2")
                                        .withCustomEventName("name 2")
                                        .withIsInternal(false)),
                                Set.of(new MobileAppMetrikaEvent()
                                                .withEventType(AppmetrikaEventType.REVENUE)
                                                .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                                .withEventName("")
                                                .withCustomName("name 1")
                                                .withIsDeleted(true),
                                        new MobileAppMetrikaEvent()
                                                .withEventType(AppmetrikaEventType.CLIENT)
                                                .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                                .withEventName("event name 2")
                                                .withCustomName("name 2")
                                                .withIsDeleted(false)),
                                true
                        },
                        new Object[] {"Было одно клиентское событие, передали неверное событие, вернулась ошибка, " +
                                "исходное не изменилось",
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.CLIENT)
                                        .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                        .withEventName("event name 1")
                                        .withCustomName("name 1")
                                        .withIsDeleted(false)),
                                singletonList(new WebMobileEvent()
                                        .withEventName("wrong event")
                                        .withCustomEventName("name 1")
                                        .withIsInternal(true)),
                                singleton(new MobileAppMetrikaEvent()
                                        .withEventType(AppmetrikaEventType.CLIENT)
                                        .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                        .withEventName("event name 1")
                                        .withCustomName("name 1")
                                        .withIsDeleted(false)),
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
    public void testUpdate() {
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
        MobileApp mobileApp = steps.mobileAppSteps()
                .createMobileApp(clientInfo, mobileContentInfo, storeHref).getMobileApp()
                .withAppMetrikaApplicationId(appMetrikaApplicationId);
        String bundleId = MobileContentService.getStoreAppIdFromMobileContent(mobileContent);

        initialEvents.forEach(e -> e.withAppMetrikaAppId(appMetrikaApplicationId)
                .withBundleId(bundleId).withStoreType(mobileAppStoreType));
        mobileAppGoalsAppmetrikaRepository.addEvents(initialEvents);

        WebUpdateMobileAppRequest webUpdateMobileAppRequest = defaultUpdateRequest()
                .withId(mobileApp.getId())
                .withAppMetrikaApplicationId(appMetrikaApplicationId)
                .withMobileEvents(updatedEvents);
        WebResponse webResponse = mobileAppController.updateMobileApp(user.getLogin(), webUpdateMobileAppRequest);

        List<MobileAppMetrikaEvent> actualEvents = mobileAppGoalsAppmetrikaRepository
                .getEventsByMobileApps(singleton(mobileApp), false);

        actualEvents.forEach(e -> e.setId(null));
        expectedEvents.forEach(e -> e.withAppMetrikaAppId(appMetrikaApplicationId)
                .withBundleId(bundleId).withStoreType(mobileAppStoreType));
        assertEquals(expectedEvents, new HashSet<>(actualEvents));
        if (isValid) {
            if (updatedEvents != null) {
                //В ответе вернулись те же события, что были в запросе
                MobileAppUpdateResponse mobileAppUpdateResponse = (MobileAppUpdateResponse) webResponse;
                assertEquals(new HashSet<>(updatedEvents),
                        new HashSet<>(nvl(mobileAppUpdateResponse.getResult().getMobileEvents(), emptyList())));
            }
        } else {
            assertEquals(((ValidationResponse) webResponse).validationResult().getErrors().get(0).getPath(),
                    "mobileEvents[0].eventName");
        }
    }
}
