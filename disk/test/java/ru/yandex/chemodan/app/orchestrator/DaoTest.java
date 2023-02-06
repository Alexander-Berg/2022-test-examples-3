package ru.yandex.chemodan.app.orchestrator;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.orchestrator.dao.Container;
import ru.yandex.chemodan.app.orchestrator.dao.ContainerDbState;
import ru.yandex.chemodan.app.orchestrator.dao.ContainersDao;
import ru.yandex.chemodan.app.orchestrator.dao.Session;
import ru.yandex.chemodan.app.orchestrator.dao.SessionFinishReason;
import ru.yandex.chemodan.app.orchestrator.dao.SessionsDao;
import ru.yandex.misc.db.q.SqlLimits;
import ru.yandex.misc.ip.HostPort;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class DaoTest extends AbstractOrchestratorCoreTest {
    @Autowired
    private SessionsDao sessionsDao;
    @Autowired
    private ContainersDao containersDao;

    private Instant zero = new Instant(0);
    private Instant now = Instant.now();

    @Test
    public void testSql() {
        Container freeContainer = new Container("free-container-id", new HostPort("localhost", 80),
                "nowhere", Option.empty(), ContainerDbState.AVAILABLE, zero, zero, 0);
        Container groupContainer = new Container("group-container-id", new HostPort("localhost", 80),
                "nowhere", Option.of("some_group_id"), ContainerDbState.LOST, zero, zero, 0);
        Assert.isTrue(containersDao.create(freeContainer));
        Assert.isTrue(containersDao.create(groupContainer));
        Assert.some(containersDao.find(groupContainer.getId()));
        Assert.hasSize(2, containersDao.find(Cf.set(groupContainer.getId(), freeContainer.getId())));
        Assert.hasSize(2, containersDao.getAll());
        Assert.isTrue(containersDao.incSessionsCount(groupContainer.getId(), 10));
        Assert.isTrue(containersDao.decSessionsCount(groupContainer.getId()));
        Assert.isTrue(containersDao.setState(groupContainer.getId(), ContainerDbState.AVAILABLE));
        Assert.some(containersDao.findAvailableContainer(Option.of("some_group_id"), Cf.set("nowhere"), 10));
        Assert.some(containersDao.findAvailableContainer(Option.empty(), Cf.set("nowhere"), 10));
        Assert.isTrue(containersDao.setGroupIdForNewSession(freeContainer.getId(), "some_group_id"));

        Session session = new Session("session-id", Option.of(freeContainer.getId()),
                Option.empty(), Option.empty(), zero, zero, zero);
        Assert.isTrue(sessionsDao.create(session));
        Assert.equals(1, sessionsDao.getCount());
        Assert.isTrue(sessionsDao.touchSession(session.getId(), zero.plus(Duration.standardSeconds(1))));
        Assert.hasSize(1, sessionsDao.getAll(SqlLimits.all()));
        Assert.some(sessionsDao.find(session.getId()));
        Assert.hasSize(1, sessionsDao.findSessionsFromContainer(freeContainer.getId()));
        Assert.hasSize(1, sessionsDao.findExpiredSessions(now));
        Assert.equals(1, sessionsDao.countSessionsFromContainer(freeContainer.getId()));
        Assert.isTrue(sessionsDao.finishSession(session.getId(), SessionFinishReason.FINISH_SIGNAL));
        Assert.equals(0, sessionsDao.countSessionsFromContainer(freeContainer.getId()));
        Assert.equals(1, sessionsDao.deleteFinishedSessions(now));
        Assert.isFalse(sessionsDao.delete(session.getId()));
        Assert.isTrue(containersDao.setSessionsCount(freeContainer.getId(), 0, 1));
        Assert.isFalse(containersDao.setSessionsCount(freeContainer.getId(), 0, 1));
        Assert.isTrue(containersDao.delete(freeContainer.getId()));
        Assert.isTrue(containersDao.delete(groupContainer.getId()));
    }

    @Test
    public void testAvailableContainer() {
        Container container1 = new Container("container1", new HostPort("localhost", 80), "nowhere", Option.of("group1"), ContainerDbState.AVAILABLE, zero, zero, 0);
        Container container2 = new Container("container2", new HostPort("localhost", 80), "nowhere", Option.of("group2"), ContainerDbState.AVAILABLE, zero, zero, 0);
        containersDao.create(container1);
        containersDao.create(container2);

        Assert.equals(
                "container1",
                containersDao.findAvailableContainer(Option.of("group1"), Cf.set("nowhere"), 3).get().getId()
        );
        Assert.equals(
                "container2",
                containersDao.findAvailableContainer(Option.of("group2"), Cf.set("nowhere"), 3).get().getId()
        );

        Assert.assertContains(
                Cf.list("container1", "container2"),
                containersDao.findAnyAvailableContainer(Cf.set("nowhere"), 3).get().getId()
        );
    }
}
