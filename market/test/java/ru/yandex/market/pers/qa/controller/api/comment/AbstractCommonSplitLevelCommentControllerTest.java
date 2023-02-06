package ru.yandex.market.pers.qa.controller.api.comment;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.qa.client.dto.CountDto;
import ru.yandex.market.pers.qa.client.dto.DtoList;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.controller.dto.CommentResultDto;
import ru.yandex.market.pers.qa.mock.mvc.GradeFixIdCommentMvcMocks;
import ru.yandex.market.pers.qa.model.CommentFilter;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.qa.service.CommentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.assertComments;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.assertResult;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkAuthor;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkChildCount;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkComment;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkPublished;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkSubComments;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkText;
import static ru.yandex.market.pers.qa.mock.mvc.AbstractCommonCommentMvcMocks.UGLY_LIMIT_TO_DISABLE_TREE_REVERSE;

/**
 * @author varvara
 * 15.02.2019
 */
public abstract class AbstractCommonSplitLevelCommentControllerTest extends AbstractCommonCommentControllerTest {

    private static final long SPLIT_LEVEL = 3L;

    @Test
    public void testBulkPreviewUid() throws Exception {
        testBulkPreviewSkeleton(false);
    }

    @Test
    public void testBulkPreviewYandexUid() throws Exception {
        testBulkPreviewSkeleton(true);
    }

    public void testBulkPreviewSkeleton(boolean isYandexUid) throws Exception {
        long comment1 = createComment(ENTITY_ID, UID, null, getAnyBody());
        long comment2 = createComment(ENTITY_ID, UID + 1, null, getAnyBody());
        long comment3 = createComment(ENTITY_ID, UID + 2, comment1, getAnyBody());
        long comment4 = createComment(ENTITY_ID, UID + 3, comment1, getAnyBody());
        deleteComment(comment1);
        deleteComment(comment2);
        deleteComment(comment4);

        long comment5 = createComment(ENTITY_ID + 1, UID + 4, null, getAnyBody());
        long comment6 = createComment(ENTITY_ID + 1, UID + 5, null, getAnyBody());
        long comment7 = createComment(ENTITY_ID + 1, UID + 6, comment5, getAnyBody());
        long comment8 = createComment(ENTITY_ID + 1, UID + 7, comment5, getAnyBody());
        deleteComment(comment6);
        deleteComment(comment8);

        long comment9 = createComment(ENTITY_ID + 2, UID + 8, null, getAnyBody());
        long comment10 = createComment(ENTITY_ID + 2, UID + 9, comment9, getAnyBody());
        long comment11 = createComment(ENTITY_ID + 2, UID + 10, comment10, getAnyBody());
        long comment12 = createComment(ENTITY_ID + 2, UID + 11, comment11, getAnyBody());

        long comment13 = createComment(ENTITY_ID + 3, UID, null, getAnyBody());
        long comment14 = createComment(ENTITY_ID + 3, UID + 1, comment13, getAnyBody());
        long comment15 = createComment(ENTITY_ID + 3, UID + 2, comment14, getAnyBody());
        long comment16 = createComment(ENTITY_ID + 3, UID + 3, comment15, getAnyBody());

        //splitLevel = 2 limit 2
        DtoList<CommentResultDto> commentResultDtoListSplitLevel2 = mvc.getBulkCommentPreview(
            new long[]{ENTITY_ID, ENTITY_ID + 1, ENTITY_ID + 2}, isYandexUid).apply(2L, 2L);
        Assertions.assertEquals(3, commentResultDtoListSplitLevel2.getData().size());
        List<CommentResultDto> commentResultDtos = commentResultDtoListSplitLevel2.getData().stream()
            .sorted(Comparator.comparingLong(CommentResultDto::getEntityId))
            .collect(Collectors.toList());

        CommentResultDto entityIdCommentResultDto = commentResultDtos.get(0);
        Assertions.assertNull(entityIdCommentResultDto.getTree());
        Assertions.assertEquals(1, entityIdCommentResultDto.getData().size());
        Assertions.assertNull(entityIdCommentResultDto.getBorderId());
        Assertions.assertEquals(comment1, entityIdCommentResultDto.getData().get(0).getId());
        Assertions.assertEquals(String.valueOf(UID), entityIdCommentResultDto.getData().get(0).getAuthor().getId());

        CommentResultDto entityId1CommentResultDto = commentResultDtos.get(1);
        Assertions.assertNull(entityId1CommentResultDto.getTree());
        Assertions.assertEquals(1, entityId1CommentResultDto.getData().size());
        Assertions.assertNull(entityIdCommentResultDto.getBorderId());
        Assertions.assertEquals(comment5, entityId1CommentResultDto.getData().get(0).getId());
        Assertions.assertEquals(String.valueOf(UID + 4), entityId1CommentResultDto.getData().get(0).getAuthor().getId());

        CommentResultDto entityId2CommentResultDto = commentResultDtos.get(2);
        Assertions.assertNull(entityId2CommentResultDto.getTree());
        Assertions.assertEquals(1, entityId2CommentResultDto.getData().size());
        Assertions.assertNull(entityId2CommentResultDto.getBorderId());
        Assertions.assertEquals(comment9, entityId2CommentResultDto.getData().get(0).getId());
        Assertions.assertEquals(String.valueOf(UID + 8), entityId2CommentResultDto.getData().get(0).getAuthor().getId());

        //splitLevel = 1 limit 2
        DtoList<CommentResultDto> commentResultDtoListSplitLevel1 = mvc.getBulkCommentPreview(
            new long[]{ENTITY_ID, ENTITY_ID + 1, ENTITY_ID + 2}, isYandexUid).apply(1L, 2L);
        Assertions.assertEquals(3, commentResultDtoListSplitLevel1.getData().size());

        commentResultDtos = commentResultDtoListSplitLevel1.getData().stream()
            .sorted(Comparator.comparingLong(CommentResultDto::getEntityId))
            .collect(Collectors.toList());

        entityIdCommentResultDto = commentResultDtos.get(0);
        Assertions.assertNull(entityIdCommentResultDto.getTree());
        Assertions.assertEquals(1, entityIdCommentResultDto.getData().size());
        Assertions.assertEquals(comment3, entityIdCommentResultDto.getData().get(0).getId());
        Assertions.assertEquals(String.valueOf(UID + 2), entityIdCommentResultDto.getData().get(0).getAuthor().getId());

        entityId1CommentResultDto = commentResultDtos.get(1);
        Assertions.assertNull(entityId1CommentResultDto.getTree());
        Assertions.assertEquals(2, entityId1CommentResultDto.getData().size());
        Assertions.assertEquals(comment7, entityId1CommentResultDto.getData().get(0).getId());
        Assertions.assertEquals(String.valueOf(UID + 6), entityId1CommentResultDto.getData().get(0).getAuthor().getId());
        Assertions.assertEquals(comment5, entityId1CommentResultDto.getData().get(1).getId());
        Assertions.assertEquals(String.valueOf(UID + 4), entityId1CommentResultDto.getData().get(1).getAuthor().getId());

        entityId2CommentResultDto = commentResultDtos.get(2);
        Assertions.assertNull(entityId2CommentResultDto.getTree());
        Assertions.assertEquals(2, entityId2CommentResultDto.getData().size());
        Assertions.assertEquals(comment11, (long) entityId2CommentResultDto.getBorderId());
        Assertions.assertEquals(comment12, entityId2CommentResultDto.getData().get(0).getId());
        Assertions.assertEquals(String.valueOf(UID + 11), entityId2CommentResultDto.getData().get(0).getAuthor().getId());
        Assertions.assertEquals(comment11, entityId2CommentResultDto.getData().get(1).getId());
        Assertions.assertEquals(String.valueOf(UID + 10), entityId2CommentResultDto.getData().get(1).getAuthor().getId());
    }
    @Test
    public void testFirstLevelCountWithDeleted() throws Exception {
        long comment1 = createComment(ENTITY_ID, UID, null, getAnyBody());
        long comment2 = createComment(ENTITY_ID, UID + 1, null, getAnyBody());
        long comment3 = createComment(ENTITY_ID, UID + 2, comment1, getAnyBody());
        long comment4 = createComment(ENTITY_ID, UID + 3, comment1, getAnyBody());
        Assertions.assertEquals(2, mvc.getFirstLevelCommentsCount(ENTITY_ID));
        deleteComment(comment1);
        Assertions.assertEquals(2, mvc.getFirstLevelCommentsCount(ENTITY_ID));
    }

