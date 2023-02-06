package ru.yandex.market.sdk.userinfo.matcher.dsl;

import java.util.Map;
import java.util.Optional;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.sdk.userinfo.domain.PassportInfo;
import ru.yandex.market.sdk.userinfo.matcher.OptionalMatcher;

/**
 * @authror dimkarp93
 */
public class PassportInfoDsl extends MatcherDsl<PassportInfo> {
    public PassportInfoDsl() {
        super(PassportInfo.class);
    }

    public PassportInfoDsl setLogin(String login) {
        return setLogin(Matchers.is(login));
    }

    public PassportInfoDsl setLogin(Matcher<String> login) {
        add("login", login, PassportInfo::getLogin);
        return this;
    }

    public PassportInfoDsl setKarma(Matcher<Integer> karma) {
        add("karma", karma, PassportInfo::getKarma);
        return this;
    }

    public PassportInfoDsl setKarmaStatus(Matcher<Integer> karmaStatus) {
        add("karmaStatus", karmaStatus, PassportInfo::getKarmaStatus);
        return this;
    }

    public PassportInfoDsl setDbFields(Matcher<Map<? extends String,? extends String>> matcher) {
        add("dbfields", matcher, PassportInfo::getDbfields);
        return this;
    }

    public PassportInfoDsl setAttributes(Matcher<Map<? extends String, ? extends String>> matcher) {
        add("attributes", matcher, PassportInfo::getAttributes);
        return this;
    }

    public PassportInfoDsl setDisplayName(String displayName) {
        add("displayName", OptionalMatcher.of(displayName), PassportInfo::getDisplayName);
        return this;
    }


    public PassportInfoDsl setDisplayName(Matcher<Optional<String>> matcher) {
        add("displayName", matcher, PassportInfo::getDisplayName);
        return this;
    }

    public PassportInfoDsl setStaffLogin(Matcher<Optional<String>> matcher) {
        add("staffLogin", matcher, PassportInfo::getStaffLogin);
        return this;
    }

    public PassportInfoDsl setStaffLogin(String login) {
        return setStaffLogin(OptionalMatcher.of(login));
    }

    public PassportInfoDsl setStaffLoginRaw(String login) {
        add("staffLoginRaw", Matchers.is(login), PassportInfo::getStaffLoginRaw);
        return this;
    }

    public PassportInfoDsl isYaPlus(boolean isYaPlus) {
        add("isYaPlus", OptionalMatcher.of(isYaPlus), PassportInfo::isYaPlus);
        return this;
    }

    public PassportInfoDsl isYaPlusRaw(String isYaPlusRaw) {
        add("isYaPlusRaw", Matchers.is(isYaPlusRaw), PassportInfo::isYaPlusRaw);
        return this;
    }

    public PassportInfoDsl setLocaions(Matcher<? super Iterable<String>> postal) {
        add("locationsPostal", OptionalMatcher.existAnd(postal), p -> p.getLocations().map(PassportInfo.Locations::getPostal));
        return this;
    }

    public PassportInfoDsl setUserInfo(UserInfoDsl dsl) {
        addAll(dsl);
        return this;
    }

}
