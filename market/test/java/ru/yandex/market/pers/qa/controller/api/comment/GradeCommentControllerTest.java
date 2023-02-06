package ru.yandex.market.pers.qa.controller.api.comment;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.db.DbUtil;
import ru.yandex.market.pers.grade.client.GradeClient;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.dto.CommentTreeDto;
import ru.yandex.market.pers.qa.client.dto.EntityCountDto;
import ru.yandex.market.pers.qa.client.model.CommentState;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.client.utils.ControllerConstants;
import ru.yandex.market.pers.qa.mock.mvc.GradeCommentMvcMocks;
import ru.yandex.market.pers.qa.mock.mvc.GradeMvcMocks;
import ru.yandex.market.pers.qa.model.AuthorFilter;
import ru.yandex.market.pers.qa.model.Comment;
import ru.yandex.market.pers.qa.model.CommentFilter;
import ru.yandex.market.pers.qa.model.Sort;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.util.ExecUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.model.CommentProject.GRADE;
import static ru.yandex.market.pers.qa.client.model.SortField.ID;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.assertForest;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.assertResult;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkAuthor;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkCanDelete;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkChildCount;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkComment;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkParams;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkParamsHas;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkPublished;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkState;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkSubComments;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkText;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkUser;
import static ru.yandex.market.pers.qa.utils.CommentUtils.PARAM_GRADE_TYPE;
import static ru.yandex.market.pers.qa.utils.CommentUtils.PARAM_GRADE_TYPE_MODEL;
import static ru.yandex.market.pers.qa.utils.CommentUtils.PARAM_ORIG_ROOT;
import static ru.yandex.market.pers.qa.utils.CommentUtils.PARAM_SHOP_ID;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 09.09.2019
 */
public class GradeCommentControllerTest extends AbstractCommentControllerTest {
    @Autowired
    private CommentService commentService;
    @Autowired
    private GradeMvcMocks gradeMvc;
    @Autowired
    private GradeCommentMvcMocks gradeCommentMvc;
    @Autowired
    private PersNotifyClient notifyClient;
    @Autowired
    private GradeClient gradeClient;

    @Test
    void testNotification() throws Exception {
        long gradeId = 343;

        PersQaServiceMockFactory.mockGradeWithFixId(gradeClient, gradeId, UID, GradeType.MODEL_GRADE, 1, 540L);
        commentService.createComment(GRADE, UID, "User comment", gradeId);
        ArgumentCaptor<NotificationEventSource> commentsCaptor = ArgumentCaptor.forClass(NotificationEventSource.class);
        verify(notifyClient, times(1)).createEvent(commentsCaptor.capture());
        assertEquals(String.valueOf(gradeId),
            commentsCaptor.getValue().getData().get(NotificationEventDataName.GRADE_ID));

        resetMocks();

        MutableInt call = new MutableInt(0);
        Mockito.doAnswer(invocation -> {
            if (call.getAndAdd(1) == 0) {
                throw new RuntimeException();
            }
            return null;
        }).when(notifyClient).createEvent(any(NotificationEventSource.class));

        int shopId = 1;
        commentService.createShopComment(GRADE, UID, "Shop comment", gradeId, shopId);
        verify(notifyClient, times(2)).createEvent(any(NotificationEventSource.class));
    }

    @Test
    void testTreeUid() throws Exception {
        checkTree(gradeIds -> {
            try {
                return gradeMvc.getCommentsBulkUid(UID + 1, gradeIds, null).getData();
            } catch (Exception e) {
                throw ExecUtils.silentError(e);
            }
        });
    }

    @Test
    void testTreeYandexUid() throws Exception {
        checkTree(gradeIds -> {
            try {
                return gradeMvc.getCommentsBulkYandexUid(YANDEXUID, gradeIds).getData();
            } catch (Exception e) {
                throw ExecUtils.silentError(e);
            }
        });
    }

