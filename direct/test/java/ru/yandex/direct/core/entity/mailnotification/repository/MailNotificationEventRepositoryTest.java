package ru.yandex.direct.core.entity.mailnotification.repository;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.mailnotification.model.EventType;
import ru.yandex.direct.core.entity.mailnotification.model.MailNotificationEvent;
import ru.yandex.direct.core.entity.mailnotification.model.ObjectType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestMailNotificationEventRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.dbschema.ppc.tables.Events.EVENTS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MailNotificationEventRepositoryTest {

    @Autowired
    private MailNotificationEventRepository repositoryUnderTest;

    @Autowired
    private TestMailNotificationEventRepository testRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private UserInfo user;
    private Integer shard;

    @Before
    public void setUp() throws Exception {
        user = steps.userSteps().createDefaultUser();
        shard = user.getShard();
    }

    @After
    public void tearDown() {
        dslContextProvider.ppc(shard)
                .deleteFrom(EVENTS)
                .where(EVENTS.OBJECTUID.eq(user.getUid()))
                .execute();
    }

    @Test
    public void addEvents_successFillAllColumns() {
        List<MailNotificationEvent> events = singletonList(new MailNotificationEvent()
                .withObjectId(1L)
                .withCampaignId(2L)
                .withEventType(EventType.PH_PRICE)
                .withObjectType(ObjectType.PHRASE)
                .withOperatorUid(user.getUid())
                .withOwnerUid(user.getUid())
                .withJsonData(""));
        repositoryUnderTest.addEvents(shard, events);

        List<MailNotificationEvent> actual =
                testRepository.getEventsByOwnerUids(shard, singletonList(user.getUid()));
        Assertions.assertThat(actual).isEqualTo(events);
    }
}
