package ru.yandex.market.pers.qa.controller.api.external;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.GradeClient;
import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.mock.mvc.VendorMvcMocks;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.pers.qa.service.CommentService;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.model.CommentProject.GRADE;
import static ru.yandex.market.pers.qa.client.model.CommentProject.QA;
import static ru.yandex.market.pers.qa.client.model.CommentState.DELETED;
import static ru.yandex.market.pers.qa.client.model.CommentState.NEW;
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
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkState;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkSubComments;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkText;
import static ru.yandex.market.pers.qa.controller.api.comment.CommentAssert.checkUser;
import static ru.yandex.market.pers.qa.utils.CommentUtils.PARAM_GRADE_TYPE;
import static ru.yandex.market.pers.qa.utils.CommentUtils.PARAM_GRADE_TYPE_MODEL;
import static ru.yandex.market.pers.qa.utils.CommentUtils.PARAM_VENDOR_ID;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 20.11.2020
 */
public class VendorApiControllerCommentTest extends QAControllerTest {
    private static final long BRAND_ID = 234293874923L;

    @Autowired
    private CommentService commentService;
    @Autowired
    private VendorMvcMocks vendorMvc;
    @Autowired
    private GradeClient gradeClient;

    @ParameterizedTest
    @ValueSource(strings = {"QA", "GRADE"})
    public void testVendorCommentSimple(CommentProject project) throws Exception {
        long[] rootIds = new long[]{createRootEntity(project), createRootEntity(project), createRootEntity(project)};
        Long parentId = null;
        Long noParent = null;

        // ensure to check with mocked fixId for one of grades
        if(project == GRADE) {
            PersQaServiceMockFactory.mockModelGradeWithFixId(gradeClient, rootIds[1], UID, 111);
        }

        //    root1
        //    - c1(v)
        //     |- c3(u)
        //     |- c4(u1)
        //    - c2(u)
        //
        //    root2
        //    - c5(v)
        //     |- c6(u)
        //       |- c7(v)
        //       |- c8(u2)
        //       |- c9(v) - premod
        // viewed backwards
        long[] ids = {
            0, // to start with 1
            //root1
            parentId = vendorMvc.createComment(project, BRAND_ID, UID, noParent, rootIds[0], "c1 VendorComment"),
            commentService.createComment(project, UID, "c2 User comment (not an answer)", rootIds[0], noParent),
            commentService.createComment(project, UID, "c3 User comment to vendor", rootIds[0], parentId),
            commentService.createComment(project, UID + 1, "c4 Other user answer to vendor", rootIds[0], parentId),
            // root2
            parentId = vendorMvc.createComment(project, BRAND_ID, UID, noParent, rootIds[1], "c5 VendorComment"),
            parentId = commentService.createComment(project, UID, "c6 User answer", rootIds[1], parentId),
            vendorMvc.createComment(project, BRAND_ID, UID, parentId, rootIds[1], "c7 Vendor response"),
            commentService.createComment(project, UID + 1, "c8 Other user response", rootIds[1], parentId),
            0
        };

        // and single premod comment by vendor in root2
        expFlagService.setFlag(CommentService.FLAG_STOP_COMMENT_PUBLICATION_VENDOR, true);
        ids[9] = vendorMvc.createComment(project, BRAND_ID, UID, parentId, rootIds[1], "c9 Vendor response");

        assertForest(vendorMvc.getComments(project, BRAND_ID, UID, rootIds),
            checkFlatTree(project, rootIds[0],
                checkComment(ids[4],
                    checkText("c4 Other user answer to vendor"),
                    checkParent(ids[1]),
                    checkUser(UserInfo.uid(UID+1)),
                    checkAuthor(UserInfo.uid(UID+1))),
                checkComment(ids[3],
                    checkText("c3 User comment to vendor"),
                    checkParent(ids[1]),
                    checkUser(UserInfo.uid(UID)),
                    checkAuthor(UserInfo.uid(UID))),
                checkComment(ids[2],
                    checkText("c2 User comment (not an answer)"),
                    checkNoParent(),
                    checkUser(UserInfo.uid(UID)),
                    checkAuthor(UserInfo.uid(UID)),
                    checkParams(project == QA ? Map.of() : Map.of(
                        PARAM_GRADE_TYPE, PARAM_GRADE_TYPE_MODEL
                    ))),
                checkComment(ids[1],
                    checkText("c1 VendorComment"),
                    checkNoParent(),
                    checkUser(UserInfo.uid(UID)),
                    checkAuthor(new UserInfo(UserType.VENDOR, BRAND_ID)),
                    checkParamsHas(Map.of(PARAM_VENDOR_ID, String.valueOf(BRAND_ID))))
            ),
            checkFlatTree(project, rootIds[1],
                checkComment(ids[9],
                    checkText("c9 Vendor response"),
                    checkParent(ids[6]),
                    checkAuthor(new UserInfo(UserType.VENDOR, BRAND_ID)),
                    checkCanDelete(true),
                    checkPublished(true),
                    checkSubComments()),
                checkComment(ids[8],
                    checkText("c8 Other user response"),
                    checkParent(ids[6]),
                    checkAuthor(UserInfo.uid(UID+1)),
                    checkSubComments()),
                checkComment(ids[7],
                    checkText("c7 Vendor response"),
                    checkParent(ids[6]),
                    checkAuthor(new UserInfo(UserType.VENDOR, BRAND_ID)),
                    checkSubComments()),
                checkComment(ids[6],
                    checkText("c6 User answer"),
                    checkParent(ids[5]),
                    checkAuthor(UserInfo.uid(UID)),
                    checkSubComments()),
                checkComment(ids[5],
                    checkText("c5 VendorComment"),
                    checkNoParent(),
                    checkAuthor(new UserInfo(UserType.VENDOR, BRAND_ID)),
                    checkSubComments())
            ),
            checkFlatTree(project, rootIds[2])
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"QA", "GRADE"})
    public void testVendorCommentEditDelete(CommentProject project) throws Exception {
        long rootId = createRootEntity(project);
        Long parentId = null;
        Long noParent = null;
        //    root
        //    - c1(v)
        //     |- c3(u)
        //     |- c4(u1)
        //    - c2(u)
        long[] ids = {
            0, // to start with 1
            parentId = vendorMvc.createComment(project, BRAND_ID, UID, noParent, rootId, "c1 VendorComment"),
            commentService.createComment(project, UID, "c2 User comment (not an answer)", rootId, noParent),
            commentService.createComment(project, UID, "c3 User comment to vendor", rootId, parentId),
            commentService.createComment(project, UID + 1, "c4 Other user answer to vendor", rootId, parentId),
        };

        vendorMvc.editComment(ids[1], BRAND_ID, UID, "Vendor changed text", status().is2xxSuccessful());
        vendorMvc.editComment(ids[2], BRAND_ID, UID, "Vendor can't change text", status().is4xxClientError());

        assertForest(vendorMvc.getComments(project, BRAND_ID, UID, new long[]{rootId}),
            checkFlatTree(project, rootId,
                checkComment(ids[4], checkText("c4 Other user answer to vendor"), checkCanDelete(false)),
                checkComment(ids[3], checkText("c3 User comment to vendor"), checkCanDelete(false)),
                checkComment(ids[2], checkText("c2 User comment (not an answer)"), checkCanDelete(false)),
                checkComment(ids[1], checkText("Vendor changed text"), checkCanDelete(true), checkState(NEW))
                )
        );

        vendorMvc.deleteComment(ids[1], BRAND_ID, UID, status().is2xxSuccessful());
        vendorMvc.deleteComment(ids[2], BRAND_ID, UID, status().is4xxClientError());

        assertForest(vendorMvc.getComments(project, BRAND_ID, UID, new long[]{rootId}),
            checkFlatTree(project, rootId,
                checkComment(ids[4], checkText("c4 Other user answer to vendor"), checkState(NEW)),
                checkComment(ids[3], checkText("c3 User comment to vendor"), checkState(NEW)),
                checkComment(ids[2], checkText("c2 User comment (not an answer)"), checkState(NEW)),
                checkComment(ids[1], checkText("Vendor changed text"), checkState(DELETED))
                )
        );
    }

    private long createRootEntity(CommentProject commentProject) throws Exception {
        if (commentProject == QA) {
            return createAnswer(createQuestion());
        }

        // simplest way to generate unique id
        return createQuestion();
    }
}