    @Test
    void testTreeWithDisabledPublication() throws Exception {
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION, true);

        // user comments are banned (loaded by other uids)
        // shop comments are fine
        long shopId = 9;
        long[] gradeIds = {1, 2, 3};
        long[] ids = prepareTestTree();

        assertForest(gradeMvc.getCommentsBulkUid(UID + 1, gradeIds, null).getData(),
            CommentAssert.checkTree(GRADE, gradeIds[0],
                checkComment(ids[1],
                    checkText("Shop comment"),
                    checkUser(null), // cleaned
                    checkAuthor(new UserInfo(UserType.SHOP, shopId)),
                    checkCanDelete(false),
                    checkPublished(true),
                    checkSubComments(
                        checkComment(ids[3],
                            checkText("User response"),
                            checkUser(null),
                            checkAuthor(UserInfo.uid(UID)),
                            checkCanDelete(false),
                            checkPublished(false),
                            checkSubComments(
                                checkComment(ids[4],
                                    checkText("Shop response"),
                                    checkUser(null),
                                    checkAuthor(new UserInfo(UserType.SHOP, shopId)),
                                    checkCanDelete(false),
                                    checkPublished(true),
                                    checkSubComments()
                                )
                            )
                        )
                    )
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[1],
                checkComment(ids[5],
                    checkText("User comment"),
                    checkUser(null),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkPublished(false),
                    checkSubComments(
                        checkComment(ids[6],
                            checkText("Shop comment"),
                            checkUser(null),
                            checkAuthor(new UserInfo(UserType.SHOP, shopId)),
                            checkCanDelete(false),
                            checkPublished(true),
                            checkSubComments()
                        )
                    )
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[2])
        );
    }

    @Test
    void testTreeWithDisabledPublicationWithShop() throws Exception {
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION, true);
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION_SHOP, true);

        // load by UID = author -> his comments seem as published
        // shop comments looks as banned
        long shopId = 9;
        long[] gradeIds = {1, 2, 3};
        long[] ids = prepareTestTree();
        assertForest(gradeMvc.getCommentsBulkUid(UID, gradeIds, null).getData(),
            CommentAssert.checkTree(GRADE, gradeIds[0],
                checkComment(ids[2],
                    checkText("User comment"),
                    checkUser(null),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(true),
                    checkPublished(true),
                    checkSubComments()
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[1],
                checkComment(ids[7],
                    checkText("User comment 2"),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(true),
                    checkPublished(true),
                    checkSubComments()
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[2])
        );
    }

    @Test
    void testTreeWithDisabledPublicationTwoUsers() throws Exception {
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION, true);
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION_SHOP, true);

        // save by uid/uid+1
        // load by UID = author -> his comments seem as published, other banned
        long shopId = 9;
        long[] gradeIds = {1, 2, 3};

        //    g1
        //    - c1(user2)
        //     |- c3(user1)
        //       |- c4(user2)
        //    - c2(user1)
        //
        //    g2
        //    - с5(user1)
        //     |- c6(user2)
        //       |- c8(user1)
        //    - c7(user1)
        //       |- c9(user1)
        //       |- c10(user1)
        //         |- c11(user1)

        Long temp = null;
        Long temp2 = null;
        long[] ids = {
            // c0 - to count from 1
            0,

            // c1
            temp = commentService.createShopComment(GRADE, UID + 1, "User comment c1", gradeIds[0], shopId),
            // c2
            commentService.createComment(GRADE, UID, "User comment c2", gradeIds[0]),
            // c3
            temp = commentService.createComment(GRADE, UID, "User response c3", gradeIds[0], temp),
            // c4
            commentService.createShopComment(GRADE, UID + 1, "User response c4", gradeIds[0], shopId, temp),

            // grade 2
            // c5
            temp = commentService.createComment(GRADE, UID, "User comment c5", gradeIds[1]),
            // c6
            temp = commentService.createShopComment(GRADE, UID + 1, "User comment c6", gradeIds[1], shopId, temp),
            // c7
            temp2 = commentService.createComment(GRADE, UID, "User comment c7", gradeIds[1]),
            // c8
            commentService.createComment(GRADE, UID, "User comment (del) c8", gradeIds[1], temp),
            // c9
            commentService.createComment(GRADE, UID, "User comment c9", gradeIds[1], temp2),
            // c10
            temp = commentService.createComment(GRADE, UID, "User comment c10", gradeIds[1], temp2),
            // c11
            temp = commentService.createComment(GRADE, UID, "User comment c11", gradeIds[1], temp)
        };

