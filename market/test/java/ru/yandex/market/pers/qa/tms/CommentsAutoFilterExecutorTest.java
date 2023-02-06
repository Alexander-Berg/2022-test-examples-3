package ru.yandex.market.pers.qa.tms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.cleanweb.CleanWebClient;
import ru.yandex.market.cleanweb.CleanWebContent;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.mock.AutoFilterServiceTestUtils;
import ru.yandex.market.pers.qa.mock.EntityForFilter;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.CommentStatus;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.State;
import ru.yandex.market.pers.qa.model.UserBanInfo;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.UserBanService;
import ru.yandex.market.pers.qa.tms.filter.CommentsAutoFilterExecutor;
import ru.yandex.market.pers.qa.utils.CommonUtils;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.pers.qa.mock.AutoFilterServiceTestUtils.ILLEGAL_WORD;

/**
 * @author korolyov
 * 20.06.18
 */
public class CommentsAutoFilterExecutorTest extends PersQaTmsTest {

    //N comments from user,  N comments from vendor
    private static final int N = 10;
    private static final long MODEL_ID = 1;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentsAutoFilterExecutor commentsAutoFilterExecutor;

    @Autowired
    private UserBanService banService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private CleanWebClient cleanWebClient;

    @Test
    @Disabled // TODO MARKETPERS-6201
    public void testFilterEnabledForAllProjects() {
        CommentProject[] commentProjects = CommentProject.values();
        List<CommentProject> autoFilteredCommentProjects = CommentService.getAutoFilteredProjects();

        Assertions.assertTrue(commentProjects.length == autoFilteredCommentProjects.size()
            && autoFilteredCommentProjects.containsAll(List.of(commentProjects)));
    }

    @Test
    public void testFilter() {
        // prepare data
        List<String> texts = AutoFilterServiceTestUtils.generateTextsForFilteringMod3(N);
        List<Long> commentsToBan = new ArrayList<>();
        Map<CommentProject, Long> projectRoot = new HashMap<>();
        for (int userId = 1; userId <= N; userId++) {
            for (CommentProject project : CommentService.getAutoFilteredProjects()) {
                final long rootId = buildRootId(project, userId);
                projectRoot.put(project, rootId);
                int textIndex = userId - 1;
                long commentId = commentService.createComment(project, userId, texts.get(textIndex), rootId);
                if (textIndex % 3 == 0) {
                    commentsToBan.add(commentId);
                }
            }
        }
        List<Long> vendorCommentIds = createVendorComments();

        // mock
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient);

        // run task
        commentsAutoFilterExecutor.filter();

