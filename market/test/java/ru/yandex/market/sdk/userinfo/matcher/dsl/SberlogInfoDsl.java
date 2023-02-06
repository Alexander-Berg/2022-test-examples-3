package ru.yandex.market.sdk.userinfo.matcher.dsl;

import java.util.Optional;

import org.hamcrest.Matcher;

import ru.yandex.market.sdk.userinfo.domain.SberlogInfo;
import ru.yandex.market.sdk.userinfo.matcher.OptionalMatcher;

/**
 * @authror dimkarp93
 */
public class SberlogInfoDsl extends MatcherDsl<SberlogInfo> {
    public SberlogInfoDsl() {
        super(SberlogInfo.class);
    }

    public SberlogInfoDsl setFatherName(String fatherName) {
        return setFatherName(OptionalMatcher.of(fatherName));
    }

    public SberlogInfoDsl setFatherName(Matcher<Optional<String>> fatherName) {
        add("fatherName", fatherName, SberlogInfo::getFatherName);
        return this;
    }

    public SberlogInfoDsl setStatus(Matcher<SberlogInfo.Status> status) {
        add("status", status, SberlogInfo::getStatus);
        return this;
    }

    public SberlogInfoDsl setUserInfo(UserInfoDsl matcher) {
        addAll(matcher);
        return this;
    }
}
