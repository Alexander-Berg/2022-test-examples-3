package ru.yandex.market.pers.grade.core.ugc;

import java.time.Instant;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.pers.grade.core.ugc.model.Complaint;
import ru.yandex.market.pers.grade.core.ugc.model.ComplaintState;
import ru.yandex.market.pers.grade.core.ugc.model.ComplaintType;
import ru.yandex.market.pers.grade.core.ugc.model.GradeComplaintReason;

import static java.lang.Long.valueOf;

/**
 * @author dinyat
 *         27/03/2017
 */
public class ComplaintServiceTest extends MockedTest {

    private final Complaint uidCommentComplaint = new Complaint(12L, "child-0009-12345", 1L, null, 1, "жалоба", ComplaintType.COMMENT_COMPLAINT, ComplaintState.NEW);
    private final Complaint uidCommentComplaint2 = new Complaint(13L, "child-0009-12346", 3L, null, 1, "жалоба", ComplaintType.COMMENT_COMPLAINT, ComplaintState.NEW);
    private final Complaint yandexUidCommentComplaint = new Complaint(12L, "child-0009-12345", null, "yandex-uid", 1, "жалоба", ComplaintType.COMMENT_COMPLAINT, ComplaintState.APPROVED_BY_MODERATOR);
    private final Complaint uidGradeComplaint = new Complaint(12L, "12345", 1L, null, 1, "жалоба", ComplaintType.GRADE_COMPLAINT, ComplaintState.REJECTED_BY_MODERATOR);
    private final Complaint yandexUidGradeComplaint = new Complaint(12L, "12345", null, "yandex-uid", 1, "жалоба", ComplaintType.GRADE_COMPLAINT, ComplaintState.NEW);
    private final long moderatorId = -1;

    @Autowired
    private ComplaintService complaintService;

    @Test
    public void testUpdateState() throws Exception {
        Long id = complaintService.save(uidCommentComplaint);

        complaintService.updateState(id, ComplaintState.REJECTED_BY_MODERATOR, moderatorId);

        Assert.assertEquals(ComplaintState.REJECTED_BY_MODERATOR, complaintService.getComplaint(id).getComplaintState());
    }

    @Test
    public void getCommentReasonDictionary() throws Exception {
        List<GradeComplaintReason> result = complaintService.getReasonDictionary(ComplaintType.COMMENT_COMPLAINT);

        Assert.assertEquals(3, result.size());

        GradeComplaintReason other = result.get(2);
        Assert.assertEquals(8, other.getId());
        Assert.assertEquals("Другое", other.getName());
        Assert.assertEquals(ComplaintType.COMMENT_COMPLAINT, other.getType());
    }

    @Test
    public void getGradeReasonDictionary() throws Exception {
        List<GradeComplaintReason> result = complaintService.getReasonDictionary(ComplaintType.GRADE_COMPLAINT);

        Assert.assertEquals(4, result.size());

        GradeComplaintReason other = result.get(0);
        Assert.assertEquals(0, other.getId());
        Assert.assertEquals("Спам, реклама", other.getName());
        Assert.assertEquals(ComplaintType.GRADE_COMPLAINT, other.getType());
    }

    @Test
    public void testLeaveCommentComplaint() throws Exception {
        Long result = complaintService.save(uidCommentComplaint);

        Assert.assertNotNull(result);
    }

    @Test
    public void testLeaveGradeComplaint() throws Exception {
        Long result = complaintService.save(uidGradeComplaint);

        Assert.assertNotNull(result);
    }

    @Test
    public void testGetCommentComplaintByComplaintId() throws Exception {
        Long id = complaintService.save(uidCommentComplaint);

        Complaint complaint = complaintService.getComplaint(id);

        assertComplaint(id, uidCommentComplaint, complaint);
    }

    @Test
    public void testGetGradeComplaintByComplaintId() throws Exception {
        Long id = complaintService.save(uidGradeComplaint);

        Complaint complaint = complaintService.getComplaint(id);

        assertComplaint(id, uidGradeComplaint, complaint);
    }

    @Test
    public void testGetCommentComplaintByUserUid() throws Exception {
        Long id = complaintService.save(uidCommentComplaint);

        Complaint complaint = complaintService.getUserComplaints(uidCommentComplaint.getAuthorUid()).get(0);

        assertComplaint(id, uidCommentComplaint, complaint);
    }

