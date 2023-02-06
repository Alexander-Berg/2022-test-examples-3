package ru.yandex.market.tsum.tms.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.tsum.clients.notifications.NotificationTarget;
import ru.yandex.market.tsum.clients.staff.StaffGroup;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.core.dao.ParamsDao;
import ru.yandex.market.tsum.core.duty.Duty;
import ru.yandex.market.tsum.core.notify.common.NotificationCenter;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static ru.yandex.market.tsum.tms.service.StaffGroupNotifier.EMAIL_DOMAIN;
import static ru.yandex.market.tsum.tms.service.StaffGroupNotifier.GROUP_NOTIFICATION_NAMESPACE;

@RunWith(SpringRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration
public class StaffGroupNotifierTest {

    private static final String TEST_GROUP_URI = "TEST_GROUP_URI";
    private static final String TEST_SERVICE_NAME = "TEST_SERVICE_NAME";
    private static final Integer TEST_CALENDAR_ID = 1;

    @Autowired
    ParamsDao paramsDaoMock;
    @Autowired
    NotificationCenter notificationCenterMock;
    @Autowired
    StaffGroupNotifier staffGroupNotifier;


    @Test
    public void skipEarlyNotification() {
        setupLastNotificationTime(Duration.ofMinutes(5));
        StaffGroupNotificationContext context = new ResolveDutyLoginState(createDuty(), createStaffGroup(), TEST_SERVICE_NAME);
        staffGroupNotifier.notifyStaffGroup(context);
        assertParamsUpdatedCount(0);
    }

    @Test
    public void ensureEmailNotificationSentForEachPersonInGroup() {

        setupLastNotificationTime(Duration.ofDays(1));

        Duty duty = createDuty();

        StaffPerson staffPerson1 = new StaffPerson("person1", 1, new StaffPerson.PersonName(),
            Collections.emptyList(), new StaffPerson.Official(), new StaffPerson.DepartmentGroup());
        StaffPerson staffPerson2 = new StaffPerson("person2", 1, new StaffPerson.PersonName(),
            Collections.emptyList(), new StaffPerson.Official(), new StaffPerson.DepartmentGroup());
        StaffPerson staffPerson3 = new StaffPerson("person3", 1, new StaffPerson.PersonName(),
            Collections.emptyList(), new StaffPerson.Official(), new StaffPerson.DepartmentGroup());

        List<StaffPerson> persons = Arrays.asList(staffPerson1, staffPerson2, staffPerson3);

        StaffGroup testGroup = createStaffGroup();
        ResolveDutyLoginState context = new ResolveDutyLoginState(duty, testGroup, TEST_SERVICE_NAME);
        context.setStaffPeople(persons);

        Mockito.doAnswer(invocation -> {
            List<NotificationTarget> notificationTargets = invocation.getArgument(1);
            assertEquals(persons.size(), notificationTargets.size());
            IntStream.range(0, persons.size()).forEach(i -> {
                StaffPerson staffPerson = persons.get(i);
                NotificationTarget notificationTarget = notificationTargets.get(i);
                assertEquals(staffPerson.getLogin() + EMAIL_DOMAIN, notificationTarget.getTarget());
            });
            return null;
        }).when(notificationCenterMock).notify(any(), ArgumentMatchers.anyCollection());

        staffGroupNotifier.notifyStaffGroup(context);

        assertParamsUpdatedCount(1);
    }

    StaffGroup createStaffGroup() {
        return new StaffGroup(1, TEST_GROUP_URI, TEST_GROUP_URI);
    }

    Duty createDuty() {
        return Duty.newBuilder()
            .withCalendarLayerId(TEST_CALENDAR_ID)
            .withDutyGroupPhone(1234)
            .withStaffGroupName(TEST_GROUP_URI)
            .withLoginExtractor(null)
            .build();
    }

    private void assertParamsUpdatedCount(int count) {
        Mockito.verify(paramsDaoMock, times(count))
            .setValue(eq(GROUP_NOTIFICATION_NAMESPACE), eq(TEST_CALENDAR_ID.toString()), any(Instant.class));
    }

    private void setupLastNotificationTime(Duration duration) {
        Mockito.when(
            paramsDaoMock.getInstant(
                GROUP_NOTIFICATION_NAMESPACE,
                TEST_CALENDAR_ID.toString(),
                Instant.MIN
            )).thenReturn(Instant.now().minus(duration));
    }


    @Configuration
    public static class Config {

        @Bean
        public ParamsDao paramsDao() {
            return Mockito.mock(ParamsDao.class);
        }

        @Bean
        public NotificationCenter notificationCenter() {
            return Mockito.mock(NotificationCenter.class);
        }

        @Bean
        public StaffGroupNotifier staffGroupNotifier(ParamsDao paramsDao,
                                                     NotificationCenter notificationCenter) {
            return new StaffGroupNotifier(notificationCenter, paramsDao, 24);
        }
    }
}