package ru.yandex.market.crm.platform.services.mis;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.common.UserId;
import ru.yandex.market.crm.platform.domain.crypta.UserIdsGraph;
import ru.yandex.market.crm.platform.models.UidRelation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static ru.yandex.market.crm.platform.services.mis.TestHelper.node;
import static ru.yandex.market.crm.platform.services.mis.TestHelper.randomUid;
import static ru.yandex.market.crm.platform.services.mis.TestHelper.randomUserId;
import static ru.yandex.market.crm.platform.services.mis.TestHelper.relation;
import static ru.yandex.market.crm.platform.services.mis.TestHelper.strongEdge;
import static ru.yandex.market.crm.platform.services.mis.TestHelper.weakEdge;

public class StrongConnectionStrengthStrategyTest {

    @Test
    public void filterUidRelation_strong() {
        Uid uid = randomUid();
        UidRelation uidRelation = relation(UidRelation.Strength.STRONG, uid);

        Collection<UserId> result = StrongConnectionStrengthStrategy.INSTANCE.filter(Collections.singleton(uidRelation));

        Assert.assertEquals("В результате должен быть элемент т.к. связь STRONG", 1, result.size());
        Assert.assertEquals(UserId.from(uid), Iterables.get(result, 0));
    }

    @Test
    public void filterUidRelation_weak() {
        Uid uid = randomUid();
        UidRelation uidRelation = relation(UidRelation.Strength.WEAK, uid);

        Collection<UserId> result = StrongConnectionStrengthStrategy.INSTANCE.filter(Collections.singleton(uidRelation));

        Assert.assertTrue("Результат должен быть пустым т.к. связь WEAK", result.isEmpty());
    }

    @Test
    public void filterGraph_empty() {
        UserId userId = randomUserId();
        List<UserIdsGraph> graphs = Collections.emptyList();

        Collection<UserId> result = StrongConnectionStrengthStrategy.INSTANCE.filter(Collections.singleton(userId), graphs);

        Assert.assertTrue(result.isEmpty());
    }

    /**
     * Проверяем фильтрацияю на графе
     * 0<-{strong}->1<-{strong}->2<-{strong}->3<-{weak}->4<-{strong}->5
     * для начального элемента 1.
     *
     * В результате должны присутствовать ноды 0, 1, 2 и 3 т.к. между ними строгая связь (граф двунаправленный).
     * Не должны присутствовать:
     * <ol>
     * <li>нода 4 т.к. до нее weak связь;</li>
     * <li>нода 5 т.к. она достижима только через ноду 4 с weak связью.</li>
     * </ol>
     */
    @Test
    public void filterGraph_single() {
        UserId uid0 = randomUserId();
        UserId uid1 = randomUserId();
        UserId uid2 = randomUserId();
        UserId uid3 = randomUserId();
        UserId uid4 = randomUserId();
        UserId uid5 = randomUserId();

        List<UserIdsGraph.Node> nodes = Arrays.asList(
            node(uid0),
            node(uid1),
            node(uid2),
            node(uid3),
            node(uid4),
            node(uid5)
        );
        List<UserIdsGraph.Edge> edges = Arrays.asList(
            strongEdge(0, 1),
            strongEdge(1, 2),
            strongEdge(2, 3),
            weakEdge(3, 4),
            strongEdge(4, 5)
        );
        UserIdsGraph graph = new UserIdsGraph(nodes, edges);

        Collection<UserId> result = StrongConnectionStrengthStrategy.INSTANCE.filter(
            Collections.singleton(uid1), Collections.singleton(graph));

        Assert.assertTrue(result.contains(uid0));
        Assert.assertTrue(result.contains(uid1));
        Assert.assertTrue(result.contains(uid2));
        Assert.assertTrue(result.contains(uid3));
        Assert.assertFalse(result.contains(uid4));
        Assert.assertFalse(result.contains(uid5));
    }

    /**
     * Проверяем фильтрацияю и склейку двух графов
     * 0<-{weak}->1, 0<-{weak}->2 и 0<-{strong}->1, 0<-{weak}->2
     * для начального элемента 0.
     *
     * В результате должны присутствовать ноды 0 и 1 т.к. между ними есть строгая связь во втором графе. Не должна
     * присутствовать нода 2 т.к. в обоих графах она достижима только по weak связи.
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

        Collection<UserId> result = StrongConnectionStrengthStrategy.INSTANCE.filter(
            Collections.singleton(uid0), Arrays.asList(graph0, graph1));

        Assert.assertTrue(result.contains(uid0));
        Assert.assertTrue(result.contains(uid1));
        Assert.assertFalse(result.contains(uid2));
    }

}