    @Test
    public void testGetCommentComplaintByYandexUid() throws Exception {
        Long id = complaintService.save(yandexUidCommentComplaint);

        Complaint complaint = complaintService.getUserComplaints(yandexUidCommentComplaint.getYandexUid()).get(0);

        assertComplaint(id, yandexUidCommentComplaint, complaint);
    }

    @Test
    public void testGetGradeComplaintByUserUid() throws Exception {
        Long id = complaintService.save(uidGradeComplaint);

        Complaint complaint = complaintService.getUserComplaints(uidGradeComplaint.getAuthorUid()).get(0);

        assertComplaint(id, uidGradeComplaint, complaint);
    }

    @Test
    public void testGetGradeComplaintByYandexUid() throws Exception {
        Long id = complaintService.save(yandexUidGradeComplaint);

        Complaint complaint = complaintService.getUserComplaints(yandexUidGradeComplaint.getYandexUid()).get(0);

        assertComplaint(id, yandexUidGradeComplaint, complaint);
    }

    @Test
    public void testGetComplaintsByTypeAndState() throws Exception {
        List<Complaint> initComplaints = complaintService.getComplaintsByTypeAndState(ComplaintType.COMMENT_COMPLAINT, ComplaintState.NEW, 10);

        insertComplaints();

        List<Complaint> complaints = complaintService.getComplaintsByTypeAndState(ComplaintType.COMMENT_COMPLAINT, ComplaintState.NEW, 10);

        Assert.assertEquals(initComplaints.size() + 1, complaints.size());
        Assert.assertTrue(complaints.contains(uidCommentComplaint));

    }

    @Test
    public void testCountComplaintsByTypeAndState() throws Exception {
        long initResult = complaintService.countComplaintsByTypeAndState(ComplaintType.COMMENT_COMPLAINT, ComplaintState.NEW);

        insertComplaints();

        long result = complaintService.countComplaintsByTypeAndState(ComplaintType.COMMENT_COMPLAINT, ComplaintState.NEW);

        Assert.assertEquals(initResult + 1, result);
    }

    @Test
    public void testRemoveHardUserComplaints() {
        Long id = complaintService.save(uidCommentComplaint);
        Long authorUid = uidCommentComplaint.getAuthorUid();

        complaintService.removeActiveUserComplaints(authorUid);
        Complaint complaint = complaintService.getComplaint(id);
        Assert.assertEquals(ComplaintState.DELETED, complaint.getComplaintState());

        Instant now = Instant.now();
        uidGradeComplaint.setComplaintState(ComplaintState.NEW);
        Long id2 = complaintService.save(uidGradeComplaint);

        complaintService.removeHardUserComplaints(authorUid, now);
        Complaint deletedComplaint = complaintService.getComplaint(id);
        Assert.assertNull(deletedComplaint);

        Complaint notDeletedComplaint = complaintService.getComplaint(id2);
        Assert.assertNotNull(notDeletedComplaint);
        Assert.assertEquals(ComplaintState.NEW, notDeletedComplaint.getComplaintState());
    }

    private void assertComplaint(Long expectedId, Complaint expected, Complaint actual) {
        Assert.assertEquals(expectedId, valueOf(actual.getId()));
        assertComplaint(expected, actual);
    }

    private void assertComplaint(Complaint expected, Complaint actual) {
        Assert.assertEquals(expected.getReason(), actual.getReason());
        Assert.assertEquals(expected.getSourceId(), actual.getSourceId());
        Assert.assertEquals(expected.getText(), actual.getText());
        Assert.assertEquals(expected.getAuthorUid(), actual.getAuthorUid());
        Assert.assertEquals(expected.getYandexUid(), actual.getYandexUid());
        Assert.assertEquals(expected.getComplaintType(), actual.getComplaintType());
        Assert.assertEquals(expected.getComplaintState(), actual.getComplaintState());
    }

    private void insertComplaints() {
        complaintService.saveAndSetId(uidCommentComplaint);
        complaintService.saveAndSetId(yandexUidCommentComplaint);
        complaintService.saveAndSetId(uidGradeComplaint);
        complaintService.saveAndSetId(yandexUidGradeComplaint);
    }

}
