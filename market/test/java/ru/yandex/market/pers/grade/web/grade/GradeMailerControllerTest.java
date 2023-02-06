package ru.yandex.market.pers.grade.web.grade;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.client.dto.mailer.MailerModelGrade;
import ru.yandex.market.pers.grade.client.dto.mailer.MailerShopGrade;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.db.DbGradeVoteService;
import ru.yandex.market.pers.grade.core.model.core.ModReason;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.model.vote.GradeVoteKind;
import ru.yandex.market.pers.grade.core.moderation.Object4Moderation;
import ru.yandex.market.pers.grade.mock.mvc.GradeMailerMvcMocks;
import ru.yandex.market.pers.service.common.util.ExpFlagService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 14.04.2021
 */
public class GradeMailerControllerTest extends MockedPersGradeTest {

    public static final long MODEL_ID = 1231231414;
    public static final long SHOP_ID = 425289823;
    public static final long USER_ID = 92847398;

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private GradeMailerMvcMocks gradeMailerMvc;

    @Autowired
    private DbGradeAdminService gradeAdminService;

    @Autowired
    public DbGradeVoteService gradeVoteService;

    @Autowired
    private ExpFlagService expFlagService;

    @Before
    public void prepareGradeMailer() {
        pgJdbcTemplate.update(
            "insert into mod_rejection_reason(id, type, name, recomendation, active, suggest_correction) " +
                "values(?,1,?,?,1,1)",
            ModReason.RUDE.forModel(), "Recomendation", "text"
        );
        pgJdbcTemplate.update(
            "insert into mod_rejection_reason(id, type, name, recomendation, active, suggest_correction) " +
                "values(?,1,?,?,1,1)",
            ModReason.RUDE.forShop(), "Recomendation", "text"
        );
    }

    @Test
    public void testLoadModelGrade() {
        ModelGrade source = GradeCreator.constructModelGrade(MODEL_ID, USER_ID);
        source.setAverageGrade(2);
        long gradeId = gradeCreator.createGrade(source);

        // check that with non-existing grade returns nothing
        assertEquals(0, gradeMailerMvc.getModelGrade(gradeId - 1).size());
        assertEquals(0, gradeMailerMvc.getShopGrade(gradeId, null).size());

        // check actual grade
        List<MailerModelGrade> result = gradeMailerMvc.getModelGrade(gradeId);
        assertEquals(1, result.size());

        assertEquals(gradeId, result.get(0).getId());
        assertEquals(USER_ID, result.get(0).getUid());
        assertEquals(MODEL_ID, result.get(0).getModelId());
        assertEquals(ModState.APPROVED, result.get(0).getModState());
        assertEquals(GradeType.MODEL_GRADE, result.get(0).getType());

        assertEquals(source.getText(), result.get(0).getText());
        assertEquals(source.getPro(), result.get(0).getPro());
        assertEquals(source.getContra(), result.get(0).getContra());

        assertFalse(result.get(0).isAnonymous());
        assertEquals(2, result.get(0).getGradeValue().intValue());

        assertNull(result.get(0).getRecommendation());
        assertEquals(0, result.get(0).getSuggestCorrection());
    }

    @Test
    public void testLoadModelGradeRejected() {
        ModelGrade source = GradeCreator.constructModelGrade(MODEL_ID, USER_ID);
        source.setAverageGrade(2);
        long gradeId = gradeCreator.createGrade(source);

        // ban grade
        gradeAdminService.moderate(List.of(
            Object4Moderation.moderated(gradeId, ModState.REJECTED, ModReason.RUDE.forModel())
        ), USER_ID);

        // check actual grade
        List<MailerModelGrade> result = gradeMailerMvc.getModelGrade(gradeId);
        assertEquals(1, result.size());

        assertEquals(gradeId, result.get(0).getId());
        assertEquals(USER_ID, result.get(0).getUid());
        assertEquals(MODEL_ID, result.get(0).getModelId());

        assertNotNull(result.get(0).getRecommendation());
        assertEquals(1, result.get(0).getSuggestCorrection());
    }

