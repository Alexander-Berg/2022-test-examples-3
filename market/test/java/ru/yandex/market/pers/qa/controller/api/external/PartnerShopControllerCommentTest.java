package ru.yandex.market.pers.qa.controller.api.external;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.GradeClient;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.mock.mvc.GradeShopCommentMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.PartnerShopMvcMocks;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.qa.service.CommentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.model.CommentProject.GRADE;
import static ru.yandex.market.pers.qa.controller.api.comment.AbstractCommentControllerTest.getBody;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.assertCommentsSet;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.assertForest;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkAuthor;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkCanDelete;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkComment;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkFlatTree;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkNoParent;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkParams;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkParamsHas;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkParent;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkPublished;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkRoot;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkSubComments;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkText;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkUser;
import static ru.yandex.market.pers.qa.utils.CommentUtils.PARAM_GRADE_TYPE;
import static ru.yandex.market.pers.qa.utils.CommentUtils.PARAM_GRADE_TYPE_SHOP;
import static ru.yandex.market.pers.qa.utils.CommentUtils.PARAM_SHOP_ID;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 12.04.2019
 */
public class PartnerShopControllerCommentTest extends QAControllerTest {
    private static final long SHOP_ID = 3458276452L;
    private static final long SHOP_ID_OTHER = 239859235L;

    @Autowired
    private CommentService commentService;
    @Autowired
    private GradeShopCommentMvcMocks gradeShopCommentMvcMocks;
    @Autowired
    private GradeClient gradeClient;
    @Autowired
    private PartnerShopMvcMocks partnerShopMvc;

    @Test
    void testCreateGradeCommentCheckFormat() {
        long commentId = commentService.createShopComment(CommentProject.GRADE, UID, "Other comment", 123, SHOP_ID);

        assertEquals(
                "Other comment",
            qaJdbcTemplate.queryForObject(
                "select c.text from com.comment c where c.id = ?",
                String.class,
                commentId
            ));
    }

