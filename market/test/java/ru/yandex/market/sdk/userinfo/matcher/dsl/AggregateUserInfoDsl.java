package ru.yandex.market.sdk.userinfo.matcher.dsl;

import ru.yandex.market.sdk.userinfo.domain.AggregateUserInfo;
import ru.yandex.market.sdk.userinfo.matcher.OptionalMatcher;

/**
 * @authror dimkarp93
 */
public class AggregateUserInfoDsl extends MatcherDsl<AggregateUserInfo> {
    public AggregateUserInfoDsl() {
        super(AggregateUserInfo.class);
    }

    public AggregateUserInfoDsl setUserInfo(UserInfoDsl dsl) {
        addAll(dsl);
        return this;
    }

    public AggregateUserInfoDsl setPassport(PassportInfoDsl dsl) {
        add("passport", OptionalMatcher.existAnd(dsl.toMatcher()), AggregateUserInfo::getPassportInfo);
        return this;
    }

    public AggregateUserInfoDsl setSber(SberlogInfoDsl dsl) {
        add("sber", OptionalMatcher.existAnd(dsl.toMatcher()), AggregateUserInfo::getSberlogInfo);
        return this;
    }
}