    @Test
    public void testLoadShopGradePublic() {
        ShopGrade source = GradeCreator.constructShopGrade(SHOP_ID, USER_ID);
        source.setAverageGrade(2);
        long gradeId = gradeCreator.createGrade(source);

        // check that with non-existing grade returns nothing
        assertEquals(0, gradeMailerMvc.getShopGrade(gradeId - 1, null).size());
        assertEquals(0, gradeMailerMvc.getModelGrade(gradeId).size());

        // check actual grade
        List<MailerShopGrade> result = gradeMailerMvc.getShopGrade(gradeId, null);
        assertEquals(1, result.size());

        assertEquals(gradeId, result.get(0).getId());
        assertEquals(USER_ID, result.get(0).getUid());
        assertEquals(SHOP_ID, result.get(0).getShopId());
        assertEquals(ModState.APPROVED, result.get(0).getModState());
        assertEquals(GradeType.SHOP_GRADE, result.get(0).getType());

        assertEquals(source.getText(), result.get(0).getText());
        assertEquals(source.getPro(), result.get(0).getPro());
        assertEquals(source.getContra(), result.get(0).getContra());

        assertFalse(result.get(0).isAnonymous());
        assertEquals(2, result.get(0).getGradeValue().intValue());

        assertNull(result.get(0).getRecommendation());
        assertEquals(0, result.get(0).getSuggestCorrection());

        // check public parameter
        assertEquals(1, gradeMailerMvc.getShopGrade(gradeId, true).size());
        assertEquals(1, gradeMailerMvc.getShopGrade(gradeId, false).size());
    }

    @Test
    public void testLoadShopGradeRejected() {
        ShopGrade source = GradeCreator.constructShopGrade(SHOP_ID, USER_ID);
        source.setAverageGrade(2);
        long gradeId = gradeCreator.createGrade(source);

        // ban grade
        gradeAdminService.moderate(List.of(
            Object4Moderation.moderated(gradeId, ModState.REJECTED, ModReason.RUDE.forShop())
        ), USER_ID);

        // check public parameter
        assertEquals(0, gradeMailerMvc.getShopGrade(gradeId, true).size());
        assertEquals(1, gradeMailerMvc.getShopGrade(gradeId, null).size());

        // check actual grade
        List<MailerShopGrade> result = gradeMailerMvc.getShopGrade(gradeId, false);
        assertEquals(1, result.size());

        assertEquals(gradeId, result.get(0).getId());
        assertEquals(USER_ID, result.get(0).getUid());
        assertEquals(SHOP_ID, result.get(0).getShopId());

        assertNotNull(result.get(0).getRecommendation());
        assertEquals(1, result.get(0).getSuggestCorrection());
    }

    @Test
    public void testShopGradeExists() {
        long gradeId = gradeCreator.createShopGrade(USER_ID, SHOP_ID, 1);
        long gradeIdRejected = gradeCreator.createShopGrade(USER_ID, SHOP_ID + 1, 1);

        gradeAdminService.moderate(List.of(
            Object4Moderation.moderated(gradeIdRejected, ModState.REJECTED, ModReason.RUDE.forShop())
        ), USER_ID);

        assertTrue(gradeMailerMvc.checkShopGrade(USER_ID, SHOP_ID));
        assertTrue(gradeMailerMvc.checkShopGrade(USER_ID, SHOP_ID + 1));
        assertFalse(gradeMailerMvc.checkShopGrade(USER_ID, SHOP_ID + 2));
    }

    @Test
    public void testVoteExists() {
        long gradeId = gradeCreator.createShopGrade(USER_ID, SHOP_ID, 1);

        // create vote
        Long voteId = gradeVoteService.createVote(gradeId, USER_ID + 1, GradeVoteKind.agree, null);
        Long voteIdOld = gradeVoteService.createVote(gradeId, USER_ID + 2, GradeVoteKind.agree, null);

        gradeVoteService.removeVote(gradeId, USER_ID + 2, null);

        assertFalse(gradeMailerMvc.checkVoteGrade(voteId - 1));
        assertTrue(gradeMailerMvc.checkVoteGrade(voteId));
        assertFalse(gradeMailerMvc.checkVoteGrade(voteIdOld));
    }

}