    @Test
    public void testFirstLevelBulkCountWithDeleted() throws Exception {
        long comment1 = createComment(ENTITY_ID, UID, null, getAnyBody());
        long comment2 = createComment(ENTITY_ID, UID + 1, null, getAnyBody());
        long comment3 = createComment(ENTITY_ID, UID + 2, comment1, getAnyBody());
        long comment4 = createComment(ENTITY_ID, UID + 3, comment1, getAnyBody());
        deleteComment(comment1);
        deleteComment(comment2);
        deleteComment(comment4);

        long comment5 = createComment(ENTITY_ID + 1, UID, null, getAnyBody());
        long comment6 = createComment(ENTITY_ID + 1, UID + 1, null, getAnyBody());
        long comment7 = createComment(ENTITY_ID + 1, UID + 2, comment5, getAnyBody());
        long comment8 = createComment(ENTITY_ID + 1, UID + 3, comment5, getAnyBody());
        deleteComment(comment6);
        deleteComment(comment8);

        long comment9 = createComment(ENTITY_ID + 2, UID, null, getAnyBody());
        long comment10 = createComment(ENTITY_ID + 2, UID + 1, null, getAnyBody());
        long comment11 = createComment(ENTITY_ID + 2, UID + 2, comment9, getAnyBody());
        long comment12 = createComment(ENTITY_ID + 2, UID + 3, comment9, getAnyBody());
        deleteComment(comment9);
        deleteComment(comment12);

        long comment13 = createComment(ENTITY_ID + 3, UID, null, getAnyBody());
        long comment14 = createComment(ENTITY_ID + 3, UID + 1, comment13, getAnyBody());
        long comment15 = createComment(ENTITY_ID + 3, UID + 2, comment14, getAnyBody());
        long comment16 = createComment(ENTITY_ID + 3, UID + 3, comment15, getAnyBody());
        long comment17 = createComment(ENTITY_ID + 3, UID + 3, comment15, getAnyBody());
        deleteComment(comment13);
        deleteComment(comment14);
        deleteComment(comment15);

        Map<Long, CountDto> map = mvc.getFirstLevelCommentsBulkCount(
            new long[]{ENTITY_ID, ENTITY_ID + 1, ENTITY_ID + 2, ENTITY_ID + 3, ENTITY_ID + 4}, 2L);
        Assertions.assertEquals(1, map.get(ENTITY_ID).getCount());
        Assertions.assertEquals(1, map.get(ENTITY_ID + 1).getCount());
        Assertions.assertEquals(2, map.get(ENTITY_ID + 2).getCount());
        Assertions.assertEquals(1, map.get(ENTITY_ID + 3).getCount());
        Assertions.assertEquals(0, map.get(ENTITY_ID + 4).getCount());

        map = mvc.getFirstLevelCommentsBulkCount(
            new long[]{ENTITY_ID, ENTITY_ID + 1, ENTITY_ID + 2, ENTITY_ID + 3, ENTITY_ID + 4}, 1L);
        Assertions.assertEquals(1, map.get(ENTITY_ID).getCount());
        Assertions.assertEquals(2, map.get(ENTITY_ID + 1).getCount());
        Assertions.assertEquals(2, map.get(ENTITY_ID + 2).getCount());
        Assertions.assertEquals(2, map.get(ENTITY_ID + 3).getCount());
        Assertions.assertEquals(0, map.get(ENTITY_ID + 4).getCount());

        // test empty request
        mvc.getFirstLevelCommentsBulkCountEmpty4xx();

        // ignore for grade only (there is entity id mapping)
        if(!(getMvc() instanceof GradeFixIdCommentMvcMocks)) {
            assertEquals(0, commentService.getCommentCount(new CommentFilter().rootIds(List.of())));
            assertEquals(4, commentService.getCommentCount(new CommentFilter().rootIds(List.of(ENTITY_ID))));
        }
    }

