package ru.yandex.market.pers.tms.export.abo;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.moderation.Object4Moderation;
import ru.yandex.market.pers.tms.MockedPersTmsTest;

import static java.util.Collections.singletonList;

/**
 * @author grigor-vlad
 * 16.03.2022
 */
public class ModerationExportProcessorTest extends MockedPersTmsTest {
    private static final Long MODERATOR_ID_1 = 1L;
    private static final Long MODERATOR_ID_2 = 2L;
    private static final Long MODERATOR_ID_3 = 3L;

    private static final Long STAFF_UID_1 = 321L;
    private static final Long STAFF_UID_2 = 322L;

    @Autowired
    private DbGradeAdminService gradeAdminService;

    @Autowired
    private GradeCreator gradeCreator;

    @Test
    public void testModBillingSqlRequest() {
        //Init approved by some moderator grades
        //Approved == Ready to publish
        createModelGradeAndApproveBy(MODERATOR_ID_1);
        createModelGradeAndApproveBy(MODERATOR_ID_2);
        createModelGradeAndApproveBy(MODERATOR_ID_3);

        //Initialize id mapping
        //We have that moderator1 has record with staffUid1 and moderator2 & moderator3 share staffUid2
        addModeratorIdMapping(MODERATOR_ID_1, STAFF_UID_1);
        addModeratorIdMapping(MODERATOR_ID_2, STAFF_UID_2);
        addModeratorIdMapping(MODERATOR_ID_3, STAFF_UID_2);

        //Call sql request forming moderation-billing. Watch only on total count of moderation
        List<ModBillingResult> modBillingResults = pgJdbcTemplate.query(
            ModerationExportProcessor.formModBillingSqlRequest(),
            (rs, rowNum) -> new ModBillingResult(rs.getLong("MODERATOR_ID"), rs.getLong("TOTAL"))
        );

        Assert.assertEquals(
            Set.of(new ModBillingResult(321L, 1L), new ModBillingResult(322L, 2L)),
            new HashSet<>(modBillingResults)
        );
    }

    private void createModelGradeAndApproveBy(Long moderatorId) {
        long gradeId = gradeCreator.createGrade(GradeCreator.constructModelGradeRnd());
        Object4Moderation object = Object4Moderation.moderated(gradeId, ModState.READY_TO_PUBLISH);
        gradeAdminService.moderate(singletonList(object), moderatorId);
    }

    private void addModeratorIdMapping(Long passportUid, Long staffUid) {
        pgJdbcTemplate.update(
            "insert into moderator_id_mapping (passport_uid, staff_uid) values(?, ?)",
            passportUid, staffUid
        );
    }


    public static class ModBillingResult {

        private long moderatorId;
        private long totalModeration;

        public ModBillingResult(long moderatorId, long totalModeration) {
            this.moderatorId = moderatorId;
            this.totalModeration = totalModeration;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ModBillingResult that = (ModBillingResult) o;
            return moderatorId == that.moderatorId && totalModeration == that.totalModeration;
        }

        @Override
        public int hashCode() {
            return Objects.hash(moderatorId, totalModeration);
        }
    }
}
