package ru.yandex.market.pers.tms.yt.yql.tables;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.tms.yt.yql.AbstractPersYqlTest;

/**
 * @author grigor-vlad
 * 29.04.2022
 */
public class GradeAgitationPushYqlTest extends AbstractPersYqlTest {

    @Test
    public void testGradeAgitationPush() {
        runTest(loadScript("/yql/tables/grade-agitation-push.sql"),
            "/tables/grade_agitation_push_expected.json",
            "/tables/grade_agitation_push.mock");
    }
}
