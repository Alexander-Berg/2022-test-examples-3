package ru.yandex.market.pers.tms.yt.yql.tables;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.tms.yt.yql.AbstractPersYqlTest;

/**
 * @author grigor-vlad
 * 28.04.2022
 */
public class AnswerAgitationPushYqlTest extends AbstractPersYqlTest {

    @Test
    public void testAnswerAgitationPush() {
        runTest(loadScript("/yql/tables/answer_agitation_push.sql"),
            "/tables/answer_agitation_push_expected.json",
            "/tables/answer_agitation_push.mock");
    }
}