        gradeMvc.deleteComment(ids[8], UID);

        // result
        //    g1
        //    - c1(user2) - banned - not show
        //     |- c3(user1) - not show, in banned
        //    - c2(user1)
        //
        //    g2
        //    - с5(user1)
        //     |- c6(user2) - banned - not show
        //       |- c8(user1) - not show, in banned
        //    - c7(user1)
        //       |- c9(user1)
        //       |- c10(user1)
        //         |- c11(user1)

        Map<Long, Long> countMap = gradeCommentMvc.getFirstLevelCommentsCountBulk(gradeIds, 2L)
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, x->x.getValue().getCount()));

        // formally all content was blocked
        assertEquals(Map.of(
            gradeIds[0], 0L,
            gradeIds[1], 0L,
            gradeIds[2], 0L
        ), countMap);

        // counts with preview y user
        Map<Long, Long> countMapForUser = gradeCommentMvc.getFirstLevelCommentsCountBulk(gradeIds, 2L, UID)
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, x->x.getValue().getCount()));

        // user can see own content on premoderation
        assertEquals(Map.of(
            gradeIds[0], 1L,
            gradeIds[1], 2L,
            gradeIds[2], 0L
        ), countMapForUser);

        // in this request all counts are zero, since correct counts are not recalculated
        assertForest(gradeMvc.getCommentsBulkUid(UID, gradeIds, null).getData(),
            CommentAssert.checkTree(GRADE, gradeIds[0],
                checkComment(ids[2],
                    checkText("User comment c2"),
                    checkAuthor(UserInfo.uid(UID)),
                    checkPublished(true),
                    checkChildCount(0, 0),
                    checkSubComments()
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[1],
                checkComment(ids[5],
                    checkText("User comment c5"),
                    checkAuthor(UserInfo.uid(UID)),
                    checkPublished(true),
                    checkChildCount(0, 0),
                    checkSubComments()
                ),
                checkComment(ids[7],
                    checkText("User comment c7"),
                    checkAuthor(UserInfo.uid(UID)),
                    checkPublished(true),
                    checkChildCount(0, 0),
                    checkSubComments(
                        checkComment(ids[9],
                            checkText("User comment c9"),
                            checkAuthor(UserInfo.uid(UID)),
                            checkPublished(true),
                            checkChildCount(0, 0),
                            checkSubComments()
                        ),
                        checkComment(ids[10],
                            checkText("User comment c10"),
                            checkAuthor(UserInfo.uid(UID)),
                            checkPublished(true),
                            checkChildCount(0, 0),
                            checkSubComments(
                                checkComment(ids[11],
                                    checkText("User comment c11"),
                                    checkAuthor(UserInfo.uid(UID)),
                                    checkPublished(true),
                                    checkChildCount(0, 0),
                                    checkSubComments()
                                )
                            )
                        )
                    )
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[2])
        );


        var getPreviewFun = gradeCommentMvc.getBulkCommentPreview(gradeIds, false);
        long splitLevel = 2;
        long limit = 10;

        assertResult(getPreviewFun.apply(splitLevel, limit).getData(),
            CommentAssert.checkResult(GRADE, gradeIds[0],
                checkComment(ids[2],
                    checkText("User comment c2"),
                    checkAuthor(UserInfo.uid(UID)),
                    checkPublished(true),
                    checkChildCount(0, 0),
                    checkSubComments()
                )
            ),
            CommentAssert.checkResult(GRADE, gradeIds[1],
                checkComment(ids[7],
                    checkText("User comment c7"),
                    checkAuthor(UserInfo.uid(UID)),
                    checkPublished(true),
                    checkChildCount(0, 3),
                    checkSubComments()
                ),
                checkComment(ids[5],
                    checkText("User comment c5"),
                    checkAuthor(UserInfo.uid(UID)),
                    checkPublished(true),
                    checkChildCount(0, 0),
                    checkSubComments()
                )
            )
        );
    }

    private long[] prepareTestTree() {
        //    g1
        //    - c1(sh)
        //     |- c3(u)
        //       |- c4(sh)
        //    - c2(u)
        //
        //    g2
        //    - с5(u)+del
        //     |- c6(sh)
        //       |- c8(u)+del
        //    - c7(u)
        long shopId = 9;
        long[] gradeIds = {1, 2, 3};

        long temp;
        long[] result = {
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
            temp = commentService.createShopComment(GRADE, UID, "Shop comment", gradeIds[1], shopId, temp),
            // c7
            commentService.createComment(GRADE, UID, "User comment 2", gradeIds[1]),
            // c8
            commentService.createComment(GRADE, UID, "User comment (del)", gradeIds[1], temp)
        };

        gradeMvc.deleteComment(result[5], UID);
        gradeMvc.deleteComment(result[8], UID);

        return result;
    }

    private void checkTree(Function<long[], List<CommentTreeDto>> commentsMethod) {
        long shopId = 9;
        long[] gradeIds = {1, 2, 3};
        long[] ids = prepareTestTree();
        assertForest(commentsMethod.apply(gradeIds),
            CommentAssert.checkTree(GRADE, gradeIds[0],
                checkComment(ids[1],
                    checkText("Shop comment"),
                    checkUser(null), // cleaned
                    checkAuthor(new UserInfo(UserType.SHOP, shopId)),
                    checkCanDelete(false),
                    checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(shopId))),
                    checkSubComments(
                        checkComment(ids[3],
                            checkText("User response"),
                            checkUser(null),
                            checkAuthor(UserInfo.uid(UID)),
                            checkCanDelete(false),
                            checkParams(Map.of(
                                PARAM_GRADE_TYPE, PARAM_GRADE_TYPE_MODEL
                            )),
                            checkSubComments(
                                checkComment(ids[4],
                                    checkText("Shop response"),
                                    checkUser(null),
                                    checkAuthor(new UserInfo(UserType.SHOP, shopId)),
                                    checkCanDelete(false),
                                    checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(shopId))),
                                    checkSubComments()
                                )
                            )
                        )
                    )
                ),
                checkComment(ids[2],
                    checkText("User comment"),
                    checkUser(null),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkSubComments()
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[1],
                checkComment(ids[5],
                    checkText("User comment"),
                    checkUser(null),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkSubComments(
                        checkComment(ids[6],
                            checkText("Shop comment"),
                            checkUser(null),
                            checkAuthor(new UserInfo(UserType.SHOP, shopId)),
                            checkCanDelete(false),
                            checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(shopId))),
                            checkSubComments()
                        )
                    )
                ),
                checkComment(ids[7],
                    checkText("User comment 2"),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkSubComments()
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[2])
        );
    }

    @Test
    public void testAuthorSearch() {
        long shopId = 9;
        long brandId = 123;

        long[] c = prepareTestTree();

        CommentFilter byUid = new CommentFilter().author(new AuthorFilter(UserType.UID, UID)).sort(Sort.asc(ID));
        CommentFilter byShop = new CommentFilter().author(new AuthorFilter(UserType.SHOP, shopId)).sort(Sort.asc(ID));
        CommentFilter byBrand = new CommentFilter().author(new AuthorFilter(UserType.VENDOR,
            brandId)).sort(Sort.asc(ID));

        assertEquals(List.of(c[2], c[3], c[5], c[7], c[8]),
            commentService.getComments(byUid)
                .stream().map(Comment::getId).collect(Collectors.toList()));

        assertEquals(List.of(c[1], c[4], c[6]),
            commentService.getComments(byShop)
                .stream().map(Comment::getId).collect(Collectors.toList()));

        assertEquals(List.of(),
            commentService.getComments(byBrand)
                .stream().map(Comment::getId).collect(Collectors.toList()));
    }

    @Test
    public void testTreeWithFixId() throws Exception {
        long modelId = 9;
        long[] gradeIds = {1, 2, 3};
        PersQaServiceMockFactory.mockModelGradeWithFixId(gradeClient, gradeIds[1], UID, modelId);
        long fixIdGr1 = PersQaServiceMockFactory.getFixIdById(gradeIds[1]);

        //    g1
        //    - c1(sh)
        //     |- c3(u)
        //       |- c4(sh)
        //    - c2(u)
        //
        //    g2
        //    - с5(u)+del
        //     |- c6(sh)
        //       |- c8(u)+del
        //    - c7(u)
        long[] ids = prepareTestTree();

        // check comments in DB
        checkCommentInDb(ids[1], gradeIds[0], GRADE.getId(), null, 0, 1, 2);
        checkCommentInDb(ids[4], gradeIds[0], GRADE.getId(), ids[3], 2, 0, 0, ids[1], ids[3]);
        checkCommentInDb(ids[5], fixIdGr1, GRADE.getId(), null, 0, 1, 1);
        checkCommentInDb(ids[6], fixIdGr1, GRADE.getId(), ids[5], 1, 0, 0, ids[5]);

        // check is same as above, except for additional properties (orig_id parameter)
        assertForest(gradeMvc.getCommentsBulkUid(UID + 1, gradeIds, null).getData(),
            CommentAssert.checkTree(GRADE, gradeIds[0],
                checkComment(ids[1],
                    checkText("Shop comment"),
                    checkUser(null),
                    checkAuthor(new UserInfo(UserType.SHOP, modelId)),
                    checkCanDelete(false),
                    checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(modelId))),
                    checkSubComments(
                        checkComment(ids[3],
                            checkText("User response"),
                            checkUser(null),
                            checkAuthor(UserInfo.uid(UID)),
                            checkCanDelete(false),
                            checkParams(Map.of(
                                PARAM_GRADE_TYPE, PARAM_GRADE_TYPE_MODEL
                            )),
                            checkSubComments(
                                checkComment(ids[4],
                                    checkText("Shop response"),
                                    checkUser(null),
                                    checkAuthor(new UserInfo(UserType.SHOP, modelId)),
                                    checkCanDelete(false),
                                    checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(modelId))),
                                    checkSubComments()
                                )
                            )
                        )
                    )
                ),
                checkComment(ids[2],
                    checkText("User comment"),
                    checkUser(null),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkSubComments()
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[1],
                checkComment(ids[5],
                    checkText("User comment"),
                    checkUser(null),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkParams(Map.of(
                        PARAM_ORIG_ROOT, "" + gradeIds[1],
                        PARAM_GRADE_TYPE, PARAM_GRADE_TYPE_MODEL
                    )),
                    checkSubComments(
                        checkComment(ids[6],
                            checkText("Shop comment"),
                            checkUser(null),
                            checkAuthor(new UserInfo(UserType.SHOP, modelId)),
                            checkCanDelete(false),
                            checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(modelId))),
                            checkSubComments()
                        )
                    )
                ),
                checkComment(ids[7],
                    checkText("User comment 2"),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkSubComments()
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[2])
        );
    }

    @Test
    void testTreeUidReversed() throws Exception {
        //    g1
        //    - c1(sh)
        //     |- c3(u)
        //     |- c4(sh)
        //    - c2(u)
        long shopId = 9;
        long[] gradeIds = {1, 2, 3};

        long temp;
        long[] ids = {
            0, // c0 - to count from 1
            temp = commentService.createShopComment(GRADE, UID, "Shop comment", gradeIds[0], shopId), // c1
            commentService.createComment(GRADE, UID, "User comment", gradeIds[0]), // c2
            commentService.createComment(GRADE, UID, "User response", gradeIds[0], temp), // c3
            commentService.createShopComment(GRADE, UID, "Shop response", gradeIds[0], shopId, temp), // c4
        };

        assertForest(gradeMvc.getCommentsBulkUid(UID + 1, gradeIds, Sort.DESC).getData(),
            CommentAssert.checkTree(GRADE, gradeIds[0],
                checkComment(ids[2],
                    checkText("User comment"),
                    checkUser(null),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkSubComments()
                ),
                checkComment(ids[1],
                    checkText("Shop comment"),
                    checkUser(null), // cleaned
                    checkAuthor(new UserInfo(UserType.SHOP, shopId)),
                    checkCanDelete(false),
                    checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(shopId))),
                    checkSubComments(
                        checkComment(ids[4],
                            checkText("Shop response"),
                            checkUser(null),
                            checkAuthor(new UserInfo(UserType.SHOP, shopId)),
                            checkCanDelete(false),
                            checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(shopId))),
                            checkSubComments()
                        ),
                        checkComment(ids[3],
                            checkText("User response"),
                            checkUser(null),
                            checkAuthor(UserInfo.uid(UID)),
                            checkCanDelete(false),
                            checkParams(Map.of(
                                PARAM_GRADE_TYPE, PARAM_GRADE_TYPE_MODEL
                            )),
                            checkSubComments()
                        )
                    )
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[1]),
            CommentAssert.checkTree(GRADE, gradeIds[2])
        );
    }

    @Test
    public void testCapiPreview() throws Exception {
        long shopId = 9;
        long[] gradeIds = {1, 2, 3};

        // ensure to check with mocked fixId for one of grades
        PersQaServiceMockFactory.mockModelGradeWithFixId(gradeClient, gradeIds[1], UID, shopId);

        long[] ids = prepareTestTree();
        int limit = 2;

        // limited tree, user info is not reduced
        //    g1
        //    - c1(sh)
        //    - c2(u)
        //
        //    g2
        //    - с5(u)
        //     |- c6(sh)
        assertForest(gradeMvc.getCommentsBulkCapiPreview(gradeIds, limit).getData(),
            CommentAssert.checkTree(GRADE, gradeIds[0],
                checkComment(ids[1],
                    checkText("Shop comment"),
                    checkUser(UserInfo.uid(UID)),
                    checkAuthor(new UserInfo(UserType.SHOP, shopId)),
                    checkCanDelete(false),
                    checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(shopId))),
                    checkSubComments()
                ),
                checkComment(ids[2],
                    checkText("User comment"),
                    checkUser(UserInfo.uid(UID)),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkSubComments()
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[1],
                checkComment(ids[5],
                    checkPublished(false),
                    checkState(CommentState.DELETED),
                    checkText("User comment"),
                    checkUser(UserInfo.uid(UID)),
                    checkAuthor(UserInfo.uid(UID)),
                    checkCanDelete(false),
                    checkSubComments(
                        checkComment(ids[6],
                            checkPublished(true),
                            checkState(CommentState.NEW),
                            checkText("Shop comment"),
                            checkUser(UserInfo.uid(UID)),
                            checkAuthor(new UserInfo(UserType.SHOP, shopId)),
                            checkCanDelete(false),
                            checkParamsHas(Map.of(PARAM_SHOP_ID, String.valueOf(shopId))),
                            checkSubComments()
                        )
                    )
                )
            ),
            CommentAssert.checkTree(GRADE, gradeIds[2])
        );
    }


    @Test
    void testBulkCountUid() throws Exception {
        checkBulkCount(gradeIds -> {
            try {
                return gradeMvc.getCommentsCountBulkUid(UID, gradeIds).getData();
            } catch (Exception e) {
                throw ExecUtils.silentError(e);
            }
        });
    }

    @Test
    void testBulkCountYandexUid() throws Exception {
        checkBulkCount(gradeIds -> {
            try {
                return gradeMvc.getCommentsCountBulkYandexUid(YANDEXUID, gradeIds).getData();
            } catch (Exception e) {
                throw ExecUtils.silentError(e);
            }
        });
    }

    private void checkBulkCount(Function<long[], List<EntityCountDto>> countMethod) {
        long[] gradeIds = {1, 2, 3};
        long[] expectedCount = {4, 2, 0};

        prepareTestTree();

        List<EntityCountDto> counts = countMethod.apply(gradeIds);
        assertEquals(3, counts.size());

        for (int idx = 0; idx < gradeIds.length; idx++) {
            EntityCountDto countDto = counts.get(idx);
            assertEquals(ControllerConstants.ENTITY_GRADE, countDto.getEntity());
            assertEquals(gradeIds[idx], countDto.getEntityId().longValue());
            assertEquals(expectedCount[idx], countDto.getCount());
        }
    }

    @Test
    public void testBulkCountFixId() throws Exception {
        int modelId = 1;
        long[] gradeIds = {1, 2, 3};
        long[] expectedCount = {4, 2, 0};

        PersQaServiceMockFactory.mockModelGradeWithFixId(gradeClient, gradeIds[1], UID, modelId);

        prepareTestTree();

        List<EntityCountDto> counts = gradeMvc.getCommentsCountBulkUid(UID, gradeIds).getData();
        assertEquals(3, counts.size());

        for (int idx = 0; idx < gradeIds.length; idx++) {
            EntityCountDto countDto = counts.get(idx);
            assertEquals(ControllerConstants.ENTITY_GRADE, countDto.getEntity());
            assertEquals(gradeIds[idx], countDto.getEntityId().longValue());
            assertEquals(expectedCount[idx], countDto.getCount());
        }

        // check data is cached in DB correctly
        assertEquals(List.of(Pair.of(gradeIds[0], null),
                Pair.of(gradeIds[1], PersQaServiceMockFactory.getFixIdById(gradeIds[1])),
                Pair.of(gradeIds[2], null)),

            jdbcTemplate.query(
                "select id, fix_id from grade_fix_id where id = ANY(?) order by id",
                (rs, rowNum) -> Pair.of(rs.getLong("id"), DbUtil.getLong(rs, "fix_id")),
                gradeIds));

        // try again, check mapping is cached
        when(gradeClient.getGradesForComments(anyCollection())).thenThrow(RuntimeException.class);

        counts = gradeMvc.getCommentsCountBulkUid(UID, gradeIds).getData();
        assertEquals(3, counts.size());

        invalidateCache();

        // still ok
        counts = gradeMvc.getCommentsCountBulkUid(UID, gradeIds).getData();
        assertEquals(3, counts.size());

        // delete data - cache should hold data
        jdbcTemplate.update("delete from grade_fix_id where id>0");
        counts = gradeMvc.getCommentsCountBulkUid(UID, gradeIds).getData();
        assertEquals(3, counts.size());

        invalidateCache();

        // now no data in DB and cache - should fail
        gradeMvc.getCommentsCountBulkUidDto(UID, gradeIds, status().is5xxServerError());
    }

    public CommentDto get(CommentTreeDto dto, long commentId) {
        return get(dto.getComments(), commentId);
    }

    public CommentDto get(CommentDto dto, long commentId) {
        return get(dto.getComments(), commentId);
    }

    public CommentDto get(List<CommentDto> comments, long commentId) {
        return comments.stream()
            .filter(x -> x.getId() == commentId)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No such comment: " + commentId));
    }
}
