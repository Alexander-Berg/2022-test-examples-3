package ru.yandex.market.mbo.billing.counter.tt;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.history.ChangeType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author: Julia Astakhova
 * @date: 07.08.2008
 */
public class DepotEntityActionCounterTest {

    private List<DepotEntityActionCounter.Operation> operations = new ArrayList<>();

    @Test
    public void test1() {
        operations.add(new DepotEntityActionCounter.Operation(1, 1, 1, ChangeType.ADDED, Calendar.getInstance()));
        operations.add(new DepotEntityActionCounter.Operation(1, 1, 1, ChangeType.UPDATED, Calendar.getInstance()));
        operations.add(new DepotEntityActionCounter.Operation(1, 1, 1, ChangeType.DELETED, Calendar.getInstance()));

        operations = DepotEntityActionCounter.normalize(operations);

        Assert.assertEquals(0, operations.size());
    }

    @Test
    public void test2() {
        operations.add(new DepotEntityActionCounter.Operation(1, 1, 1, ChangeType.UPDATED, Calendar.getInstance()));
        operations.add(new DepotEntityActionCounter.Operation(1, 1, 1, ChangeType.UPDATED, Calendar.getInstance()));
        operations.add(new DepotEntityActionCounter.Operation(1, 1, 1, ChangeType.UPDATED, Calendar.getInstance()));

        operations = DepotEntityActionCounter.normalize(operations);

        Assert.assertEquals(1, operations.size());
    }

    @Test
    public void test3() {
        operations.add(new DepotEntityActionCounter.Operation(1, 1, 1, ChangeType.FILLED_UP, Calendar.getInstance()));
        operations.add(new DepotEntityActionCounter.Operation(1, 1, 1, ChangeType.UPDATED, Calendar.getInstance()));
        operations.add(new DepotEntityActionCounter.Operation(1, 1, 2, ChangeType.UPDATED, Calendar.getInstance()));

        operations = DepotEntityActionCounter.normalize(operations);

        Assert.assertEquals(1, operations.size());
        Assert.assertEquals(2, operations.get(0).entityType);
    }
}
