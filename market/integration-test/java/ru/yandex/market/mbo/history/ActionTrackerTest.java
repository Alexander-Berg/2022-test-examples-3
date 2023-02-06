package ru.yandex.market.mbo.history;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.mbo.configs.TestConfiguration;
import ru.yandex.market.mbo.history.model.EntityAction;
import ru.yandex.market.mbo.history.model.LogObjectKeys;
import ru.yandex.market.mbo.history.model.Snapshot;
import ru.yandex.market.mbo.history.model.ValueType;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.utils.Pair;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author danfertev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfiguration.class
})
public class ActionTrackerTest {
    private static final Random RANDOM = new Random();
    private static final int ACTION_SIZE = 3;

    private ActionTrackerImpl actionTracker;
    private AutoUser autoUser;

    @Resource(name = "actionTracker")
    public void setActionTracker(ActionTrackerImpl actionTracker) {
        this.actionTracker = actionTracker;
    }

    @Resource(name = "autoUser")
    public void setAutoUser(AutoUser autoUser) {
        this.autoUser = autoUser;
    }

    @Test
    public void testRegisterAction() throws Exception {
        long entityId = RANDOM.nextLong();
        EntityAction action = createEntityAction(entityId);
        actionTracker.registerMboAction(action);
        Snapshot loadedSnapshot = actionTracker.getLastSnapshot(EntityType.LOG_OBJECT, entityId);

        Assert.assertTrue(Snapshot.findDiff(action.getNewValue(), loadedSnapshot).isEmpty());
    }

    @Test
    public void testBatchRegisterAction() throws Exception {
        List<Long> entityIds = RANDOM.longs(ACTION_SIZE).boxed().collect(Collectors.toList());
        List<EntityAction> actions = entityIds.stream().map(this::createEntityAction).collect(Collectors.toList());
        actionTracker.registerMboAction(actions);
        List<Snapshot> loadedSnapshots = entityIds.stream()
            .map(id -> actionTracker.getLastSnapshot(EntityType.LOG_OBJECT, id))
            .collect(Collectors.toList());

        Iterator<EntityAction> actionIt = actions.iterator();
        Iterator<Snapshot> loadedSnapshotIt = loadedSnapshots.iterator();

        List<Pair<EntityAction, Snapshot>> pairs = new ArrayList<>();

        while (actionIt.hasNext() && loadedSnapshotIt.hasNext()) {
            pairs.add(Pair.makePair(actionIt.next(), loadedSnapshotIt.next()));
        }

        for (Pair<EntityAction, Snapshot> pair : pairs) {
            EntityAction action = pair.getFirst();
            Snapshot snapshot = pair.getSecond();
            Assert.assertTrue("Snapshots for entity " + action.getEntityId() + " are different",
                Snapshot.findDiff(action.getNewValue(), snapshot).isEmpty());
        }
    }


    private EntityAction createEntityAction(long entityId) {
        Snapshot snapshot = new Snapshot();

        snapshot.put(LogObjectKeys.GURU_CATEGORY_ID, ValueType.INTEGER, "guru_category_id_" + entityId);
        snapshot.put(LogObjectKeys.TASK_ID, ValueType.INTEGER, "task_id_" + entityId);
        snapshot.put(LogObjectKeys.OFFER_ID, ValueType.STRING, "offer_id_" + entityId);
        snapshot.put(LogObjectKeys.TYPE, ValueType.INTEGER, "offer_type_" + entityId);

        return new EntityAction(
            autoUser.getId(),
            EntityType.LOG_OBJECT,
            entityId,
            ChangeType.ADDED,
            snapshot
        );
    }
}
