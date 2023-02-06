package ru.yandex.market.pers.grade.web.grade;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.cache.AchievementCacher;
import ru.yandex.market.pers.grade.client.model.achievements.UserAchievement;
import ru.yandex.market.pers.grade.core.achievements.AchievementEntityType;
import ru.yandex.market.pers.grade.core.achievements.AchievementEventType;
import ru.yandex.market.pers.grade.core.achievements.AchievementType;
import ru.yandex.market.pers.grade.core.article.model.ArticleModState;
import ru.yandex.market.pers.grade.core.article.service.ArticleModerationService;
import ru.yandex.market.pers.grade.core.service.AchievementsResolverService;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author vvolokh
 * 09.04.2019
 */
public class ArticleAchievementsTest extends AchievementsBaseControllerTest {
    private static final long ARTICLE_ID = 1L;
    private static final int REVISION = 0;

    @Autowired
    ArticleModerationService moderationService;

    @Autowired
    private AchievementsResolverService achievementsResolverService;

    @Autowired
    private AchievementCacher achievementCacher;

    @Before
    public void enableAchievement() {
        switchAchievementState(1);
    }

    public void switchAchievementState(int enabled) {
        pgJdbcTemplate.update("update achievement set enabled = ? where id = ?", enabled, AchievementType.BLOGGER.value());
    }

    @Test
    public void testDisabledAchievement() throws Exception {
        switchAchievementState(0);
        List<UserAchievement> expectedAchievements = Collections.emptyList();

        createArticleOnModeration(ARTICLE_ID, REVISION);

        assertAchievements(expectedAchievements);
    }

