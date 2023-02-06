package ru.yandex.market.crm.platform.reducers;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.UidRelation;
import ru.yandex.market.crm.util.Randoms;

public class UidRelationReducerTest {

    private static UidRelation createRelation() {
        return createRelation(
                Randoms.stringNumber(),
                Randoms.enumValue(UidType.class),
                Randoms.enumValue(UidRelation.Strength.class)
        );
    }

    private static UidRelation createRelation(String value, UidType type, UidRelation.Strength strength) {
        Uid uid = Uids.create(type, value);

        UidRelation.Relation relation = UidRelation.Relation.newBuilder()
                .setUid(uid)
                .setStrength(strength)
                .build();

        return UidRelation.newBuilder()
                .setUid(createUserIds())
                .addRelations(relation)
                .build();
    }

    private static Uid createUserIds() {
        return Uids.create(UidType.EMAIL, UUID.randomUUID().toString());
    }

    @Test
    public void newRelation() {
        UidRelation newRelation = createRelation();

        YieldMock collector = new YieldMock();
        new UidRelationReducer().reduce(Lists.newArrayList(), Collections.singleton(newRelation), collector);

        Collection<Message> added = collector.getAdded("UidRelation");
        Assert.assertEquals("Должны добавить одну связь", 1, added.size());
        Assert.assertEquals(newRelation, Iterables.get(added, 0));
    }

    @Test
    public void addedExisted() {
        UidRelation relation = createRelation();

        YieldMock collector = new YieldMock();
        new UidRelationReducer().reduce(Lists.newArrayList(relation), Collections.singleton(relation), collector);

        Collection<Message> added = collector.getAdded("UidRelation");
        Assert.assertTrue("Не должны добавлять связь т.к. она уже существует", added.isEmpty());
    }

    @Test
    public void merge() {
        UidRelation relation1 = createRelation();
        UidRelation relation2 = createRelation();

        YieldMock collector = new YieldMock();
        new UidRelationReducer().reduce(Lists.newArrayList(relation1), Collections.singleton(relation2), collector);

        Collection<UidRelation> added = collector.getAdded("UidRelation");
        Assert.assertEquals("Должны добавлять связь т.к. у добавляемой связи не существующий uid", 1, added.size());

        UidRelation result = Iterables.get(added, 0);
        Assert.assertTrue("В сохраненном результате должен присутствовать идентификатор из relation1",
                result.getRelationsList().contains(relation1.getRelations(0)));
        Assert.assertTrue("В сохраненном результате должен присутствовать идентификатор из relation2",
                result.getRelationsList().contains(relation2.getRelations(0)));
    }

    @Test
    public void mergeStrongAndWeak() {
        String value = Randoms.stringNumber();
        UidType type = Randoms.enumValue(UidType.class);

        UidRelation relation1 = createRelation(value, type, UidRelation.Strength.STRONG);
        UidRelation relation2 = createRelation(value, type, UidRelation.Strength.WEAK);

        YieldMock collector = new YieldMock();
        new UidRelationReducer().reduce(Lists.newArrayList(relation1), Collections.singleton(relation2), collector);

        Collection<UidRelation> added = collector.getAdded("UidRelation");
        Assert.assertTrue("Не должны добавлять факт т.к. у сохраненного факта более сильная связь STRONG",
                added.isEmpty());
    }

    @Test
    public void mergeWeakAndStrong() {
        String value = Randoms.stringNumber();
        UidType type = Randoms.enumValue(UidType.class);

        UidRelation relation1 = createRelation(value, type, UidRelation.Strength.WEAK);
        UidRelation relation2 = createRelation(value, type, UidRelation.Strength.STRONG);

        YieldMock collector = new YieldMock();
        new UidRelationReducer().reduce(Lists.newArrayList(relation1), Collections.singleton(relation2), collector);

        Collection<UidRelation> added = collector.getAdded("UidRelation");
        Assert.assertEquals("Должны добавить факт т.к. у нового факта более сильная связь STRONG",
                1, added.size());

        UidRelation result = Iterables.get(added, 0);
        Assert.assertEquals("Доллжна присутствовать только связь с одним uid-ом", 1, result.getRelationsCount());
        Assert.assertTrue("В сохраненном результате должен присутствовать STRONG-связь",
                result.getRelationsList().contains(relation2.getRelations(0)));
    }
}
