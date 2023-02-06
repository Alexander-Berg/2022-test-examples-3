package ru.yandex.market.pers.grade.ugc;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.client.dto.GradePager;
import ru.yandex.market.pers.grade.client.dto.grade.GradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.grade.ModelGradeResponseDto;
import ru.yandex.market.pers.grade.client.dto.grade.WhiteGradeResponseDto;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.db.DbGradeVoteService;
import ru.yandex.market.pers.grade.core.db.GradeCommentDao;
import ru.yandex.market.pers.grade.core.db.VendorGradeService;
import ru.yandex.market.pers.grade.core.db.model.VendorGradesRequest;
import ru.yandex.market.pers.grade.core.model.Comment;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.SecurityData;
import ru.yandex.market.pers.grade.core.model.vote.GradeVoteKind;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.grade.core.service.VerifiedGradeService;
import ru.yandex.market.pers.grade.web.grade.GradeCreationHelper;
import ru.yandex.market.pers.qa.client.dto.AuthorIdDto;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.CommentState;
import ru.yandex.market.pers.qa.client.model.UserType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.pers.qa.client.model.CommentState.DELETED;
import static ru.yandex.market.pers.qa.client.model.CommentState.NEW;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 28.02.2018
 */
public class MultifactorGradeServiceTest extends MockedPersGradeTest {
    // can't check grade votes since they are in mat.view and can't be changed in tests
    public static final int EXPECTED_VOTES_AGREE = 0;
    public static final int EXPECTED_VOTES_REJECT = 0;

    @Autowired
    private MultifactorGradeService multifactorGradeService;
    @Autowired
    private DbGradeService dbGradeService;
    @Autowired
    private DbGradeVoteService dbGradeVoteService;
    @Autowired
    private GradeModeratorModificationProxy moderatorModificationProxy;
    @Autowired
    private GradeCommentDao gradeCommentDao;
    @Autowired
    private VerifiedGradeService verifiedGradeService;
    @Autowired
    private VendorGradeService vendorGradeService;

    private void prepareMatView() {
        pgJdbcTemplate.update("refresh materialized view mv_vendor_grade");
    }

    @Test
    public void getVendorGradesNotExistingVendor() {
        assertTrue(multifactorGradeService.getVendorGrades(notExistingVendorRequest()).getData().isEmpty());
    }

    @Test
    public void getVendorGradesExistingVendor() {
        long vendorId = 123L;
        ModelGrade expected = createModelGrade(vendorId);
        createVotesForGrade(expected.getId());

        prepareMatView();
        List<GradeResponseDto> actual = multifactorGradeService.getVendorGrades(
            vendorRequest(vendorId, expected.getResourceId())).getData();
        assertEquals(1, actual.size());
        assertEquals(1, vendorGradeService.getVendorGradesCount(vendorId));
        assertEquals(0, vendorGradeService.getVendorGradesCount(vendorId + 1));
        assertGradesEquals(expected, (ModelGradeResponseDto) actual.get(0));
        verifyVotesForGrade(EXPECTED_VOTES_AGREE, EXPECTED_VOTES_REJECT, actual.get(0));
    }

    @Test
    public void getVendorGradesVerified() {
        long vendorId = 123L;
        ModelGrade expected = createModelGrade(vendorId);

        prepareMatView();
        List<GradeResponseDto> actual = multifactorGradeService.getVendorGrades(
            vendorRequest(vendorId, expected.getResourceId())).getData();
        assertEquals(false, ((WhiteGradeResponseDto) actual.get(0)).getVerified());
    }

    @Test
    public void getVendorGradesCpa() {
        long vendorId = 123L;
        ModelGrade expected = createModelGrade(vendorId, true);

        prepareMatView();
        List<GradeResponseDto> actual = multifactorGradeService.getVendorGrades(
            vendorRequest(vendorId, expected.getResourceId(), true)).getData();
        assertEquals(true, ((WhiteGradeResponseDto) actual.get(0)).getCpa());
    }

    @Test
    public void getVendorGradesByDate() {
        long vendorId = 123L;
        ModelGrade expected = createModelGrade(vendorId, true);

        prepareMatView();
        VendorGradesRequest request = vendorRequest(vendorId, null);
        // should find grades, created today
        request.setDateFrom(new Date());
        request.setDateTo(new Date());

        List<GradeResponseDto> actual = multifactorGradeService.getVendorGrades(
            request).getData();
        assertEquals(1, actual.size());
        assertEquals(true, ((WhiteGradeResponseDto) actual.get(0)).getCpa());

        //call with wrong dates
        request.setDateFrom(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));
        request.setDateTo(new Date(System.currentTimeMillis() + 2 * TimeUnit.DAYS.toMillis(1)));

