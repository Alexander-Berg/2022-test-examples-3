package ru.yandex.calendar.logic.event;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.layer.LayerDao;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;

import java.util.Optional;

public class GetEventsTest extends AbstractConfTest {

    @Autowired
    private EventInfoDbLoader eventInfoDbLoader;
    @Autowired
    private UserManager userManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    EventDao eventDao;
    @Autowired
    private LayerDao layerDao;
    @Autowired
    private LayerRoutines layerRoutines;

    @Test
    public void getEventInfos() {
        TestUserInfo me = testManager.prepareRandomYaTeamUser(3454);
        TestUserInfo userWithPrivateLayer = testManager.prepareRandomYaTeamUser(6526);

        testManager.createDefaultEventWithEventLayerAndEventUser(me.getUid(), "user1 event 1", new Event(), Optional.of(true));
        testManager.createDefaultEventWithEventLayerAndEventUser(me.getUid(), "user1 event 2", new Event(), Optional.of(true));

        ListF<EventInfo> forSelf = eventInfoDbLoader.getEventInfosOnLayer(
                Option.of(me.getUid()), EventGetProps.any(),
                me.getDefaultLayerId(), EventLoadLimits.noLimits(), ActionSource.WEB);

        ListF<EventInfo> forStranger = eventInfoDbLoader.getEventInfosOnLayer(
                Option.of(userWithPrivateLayer.getUid()), EventGetProps.any(),
                me.getDefaultLayerId(), EventLoadLimits.noLimits(), ActionSource.WEB);

        Assert.A.equals(Cf.set("user1 event 1", "user1 event 2"),
                forSelf.map(e -> e.getEvent().getName()).unique());
        Assert.A.equals(Cf.set("user1 event 1", "user1 event 2"),
                forStranger.map(e -> e.getEvent().getName()).unique());

        Assert.A.forAll(forSelf, EventInfo::mayView);
        Assert.A.forAll(forStranger, ei -> !ei.mayView());
    }
}
