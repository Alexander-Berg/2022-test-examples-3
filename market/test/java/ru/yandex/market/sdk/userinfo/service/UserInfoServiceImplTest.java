package ru.yandex.market.sdk.userinfo.service;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.matcher.dsl.UidDsl;

/**
 * @authror dimkarp93
 */
public class UserInfoServiceImplTest {

    private ResolveUidService resolveUidService = new ResolveUidServiceImpl();

    @Test
    public void resolveTest() {
        Collection<Uid> uids = resolveUidService.resolve(
                Arrays.asList(
                        1L << 50,
                        23L,
                        1L << 61,
                        1L << 60,
                        (1L << 60) + 1,
                        1L << 63L,
                        2_190_550_858_753_009_250L,
                        2_190_550_858_753_437_194L,
                        2_190_550_858_753_437_195L,
                        2_190_550_859_753_437_194L,
                        2_305_843_009_213_693_952L,
                        2_305_843_009_213_693_951L
                )
        );

        Assert.assertThat(
                uids,
                Matchers.containsInAnyOrder(
                        UidDsl.asUid(Uid.ofPassport(1L << 50, false)).toMatcher(),
                        UidDsl.asUid(Uid.ofPassport(23, false)).toMatcher(),
                        UidDsl.asUid(Uid.ofPassport(1L << 61, false)).toMatcher(),
                        UidDsl.asUid(Uid.ofMarket(1L << 60)).toMatcher(),
                        UidDsl.asUid(Uid.ofMarket((1L << 60) + 1)).toMatcher(),
                        UidDsl.asUid(Uid.ofPassport(1L << 63, false)).toMatcher(),
                        UidDsl.asUid(Uid.ofSberlog(2_190_550_858_753_009_250L)).toMatcher(),
                        UidDsl.asUid(Uid.ofSberlog(2_190_550_858_753_437_194L)).toMatcher(),
                        UidDsl.asUid(Uid.ofPassport(2_190_550_858_753_437_195L, true)).toMatcher(),
                        UidDsl.asUid(Uid.ofPassport(2_190_550_859_753_437_194L, true)).toMatcher(),
                        UidDsl.asUid(Uid.ofPassport(2_305_843_009_213_693_952L, false)).toMatcher(),
                        UidDsl.asUid(Uid.ofSberlog(2_305_843_009_213_693_951L)).toMatcher()
                        )
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveUnusedRangeStartTest() {
        resolveUidService.resolve(2_190_550_859_753_437_195L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveUnusedRangeEndTest() {
        resolveUidService.resolve(2_305_843_009_213_693_950L);
    }
}