    @Test
    public void testGetDeletedCommentsWithSplitLevel3ByUid() throws Exception {
        checkDeletedComments(false);
    }

    @Test
    public void testGetDeletedCommentsWithSplitLevel3ByYandexUid() throws Exception {
        checkDeletedComments(true);
    }

//    c1      c2
//    |  \
//    c3  c4
//    |   \
//    c5  c7
//    |
//    c6
    private void checkDeletedComments(boolean isYandexUid) throws Exception {
        final BiFunction<Long, Long, CommentResultDto> getCommentsWithSplitLevel = mvc
            .getCommentResultDtoByParentAndSplitLevel(ENTITY_ID, isYandexUid);
        CommentResultDto dto;

        long comment1 = createComment(ENTITY_ID, UID, null, getAnyBody());
        long comment2 = createComment(ENTITY_ID, UID + 1, null, getAnyBody());
        long comment3 = createComment(ENTITY_ID, UID + 2, comment1, getAnyBody());
        long comment4 = createComment(ENTITY_ID, UID + 3, comment1, getAnyBody());
        long comment5 = createComment(ENTITY_ID, UID + 4, comment3, getAnyBody());
        long comment6 = createComment(ENTITY_ID, UID + 5, comment5, getAnyBody());
        long comment7 = createComment(ENTITY_ID, UID + 6, comment4, getAnyBody());

        setUpdateTime(comment1, comment2, comment3, comment4, comment5, comment6);

        //  comment tree for splitLevel = 3
        //  comment1 childCount=2
        //      comment3 childCount=2
        //          comment5 childCount=0
        //          comment6 childCount=0
        //      comment4 childCount=1
        //          comment7 childCount=0
        //  comment2 childCount=0
        assertEquals(2, mvc.getFirstLevelCommentsCount(ENTITY_ID));
        dto = getCommentsWithSplitLevel.apply(null, SPLIT_LEVEL);
        checkData(ROOT_ID, dto, comment1, comment2);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment1, comment2);
        checkCount(dto, comment1, 2L);
        checkCount(dto, comment2, 0L);

        dto = getCommentsWithSplitLevel.apply(comment1, SPLIT_LEVEL);
        checkData(comment1, dto, comment3, comment4);
        checkBranch(dto, comment1, COMMENT_CMP, comment3, comment4);
        checkCount(dto, comment3, 2L);
        checkCount(dto, comment4, 1L);

        dto = getCommentsWithSplitLevel.apply(comment3, SPLIT_LEVEL);
        checkData(comment3, dto, comment5, comment6);
        checkBranch(dto, comment3, COMMENT_CMP, comment5, comment6);
        checkCount(dto, comment5, 0L);
        checkCount(dto, comment6, 0L);

