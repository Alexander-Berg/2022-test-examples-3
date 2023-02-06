package ru.yandex.market.pers.qa.controller.api.external;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.GradeClient;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.dto.CommentTreeDto;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.mock.mvc.GradeCommentMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.GradeMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.PartnerShopMvcMocks;
import ru.yandex.market.pers.qa.model.Comment;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.utils.CommentUtils;
import ru.yandex.market.util.ListUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.qa.PersQaServiceMockFactory.mockGradeSimple;
import static ru.yandex.market.pers.qa.PersQaServiceMockFactory.mockGradeWithFixId;
import static ru.yandex.market.pers.qa.PersQaServiceMockFactory.mockGradeWithSameFixId;
import static ru.yandex.market.pers.qa.client.model.CommentProject.GRADE;
import static ru.yandex.market.pers.qa.controller.api.comment.AbstractCommentControllerTest.getBody;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.assertForest;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkComment;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkFlatTree;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkText;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 05.08.2019
 */
public class PartnerApiControllerTest extends QAControllerTest {
    @Autowired
    private CommentService commentService;
    @Autowired
    private GradeMvcMocks gradeMvc;
    @Autowired
    private GradeClient gradeClient;
    @Autowired
    private GradeCommentMvcMocks gradeCommentMvc;
    @Autowired
    private PartnerShopMvcMocks partnerShopMvc;

    @Test
    void testTree() throws Exception {
        long shopId = 9;
        long[] gradeIds = {1, 2};

        // ensure to check with mocked fixId for one of grades
        mockGradeWithFixId(gradeClient, gradeIds[0], UID, GradeType.MODEL_GRADE, shopId);
        mockGradeSimple(gradeClient, gradeIds[1], UID, GradeType.MODEL_GRADE, shopId);

        Long noParent = null;

        long shopComment = partnerShopMvc.createGradeComment(shopId, UID, noParent, gradeIds[0], "Shop comment");
        long userComment = gradeCommentMvc.createComment(gradeIds[0], UID, noParent, getBody("User comment"));
        long userResponse = gradeCommentMvc.createComment(gradeIds[0], UID, shopComment, getBody("User response"));
        long shopResponse = partnerShopMvc.createGradeComment(shopId, UID, userComment, gradeIds[0], "Shop response");

        long userComment2 = gradeCommentMvc.createComment(gradeIds[1], UID, noParent, getBody("User comment"));
        long shopComment2 = partnerShopMvc.createGradeComment(shopId, UID, userComment2, gradeIds[1], "Shop comment");
        long userComment21 = gradeCommentMvc.createComment(gradeIds[1], UID, noParent, getBody("User comment 2"));
        // also create shop comment in old format
        long shopComment21 = commentService.createComment(
            Comment.builder(GRADE, gradeIds[1], "test old", UID)
                .author(new UserInfo(UserType.SHOP, shopId)));

        List<CommentTreeDto> comments = gradeMvc.getCommentsBulkForPapi(shopId, UID, gradeIds).getData();
        assertEquals(2, comments.size());

        CommentTreeDto commentTreeDto = comments.get(0);
        Map<Long, CommentDto> commentsMap = ListUtils.toMap(commentTreeDto.getComments(), CommentDto::getId);
        assertEquals(gradeIds[0], commentTreeDto.getEntityId().longValue());
        assertEquals(4, commentTreeDto.getComments().size());
        assertTrue(Sets.newHashSet(shopComment, userComment, userResponse, shopResponse).containsAll(
            ListUtils.toList(commentTreeDto.getComments(), CommentDto::getId)
        ));

        commentTreeDto = comments.get(1);
        commentsMap = ListUtils.toMap(commentTreeDto.getComments(), CommentDto::getId);
        assertEquals(gradeIds[1], commentTreeDto.getEntityId().longValue());
        assertEquals(4, commentTreeDto.getComments().size());
        assertTrue(Sets.newHashSet(userComment2, shopComment2, userComment21, shopComment21).containsAll(
            ListUtils.toList(commentTreeDto.getComments(), CommentDto::getId)
        ));

        // check authors
        assertEquals(UserType.UID, commentsMap.get(userComment2).getAuthor().getUserType());
        assertEquals(UID_STR, commentsMap.get(userComment2).getAuthor().getId());
        assertEquals(UserType.SHOP, commentsMap.get(shopComment2).getAuthor().getUserType());
        assertEquals(String.valueOf(shopId), commentsMap.get(shopComment2).getAuthor().getId());
        assertEquals(UserType.SHOP, commentsMap.get(shopComment21).getAuthor().getUserType());
        assertEquals(String.valueOf(shopId), commentsMap.get(shopComment21).getAuthor().getId());

        // check projectId in properties
        commentTreeDto.getComments().stream()
                .filter(x->x.getParameters().get(CommentUtils.PARAM_GRADE_TYPE) == null)
                .findAny()
                .ifPresent(x-> {throw new RuntimeException("Should be none grade comments without grade_id param");});
    }