    @Test
    void testCreateComment() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId);

        Long[] ids = {
            partnerShopMvc.createAnswerComment(SHOP_ID, UID, answerId, "First one here!"),
            commentService.createAnswerComment(UID + 1, "User comment", answerId),
            commentService.createAnswerComment(UID, "User comment (shop manager)", answerId),
            partnerShopMvc.createAnswerComment(SHOP_ID_OTHER, UID + 2, answerId, "Second shop comment"),
            0L
        };

        // and single premod comment by vendor in root2
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION_SHOP, true);
        ids[4] = partnerShopMvc.createAnswerComment(SHOP_ID, UID + 2, answerId, "Premod shop comment");

        // check result
        assertCommentsSet(partnerShopMvc.getAnswerComments(SHOP_ID, UID, answerId),
            checkComment(ids[4],
                checkRoot(CommentProject.QA, answerId),
                checkNoParent(),
                checkText("Premod shop comment"),
                checkUser(UserInfo.uid(UID + 2)),
                checkAuthor(new UserInfo(UserType.SHOP, SHOP_ID)),
                checkCanDelete(true),
                checkPublished(true)),
            checkComment(ids[3],
                checkRoot(CommentProject.QA, answerId),
                checkNoParent(),
                checkText("Second shop comment"),
                checkUser(UserInfo.uid(UID + 2)),
                checkAuthor(new UserInfo(UserType.SHOP, SHOP_ID_OTHER)),
                checkCanDelete(false),
                checkParams(Map.of(PARAM_SHOP_ID, String.valueOf(SHOP_ID_OTHER)))),
            checkComment(ids[2],
                checkRoot(CommentProject.QA, answerId),
                checkNoParent(),
                checkText("User comment (shop manager)"),
                checkUser(UserInfo.uid(UID)),
                checkAuthor(UserInfo.uid(UID)),
                checkCanDelete(false),
                checkParams(Map.of())),
            checkComment(ids[1],
                checkRoot(CommentProject.QA, answerId),
                checkNoParent(),
                checkText("User comment"),
                checkUser(UserInfo.uid(UID + 1)),
                checkAuthor(UserInfo.uid(UID + 1)),
                checkCanDelete(false),
                checkParams(Map.of())),
            checkComment(ids[0],
                checkRoot(CommentProject.QA, answerId),
                checkNoParent(),
                checkText("First one here!"),
                checkUser(UserInfo.uid(UID)),
                checkAuthor(new UserInfo(UserType.SHOP, SHOP_ID)),
                checkCanDelete(true),
                checkParams(Map.of(PARAM_SHOP_ID, String.valueOf(SHOP_ID))))
        );
    }


    @Test
    void testCreateGradeComment() throws Exception {
        //    g1
        //    - c1(sh)
        //     |- c3(u)
        //       |- c4(sh)
        //    - c2(u)
        //
        //    g2
        //    - с5(u)
        //     |- c6(sh)
        //    - c7(u)
        //    - c8(sh)
        // viewed backwards
        long[] gradeIds = {1, 2, 3};

        // ensure to check with mocked fixId for one of grades
        PersQaServiceMockFactory.mockGradeSimple(gradeClient, gradeIds[0], UID, GradeType.SHOP_GRADE, SHOP_ID);
        PersQaServiceMockFactory.mockGradeWithFixId(gradeClient, gradeIds[1], UID, GradeType.SHOP_GRADE, SHOP_ID);
        PersQaServiceMockFactory.mockGradeSimple(gradeClient, gradeIds[2], UID, GradeType.SHOP_GRADE, SHOP_ID);

        Long parentId = null;
        Long noParent = null;
        long[] ids = new long[]{
            0, // c0 - to count from 1

            // grade 1
            parentId = partnerShopMvc.createGradeComment(SHOP_ID, UID + 1, noParent, gradeIds[0], "c1 Shop comment"),
            gradeShopCommentMvcMocks.createComment(gradeIds[0], UID, noParent, getBody("c2 User comment")),
            parentId = gradeShopCommentMvcMocks.createComment(gradeIds[0], UID, parentId, getBody("c3 User response")),
            partnerShopMvc.createGradeComment(SHOP_ID + 1, UID + 1, parentId, gradeIds[0], "c4 Shop shop.ru response"),

            // grade 2
            parentId = gradeShopCommentMvcMocks.createComment(gradeIds[1], UID, noParent, getBody("c5 User comment")),
            partnerShopMvc.createGradeComment(SHOP_ID, UID + 1, parentId, gradeIds[1], "c6 Shop comment"),
            gradeShopCommentMvcMocks.createComment(gradeIds[1], UID, noParent, getBody("c7 User comment 2")),
            0
        };

        // and single premod comment by vendor in root2
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION_SHOP, true);
        ids[8] = partnerShopMvc.createGradeComment(SHOP_ID, UID + 1, noParent, gradeIds[1], "c8 Shop comment");

        // check result
        assertForest(partnerShopMvc.getGradeComments(SHOP_ID, UID, gradeIds, false),
            checkFlatTree(GRADE, gradeIds[0],
                checkComment(ids[4],
                    checkText("c4 Shop shop.ru response"),
                    checkParent(ids[3]),
                    checkUser(UserInfo.uid(UID + 1)),
                    checkAuthor(new UserInfo(UserType.SHOP, SHOP_ID + 1)),
                    checkCanDelete(true),
                    checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(SHOP_ID + 1))),
                    checkSubComments()
                ),
                checkComment(ids[3],
                    checkText("c3 User response"),
                    checkParent(ids[1]),
                    checkUser(UserInfo.uid(UID)),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkSubComments()
                ),
                checkComment(ids[2],
                    checkText("c2 User comment"),
                    checkNoParent(),
                    checkUser(UserInfo.uid(UID)),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    // user comments under shop grade do not have projectId param
                    checkParams(Map.of(
                        PARAM_GRADE_TYPE, PARAM_GRADE_TYPE_SHOP
                    )),
                    checkSubComments()
                ),
                checkComment(ids[1],
                    checkText("c1 Shop comment"),
                    checkNoParent(),
                    checkUser(UserInfo.uid(UID + 1)),
                    checkAuthor(new UserInfo(UserType.SHOP, SHOP_ID)),
                    checkCanDelete(false),
                    checkParams(Map.of(PARAM_SHOP_ID, String.valueOf(SHOP_ID))),
                    checkSubComments()
                )
            ),
            checkFlatTree(GRADE, gradeIds[1],
                checkComment(ids[8],
                    checkText("c8 Shop comment"),
                    checkNoParent(),
                    checkUser(UserInfo.uid(UID + 1)),
                    checkAuthor(new UserInfo(UserType.SHOP, SHOP_ID)),
                    checkCanDelete(true),
                    checkPublished(true),
                    checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(SHOP_ID))),
                    checkSubComments()
                ),
                checkComment(ids[7],
                    checkText("c7 User comment 2"),
                    checkNoParent(),
                    checkUser(UserInfo.uid(UID)),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkSubComments()
                ),
                checkComment(ids[6],
                    checkText("c6 Shop comment"),
                    checkParent(ids[5]),
                    checkUser(UserInfo.uid(UID + 1)),
                    checkAuthor(new UserInfo(UserType.SHOP, SHOP_ID)),
                    checkCanDelete(false),
                    checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(SHOP_ID))),
                    checkSubComments()
                ),
                checkComment(ids[5],
                    checkText("c5 User comment"),
                    checkNoParent(),
                    checkUser(UserInfo.uid(UID)),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkSubComments()
                )
            ),
            checkFlatTree(GRADE, gradeIds[2])
        );

        // try to delete shop comment
        partnerShopMvc.deleteComment(SHOP_ID, UID + 10, ids[1], status().is4xxClientError()); //leaf
        partnerShopMvc.deleteComment(SHOP_ID, UID + 10, ids[2], status().is4xxClientError()); // user
        partnerShopMvc.deleteComment(SHOP_ID_OTHER, UID + 1, ids[4], status().is2xxSuccessful());
    }

    @Test
    void testCreateGradeCommentDefSort() throws Exception {
        //    g1
        //    - c1(sh)
        //     |- c3(u)
        //       |- c4(sh)
        //    - c2(u)
        // viewed backwards
        long[] gradeIds = {1, 2, 3};

        when(gradeClient.getGradeForComments( ArgumentMatchers.longThat(
            argument -> argument != null && Arrays.stream(gradeIds).anyMatch(x -> x == argument)
        ))).then(PersQaServiceMockFactory.getShopBuilder(UID, SHOP_ID));

        Long parentId = null;
        Long noParent = null;
        long[] ids = new long[]{
            0, // c0 - to count from 1

            // grade 1
            parentId = partnerShopMvc.createGradeComment(SHOP_ID, UID + 1, noParent, gradeIds[0], "c1 Shop comment"),
            gradeShopCommentMvcMocks.createComment(gradeIds[0], UID, noParent, getBody("c2 User comment")),
            parentId = gradeShopCommentMvcMocks.createComment(gradeIds[0], UID, parentId, getBody("c3 User response")),
            partnerShopMvc.createGradeComment(SHOP_ID, UID + 1, parentId, gradeIds[0], "c4 Shop response"),
        };

        // check result
        assertForest(partnerShopMvc.getGradeComments(SHOP_ID, UID, gradeIds, null),
            checkFlatTree(GRADE, gradeIds[0],
                checkComment(ids[1],
                    checkText("c1 Shop comment"),
                    checkNoParent(),
                    checkUser(UserInfo.uid(UID + 1)),
                    checkAuthor(new UserInfo(UserType.SHOP, SHOP_ID)),
                    checkCanDelete(false),
                    checkParams(Map.of(PARAM_SHOP_ID, String.valueOf(SHOP_ID))),
                    checkSubComments()
                ),
                checkComment(ids[2],
                    checkText("c2 User comment"),
                    checkNoParent(),
                    checkUser(UserInfo.uid(UID)),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    // user comments under shop grade do not have projectId param
                    checkParams(Map.of(
                        PARAM_GRADE_TYPE, PARAM_GRADE_TYPE_SHOP
                    )),
                    checkSubComments()
                ),
                checkComment(ids[3],
                    checkText("c3 User response"),
                    checkParent(ids[1]),
                    checkUser(UserInfo.uid(UID)),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkSubComments()
                ),
                checkComment(ids[4],
                    checkText("c4 Shop response"),
                    checkParent(ids[3]),
                    checkUser(UserInfo.uid(UID + 1)),
                    checkAuthor(new UserInfo(UserType.SHOP, SHOP_ID)),
                    checkCanDelete(true),
                    checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(SHOP_ID))),
                    checkSubComments()
                )
            ),
            checkFlatTree(GRADE, gradeIds[1]),
            checkFlatTree(GRADE, gradeIds[2])
        );
    }

    @Test
    void testCreateCommentWithUrl() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId);

        String result = partnerShopMvc.createAnswerComment(SHOP_ID, UID, answerId, "Some comment to check tes.by/content link", status().is4xxClientError());

        assertTrue(result.contains("Text contains illegal URL"));
        assertTrue(result.contains("\"result\":58"));
        assertTrue(result.contains("\"code\":\"ILLEGAL_URL_FOUND\""));
    }

    @Test
    void testDeleteComment() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId);

        Long[] ids = {
            partnerShopMvc.createAnswerComment(SHOP_ID, UID, answerId, "First one here!"),
            commentService.createAnswerComment(UID + 1, "User comment", answerId),
            commentService.createAnswerComment(UID, "User comment (shop manager)", answerId),
            partnerShopMvc.createAnswerComment(SHOP_ID + 1, UID + 2, answerId, "Second shop comment"),
        };

        List<CommentDto> comments = partnerShopMvc.getAnswerComments(SHOP_ID, UID, answerId);
        assertEquals(ids.length, comments.size());

        // can delete own comment
        for (long commentId : ids) {
            if (commentId == ids[0]) {
                // should be fine
                partnerShopMvc.deleteComment(SHOP_ID, UID, commentId, status().is2xxSuccessful());
            } else {
                String response = partnerShopMvc.deleteComment(SHOP_ID, UID, commentId, status().is4xxClientError());
                assertTrue(response.contains("hasn't rights to remove comment with id=" + commentId));
            }
        }
    }

    @Test
    void testGetCommentsForGradesWithSameFixId() throws Exception {
        long shopId = 9;
        long[] gradeIds = {1, 2};

        PersQaServiceMockFactory.mockGradeWithSameFixId(gradeClient, gradeIds[0], UID, GradeType.MODEL_GRADE, shopId);
        PersQaServiceMockFactory.mockGradeWithSameFixId(gradeClient, gradeIds[1], UID, GradeType.MODEL_GRADE, shopId);

        Long noParent = null;

        partnerShopMvc.createGradeComment(shopId, UID, noParent, gradeIds[0], "Shop comment");
        partnerShopMvc.createGradeComment(shopId, UID, noParent, gradeIds[1], "Shop comment");

        partnerShopMvc.getGradeComments(SHOP_ID, UID, gradeIds, null, status().is4xxClientError());
    }

}