        deleteComment(comment5);
        //  comment tree for splitLevel = 3
        //  comment1 childCount=2
        //      comment3 childCount=1
        //          comment5[deleted] childCount=0
        //          comment6 childCount=0
        //      comment4 childCount=1
        //          comment7 childCount=0
        //  comment2 childCount=0
        dto = getCommentsWithSplitLevel.apply(comment5, SPLIT_LEVEL);
        checkData(dto);
        dto = getCommentsWithSplitLevel.apply(null, SPLIT_LEVEL);
        checkData(ROOT_ID, dto, comment1, comment2);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment1, comment2);
        checkCount(dto, comment1, 2L);
        checkCount(dto, comment2, 0L);

        dto = getCommentsWithSplitLevel.apply(comment1, SPLIT_LEVEL);
        checkData(comment1, dto, comment3, comment4);
        checkBranch(dto, comment1, COMMENT_CMP, comment3, comment4);
        checkCount(dto, comment3, 1L);
        checkCount(dto, comment4, 1L);

        dto = getCommentsWithSplitLevel.apply(comment3, SPLIT_LEVEL);
        checkData(comment3, dto, comment6);
        checkBranch(dto, comment3, COMMENT_CMP, comment6);
        checkCount(dto, comment6, 0L);

        deleteComment(comment6);
        //  comment tree for splitLevel = 3
        //  comment1 childCount=2
        //      comment3 childCount=0
        //          comment5[deleted] childCount=0
        //          comment6[deleted] childCount=0
        //      comment4 childCount=1
        //          comment7 childCount=0
        //  comment2 childCount=0
        dto = getCommentsWithSplitLevel.apply(comment5, SPLIT_LEVEL);
        checkData(dto);
        dto = getCommentsWithSplitLevel.apply(null, SPLIT_LEVEL);
        checkData(ROOT_ID, dto, comment1, comment2);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment1, comment2);
        checkCount(dto, comment1, 2L);
        checkCount(dto, comment2, 0L);

        dto = getCommentsWithSplitLevel.apply(comment1, SPLIT_LEVEL);
        checkData(comment1, dto, comment3, comment4);
        checkBranch(dto, comment1, COMMENT_CMP, comment3, comment4);
        checkCount(dto, comment3, 0L);
        checkCount(dto, comment4, 1L);

        dto = getCommentsWithSplitLevel.apply(comment3, SPLIT_LEVEL);
        checkData(dto);

        deleteComment(comment4);
        //  comment tree for splitLevel = 3
        //  comment1 childCount=2
        //      comment3 childCount=0
        //          comment5[deleted] childCount=0
        //          comment6[deleted] childCount=0
        //      comment4[deleted] childCount=1
        //          comment7 childCount=0
        //  comment2 childCount=0
        dto = getCommentsWithSplitLevel.apply(comment4, SPLIT_LEVEL);
        checkData(comment4, dto, comment7);
        checkBranch(dto, comment4, COMMENT_CMP, comment7);
        checkCount(dto, comment7, 0L);
        dto = getCommentsWithSplitLevel.apply(null, SPLIT_LEVEL);
        checkData(ROOT_ID, dto, comment1, comment2);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment1, comment2);
        checkCount(dto, comment1, 2L);
        checkCount(dto, comment2, 0L);

        dto = getCommentsWithSplitLevel.apply(comment1, SPLIT_LEVEL);
        checkData(comment1, dto, comment3, comment4);
        checkBranch(dto, comment1, COMMENT_CMP, comment3, comment4);
        checkCount(dto, comment3, 0L);
        checkCount(dto, comment4, 1L);

        deleteComment(comment7);
        //  comment tree for splitLevel = 3
        //  comment1 childCount=1
        //      comment3 childCount=0
        //          comment5[deleted] childCount=0
        //          comment6[deleted] childCount=0
        //      comment4[deleted] childCount=0
        //          comment7[deleted] childCount=0
        //  comment2 childCount=0
        dto = getCommentsWithSplitLevel.apply(comment7, SPLIT_LEVEL);
        checkData(dto);
        dto = getCommentsWithSplitLevel.apply(null, SPLIT_LEVEL);
        checkData(ROOT_ID, dto, comment1, comment2);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment1, comment2);
        checkCount(dto, comment1, 1L);
        checkCount(dto, comment2, 0L);

        dto = getCommentsWithSplitLevel.apply(comment1, SPLIT_LEVEL);
        checkData(comment1, dto, comment3);
        checkBranch(dto, comment1, COMMENT_CMP, comment3);
        checkCount(dto, comment3, 0L);

        deleteComment(comment3);
        //  comment tree for splitLevel = 3
        //  comment1 childCount=0
        //      comment3[deleted] childCount=0
        //          comment5[deleted] childCount=0
        //          comment6[deleted] childCount=0
        //      comment4[deleted] childCount=0
        //          comment7[deleted] childCount=0
        //  comment2 childCount=0
        dto = getCommentsWithSplitLevel.apply(comment3, SPLIT_LEVEL);
        checkData(dto);
        dto = getCommentsWithSplitLevel.apply(null, SPLIT_LEVEL);
        checkData(ROOT_ID, dto, comment1, comment2);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment1, comment2);
        checkCount(dto, comment1, 0L);
        checkCount(dto, comment2, 0L);

        dto = getCommentsWithSplitLevel.apply(comment1, SPLIT_LEVEL);
        checkData(dto);

        deleteComment(comment2);
        //  comment tree for splitLevel = 3
        //  comment1 childCount=0
        //      comment3[deleted] childCount=0
        //          comment5[deleted] childCount=0
        //          comment6[deleted] childCount=0
        //      comment4[deleted] childCount=0
        //          comment7[deleted] childCount=0
        //  comment2[deleted] childCount=0
        assertEquals(1, mvc.getFirstLevelCommentsCount(ENTITY_ID));
        dto = getCommentsWithSplitLevel.apply(comment2, SPLIT_LEVEL);
        checkData(dto);
        dto = getCommentsWithSplitLevel.apply(null, SPLIT_LEVEL);
        checkData(ROOT_ID, dto, comment1);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment1);
        checkCount(dto, comment1, 0L);

        deleteComment(comment1);
        //  comment tree for splitLevel = 3
        //  comment1[deleted] childCount=0
        //      comment3[deleted] childCount=0
        //          comment5[deleted] childCount=0
        //          comment6[deleted] childCount=0
        //      comment4[deleted] childCount=0
        //          comment7[deleted] childCount=0
        //  comment2[deleted] childCount=0
        assertEquals(0, mvc.getFirstLevelCommentsCount(ENTITY_ID));
        dto = getCommentsWithSplitLevel.apply(comment1, SPLIT_LEVEL);
        checkData(dto);
        dto = getCommentsWithSplitLevel.apply(null, SPLIT_LEVEL);
        checkData(dto);
    }


    @Test
    public void testPagingWithSplitLevel3ByUid() throws Exception {
        checkTestPagingWithSplitLevel3(false);
    }

    @Test
    public void testPagingWithSplitLevel3ByYandexUid() throws Exception {
        checkTestPagingWithSplitLevel3(true);
    }

