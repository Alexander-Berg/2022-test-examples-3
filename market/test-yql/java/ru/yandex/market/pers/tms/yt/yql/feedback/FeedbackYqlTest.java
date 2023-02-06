package ru.yandex.market.pers.tms.yt.yql.feedback;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.tms.yt.yql.AbstractPersYqlTest;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 14.09.2021
 */
public class FeedbackYqlTest extends AbstractPersYqlTest {
    @Test
    public void testFeedback() {
        runTest(
            loadScript("/yql/feedback/feedback_to_grade.sql"),
            "/feedback/feedback_grade_expected.json",
            "/feedback/feedback_grade.mock"
        );
    }
}
