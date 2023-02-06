package ru.yandex.direct.web.entity.mobilecontent.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.mobileapp.model.ExternalTrackerEventName;
import ru.yandex.direct.core.entity.mobileapp.model.MobileExternalTrackerEvent;
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsExternalTrackerRepository;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileEvent;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.mobilecontent.model.MobileAppUpdateResponse;
import ru.yandex.direct.web.entity.mobilecontent.model.WebUpdateMobileAppRequest;
import ru.yandex.direct.web.validation.model.ValidationResponse;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.utils.CommonUtils.nvl;

@DirectWebTest
@RunWith(Parameterized.class)
public class MobileAppControllerExternalTrackerEventsUpdateTest extends MobileAppControllerBaseTest {
    @Autowired
    private MobileAppGoalsExternalTrackerRepository mobileAppGoalsExternalTrackerRepository;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public List<MobileExternalTrackerEvent> initialEvents;

    @Parameterized.Parameter(2)
    public List<WebMobileEvent> updatedEvents;

    @Parameterized.Parameter(3)
    public Set<MobileExternalTrackerEvent> expectedEvents;

    @Parameterized.Parameter(4)
    public boolean isValid;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"Событий не было, ничего не передавали, ничего не сохранилось",
                        emptyList(),
                        emptyList(),
                        Set.of(),
                        true
                },
                {"Событий не было, передали одно, оно сохранилось",
                        emptyList(),
                        singletonList(new WebMobileEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1.name())
                                .withCustomEventName("name 1")
                                .withIsInternal(true)),
                        Set.of(new MobileExternalTrackerEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                .withCustomName("name 1")
                                .withIsDeleted(false)),
                        true
                },
                {"Было одно событие, передали пустой список, исходное пометилось удаленным",
                        singletonList(new MobileExternalTrackerEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                .withCustomName("name 1")
                                .withIsDeleted(false)),
                        emptyList(),
                        Set.of(new MobileExternalTrackerEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                .withCustomName("name 1")
                                .withIsDeleted(true)),
                        true
                },
                {"Было одно событие, передали null, ничего не поменялось (на случай отключения фичи)",
                        singletonList(new MobileExternalTrackerEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                .withCustomName("name 1")
                                .withIsDeleted(false)),
                        null,
                        Set.of(new MobileExternalTrackerEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                .withCustomName("name 1")
                                .withIsDeleted(false)),
                        true
                },
                {"Было одно событие, передали передали его же с другим описанием, описание изменилось",
                        singletonList(new MobileExternalTrackerEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                .withCustomName("name 1")
                                .withIsDeleted(false)),
                        singletonList(new WebMobileEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1.name())
                                .withCustomEventName("name 2")
                                .withIsInternal(true)),
                        Set.of(new MobileExternalTrackerEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                .withCustomName("name 2")
                                .withIsDeleted(false)),
                        true
                },
                {"Было одно событие, помеченное удаленным, передали его же, удаление снялось",
                        singletonList(new MobileExternalTrackerEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                .withCustomName("name 1")
                                .withIsDeleted(true)),
                        singletonList(new WebMobileEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1.name())
                                .withCustomEventName("name 1")
                                .withIsInternal(true)),
                        Set.of(new MobileExternalTrackerEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                .withCustomName("name 1")
                                .withIsDeleted(false)),
                        true
                },
                {"Было одно событие, передали другое, первое пометилось удаленным, второе сохранилось",
                        singletonList(new MobileExternalTrackerEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                .withCustomName("name 1")
                                .withIsDeleted(false)),
                        singletonList(new WebMobileEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_2.name())
                                .withCustomEventName("name 2")
                                .withIsInternal(true)),
                        Set.of(new MobileExternalTrackerEvent()
                                        .withEventName(ExternalTrackerEventName.EVENT_1)
                                        .withCustomName("name 1")
                                        .withIsDeleted(true),
                                new MobileExternalTrackerEvent()
                                        .withEventName(ExternalTrackerEventName.EVENT_2)
                                        .withCustomName("name 2")
                                        .withIsDeleted(false)),
                        true
                },
                {"Было одно событие, передали неверное событие, вернулась ошибка, исходное не изменилось",
                        singletonList(new MobileExternalTrackerEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                .withCustomName("name 1")
                                .withIsDeleted(false)),
                        singletonList(new WebMobileEvent()
                                .withEventName("wrong event")
                                .withCustomEventName("name 1")
                                .withIsInternal(true)),
                        Set.of(new MobileExternalTrackerEvent()
                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                .withCustomName("name 1")
                                .withIsDeleted(false)),
                        false
                },
        });
    }

    @Before
    public void before() {
        super.before();
    }

    @Test
    public void testUpdate() {
        Long mobileAppId = steps.mobileAppSteps().createDefaultMobileApp(clientInfo).getMobileAppId();
        initialEvents.forEach(e -> e.setMobileAppId(mobileAppId));
        mobileAppGoalsExternalTrackerRepository.addEvents(clientInfo.getShard(), initialEvents);

        WebUpdateMobileAppRequest webUpdateMobileAppRequest = defaultUpdateRequest()
                .withId(mobileAppId)
                .withMobileEvents(updatedEvents);
        WebResponse webResponse = mobileAppController.updateMobileApp(user.getLogin(), webUpdateMobileAppRequest);

        List<MobileExternalTrackerEvent> actualEvents = mobileAppGoalsExternalTrackerRepository
                .getEventsByAppIds(clientInfo.getShard(), singleton(mobileAppId), false);

        actualEvents.forEach(e -> e.setId(null));
        expectedEvents.forEach(e -> e.setMobileAppId(mobileAppId));
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