//    c1    c2   c8  c9  c10
//    |  \
//    c3  c4_
//    |   \  \
//    c5  c7 c11
//    |
//    c6__ ___ ___
//    |   \   \   \
//    c12 c14 c15 c16
//    |
//    c13
    private void checkTestPagingWithSplitLevel3(boolean isYandexUid) throws Exception {
        final BiFunction<Long, Long, CommentResultDto> getComments = mvc
            .getCommentResultDtoByParentAndSplitLevel(ENTITY_ID, isYandexUid);
        CommentResultDto dto;

        long comment1 = createComment(ENTITY_ID, UID, null, getAnyBody());
        long comment2 = createComment(ENTITY_ID, UID + 1, null, getAnyBody());
        long comment3 = createComment(ENTITY_ID, UID + 5, comment1, getAnyBody());
        long comment4 = createComment(ENTITY_ID, UID + 6, comment1, getAnyBody());
        long comment5 = createComment(ENTITY_ID, UID + 7, comment3, getAnyBody());
        long comment6 = createComment(ENTITY_ID, UID + 10, comment5, getAnyBody());
        long comment7 = createComment(ENTITY_ID, UID + 8, comment4, getAnyBody());
        long comment8 = createComment(ENTITY_ID, UID + 2, null, getAnyBody());
        long comment9 = createComment(ENTITY_ID, UID + 3, null, getAnyBody());
        long comment10 = createComment(ENTITY_ID, UID + 4, null, getAnyBody());
        long comment11 = createComment(ENTITY_ID, UID + 9, comment4, getAnyBody());
        long comment12 = createComment(ENTITY_ID, UID + 11, comment6, getAnyBody());
        long comment13 = createComment(ENTITY_ID, UID + 15, comment12, getAnyBody());
        long comment14 = createComment(ENTITY_ID, UID + 12, comment6, getAnyBody());
        long comment15 = createComment(ENTITY_ID, UID + 13, comment6, getAnyBody());
        long comment16 = createComment(ENTITY_ID, UID + 14, comment6, getAnyBody());


        setUpdateTime(comment1, comment2, comment3, comment4, comment5, comment6, comment7, comment8, comment9, comment10,
            comment11, comment12, comment13, comment14, comment15, comment16);

        //  comment tree for splitLevel = 3
        //  comment10 childCount=0
        //  comment9 childCount=0
        //  comment8 childCount=0
        //  comment2 childCount=0
        //  comment1 childCount=2
        //      comment3 childCount=2
        //          comment5 childCount=0
        //          comment6 childCount=0
        //          comment12 childCount=0
        //          comment13 childCount=0
        //          comment14 childCount=0
        //          comment15 childCount=0
        //          comment16 childCount=0
        //      comment4 childCount=1
        //          comment7 childCount=0
        //          comment11 childCount=0

        // смотрим на первый уровень, 3 коммента от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, null, null, 3L);
        checkData(ROOT_ID, dto, comment10, comment9, comment8);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment10, comment9, comment8);
        checkBorderId(dto, comment8);

        // смотрим на первый уровень, 4 коммента от корня (обратная сортировка, хак через спец. лимит)
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, null, null, UGLY_LIMIT_TO_DISABLE_TREE_REVERSE);
        checkData(ROOT_ID, dto, comment2, comment8, comment9, comment10);
        checkBranch(dto, ROOT_ID, COMMENT_CMP, comment2, comment8, comment9, comment10);
        checkBorderId(dto, comment2);

        // смотрим на первый уровень, на 3 коммента от comment3 не включительно
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, null, comment3, 3L);
        checkData(ROOT_ID, dto, comment2, comment1);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment2, comment1);
        checkBorderId(dto, null);

        // смотрим на первый уровень, 5 комментов от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, null, null, 5L);
        checkData(ROOT_ID, dto, comment10, comment9, comment8, comment2, comment1);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment10, comment9, comment8, comment2, comment1);
        checkBorderId(dto, null);

        // смотрим на первый уровень, 6 комментов от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, null, null, 6L);
        checkData(ROOT_ID, dto, comment10, comment9, comment8, comment2, comment1);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment10, comment9, comment8, comment2, comment1);
        checkBorderId(dto, null);

        // смотрим на второй уровень у коммента comment1, 3 коммента от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment1, null, 3L);
        checkData(comment1, dto, comment4, comment3);
        checkBranch(dto, comment1, COMMENT_CMP, comment4, comment3);
        checkBorderId(dto, null);

        // смотрим на второй уровень у коммента comment1, 1 коммент от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment1, null, 1L);
        checkData(comment1, dto, comment4);
        checkBranch(dto, comment1, COMMENT_CMP, comment4);
        checkBorderId(dto, comment4);

        // смотрим на второй уровень у коммента comment1, 2 коммента от comment4 не включительно
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment1, comment4, 2L);
        checkData(comment1, dto, comment3);
        checkBranch(dto, comment1, COMMENT_CMP, comment3);
        checkBorderId(dto, null);

        // смотрим на трейтий уровень у коммента comment3, 3 коммента от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment3, null, 3L);
        checkData(comment3, dto, comment16, comment15, comment14);
        checkBranch(dto, comment3, COMMENT_CMP, comment16, comment15, comment14);
        checkBorderId(dto, comment14);

        // смотрим на третий уровень у коммента comment3, 3 коммента от comment14 не включительно
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment3, comment14, 3L);
        checkData(comment3, dto, comment13, comment12, comment6);
        checkBranch(dto, comment3, COMMENT_CMP, comment13, comment12, comment6);
        checkBorderId(dto, comment6);

        // смотрим на третий уровень у коммента comment3, 3 коммента от comment6 не включительно
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment3, comment6, 3L);
        checkData(comment3, dto, comment5);
        checkBranch(dto, comment3, COMMENT_CMP, comment5);
        checkBorderId(dto, null);

        // смотрим на третий уровень у коммента comment3, 5 комментов от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment3, null, 5L);
        checkData(comment3, dto, comment16, comment15, comment14, comment13, comment12);
        checkBranch(dto, comment3, COMMENT_CMP, comment16, comment15, comment14, comment13, comment12);
        checkBorderId(dto, comment12);

        // смотрим на третий уровень у коммента comment3, 5 комментов от comment12 не включительно
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment3, comment12, 5L);
        checkData(comment3, dto, comment6, comment5);
        checkBranch(dto, comment3, COMMENT_CMP, comment6, comment5);
        checkBorderId(dto, null);

        // смотрим на третий уровень у коммента comment3, 10 комментов от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment3, null, 10L);
        checkData(comment3, dto, comment16, comment15, comment14, comment13, comment12, comment6, comment5);
        checkBranch(dto, comment3, COMMENT_CMP, comment16, comment15, comment14, comment13, comment12, comment6, comment5);
        checkBorderId(dto, null);

        deleteComment(comment1);
        deleteComment(comment15);
        //  comment tree for splitLevel = 3
        //  comment10 childCount=0
        //  comment9 childCount=0
        //  comment8 childCount=0
        //  comment2 childCount=0
        //  comment1 childCount=2 [deleted]
        //      comment3 childCount=2
        //          comment5 childCount=0
        //          comment6 childCount=0
        //          comment12 childCount=0
        //          comment13 childCount=0
        //          comment14 childCount=0
        //          comment15 childCount=0 [deleted]
        //          comment16 childCount=0
        //      comment4 childCount=1
        //          comment7 childCount=0
        //          comment11 childCount=0

        // смотрим на первый уровень, 3 коммента от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, null, null, 3L);
        checkData(ROOT_ID, dto, comment10, comment9, comment8);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment10, comment9, comment8);
        checkBorderId(dto, comment8);

        // смотрим на второй уровень у коммента comment1, 3 коммента от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment1, null, 3L);
        checkData(comment1, dto, comment4, comment3);
        checkBranch(dto, comment1, COMMENT_CMP, comment4, comment3);
        checkBorderId(dto, null);

        // смотрим на второй уровень у коммента comment1, 1 коммент от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment1, null, 1L);
        checkData(comment1, dto, comment4);
        checkBranch(dto, comment1, COMMENT_CMP, comment4);
        checkBorderId(dto, comment4);

        // смотрим на второй уровень у коммента comment1, 2 коммента от comment4 не включительно
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment1, comment4, 2L);
        checkData(comment1, dto, comment3);
        checkBranch(dto, comment1, COMMENT_CMP, comment3);
        checkBorderId(dto, null);

        // смотрим на третий уровень у коммента comment3, 3 коммента от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment3, null, 3L);
        checkData(comment3, dto, comment16, comment14, comment13);
        checkBranch(dto, comment3, COMMENT_CMP, comment16, comment14, comment13);
        checkBorderId(dto, comment13);

        // смотрим на третий уровень у коммента comment3, 3 коммента от comment13 не включительно
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment3, comment13, 3L);
        checkData(comment3, dto, comment12, comment6, comment5);
        checkBranch(dto, comment3, COMMENT_CMP, comment12, comment6, comment5);
        checkBorderId(dto, null);

        // смотрим на третий уровень у коммента comment3, 5 комментов от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment3, null, 5L);
        checkData(comment3, dto, comment16, comment14, comment13, comment12, comment6);
        checkBranch(dto, comment3, COMMENT_CMP, comment16, comment14, comment13, comment12, comment6);
        checkBorderId(dto, comment6);

        // смотрим на третий уровень у коммента comment3, 5 комментов от comment6 не включительно
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment3, comment6, 5L);
        checkData(comment3, dto, comment5);
        checkBranch(dto, comment3, COMMENT_CMP, comment5);
        checkBorderId(dto, null);

        // смотрим на третий уровень у коммента comment3, 10 комментов от корня
        dto = getCommentsWithBorderIdAndLimit(isYandexUid, comment3, null, 10L);
        checkData(comment3, dto, comment16, comment14, comment13, comment12, comment6, comment5);
        checkBranch(dto, comment3, COMMENT_CMP, comment16, comment14, comment13, comment12, comment6, comment5);
        checkBorderId(dto, null);
    }

    protected CommentResultDto getCommentsWithBorderIdAndLimit(boolean isYandexUid, Long parentId, Long borderId, Long limit) throws Exception {
        if (isYandexUid) {
            return mvc
                .getResponseDtoByYandexUidAndBorderId(ENTITY_ID, YANDEXUID, parentId, SPLIT_LEVEL, limit, borderId);
        } else {
            return mvc
                .getResponseDtoByUidAndBorderIdId(ENTITY_ID, UID, parentId, SPLIT_LEVEL, limit, borderId);
        }
    }

    @Test
    public void testCommentsWithSplitLevelByUid() throws Exception {
        checkCommentsWithSplitLevel(false);
    }

    @Test
    public void testCommentsWithSplitLevelByYandexUid() throws Exception {
        checkCommentsWithSplitLevel(true);
    }

