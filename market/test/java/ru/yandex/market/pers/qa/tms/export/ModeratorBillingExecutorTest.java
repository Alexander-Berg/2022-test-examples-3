package ru.yandex.market.pers.qa.tms.export;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.core.expimp.storage.QueryToStorageExtractor;
import ru.yandex.market.core.expimp.storage.export.storage.Uploader;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.model.Complaint;
import ru.yandex.market.pers.qa.model.ModerationEntityType;
import ru.yandex.market.pers.qa.model.QaEntityFeature;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.service.ModerationService;

import static org.mockito.ArgumentMatchers.any;

class ModeratorBillingExecutorTest extends PersQaTmsTest {

    private final long MODERATOR_ID_1 = 1324;
    private final long MODERATOR_ID_2 = 1325;
    private final long MODERATOR_ID_3 = 1326;


    public static final String MODERATION_BILLING_QUERY = "select moderator_id, mod_day, " +
            "comment_complaint_post, " +
            "comment_complaint_video, " +
            "comment_complaint_article, " +
            "comment_complaint_versus, " +
            "comment_complaint_grade, " +
            "comment_complaint_qa, " +
            "video_author " +
            "from qa.v_moderation_for_billing " +
            "where moderator_id is not null";

    @Autowired
    private ModerationService moderationService;
    @Autowired
    private ModeratorBillingExportExecutor executor;
    @Autowired
    private QueryToStorageExtractor queryToStorageExtractor;
    @Captor
    private ArgumentCaptor<String> queryArgCapture;
    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Test
    void testModerationBillingCommentComplaintPost() {
        moderationBillingTest(QaEntityType.COMMENT_POST, ModerationEntityType.COMPLAINT_COMMENT_POST);
    }

    @Test
    void testModerationBillingCommentComplaintVideo() {
        moderationBillingTest(QaEntityType.COMMENT_POST, ModerationEntityType.COMPLAINT_COMMENT_POST);
    }

    @Test
    void testModerationBillingCommentComplaintArticle() {
        moderationBillingTest(QaEntityType.COMMENT_ARTICLE, ModerationEntityType.COMPLAINT_COMMENT_ARTICLE);
    }

    @Test
    void testModerationBillingCommentComplaintVersus() {
        moderationBillingTest(QaEntityType.COMMENT_VERSUS, ModerationEntityType.COMPLAINT_COMMENT_VERSUS);
    }

    @Test
    void testModerationBillingCommentComplaintGrade() {
        moderationBillingTest(QaEntityType.COMMENT_GRADE, ModerationEntityType.COMPLAINT_COMMENT_GRADE);
    }

    @Test
    void testModerationBillingCommentComplaintQa() {
        moderationBillingTest(QaEntityType.COMMENT, ModerationEntityType.COMPLAINT_COMMENT_QA);
    }


    @Test
    void testBillingCommentComplaintsAllProjects() {
        List<QaEntityType> allCommentTypes = Stream.of(CommentProject.values())
            .map(QaEntityType::getByCommentProject)
            .filter(Objects::nonNull)
            .filter(it -> it.support(QaEntityFeature.COMPLAINING))
            .collect(Collectors.toList());
        Assertions.assertTrue(allCommentTypes.stream().allMatch(it -> ModerationEntityType.getModerationByType(it) != null));
    }

    @Test
    void testBillingOnDifferentTypes() {
        Complaint complaint1 = new Complaint(1, QaEntityType.COMMENT_POST);
        Complaint complaint2 = new Complaint(2, QaEntityType.COMMENT_VIDEO);
        Complaint complaint3 = new Complaint(2, QaEntityType.COMMENT_VIDEO);
        moderationService.moderate(Arrays.asList(complaint1, complaint2, complaint3), MODERATOR_ID_1);

        executor.exportModeratorBilling();

        checkModerator(MODERATOR_ID_1, ModerationEntityType.COMPLAINT_COMMENT_POST, 1);
        checkModerator(MODERATOR_ID_1, ModerationEntityType.COMPLAINT_COMMENT_VIDEO, 2);
    }

    void moderationBillingTest(QaEntityType type, ModerationEntityType moderationType) {
        List<Complaint> complaints = Stream.iterate(1L, n -> n + 1).limit(10)
            .map(it -> new Complaint(it, type))
            .collect(Collectors.toList());
        moderationService.moderate(complaints, MODERATOR_ID_1);
        long countForModerator1 = complaints.size();

        complaints = Stream.iterate(5L, n -> n + 1).limit(6)
            .map(it -> new Complaint(it, type))
            .collect(Collectors.toList());
        moderationService.moderate(complaints, MODERATOR_ID_2);
        long countForModerator2 = complaints.size();

        executor.exportModeratorBilling();

        Mockito.verify(queryToStorageExtractor).process(queryArgCapture.capture(), any(Uploader.class));
        Assertions.assertEquals(MODERATION_BILLING_QUERY, queryArgCapture.getValue());

        checkModerator(MODERATOR_ID_1, moderationType, countForModerator1);
        checkModerator(MODERATOR_ID_2, moderationType, countForModerator2);
        checkModerator(MODERATOR_ID_3, moderationType, 0);
    }

    @Test
    void testModerationBillingVideoAuthor() {
        moderationService.moderateEntity(ModerationEntityType.VIDEO_AUTHOR, List.of(1L, 2L, 3L), MODERATOR_ID_1);
        moderationService.moderateEntity(ModerationEntityType.VIDEO_AUTHOR, List.of(3L, 4L), MODERATOR_ID_2);

        executor.exportModeratorBilling();

        Mockito.verify(queryToStorageExtractor).process(queryArgCapture.capture(), any(Uploader.class));
        Assertions.assertEquals(MODERATION_BILLING_QUERY, queryArgCapture.getValue());

        checkModerator(MODERATOR_ID_1, ModerationEntityType.VIDEO_AUTHOR, 3);
        checkModerator(MODERATOR_ID_2, ModerationEntityType.VIDEO_AUTHOR, 2);
        checkModerator(MODERATOR_ID_3, ModerationEntityType.VIDEO_AUTHOR, 0);
    }

    private void checkModerator(long moderatorId, ModerationEntityType type, long expectedCount) {
        long countReal = jdbcTemplate.queryForObject(
            "select coalesce(sum(" + type.getFieldInBillingView() + "), 0) from qa.v_moderation_for_billing where " +
                "moderator_id = ?",
            Long.class, moderatorId);
        Assertions.assertEquals(expectedCount, countReal);
    }

}