        List<GradeResponseDto> wrong = multifactorGradeService.getVendorGrades(
            request).getData();
        assertEquals(0, wrong.size());
    }

    @Test
    public void getVendorGradesWithReply() {
        long modelId = 45245;
        long vendorId = 123L;
        long userId = 34134134;

        ModelGrade[] expected = {
            createModelGrade(modelId, vendorId, userId, "text", true),
            createModelGrade(modelId, vendorId, userId, "updated", true),
            createModelGrade(modelId + 1, vendorId, userId, "deleted vendor comment", true),
            createModelGrade(modelId + 2, vendorId, userId, "vendor comment direct", true),
            createModelGrade(modelId + 3, vendorId, userId, "with user comment", true),
        };

        // ensure 0-1 grades are linked
        assertEquals(expected[0].getId(), expected[0].getFixId());
        assertEquals(expected[0].getFixId(), expected[1].getFixId());

        // grade[0] is outdated and grade[2] has only deleted comment, grade[4] has userReply
        gradeCommentDao.save(Comment.fromDto(buildComment(123, expected[0].getId(), NEW, UserType.VENDOR)));
        gradeCommentDao.save(Comment.fromDto(buildComment(124, expected[2].getId(), DELETED, UserType.VENDOR)));
        gradeCommentDao.save(Comment.fromDto(buildComment(125, expected[3].getId(), NEW, UserType.VENDOR)));
        gradeCommentDao.save(Comment.fromDto(buildComment(126, expected[4].getId(), NEW, UserType.UID)));

        VendorGradesRequest request = new VendorGradesRequest(vendorId, null, null, null, true,
            null, false, false, 1, 10,
            new VendorGradesRequest.Sort(VendorGradesRequest.SortType.DATE, false));

        prepareMatView();
        List<GradeResponseDto> actual = multifactorGradeService.getVendorGrades(request).getData();
        assertEquals(2, actual.size());
        assertEquals(List.of(expected[1].getId(), expected[3].getId()),
            actual.stream().map(GradeResponseDto::getId).collect(Collectors.toList()));

        VendorGradesRequest requestNoReply = new VendorGradesRequest(vendorId, null, null, null, false,
            null, false, false, 1, 10,
            new VendorGradesRequest.Sort(VendorGradesRequest.SortType.DATE, false));


        List<GradeResponseDto> responseNoReply = multifactorGradeService.getVendorGrades(requestNoReply).getData();
        assertEquals(2, responseNoReply.size());
        assertEquals(List.of(expected[2].getId(), expected[4].getId()),
            responseNoReply.stream().map(GradeResponseDto::getId).collect(Collectors.toList()));
    }

    @Test
    public void getVendorGradesGoodPager() {
        long vendorId = 123L;
        ModelGrade expected = createModelGrade(vendorId);

        prepareMatView();
        GradePager<GradeResponseDto> actual = multifactorGradeService.getVendorGrades(
            vendorRequest(vendorId, expected.getResourceId()));
        assertEquals(1, actual.getPager().getCount());
    }

    private void assertGradesEquals(ModelGrade expected, ModelGradeResponseDto actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getModelId(), actual.getModelId());
        assertEquals(expected.getCatId(), actual.getCategoryId());
        assertEquals(expected.getPro(), actual.getPro());
        assertEquals(expected.getContra(), actual.getContra());
        assertEquals(expected.getText(), actual.getText());
        assertEquals(expected.getUsageTime(), actual.getUsageTime());
        assertEquals(expected.getAnonymous(), actual.getAnonymity());
        assertEquals(expected.getRecommend(), actual.getRecommend());
        assertEquals(expected.getRegionId(), actual.getRegion().getId());
        assertEquals(expected.getAverageGrade().intValue(), actual.getAverageGrade().intValue());
        assertEquals(expected.getSource(), actual.getSource());
        assertEquals(expected.getModState(), actual.getModState());
        assertEquals(expected.getModReason(), actual.getModerationReason());
        assertEquals(expected.getType(), actual.getType());
    }

    private VendorGradesRequest notExistingVendorRequest() {
        return vendorRequest(-11111L, null);
    }

    private VendorGradesRequest vendorRequest(long vendorId, Long modelId) {
        return vendorRequest(vendorId, modelId, null);
    }

    private VendorGradesRequest vendorRequest(long vendorId, Long modelId, Boolean cpa) {
        return new VendorGradesRequest(vendorId, null, null, null, null,
            modelId, false, cpa, 1, 10,
            new VendorGradesRequest.Sort(VendorGradesRequest.SortType.DATE, false));
    }

    private ModelGrade createModelGrade(long vendorId) {
        return createModelGrade(vendorId, false);
    }

    private ModelGrade createModelGrade(long vendorId, boolean cpa) {
        int modelId = 1;
        int userId = 1;
        return createModelGrade(modelId, vendorId, userId, null, cpa);
    }

    private ModelGrade createModelGrade(long modelId, long vendorId, long uid, String text, boolean cpa) {
        ModelGrade result = GradeCreationHelper.constructModelGrade(modelId, uid, vendorId);

        if (text != null) {
            result.setText(text);
        }

        GradeResponseDto gradeResponseDto = multifactorGradeService.createGrade(result, getTestSecurityData());
        moderatorModificationProxy.moderateGradeReplies(Collections.singletonList(gradeResponseDto.getId()),
            Collections.emptyList(), 1L, ModState.APPROVED);

        verifiedGradeService.setCpaInDB(List.of(gradeResponseDto.getId()), cpa);
        return (ModelGrade) dbGradeService.getGrade(gradeResponseDto.getId());
    }

    private void verifyVotesForGrade(int karmaVotesAgree, int karmaVotesReject, GradeResponseDto gradeResponseDto) {
        assertEquals(karmaVotesAgree, gradeResponseDto.getVotes().getPositive());
        assertEquals(karmaVotesReject, gradeResponseDto.getVotes().getNegative());
    }

    private void createVotesForGrade(Long gradeId) {
        dbGradeVoteService.createVote(gradeId, 123L, GradeVoteKind.agree, "12312312");
        dbGradeVoteService.createVote(gradeId, 124L, GradeVoteKind.reject, "12312312");
    }

    private SecurityData getTestSecurityData() {
        return GradeCreator.defaultSecurityData();
    }

    private CommentDto buildComment(long id, long gradeId, CommentState state, UserType author) {
        CommentDto result = new CommentDto();
        result.setId(id);
        result.setEntityId(gradeId);
        result.setProjectId(CommentProject.GRADE.getId());
        result.setStateEnum(state);
        result.setUser(new AuthorIdDto(UserType.UID, "123"));
        result.setAuthor(new AuthorIdDto(author, "123"));
        result.setCreateTime(new Date());
        result.setUpdateTime(new Date());
        return result;
    }

}
