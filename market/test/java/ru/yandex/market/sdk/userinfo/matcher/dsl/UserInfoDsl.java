package ru.yandex.market.sdk.userinfo.matcher.dsl;

import java.time.LocalDate;
import java.util.Optional;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.sdk.userinfo.domain.Sex;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.domain.UserInfo;
import ru.yandex.market.sdk.userinfo.matcher.OptionalMatcher;

/**
 * @authror dimkarp93
 */
public class UserInfoDsl extends MatcherDsl<UserInfo> {
    public UserInfoDsl() {
        super(UserInfo.class);
    }

    public UserInfoDsl setUid(Matcher<Uid> uidMatcher) {
        add("uid", uidMatcher, UserInfo::getUid);
        return this;
    }

    public UserInfoDsl setFirstName(String firstName) {
        return setFirstName(OptionalMatcher.of(firstName));
    }

    public UserInfoDsl setFirstName(Matcher<Optional<String>> firstName) {
        add("firstName", firstName, UserInfo::getFirstName);
        return this;
    }

    public UserInfoDsl setLastName(String lastName) {
        return setLastName(OptionalMatcher.of(lastName));
    }

    public UserInfoDsl setLastName(Matcher<Optional<String>> lastName) {
        add("lastName", lastName, UserInfo::getLastName);
        return this;
    }

    public UserInfoDsl setSex(Sex sex) {
        return setSex(OptionalMatcher.of(sex));
    }

    public UserInfoDsl setSex(Matcher<Optional<Sex>> sex) {
        add("sex", sex, UserInfo::getSex);
        return this;
    }

    public UserInfoDsl setRawSex(String rawSex) {
        add("rawSex", Matchers.is(rawSex), UserInfo::getRawSex);
        return this;
    }

    public UserInfoDsl setBirthDate(LocalDate localDate) {
        return setBirthDate(OptionalMatcher.of(localDate));
    }

    public UserInfoDsl setBirthDate(Matcher<Optional<LocalDate>> birthDate) {
        add("birthDate", birthDate, UserInfo::getBirthDate);
        return this;
    }

    public UserInfoDsl setFio(String fio) {
        return setFio(OptionalMatcher.of(fio));
    }

    public UserInfoDsl setFio(Matcher<Optional<String>> fio) {
        add("fio", fio, UserInfo::getFio);
        return this;
    }

    public UserInfoDsl setFullFio(String fio) {
        return setFullFio(OptionalMatcher.of(fio));
    }

    public UserInfoDsl setFullFio(Matcher<Optional<String>> fio) {
        add("fullFio", fio, UserInfo::getFullFio);
        return this;
    }

    public UserInfoDsl setPublicName(String publicName) {
        return setPublicName(OptionalMatcher.of(publicName));
    }

    public UserInfoDsl setPublicName(Matcher<Optional<String>> publicName) {
        add("publicName", publicName, UserInfo::getPublicName);
        return this;
    }

    public UserInfoDsl setAvatar(String avatar, String size) {
        add("avatar", OptionalMatcher.of(avatar), p -> p.getAvatar(size));
        return this;
    }

    public UserInfoDsl setAvatar(Matcher<Optional<String>> avatar, String size) {
        add("avatar", avatar, p -> p.getAvatar(size));
        return this;
    }

    public UserInfoDsl setCAPIDisplayName(String displayName) {
        return setCAPIDisplayName(OptionalMatcher.of(displayName));
    }

    public UserInfoDsl setCAPIDisplayName(Matcher<Optional<String>> displayName) {
        add("CAPIDisplayName", displayName, UserInfo::getCAPIDisplayName);
        return this;
    }

    public UserInfoDsl setEmails(Matcher<? super Iterable<String>> emails) {
        add("emails", emails, UserInfo::getEmails);
        return this;
    }

    public UserInfoDsl setPhones(Matcher<? super Iterable<String>> phones) {
        add("phones", phones, UserInfo::getPhones);
        return this;
    }

}
