package ru.yandex.market.crm.platform.services.mis;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crm.platform.common.UserId;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.domain.crypta.UserIdsGraph;
import ru.yandex.market.crm.platform.models.UidRelation;

import static ru.yandex.market.crm.platform.services.mis.TestHelper.node;
import static ru.yandex.market.crm.platform.services.mis.TestHelper.randomUid;
import static ru.yandex.market.crm.platform.services.mis.TestHelper.randomUserId;
import static ru.yandex.market.crm.platform.services.mis.TestHelper.relation;
import static ru.yandex.market.crm.platform.services.mis.TestHelper.strongEdge;
import static ru.yandex.market.crm.platform.services.mis.TestHelper.weakEdge;

public class WeakConnectionStrengthStrategyTest {

    @Test
    public void filterUidRelation_strong() {
        Uid uid = randomUid();
        UidRelation uidRelation = relation(UidRelation.Strength.STRONG, uid);

        Collection<UserId> result = WeakConnectionStrengthStrategy.INSTANCE.filter(Collections.singleton(uidRelation));

        Assert.assertEquals("В результате должен быть элемент т.к. связь STRONG", 1, result.size());
        Assert.assertEquals(UserId.from(uid), Iterables.get(result, 0));
    }

    @Test
    public void filterUidRelation_weak() {
        Uid uid = randomUid();
        UidRelation uidRelation = relation(UidRelation.Strength.WEAK, uid);

        Collection<UserId> result = WeakConnectionStrengthStrategy.INSTANCE.filter(Collections.singleton(uidRelation));

        Assert.assertEquals("В результате должен быть элемент т.к. стратегии удовлетворяет связь WEAK", 1,
                result.size());
        Assert.assertEquals(UserId.from(uid), Iterables.get(result, 0));
    }

    /**
     * Проверяем фильтрацияю на графе
     * 0-{strong}->1-{strong}->2-{weak}->3->{strong}->4
     * для начального элемента 1.
     * <p>
     * В результате должны присутствовать все ноды т.к. стратегии удовлетворяют все виды связей.
     */
    @Test
    public void filterGraph_single() {
        UserId uid0 = randomUserId();
        UserId uid1 = randomUserId();
        UserId uid2 = randomUserId();
        UserId uid3 = randomUserId();
        UserId uid4 = randomUserId();

        List<UserIdsGraph.Node> nodes = Arrays.asList(
                node(uid0),
                node(uid1),
                node(uid2),
                node(uid3),
                node(uid4)
        );
        List<UserIdsGraph.Edge> edges = Arrays.asList(
                strongEdge(0, 1),
                strongEdge(1, 2),
                weakEdge(2, 3),
                strongEdge(3, 4)
        );
        UserIdsGraph graph = new UserIdsGraph(nodes, edges);

        Collection<UserId> result = WeakConnectionStrengthStrategy.INSTANCE.filter(
                Collections.singleton(uid1), Collections.singleton(graph));

        Assert.assertTrue(result.contains(uid0));
        Assert.assertTrue(result.contains(uid1));
        Assert.assertTrue(result.contains(uid2));
        Assert.assertTrue(result.contains(uid3));
        Assert.assertTrue(result.contains(uid4));
    }

    /**
     * Проверяем фильтрацияю и склейку двух графов
     * 0-{weak}->1, 0-{weak}->2 и 0-{strong}->1, 0-{weak}->2
     * для начального элемента 0.
     * <p>
     * В результате должны присутствовать все ноды т.к. стратегии удовлетворяют все виды связей.
     */
    @Test
    public void filterGraph_many() {
        UserId uid0 = randomUserId();
        UserId uid1 = randomUserId();
        UserId uid2 = randomUserId();

        List<UserIdsGraph.Node> nodes = Arrays.asList(
                node(uid0),
                node(uid1),
                node(uid2)
        );
        List<UserIdsGraph.Edge> edges0 = Arrays.asList(
                weakEdge(0, 1),
                weakEdge(0, 2)
        );
        UserIdsGraph graph0 = new UserIdsGraph(nodes, edges0);

        List<UserIdsGraph.Edge> edges1 = Arrays.asList(
                strongEdge(0, 1),
                weakEdge(0, 2)
        );
        UserIdsGraph graph1 = new UserIdsGraph(nodes, edges1);

        Collection<UserId> result = WeakConnectionStrengthStrategy.INSTANCE.filter(
                Collections.singleton(uid0), Arrays.asList(graph0, graph1));

        Assert.assertTrue(result.contains(uid0));
        Assert.assertTrue(result.contains(uid1));
        Assert.assertTrue(result.contains(uid2));
    }

}