//    c1      c2
//    |  \
//    c3  c4
//    |
//    c5
//    |
//    c6
    private void checkCommentsWithSplitLevel(boolean isYandexUid) throws Exception {
        final BiFunction<Long, Long, CommentResultDto> getComments = mvc
            .getCommentResultDtoByParentAndSplitLevel(ENTITY_ID, isYandexUid);
        CommentResultDto dto;

        long comment1 = createComment(ENTITY_ID, UID, null, getAnyBody());
        long comment2 = createComment(ENTITY_ID, UID + 1, null, getAnyBody());
        long comment3 = createComment(ENTITY_ID, UID + 2, comment1, getAnyBody());
        long comment4 = createComment(ENTITY_ID, UID + 3, comment1, getAnyBody());
        long comment5 = createComment(ENTITY_ID, UID + 4, comment3, getAnyBody());
        long comment6 = createComment(ENTITY_ID, UID + 5, comment5, getAnyBody());
        setUpdateTime(comment1, comment2, comment3, comment4, comment5, comment6);


        dto = getComments.apply(null, null);
        checkData(dto, comment1, comment2);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment1, comment2);

        dto = getComments.apply(null, 1L);
        checkData(dto, comment1, comment2, comment3, comment4, comment5, comment6);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment1, comment2, comment3, comment4, comment5, comment6);

        dto = getComments.apply(null, 2L);
        checkData(dto, comment1, comment2);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment1, comment2);

        dto = getComments.apply(null, 3L);
        checkData(dto, comment1, comment2);
        checkBranch(dto, ROOT_ID, FIRST_LEVEL_COMMENT_CMP, comment1, comment2);

        //---------------------------------------
        dto = getComments.apply(comment1, 1L);
        checkData(dto);

        dto = getComments.apply(comment1, 2L);
        checkData(dto, comment3, comment4, comment5, comment6);
        checkBranch(dto, comment1, COMMENT_CMP, comment3, comment4, comment5, comment6);

        dto = getComments.apply(comment1, 3L);
        checkData(dto, comment3, comment4);
        checkBranch(dto, comment1, COMMENT_CMP, comment3, comment4);

        dto = getComments.apply(comment1, 4L);
        checkData(dto, comment3, comment4);
        checkBranch(dto, comment1, COMMENT_CMP, comment3, comment4);

        //---------------------------------------
        dto = getComments.apply(comment2, null);
        checkData(dto);
        checkBranch(dto, comment2, COMMENT_CMP);

        dto = getComments.apply(comment2, 1L);
        checkData(dto);

        dto = getComments.apply(comment2, 2L);
        checkData(dto);

        dto = getComments.apply(comment2, 3L);
        checkData(dto);

        //---------------------------------------
        dto = getComments.apply(comment3, null);
        checkData(dto, comment5);
        checkBranch(dto, comment3, COMMENT_CMP, comment5);

        dto = getComments.apply(comment3, 1L);
        checkData(dto);

        dto = getComments.apply(comment3, 2L);
        checkData(dto);

        dto = getComments.apply(comment3, 3L);
        checkData(dto, comment5, comment6);
        checkBranch(dto, comment3, COMMENT_CMP, comment5, comment6);

        dto = getComments.apply(comment3, 4L);
        checkData(dto, comment5);
        checkBranch(dto, comment3, COMMENT_CMP, comment5);

        //---------------------------------------
        dto = getComments.apply(comment4, null);
        checkData(dto);

        dto = getComments.apply(comment4, 1L);
        checkData(dto);

        dto = getComments.apply(comment4, 2L);
        checkData(dto);

        dto = getComments.apply(comment4, 3L);
        checkData(dto);

        dto = getComments.apply(comment4, 4L);
        checkData(dto);

        //---------------------------------------
        dto = getComments.apply(comment5, null);
        checkData(dto, comment6);
        checkBranch(dto, comment5, COMMENT_CMP, comment6);

        dto = getComments.apply(comment5, 1L);
        checkData(dto);

        dto = getComments.apply(comment5, 2L);
        checkData(dto);

        dto = getComments.apply(comment5, 3L);
        checkData(dto);

        dto = getComments.apply(comment5, 4L);
        checkData(dto, comment6);
        checkBranch(dto, comment5, COMMENT_CMP, comment6);

        //---------------------------------------
        dto = getComments.apply(comment6, null);
        checkData(dto);

        dto = getComments.apply(comment6, 1L);
        checkData(dto);

        dto = getComments.apply(comment6, 2L);
        checkData(dto);

        dto = getComments.apply(comment6, 3L);
        checkData(dto);

        dto = getComments.apply(comment6, 4L);
        checkData(dto);
    }

    //    g1
    //    - c1(user2) - ok - cnt 3
    //     |- c3(user1) - ok
    //       |- c4(user2) - pre - skipped
    //         |- c5(user1) - pre
    //       |- c6(user1) - pre
    //         |- c7(user1) - pre
    //           |- c8(user1) - pre
    //         |- c9(user2) - pre - skipped
    //    - c2(user1) - ok - cnt 1
    //      |- c10(user1) - pre
    //
    //    g2
    //    - с11(user1) - pre
    //     |- c13(user2)  - pre
    //       |- c14(user1)  - pre
    //    - c12(user1)  - pre
    @Test
    public void checkCommentsWithSplitLevelAndPremoderation() throws Exception {
        long c1 = createComment(ENTITY_ID, UID+1, null, getBody("c1"));
        long c2 = createComment(ENTITY_ID, UID, null, getBody("c2"));
        long c3 = createComment(ENTITY_ID, UID, c1, getBody("c3"));

        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION, true);


        Long temp = null;
        Long temp2 = null;
        long[] ids = {
            // c0 - to count from 1
            0,
            c1, c2, c3,
            temp = createComment(ENTITY_ID, UID + 1, c3, getBody("c4")),
            createComment(ENTITY_ID, UID, temp, getBody("c5")),
            temp = createComment(ENTITY_ID, UID, c3, getBody("c6")),
            temp2 = createComment(ENTITY_ID, UID, temp, getBody("c7")),
            createComment(ENTITY_ID, UID, temp2, getBody("c8")),
            createComment(ENTITY_ID, UID + 1, temp, getBody("c9")),
            createComment(ENTITY_ID, UID, c2, getBody("c10")),
            //
            temp = createComment(ENTITY_ID + 1, UID, null, getBody("c11")),
            createComment(ENTITY_ID + 1, UID, null, getBody("c12")),
            temp = createComment(ENTITY_ID + 1, UID + 1, temp, getBody("c13")),
            createComment(ENTITY_ID + 1, UID, temp, getBody("c14")),
        };

        CommentProject project = mvc.getProject();
        long[] entityIds = {ENTITY_ID, ENTITY_ID + 1};
        var getPreviewFun = mvc.getBulkCommentPreview(entityIds, false);
        long limit = 10;

        assertResult(getPreviewFun.apply(SPLIT_LEVEL, limit).getData(),
            CommentAssert.checkResult(project, entityIds[0],
                checkComment(ids[2],
                    checkText("c2"),
                    checkAuthor(UserInfo.uid(UID)),
                    checkPublished(true),
                    checkChildCount(0, 1),
                    checkSubComments()
                ),
                checkComment(ids[1],
                    checkText("c1"),
                    checkAuthor(UserInfo.uid(UID+1)),
                    checkPublished(true),
                    checkChildCount(1, 1),
                    checkSubComments()
                )
            ),
            CommentAssert.checkResult(project, entityIds[1],
                checkComment(ids[12],
                    checkText("c12"),
                    checkAuthor(UserInfo.uid(UID)),
                    checkPublished(true),
                    checkChildCount(0, 0),
                    checkSubComments()
                ),
                checkComment(ids[11],
                    checkText("c11"),
                    checkAuthor(UserInfo.uid(UID)),
                    checkPublished(true),
                    checkChildCount(0, 0),
                    checkSubComments()
                )
            )
        );


        var getByParent = mvc.getCommentResultDtoByParentAndSplitLevel(ENTITY_ID, false);
        assertComments(getByParent.apply(ids[1], SPLIT_LEVEL).getData(),
            checkComment(ids[3],
                checkText("c3"),
                checkAuthor(UserInfo.uid(UID)),
                checkPublished(true),
                checkChildCount(0, 4),
                checkSubComments()
            )
        );

        assertComments(getByParent.apply(ids[3], SPLIT_LEVEL).getData(),
            checkComment(ids[8],
                checkText("c8"),
                checkAuthor(UserInfo.uid(UID)),
                checkPublished(true),
                checkChildCount(0, 0),
                checkSubComments()
            ),
            checkComment(ids[7],
                checkText("c7"),
                checkAuthor(UserInfo.uid(UID)),
                checkPublished(true),
                checkChildCount(0, 0),
                checkSubComments()
            ),
            checkComment(ids[6],
                checkText("c6"),
                checkAuthor(UserInfo.uid(UID)),
                checkPublished(true),
                checkChildCount(0, 0),
                checkSubComments()
            ),
            checkComment(ids[5],
                checkText("c5"),
                checkAuthor(UserInfo.uid(UID)),
                checkPublished(true),
                checkChildCount(0, 0),
                checkSubComments()
            )
        );
    }

}
