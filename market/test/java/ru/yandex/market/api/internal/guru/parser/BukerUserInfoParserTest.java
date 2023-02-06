package ru.yandex.market.api.internal.guru.parser;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.internal.guru.data.BukerUserInfo;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class BukerUserInfoParserTest {

    private BukerUserInfoParser bukerUserInfoParser = new BukerUserInfoParser();

    @Test
    public void testParse() throws Exception {
        Map<Long, BukerUserInfo> actual = bukerUserInfoParser.parse(
            ResourceHelpers.getResource("buker-users-info.xml")
        );

        BukerUserInfo user = actual.get(12345l);
        Assert.assertEquals(12345l, user.getUid());
        Assert.assertEquals(5, user.getGrades());

        user = actual.get(23456l);
        Assert.assertEquals(23456l, user.getUid());
        Assert.assertEquals(2, user.getGrades());

        user = actual.get(34567l);
        Assert.assertEquals(34567l, user.getUid());
        Assert.assertEquals(1, user.getGrades());

        user = actual.get(45678l);
        Assert.assertEquals(45678l, user.getUid());
        Assert.assertEquals(2, user.getGrades());

        user = actual.get(56789l);
        Assert.assertEquals(56789l, user.getUid());
        Assert.assertEquals(1, user.getGrades());
    }
}