    @Test
    public void testAchievementEventOnArticleCreate() throws Exception {
        List<UserAchievement> expectedAchievements =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 1, 0));

        createArticleOnModeration(ARTICLE_ID, REVISION);

        assertAchievements(expectedAchievements);
    }

    @Test
    public void testSingleAchievementOnMultipleRevisions() throws Exception {
        List<UserAchievement> expectedAchievements =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 1, 0));

        createArticleOnModeration(ARTICLE_ID, REVISION);
        assertAchievements(expectedAchievements);

        createArticleOnModeration(ARTICLE_ID, REVISION + 1);
        achievementsModerationAndCacheClean();
        assertAchievements(expectedAchievements);
    }

    @Test
    public void testConfirmOnApproveArticle() throws Exception {
        List<UserAchievement> expectedAchievementsBeforeMod =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 1, 0));
        List<UserAchievement> expectedAchievementsAfterMod =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 0, 1));

        createArticleOnModeration(ARTICLE_ID, REVISION);
        Long articleModerationId = getArticleModerationId();
        assertAchievements(expectedAchievementsBeforeMod);

        moderationService.moderate(articleModerationId, ARTICLE_ID, REVISION, ArticleModState.APPROVED, null,
            FAKE_USER);
        achievementsModerationAndCacheClean();
        assertAchievements(expectedAchievementsAfterMod);
    }

    @Test
    public void testRejectOnRejectArticle() throws Exception {
        List<UserAchievement> expectedAchievementsBeforeMod =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 1, 0));
        List<UserAchievement> expectedAchievementsAfterMod =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 0, 0));

        createArticleOnModeration(ARTICLE_ID, REVISION);
        Long articleModerationId = getArticleModerationId();
        assertAchievements(expectedAchievementsBeforeMod);

        moderationService.moderate(articleModerationId, ARTICLE_ID, REVISION, ArticleModState.REJECTED, null,
            FAKE_USER);
        achievementsModerationAndCacheClean();
        assertAchievements(expectedAchievementsAfterMod);
        assertAchievementEventType(articleModerationId, AchievementEventType.REJECTED);
    }

    @Test
    public void testDeleteOnDeleteArticleAfterReject() throws Exception {
        List<UserAchievement> expectedAchievementsBeforeMod =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 1, 0));
        List<UserAchievement> expectedAchievementsAfterMod =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 0, 0));

        createArticleOnModeration(ARTICLE_ID, REVISION);
        Long articleModerationId = getArticleModerationId();
        assertAchievements(expectedAchievementsBeforeMod);

        moderationService.moderate(articleModerationId, ARTICLE_ID, REVISION, ArticleModState.REJECTED, null,
            FAKE_USER);
        achievementsModerationAndCacheClean();
        assertAchievements(expectedAchievementsAfterMod);
        assertAchievementEventType(articleModerationId, AchievementEventType.REJECTED);

        moderationService.stopArticleModeration(ARTICLE_ID);
        achievementsModerationAndCacheClean();
        assertAchievements(expectedAchievementsAfterMod);
        assertAchievementEventType(articleModerationId, AchievementEventType.DELETED);
    }

    @Test
    public void testDeleteOnDeleteArticleAfterCreate() throws Exception {
        List<UserAchievement> expectedAchievementsBeforeMod =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 1, 0));
        List<UserAchievement> expectedAchievementsAfterMod =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 0, 0));

        createArticleOnModeration(ARTICLE_ID, REVISION);
        Long articleModerationId = getArticleModerationId();
        assertAchievements(expectedAchievementsBeforeMod);

        moderationService.stopArticleModeration(ARTICLE_ID);
        achievementsModerationAndCacheClean();
        assertAchievements(expectedAchievementsAfterMod);
        assertAchievementEventType(articleModerationId, AchievementEventType.DELETED);
    }

    @Test
    public void testDeleteOnDeleteArticleAfterApprove() throws Exception {
        List<UserAchievement> expectedAchievementsBeforeMod =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 1, 0));
        List<UserAchievement> expectedAchievementsAfterMod =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 0, 1));

        createArticleOnModeration(ARTICLE_ID, REVISION);
        Long articleModerationId = getArticleModerationId();
        assertAchievements(expectedAchievementsBeforeMod);

        moderationService.moderate(articleModerationId, ARTICLE_ID, REVISION, ArticleModState.APPROVED, null,
            FAKE_USER);
        achievementsModerationAndCacheClean();
        assertAchievements(expectedAchievementsAfterMod);
        assertAchievementEventType(articleModerationId, AchievementEventType.CONFIRMED);

        moderationService.stopArticleModeration(ARTICLE_ID);
        achievementsModerationAndCacheClean();
        assertAchievements(expectedAchievementsAfterMod);
        assertAchievementEventType(articleModerationId, AchievementEventType.CONFIRMED);
    }

    private void assertAchievementEventType(Long articleModerationId, AchievementEventType expectedEventType) {
        AchievementEventType actualEventType = AchievementEventType.of(
            pgJdbcTemplate.queryForObject(
                "SELECT event_type FROM ACHIEVEMENT_EVENT WHERE entity_id = ? AND entity_type = ?", Integer.class,
                articleModerationId,
                AchievementEntityType.ARTICLE.getValue()));
        assertEquals(expectedEventType, actualEventType);
    }

    @Test
    public void testRejectAfterConfirmedRevision() throws Exception {
        List<UserAchievement> expectedAchievementsBeforeMod =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 1, 0));
        List<UserAchievement> expectedAchievementsAfterMod =
            Collections.singletonList(new UserAchievement(AchievementType.BLOGGER.value(), 0, 1));

        createArticleOnModeration(ARTICLE_ID, REVISION);
        Long articleModerationId = getArticleModerationId();
        assertAchievements(expectedAchievementsBeforeMod);
        moderationService.moderate(articleModerationId, ARTICLE_ID, REVISION, ArticleModState.APPROVED, null,
            FAKE_USER);
        achievementsModerationAndCacheClean();
        assertAchievements(expectedAchievementsAfterMod);

        createArticleOnModeration(ARTICLE_ID, REVISION + 1);
        Long nextArticleModerationId = getArticleModerationId();
        moderationService.moderate(nextArticleModerationId, ARTICLE_ID, (REVISION + 1), ArticleModState.REJECTED, null,
            FAKE_USER);
        achievementsModerationAndCacheClean();
        assertAchievements(expectedAchievementsAfterMod);
    }

    private void assertAchievements(List<UserAchievement> expectedAchievementsBeforeMod) throws Exception {
        List<UserAchievement> userAchievements = getUserAchievements(FAKE_USER);
        assertListAchievementsEquals(expectedAchievementsBeforeMod, userAchievements);
    }

    private Long getArticleModerationId() {
        return pgJdbcTemplate.queryForObject(
            "SELECT id FROM article_moderation WHERE article_id = ? ORDER BY CR_TIME DESC FETCH FIRST 1 ROW ONLY",
            Long.class, ARTICLE_ID);
    }

    private void createArticleOnModeration(long articleId, int revision) throws Exception {
        mockMvc.perform(
            post("/api/article/moderation?articleId=" + articleId + "&revision=" + revision + "&userId=" + FAKE_USER))
            .andDo(print())
            .andExpect(status().is2xxSuccessful());
    }

    private void achievementsModerationAndCacheClean() throws Exception {
        achievementsResolverService.resolvePendingSync();
        achievementsResolverService.resolveRejectedSync();
        achievementCacher.cleanForUser(FAKE_USER);
    }
}
