package ru.yandex.market.sdk.userinfo.matcher.dsl;

import org.hamcrest.Matchers;

import ru.yandex.market.sdk.userinfo.domain.SberlogInfo;

/**
 * @authror dimkarp93
 */
public class StatusDsl extends MatcherDsl<SberlogInfo.Status> {
    public StatusDsl() {
        super(SberlogInfo.Status.class);
    }

    public StatusDsl setCode(long code) {
        add("code", Matchers.is(code), SberlogInfo.Status::getCode);
        return this;
    }

    public StatusDsl setText(String text) {
        add("text", Matchers.is(text), SberlogInfo.Status::getText);
        return this;
    }
}