    @Test
    void testCreateShopGradeComments() throws Exception{
        long shopId = 9;
        long gradeId = 13413;

        // shop grade opinion
        when(gradeClient.getGradeForComments(gradeId)).then(PersQaServiceMockFactory.getShopBuilder(UID, shopId));

        long shopComment = commentService.createShopComment(GRADE, UID, "Shop comment", gradeId, shopId);
        long userComment = commentService.createComment(GRADE, UID, "User comment", gradeId);
        long userResponse = commentService.createComment(GRADE, UID, "User response", gradeId, shopComment);
        long shopResponse = commentService.createShopComment(GRADE, UID, "Shop response", gradeId, shopId, userComment);

//        this is fine now
//        assertThrows(IllegalArgumentException.class, () -> {
//            commentService.createShopComment(GRADE, UID, "Shop comment (illegal)", gradeId, shopId + 1);
//        });

        assertThrows(IllegalArgumentException.class, () -> {
            commentService.createComment(GRADE, UID + 1, "User comment (illegal)", gradeId);
        });

        List<CommentTreeDto> comments = gradeMvc.getCommentsBulkForPapi(shopId, UID, new long[]{gradeId}).getData();
        assertEquals(1, comments.size());
        assertEquals(4, comments.get(0).getComments().size());
    }

    @Test
    void testGetCommentsForGradesWithOldFixId() throws Exception {
        long shopId = 9;
        long[] gradeIds = {1, 2};

        // first grade without fix_id, second with fix_id=gradeIds[0]
        mockGradeSimple(gradeClient, gradeIds[0], UID, GradeType.MODEL_GRADE, shopId);
        mockGradeWithFixId(gradeClient, gradeIds[1], UID, GradeType.MODEL_GRADE, shopId, gradeIds[0]);

        Long noParent = null;

        long gradeCommentFirst = partnerShopMvc.createGradeComment(shopId, UID, noParent, gradeIds[0], "Shop comment 1");
        long gradeCommentSecond = partnerShopMvc.createGradeComment(shopId, UID, noParent, gradeIds[1], "Shop comment 2");

        // check result
        assertForest(gradeMvc.getCommentsBulkForPapi(shopId, UID, gradeIds).getData(),
            checkFlatTree(GRADE, gradeIds[0]),
            checkFlatTree(GRADE, gradeIds[1],
                checkComment(gradeCommentFirst,
                    checkText("Shop comment 1")
                ),
                checkComment(gradeCommentSecond,
                    checkText("Shop comment 2")
                )
            )
        );

        invalidateCache();

        assertForest(gradeMvc.getCommentsBulkForPapi(shopId, UID, gradeIds).getData(),
            checkFlatTree(GRADE, gradeIds[0]),
            checkFlatTree(GRADE, gradeIds[1],
                checkComment(gradeCommentFirst),
                checkComment(gradeCommentSecond)
            )
        );
    }

    @Test
    void testGetCommentsForGradesWithSameFixId() throws Exception {
        long shopId = 9;
        long[] gradeIds = {1, 2};

        mockGradeWithSameFixId(gradeClient, gradeIds[0], UID, GradeType.MODEL_GRADE, shopId);
        mockGradeWithSameFixId(gradeClient, gradeIds[1], UID, GradeType.MODEL_GRADE, shopId);

        Long noParent = null;

        long gradeCommentFirst = partnerShopMvc.createGradeComment(shopId, UID, noParent, gradeIds[0], "Shop comment 1");
        long gradeCommentSecond = partnerShopMvc.createGradeComment(shopId, UID, noParent, gradeIds[1], "Shop comment 2");

        // check result
        assertForest(gradeMvc.getCommentsBulkForPapi(shopId, UID, gradeIds).getData(),
            checkFlatTree(GRADE, gradeIds[0]),
            checkFlatTree(GRADE, gradeIds[1],
                checkComment(gradeCommentFirst),
                checkComment(gradeCommentSecond)
            )
        );

        invalidateCache();

        assertForest(gradeMvc.getCommentsBulkForPapi(shopId, UID, gradeIds).getData(),
            checkFlatTree(GRADE, gradeIds[0]),
            checkFlatTree(GRADE, gradeIds[1],
                checkComment(gradeCommentFirst),
                checkComment(gradeCommentSecond)
            )
        );
    }

}
