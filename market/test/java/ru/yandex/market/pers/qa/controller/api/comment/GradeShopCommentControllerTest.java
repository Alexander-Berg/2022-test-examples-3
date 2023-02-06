package ru.yandex.market.pers.qa.controller.api.comment;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.pers.grade.client.GradeClient;
import ru.yandex.market.pers.grade.client.dto.GradeForCommentDto;
import ru.yandex.market.pers.grade.client.dto.UserDto;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.dto.CommentResultDto;
import ru.yandex.market.pers.qa.mock.mvc.GradeShopCommentMvcMocks;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.util.ListUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.model.CommentProject.GRADE;
import static ru.yandex.market.pers.qa.controller.api.comment.AbstractCommonCommentControllerTest.FIRST_LEVEL_COMMENT_CMP;
import static ru.yandex.market.pers.qa.controller.api.comment.AbstractCommonCommentControllerTest.ROOT_ID;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.03.2020
 */
public class GradeShopCommentControllerTest extends AbstractCommentControllerTest {

    private final long shopId = 9;
    private final long[] gradeIds = {1, 2, 3};

    @Autowired
    private CommentService commentService;

    @Autowired
    private GradeShopCommentMvcMocks gradeShopCommentMvc;

    @Autowired
    private GradeClient gradeClient;

    private long[] prepareTree() {
        //    g1
        //    - c1(sh)
        //     |- c3(u)
        //       |- c4(sh)
        //    - c2(u)
        //
        //    g2
        //    - с5(u)
        //     |- c7(sh)
        //       |- c8(u) - del, not visible
        //         |- c9(sh) - del, not visible
        //       |- c10(u) - del, visible
        //         |- c11(sh) - leaf
        //           |- c12(u) - del, not visible
        //    - c6(u)

        // mock grade client
        // shop grade opinion
        when(gradeClient.getGradeForComments(anyLong())).then(invocation -> {
            Long gradeId = invocation.getArgument(0);
            assertEquals(1, LongStream.of(gradeIds)
                .filter(x -> x == gradeId)
                .count(), "Grade should be expected one");
            return new GradeForCommentDto(gradeId, null, GradeType.SHOP_GRADE.value(), new UserDto(UID), shopId);
        });

        long temp;
        long temp2;
        long[] c = {
            // c0 - to count from 1
            0,

            // c1
            temp = commentService.createShopComment(GRADE, UID, "Shop comment", gradeIds[0], shopId),
            // c2
            commentService.createComment(GRADE, UID, "User comment", gradeIds[0]),
            // c3
            temp = commentService.createComment(GRADE, UID, "User response", gradeIds[0], temp),
            // c4
            commentService.createShopComment(GRADE, UID, "Shop response", gradeIds[0], shopId, temp),

            // grade 2
            // c5
            temp = commentService.createComment(GRADE, UID, "User comment", gradeIds[1]),
            // c6
            commentService.createComment(GRADE, UID, "User comment 2", gradeIds[1]),
            // c7
            temp = commentService.createShopComment(GRADE, UID, "Shop comment", gradeIds[1], shopId, temp),
            // c8
            temp2 = commentService.createComment(GRADE, UID, "User del resp 1", gradeIds[1], temp),
            // c9
            commentService.createShopComment(GRADE, UID, "Shop del resp", gradeIds[1], shopId, temp2),
            // c10
            temp2 = commentService.createComment(GRADE, UID, "User del resp 2", gradeIds[1], temp),
            // c11
            temp2 = commentService.createShopComment(GRADE, UID + 1, "Shop ok resp resp", gradeIds[1], shopId, temp2),
            // c12
            commentService.createComment(GRADE, UID, "User del resp 3", gradeIds[1], temp2),
        };

        // delete 8, 9, 10 comments
        commentService.banCommentsByManager(Arrays.asList(c[8], c[9], c[10], c[12]));

        return c;
    }