        // check mod states
        checkCommentsStates(vendorCommentIds);
        // check comments count on each root
        checkCommentCount(projectRoot);
        // check status field in CommentProperties
        checkCommentsStatuses(commentsToBan);
    }

    @Test
    public void testWhiteFilter() {
        // create comments with bad word for all projects with different authors
        // add half of authors in white list
        // process auto filters and check results
        int userTypesSize = UserType.values().length;
        int vendorIdShift = userTypesSize * 4;
        int shopIdShift = userTypesSize * 8;
        List<Long> rejectedIds = new ArrayList<>();
        for (int userId = 1; userId <= userTypesSize * 2; userId++) {
            int userTypeId = userId % userTypesSize;
            UserType userType = UserType.valueOf(userTypeId);

            int authorId = userId;
            if (userType == UserType.SHOP) {
                authorId = shopIdShift + userId;
            } else if (userType == UserType.VENDOR) {
                authorId = vendorIdShift + userId;
            }

            // half of authors in white list
            if (userId < userTypesSize) {
                banService.trust(UserBanInfo.forever(
                    userType,
                    String.valueOf(authorId),
                    "tests",
                    12345L
                ));
            }

            for (CommentProject project : CommentService.getAutoFilteredProjects()) {
                final long rootId = buildRootId(project, userId);
                String text = ILLEGAL_WORD + " text " + userType.getDescription() + " " + project.getName();
                Long commentId = createComment(userId, userType, authorId, project, text, rootId);
                if (commentId != null && userId >= userTypesSize && !commentService.isAlwaysTrustedInText(project,
                    userType)) {
                    rejectedIds.add(commentId);
                }
            }
        }

        // mock
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient);

        // run task
        commentsAutoFilterExecutor.filter();

        // check result
        List<EntityForFilter> comments = getCommentsWithModStates();
        long commentProjects = CommentService.getAutoFilteredProjects().size();

        // there should be 2 comments for each comment except for yandexuid, brand & business
        assertEquals(2 * commentProjects * (userTypesSize - 3), comments.size());
        comments.forEach(comment -> {
            ModState modState;
            if (rejectedIds.contains(comment.id)) {
                modState = ModState.AUTO_FILTER_REJECTED;
            } else {
                modState = ModState.AUTO_FILTER_PASSED;
            }
            assertEquals(modState, comment.modState,
                String.format("Comment %s with text [%s] has correct mod state", comment.id, comment.text));
        });
    }

    @Test
    public void testSuggestFilter() {
        // create comments with bad word for all projects with different authors
        // add half of authors comments ban word in text
        // process auto filters and check results
        int userTypesSize = UserType.values().length;
        int vendorIdShift = userTypesSize * 4;
        int shopIdShift = userTypesSize * 8;
        List<Long> rejectedIds = new ArrayList<>();
        for (int userId = 1; userId <= userTypesSize * 2; userId++) {
            int userTypeId = userId % userTypesSize;
            UserType userType = UserType.valueOf(userTypeId);

            int authorId = userId;
            if (userType == UserType.SHOP) {
                authorId = shopIdShift + userId;
            } else if (userType == UserType.VENDOR) {
                authorId = vendorIdShift + userId;
            }


            for (CommentProject project : CommentService.getAutoFilteredProjects()) {
                final long rootId = buildRootId(project, userId);
                String text = "text " + userType.getDescription() + " project " + project.getName();
                // ban half of authors
                if (userId >= userTypesSize) {
                    text = ILLEGAL_WORD + " " + text;
                }
                Long commentId = createComment(userId, userType, authorId, project, text, rootId);
                if (commentId != null && userId >= userTypesSize && !commentService.isAlwaysTrustedInText(project,
                    userType)) {
                    rejectedIds.add(commentId);
                }
            }
        }

        // mock
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient);

        // run task
        commentsAutoFilterExecutor.filter();

        // check result
        List<EntityForFilter> comments = getCommentsWithModStates();
        long commentProjects = CommentService.getAutoFilteredProjects().size();

        // there should be 2 comments for each comment except for yandexuid, brand & business
        assertEquals(2 * commentProjects * (userTypesSize - 3), comments.size());
        comments.forEach(comment -> {
            ModState modState;
            if (rejectedIds.contains(comment.id)) {
                modState = ModState.AUTO_FILTER_REJECTED;
            } else {
                modState = ModState.AUTO_FILTER_PASSED;
            }
            assertEquals(modState, comment.modState,
                String.format("Comment %s with text [%s] has correct mod state", comment.id, comment.text));
        });
    }

    @Test
    public void testCleanWebFilter() {
        // create comments with bad word for all projects with different authors
        // add half of authors comments ban word in text
        // process auto filters and check results
        int userTypesSize = UserType.values().length;
        int vendorIdShift = userTypesSize * 4;
        int shopIdShift = userTypesSize * 8;
        List<Long> rejectedIds = new ArrayList<>();
        for (int userId = 1; userId <= userTypesSize * 2; userId++) {
            int userTypeId = userId % userTypesSize;
            UserType userType = UserType.valueOf(userTypeId);

            int authorId = userId;
            if (userType == UserType.SHOP) {
                authorId = shopIdShift + userId;
            } else if (userType == UserType.VENDOR) {
                authorId = vendorIdShift + userId;
            }

            for (CommentProject project : CommentService.getAutoFilteredProjects()) {
                final long rootId = buildRootId(project, userId);
                String text = "text " + userType.getDescription() + " project " + project.getName();
                // ban half of authors
                if (userId >= userTypesSize) {
                    text = ILLEGAL_WORD + " " + text;
                }
                Long commentId = createComment(userId, userType, authorId, project, text, rootId);
                if (commentId != null && userId >= userTypesSize && !commentService.isAlwaysTrustedInText(project,
                    userType)) {
                    rejectedIds.add(commentId);
                }
            }
        }

        // mock
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient);

        // run task
        commentsAutoFilterExecutor.filter();

        // check result
        List<EntityForFilter> comments = getCommentsWithModStates();
        long commentProjects = CommentService.getAutoFilteredProjects().size();

        // there should be 2 comments for each comment except for yandexuid, brand & business
        assertEquals(2 * commentProjects * (userTypesSize - 3), comments.size());
        comments.forEach(comment -> {
            ModState modState;
            if (rejectedIds.contains(comment.id)) {
                modState = ModState.AUTO_FILTER_REJECTED;
            } else {
                modState = ModState.AUTO_FILTER_PASSED;
            }
            assertEquals(modState, comment.modState,
                String.format("Comment %s with text [%s] has correct mod state", comment.id, comment.text));
        });
    }

    @Test
    public void testFilterBanned() {
        // create comments for all projects with different authors
        int userTypesSize = UserType.values().length;
        int vendorIdShift = userTypesSize * 4;
        int shopIdShift = userTypesSize * 8;
        List<Long> rejectedIds = new ArrayList<>();
        for (int userId = 1; userId <= userTypesSize * 2; userId++) {
            int userTypeId = userId % userTypesSize;
            UserType userType = UserType.valueOf(userTypeId);

            int authorId = userId;
            if (userType == UserType.SHOP) {
                authorId = shopIdShift + userId;
            } else if (userType == UserType.VENDOR) {
                authorId = vendorIdShift + userId;
            }

            // ban half of authors
            if (userId >= userTypesSize) {
                banService.ban(UserBanInfo.forever(
                    userType,
                    String.valueOf(authorId),
                    "tests",
                    12345L
                ));
            }

            for (CommentProject project : CommentService.getAutoFilteredProjects()) {
                final long rootId = buildRootId(project, userId);
                String text = "text " + userType.name();
                Long commentId = createComment(userId, userType, authorId, project, text, rootId);
                if (commentId != null && userId >= userTypesSize) {
                    rejectedIds.add(commentId);
                }
            }
        }

        // mock
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient);

        // run task
        commentsAutoFilterExecutor.filter();

        // check result
        List<EntityForFilter> comments = getCommentsWithModStates();
        long commentProjects = CommentService.getAutoFilteredProjects().size();

        // there should be 2 comments for each comment except for yandexuid, brand & business
        assertEquals(2 * commentProjects * (userTypesSize - 3), comments.size());
        comments.forEach(comment -> {
            ModState modState;
            if (rejectedIds.contains(comment.id)) {
                modState = ModState.AUTO_FILTER_REJECTED;
            } else {
                modState = ModState.AUTO_FILTER_PASSED;
            }
            assertEquals(modState, comment.modState,
                String.format("Comment %s with text [%s] has correct mod state", comment.id, comment.text));
        });
    }

    @Test
    public void testRecheckAfterUnknownStatus() {
        //mock

        final Question question = questionService.createModelQuestion(1, "Question", 1);
        final Answer answer = answerService.createAnswer(1, "Answer", question.getId());
        commentService.createAnswerComment(1, "text", answer.getId());

        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient, ModState.AUTO_FILTER_UNKNOWN);
        commentsAutoFilterExecutor.filter();

        List<EntityForFilter> comments = getCommentsWithModStates();
        assertEquals(1, comments.size());
        assertEquals(ModState.AUTO_FILTER_PASSED, comments.get(0).modState);
        assertEquals(0, getCommentsStatusesBanned());
        Assertions.assertEquals(1, getRechecks().size());


        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient, ModState.AUTO_FILTER_PASSED);
        commentsAutoFilterExecutor.filter();

        comments = getCommentsWithModStates();
        assertEquals(1, comments.size());
        assertEquals(ModState.AUTO_FILTER_PASSED, comments.get(0).modState);
        assertEquals(0, getCommentsStatusesBanned());
        assertTrue(getRechecks().isEmpty(), "must be empty");
    }

    @Test
    public void testRecheckAfterEdit() {
        //mock
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient, ModState.AUTO_FILTER_PASSED);

        final Question question = questionService.createModelQuestion(1, "Question", 1);
        final Answer answer = answerService.createAnswer(1, "Answer", question.getId());
        long commentId = commentService.createAnswerComment(1, "text", answer.getId());

        // check comment is fine
        commentService.autoFilterComments(CommentProject.QA);
        List<EntityForFilter> comments = getCommentsWithModStates();
        assertEquals(1, comments.size());
        assertEquals(ModState.AUTO_FILTER_PASSED, comments.get(0).modState);
        assertEquals(0, getCommentsStatusesBanned());
        assertTrue(getRechecks().isEmpty(), "must be empty");

        ArgumentCaptor<CleanWebContent> cleanWebClientArgumentCaptor = ArgumentCaptor.forClass(CleanWebContent.class);
        verify(cleanWebClient).sendContent(cleanWebClientArgumentCaptor.capture(), anyBoolean());
        CleanWebContent value = cleanWebClientArgumentCaptor.getValue();
        assertEquals(Map.of("uid", "1", "is_shop", false, "is_brand", false), value.getParameters());

        // edit comment - now in recheck
        commentService.editComment(CommentProject.QA, commentId, "fixed text", UserInfo.uid(1));
        assertEquals(1, getRechecks().size());

        // run filers again - it is now fine
        commentService.autoFilterComments(CommentProject.QA);

        comments = getCommentsWithModStates();
        assertEquals(1, comments.size());
        assertEquals(ModState.AUTO_FILTER_PASSED, comments.get(0).modState);
        assertEquals(0, getCommentsStatusesBanned());
        assertTrue(getRechecks().isEmpty(), "must be empty");
    }

    @Test
    public void testCleanWebServiceRequestAllFields() {
        //mock
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient, ModState.AUTO_FILTER_PASSED);

        final Question question = questionService.createModelQuestion(1, "Question", 1);
        final Answer answer = answerService.createAnswer(1, "Answer", question.getId());
        long commentId = commentService.createAnswerComment(1, "text", answer.getId());
        //add security data
        jdbcTemplate.update(
            "insert into qa.security_data (entity_type, entity_id, ip, user_agent, headers) " +
            "values (?, ?, ?, ?, ?)",
            QaEntityType.getByCommentProject(CommentProject.QA).getValue(),
            commentId,
            "ip",
            "user_agent",
            SECURITY_DATA_HEADERS
        );

        commentService.autoFilterComments(CommentProject.QA);

        ArgumentCaptor<CleanWebContent> cleanWebClientArgumentCaptor = ArgumentCaptor.forClass(CleanWebContent.class);
        verify(cleanWebClient).sendContent(cleanWebClientArgumentCaptor.capture(), anyBoolean());
        CleanWebContent value = cleanWebClientArgumentCaptor.getValue();
        assertEquals(
            Map.of(
                "uid", "1",
                "ip", "ip",
                "user_agent", "user_agent",
                "is_shop", false,
                "is_brand", false,
                CommonUtils.HEADER_ICOOKIE, "icookie"
            ),
            value.getParameters()
        );
    }

    @Test
    public void testCleanWebServiceRequestWithNullFields() {
        //mock
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient, ModState.AUTO_FILTER_PASSED);

        final Question question = questionService.createModelQuestion(1, "Question", 1);
        final Answer answer = answerService.createAnswer(1, "Answer", question.getId());
        long commentId = commentService.createAnswerComment(1, "text", answer.getId());
        //add security data
        jdbcTemplate.update(
            "insert into qa.security_data (entity_type, entity_id, ip, user_agent, headers) " +
            "values (?, ?, ?, ?, ?)",
            QaEntityType.getByCommentProject(CommentProject.QA).getValue(),
            commentId,
            "ip",
            null,
            null
        );

        commentService.autoFilterComments(CommentProject.QA);

        ArgumentCaptor<CleanWebContent> cleanWebClientArgumentCaptor = ArgumentCaptor.forClass(CleanWebContent.class);
        verify(cleanWebClient).sendContent(cleanWebClientArgumentCaptor.capture(), anyBoolean());
        CleanWebContent value = cleanWebClientArgumentCaptor.getValue();
        assertEquals(
            Map.of("uid", "1", "ip", "ip", "is_shop", false, "is_brand", false),
            value.getParameters()
        );
    }

    @Test
    public void testRejectCommentOnPremoderation() {
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION, true);
        long commentId = commentService.createComment(CommentProject.ARTICLE, 1, ILLEGAL_WORD, 123);

        Map<Long, CommentStatus> commentsStatuses = getCommentsStatuses();
        assertEquals(CommentStatus.PREMODERATION, commentsStatuses.entrySet().stream().findFirst().get().getValue());

        // mock
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient);

        // run task
        commentsAutoFilterExecutor.filter();

        // check result
        commentsStatuses = getCommentsStatuses();
        assertEquals(CommentStatus.REJECTED_BY_REGEXP, commentsStatuses.entrySet().stream().findFirst().get().getValue());
    }

    public void testCleanWebFilterNonUserComment() {
        expFlagService.setFlag(CommentService.FLAG_MODERATE_SHOP_COMMENTS, true);
        expFlagService.setFlag(CommentService.FLAG_MODERATE_VENDOR_COMMENTS, true);

        Map<Long, CommentStatus> commentsStatuses = getCommentsStatuses();

        // mock
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient);

        // run task
        commentsAutoFilterExecutor.filter();

        // check result
        commentsStatuses = getCommentsStatuses();
        assertEquals(CommentStatus.REJECTED_BY_REGEXP, commentsStatuses.entrySet().stream().findFirst().get().getValue());
    }

    @Test
    public void testCleanWebFilterShopComment() {
        commentService.createShopComment(CommentProject.ARTICLE, 1, ILLEGAL_WORD, 123, 123);
        testCleanWebFilterNonUserComment();
    }

    @Test
    public void testCleanWebFilterVendorComment() {
        commentService.createVendorComment(CommentProject.ARTICLE, 2, ILLEGAL_WORD, 123, 1234);
        testCleanWebFilterNonUserComment();
    }

    @Test
    public void testFilterWithText() {
        // prepare data
        List<String> texts = AutoFilterServiceTestUtils.generateTextsForCheckAutoFilterRegexp();
        List<Long> commentsToBan = new ArrayList<>();
        Map<CommentProject, Long> projectRoot = new HashMap<>();
        for (int userId = 1; userId <= texts.size(); userId++) {
            for (CommentProject project : CommentService.getAutoFilteredProjects()) {
                final long rootId = buildRootId(project, userId);
                projectRoot.put(project, rootId);
                int textIndex = userId - 1;
                long commentId = commentService.createComment(project, userId, texts.get(textIndex), rootId);
                commentsToBan.add(commentId);
            }
        }

        // mock
        AutoFilterServiceTestUtils.mockCleanWebClient(cleanWebClient);

        // run task
        commentsAutoFilterExecutor.filter();

        checkCommentsStatuses(commentsToBan);
    }


    @NotNull
    private List<Long> getRechecks() {
        return jdbcTemplate.queryForList("SELECT id FROM qa.auto_recheck", Long.class);
    }

    private long buildRootId(CommentProject project, int userId) {
        if (project == CommentProject.POST) {
            Question question = questionService.createQuestion(Question.buildInterestPost(userId,
                "post text",
                "post title",
                0), null);
            return question.getId();
        } else {
            // create roots as answer ids to simplify tests and make root unique
            final Question question = questionService.createModelQuestion(userId,
                "Test question? " + project,
                MODEL_ID);
            return answerService.createAnswer(userId, "Answer!", question.getId()).getId();
        }
    }

    protected Long createComment(int userId,
                                 UserType userType,
                                 int authorId,
                                 CommentProject project,
                                 String text,
                                 long rootId) {
        switch (userType) {
            case SHOP:
                return commentService.createShopComment(project, userId, text, rootId, authorId);
            case VENDOR:
                return commentService.createVendorComment(project, userId, text, rootId, authorId);
            case UID:
                return commentService.createComment(project, userId, text, rootId);
            // except for yandexuid and business yet
            //todo: fix this later with comments for posts
            case YANDEXUID:
            case BUSINESS:
            default:
                return null;
        }
    }

    private List<Long> createVendorComments() {
        List<Long> vendorCommentIds = new ArrayList<>();
        List<String> texts = AutoFilterServiceTestUtils.generateTextsForFilteringMod3(N);
        for (int userId = 1, brandId = 100500; userId <= N; userId++, brandId++) {
            final Question question = questionService.createModelQuestion(userId,
                UUID.randomUUID().toString(),
                MODEL_ID);
            final Answer answer = answerService.createAnswer(userId, UUID.randomUUID().toString(), question.getId());
            final long commentId = commentService.createVendorQaComment(userId,
                texts.get(userId - 1),
                answer.getId(),
                userId);
            vendorCommentIds.add(commentId);
        }
        return vendorCommentIds;
    }

    private List<EntityForFilter> getCommentsWithModStates() {
        return jdbcTemplate.query(
            "SELECT *\n" +
                "FROM com.comment",
            (rs, rowNum) -> {
                EntityForFilter entityForFilter = new EntityForFilter();
                entityForFilter.id = Long.parseLong(rs.getString("id"));
                entityForFilter.userId = Long.parseLong(rs.getString("user_id"));
                entityForFilter.text = rs.getString("text");
                entityForFilter.modState = rs.getInt("state") == State.NEW.getValue()
                    && !CommentStatus.getById(rs.getInt("mod_state")).isBanned()
                    ? ModState.AUTO_FILTER_PASSED : ModState.AUTO_FILTER_REJECTED;
                entityForFilter.project = CommentProject.getByProjectId(rs.getInt("project"));
                return entityForFilter;
            });
    }

    private long getCommentsStatusesBanned() {
        return getCommentsStatuses().values().stream()
            .filter(CommentStatus::isBanned)
            .count();
    }

    private Map<Long, CommentStatus> getCommentsStatuses() {
        Map<Long, CommentStatus> map = new HashMap<>();
        jdbcTemplate.query(
            "SELECT id, mod_state FROM com.comment",
            (rs, rowNum) -> {
                map.put(
                    rs.getLong("id"),
                    CommentStatus.getById(rs.getInt("mod_state"))
                );
                return null;
            });
        return map;
    }

    private void checkCommentsStatuses(List<Long> commentsToBan) {
        Map<Long, CommentStatus> statusMap = getCommentsStatuses();
        commentsToBan.forEach(id -> assertEquals(CommentStatus.REJECTED_BY_REGEXP, statusMap.get(id)));
    }

    private void checkCommentsStates(List<Long> vendorCommentIds) {
        List<EntityForFilter> comments = getCommentsWithModStates();
        long commentProjects = CommentService.getAutoFilteredProjects().size();
        assertEquals(commentProjects * N + vendorCommentIds.size(), comments.size());
        comments.forEach(comment -> {
            ModState modState;
            if (vendorCommentIds.contains(comment.id)) {
                modState = ModState.AUTO_FILTER_PASSED;
            } else {
                switch ((int) (comment.userId % 3)) {
                    case 1:
                        modState = ModState.AUTO_FILTER_REJECTED;
                        break;
                    case 0:
                    case 2:
                        modState = ModState.AUTO_FILTER_PASSED; //same as ModState.AUTO_FILTER_UNKNOWN for comments
                        break;
                    default:
                        throw new IllegalStateException("Never happened");
                }
            }
            assertEquals(modState, comment.modState,
                String.format("Comment %s with text [%s] has correct mod state", comment.id, comment.text));
        });
    }

    private void checkCommentCount(Map<CommentProject, Long> projectRoot) {
        for (CommentProject project : CommentService.getAutoFilteredProjects()) {
            long id = projectRoot.get(project);
            Long count = getExpectedCommentsCount(id, project.getProjectId());
            final List<Long> childCount = jdbcTemplate.queryForList(
                "SELECT child_count FROM com.child_count where root_id = ? and project = ?",
                Long.class, id, project.getProjectId());
            assertEquals(1, childCount.size());
            assertEquals(count,
                childCount.get(0),
                "comments count equals in com.comment and com.child_count root_id=" + id + ",project=" + project
                    .getProjectId());
        }
    }

    private Long getExpectedCommentsCount(long rootId, long project) {
        final List<Long> counts = jdbcTemplate.queryForList(
            "SELECT count(*) FROM com.v_pub_comment where root_id = ? and project = ?",
            Long.class, rootId, project);
        assertEquals(1, counts.size());
        return counts.get(0);
    }
}
