package ru.yandex.market.sdk.userinfo.matcher.dsl;

import org.hamcrest.Matchers;

import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.domain.UidType;

/**
 * @authror dimkarp93
 */
public class UidDsl extends MatcherDsl<Uid> {
    public UidDsl() {
        super(Uid.class);
    }

    public static UidDsl asUid(Uid uid) {
        return new UidDsl().setType(uid.getType())
                .setUid(uid.getUid())
                .setRestrictSideEffectsFlag(uid.getRestrictSideEffectsFlag());
    }

    public UidDsl setType(UidType type) {
        add("type", Matchers.is(type), Uid::getType);
        return this;
    }

    public UidDsl setRestrictSideEffectsFlag(boolean restrictSideEffectsFlag) {
        add("restrictSideEffectsFlag", Matchers.is(restrictSideEffectsFlag), Uid::getRestrictSideEffectsFlag);
        return this;
    }

    public UidDsl setUid(long uid) {
        add("uid", Matchers.is(uid), Uid::getUid);
        return this;
    }
}