    @ParameterizedTest
    @MethodSource("getUserTypes")
    public void testGradeShopComments(UserType userType) throws Exception {
        long[] c = prepareTree();
        long splitLevel = 1;
        long pageSize = 2;
        boolean isYandexUid = userType == UserType.YANDEXUID;

        //    g1
        //    - c1(sh)
        //     |- c3(u)
        //       |- c4(sh)
        //    - c2(u)
        //
        //    g2
        //    - с5(u)
        //     |- c7(sh)
        //       |- c8(u) - del, not visible
        //         |- c9(sh) - del, not visible
        //       |- c10(u) - del, visible
        //         |- c11(sh) - leaf
        //           |- c12(u) - del, not visible
        //    - c6(u)

        // read simple count
        assertEquals(4, gradeShopCommentMvc.getFirstLevelCommentsCount(gradeIds[0], splitLevel));

        // read bulk count
        Map<Long, CountDto> bulkCounts = gradeShopCommentMvc.getFirstLevelCommentsBulkCount(gradeIds, splitLevel);
        assertEquals(3, bulkCounts.size());
        assertEquals(4, bulkCounts.get(gradeIds[0]).getCount());
        assertEquals(5, bulkCounts.get(gradeIds[1]).getCount());
        assertEquals(0, bulkCounts.get(gradeIds[2]).getCount());

        // read preview
        BiFunction<Long, Long, DtoList<CommentResultDto>> bulkPreviewLoader = gradeShopCommentMvc
            .getBulkCommentPreview(gradeIds, isYandexUid);

        DtoList<CommentResultDto> preview = bulkPreviewLoader.apply(splitLevel, pageSize);
        Map<Long, CommentResultDto> previewMap = ListUtils.toMap(preview.getData(), CommentResultDto::getEntityId);
        assertEquals(2, preview.getData().size());
        checkDataExact(previewMap.get(gradeIds[0]),  c[4], c[3]);
        checkBorderId(previewMap.get(gradeIds[0]), c[3]);
        checkDataExact(previewMap.get(gradeIds[1]),  c[11], c[10]);
        checkBorderId(previewMap.get(gradeIds[1]), c[10]);

        // check isLeaf property. In grade[1] loaded dat only c[11] is leaf. c[10] is not
        assertArrayEquals(new long[]{c[11]}, previewMap.get(gradeIds[1]).getData().stream()
            .filter(CommentDto::isLeaf)
            .mapToLong(CommentDto::getId)
            .toArray());

        // read next (and last) page for grade[0]. c[3] is last border
        CommentResultDto result = gradeShopCommentMvc.getCommentsWithBorderIdAndLimit(
            gradeIds[0],
            isYandexUid,
            null,
            splitLevel,
            c[3],
            pageSize);
        checkDataExact(result, c[2], c[1]);
        checkBranch(result, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, c[2], c[1]);
        checkBorderId(result, null);

        // read next (second) page for grade[1]. c[10] is last border
        result = gradeShopCommentMvc.getCommentsWithBorderIdAndLimit(
            gradeIds[1],
            isYandexUid,
            null,
            splitLevel,
            c[10],
            pageSize);
        checkDataExact(result, c[7], c[6]);
        checkBranch(result, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, c[7], c[6]);
        checkBorderId(result, c[6]);

        // read last page for grade[1]
        result = gradeShopCommentMvc.getCommentsWithBorderIdAndLimit(
            gradeIds[1],
            isYandexUid,
            null,
            splitLevel,
            c[6],
            pageSize);
        checkDataExact(result, c[5]);
        checkBranch(result, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, c[5]);
        checkBorderId(result, null);
    }

    @Test
    public void testConstraints4xx2() throws Exception {
        when(gradeClient.getGradeForComments(anyLong())).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        assertTrue(gradeShopCommentMvc.createComment4xx(gradeIds[0], UID, getAnyBody())
                .contains("Can't create comment for nonexistent grade"));
    }

    @Test
    public void testConstraints4xx() throws Exception {
        long[] c = prepareTree();

        //    g1
        //    - c1(sh)
        //     |- c3(u)
        //       |- c4(sh)
        //    - c2(u)
        //
        //    g2
        //    - с5(u)
        //     |- c7(sh)
        //       |- c8(u) - del, not visible
        //         |- c9(sh) - del, not visible
        //       |- c10(u) - del, visible
        //         |- c11(sh) - leaf
        //           |- c12(u) - del, not visible
        //    - c6(u)

        // edit only leaf
        assertTrue(gradeShopCommentMvc.editComment(c[1], UID, getAnyBody(), status().is4xxClientError(), x -> x)
            .contains("Can't edit non-leaf comment"));
        assertTrue(gradeShopCommentMvc.editComment(c[3], UID, getAnyBody(), status().is4xxClientError(), x -> x)
            .contains("Can't edit non-leaf comment"));
        assertTrue(gradeShopCommentMvc.editComment(c[4], UID, getAnyBody(), status().is4xxClientError(), x -> x)
            .contains("hasn't rights to edit comment"));
        assertTrue(gradeShopCommentMvc.editComment(c[2], UID + 1, getAnyBody(), status().is4xxClientError(), x -> x)
            .contains("hasn't rights to edit comment"));
        gradeShopCommentMvc.editComment(c[2], UID, getAnyBody(), status().is2xxSuccessful(), x -> x);

        // delete only leaf
        assertTrue(gradeShopCommentMvc.deleteComment4xx(c[1], UID).contains("Can't remove non-leaf comment"));
        assertTrue(gradeShopCommentMvc.deleteComment4xx(c[3], UID).contains("Can't remove non-leaf comment"));
        assertTrue(gradeShopCommentMvc.deleteComment4xx(c[4], UID)
            .contains("hasn't rights to remove comment"));
        assertTrue(gradeShopCommentMvc.deleteComment4xx(c[2], UID + 1)
            .contains("hasn't rights to remove comment"));
        gradeShopCommentMvc.deleteComment(c[2]);

        // comment only under leaf or root
        // can't comment under own comment
        assertTrue(gradeShopCommentMvc.createComment4xx(gradeIds[1], UID, c[5], getAnyBody())
            .contains("Can't comment under non-leaf comment"));
        assertTrue(gradeShopCommentMvc.createComment4xx(gradeIds[1], UID, c[7], getAnyBody())
            .contains("Can't comment under non-leaf comment"));
        assertTrue(gradeShopCommentMvc.createComment4xx(gradeIds[1], UID, c[6], getAnyBody())
            .contains("Can't comment under your own comment"));
        assertTrue(gradeShopCommentMvc.createComment4xx(gradeIds[1], UID + 2, c[11], getAnyBody())
            .contains("Attempt to create comment by illegal author"));
        gradeShopCommentMvc.createComment(gradeIds[1], UID, c[11], getAnyBody());
    }

    private static Stream<Arguments> getUserTypes() {
        return Stream.of(UserType.UID, UserType.YANDEXUID)
            .map(Arguments::of);
    }
}
