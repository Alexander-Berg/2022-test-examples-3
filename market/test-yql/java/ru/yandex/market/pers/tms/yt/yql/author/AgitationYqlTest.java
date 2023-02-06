package ru.yandex.market.pers.tms.yt.yql.author;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.tms.yt.yql.AbstractPersYqlTest;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 02.09.2021
 */
public class AgitationYqlTest extends AbstractPersYqlTest {

    @Test
    public void testAgitationVideoResult() {
        runTest(
            loadScript("/yql/author/agitation.sql").requestProperty("video_result"),
            "/agitation/video_result_expected.json",
            "/agitation/video_result.mock"
        );
    }

    @Test
    public void testAgitationGradeResult() {
        runTest(
            loadScript("/yql/author/agitation.sql").requestProperty("grade_result"),
            "/agitation/grade_result_expected.json",
            "/agitation/grade_result.mock"
        );
    }

    @Test
    public void testAgitationGradeTextResult() {
        runTest(
            loadScript("/yql/author/agitation.sql").requestProperty("grade_text_result"),
            "/agitation/grade_text_result_expected.json",
            "/agitation/grade_text_result.mock"
        );
    }

    @Test
    public void testAgitationGradePhotoResult() {
        runTest(
            loadScript("/yql/author/agitation.sql").requestProperty("grade_photo_result"),
            "/agitation/grade_photo_result_expected.json",
            "/agitation/grade_photo_result.mock"
        );
    }

    @Test
    public void testAllAgitationsExcludeCanceledWithWindow() {
        runTest(
            loadScript("/yql/author/agitation.sql").requestProperty("all_agitations_exclude_canceled_with_window"),
            "/agitation/all_agitations_exclude_canceled_with_window_expected.json",
            "/agitation/all_agitations_exclude_canceled_with_window.mock"
        );
    }

    @Test
    public void testAuthorSaas() {
        runTest(
            loadScript("/yql/author/author_saas.sql").requestProperty("yql"),
            "/agitation/author_saas_expected.json",
            "/agitation/author_saas.mock"
        );
    }
}
