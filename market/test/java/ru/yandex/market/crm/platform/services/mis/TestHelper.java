package ru.yandex.market.crm.platform.services.mis;

import ru.yandex.market.crm.platform.common.UserId;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.domain.crypta.EdgeTypeConfig;
import ru.yandex.market.crm.platform.domain.crypta.UserIdsGraph;
import ru.yandex.market.crm.platform.models.UidRelation;
import ru.yandex.market.crm.util.Randoms;

public class TestHelper {

    public static UserIdsGraph.Node node(UserId uid) {
        return new UserIdsGraph.Node(uid.getType(), uid.getValue());
    }

    public static Uid randomUid() {
        return Uid.newBuilder()
                .setType(UidType.UUID)
                .setStringValue(Randoms.stringNumber())
                .build();
    }

    public static UserId randomUserId() {
        return UserId.from(randomUid());
    }

    public static UidRelation relation(UidRelation.Strength strength, Uid uid) {
        UidRelation.Relation r = UidRelation.Relation.newBuilder()
                .setUid(uid)
                .setStrength(strength)
                .build();

        return UidRelation.newBuilder()
                .addRelations(r)
                .build();
    }

    public static UserIdsGraph.Edge strongEdge(int first, int second) {
        EdgeTypeConfig config = EdgeTypeConfig.noActivity(EdgeTypeConfig.EdgeTypeStrength.TRUSTED).build();
        return new UserIdsGraph.Edge(first, second, config);
    }

    public static UserIdsGraph.Edge weakEdge(int first, int second) {
        EdgeTypeConfig config = EdgeTypeConfig.noActivity(EdgeTypeConfig.EdgeTypeStrength.PROBABILISTIC).build();
        return new UserIdsGraph.Edge(first, second, config);
    }
}
