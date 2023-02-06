package ru.yandex.market.pers.grade.core.db;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.grade.client.model.Delivery;
import ru.yandex.market.pers.grade.client.model.GradeState;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.client.model.UsageTime;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.model.core.VerifiedType;
import ru.yandex.market.pers.grade.core.model.vote.GradeVoteKind;
import ru.yandex.market.pers.grade.core.service.VerifiedGradeService;
import ru.yandex.market.pers.grade.core.ugc.FactorService;
import ru.yandex.market.pers.grade.core.ugc.PhotoService;
import ru.yandex.market.pers.grade.core.ugc.model.GradeFactor;
import ru.yandex.market.pers.grade.core.ugc.model.GradeFactorValue;
import ru.yandex.market.pers.grade.core.ugc.model.Photo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 27.05.2021
 */
public class GradePgMigrationTest extends MockedTest {
    public static final long MODEL_ID = 3413414;
    public static final long SHOP_ID = 891374;
    public static final long UID = 9245245;
    @Autowired
    private GradeCreator gradeCreator;

    @Qualifier("pgJdbcTemplate")
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Autowired
    private MoveGradeService moveGradeService;

    @Autowired
    private DbGradeAdminService gradeAdminService;

    @Autowired
    private VerifiedGradeService verifiedGradeService;

    @Autowired
    private FactorService factorService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    public DbGradeVoteService gradeVoteService;

    @Before
    public void initExp() {
        // check disabled by default

        // check later for flags disabling
//        assertFalse(expFlagService.isGradePgSaveSecEnabled());
    }

    @Test
    public void testGradePgCreationDisabledByDefault() {

        long gradeId = gradeCreator.createModelGrade(MODEL_ID, UID);
        assertEquals(1, readGradePg().size());

        List<Photo> photos = photoService.getPhotosByGrade(gradeId);
        assertEquals(1, photos.size());

        List<GradeFactorValue> factors = factorService.getFactorValuesByGrade(gradeId);
        assertEquals(1, factors.size());

        assertEquals(1, pgJdbcTemplate.queryForObject(
            "select count(*) from security_data", Long.class).longValue());

        assertEquals(0, pgJdbcTemplate.queryForObject(
            "select count(*) from grade_vote", Long.class).longValue());
    }

    @Test
    public void testModelGradePgCreation() {
        ModelGrade gradeDefinition = GradeCreator.constructModelGrade(MODEL_ID, UID)
            .fillCommonModelGradeCreationFields(UsageTime.MORE_THAN_A_YEAR)
            .fillReportModel(GradeCreator.mockReportModel(MODEL_ID, "modelname", null, null, null));
        gradeDefinition.setText("text");
        gradeDefinition.setPro("pro");
        gradeDefinition.setContra("contra");

        gradeDefinition.setPhotos(List.of(
            Photo.buildForTest("group", "image1", ModState.UNMODERATED),
            Photo.buildForTest("group", "image2", ModState.UNMODERATED)
        ));
        gradeDefinition.setGradeFactorValues(List.of(
            new GradeFactorValue(2, null, null, 4),
            new GradeFactorValue(3, null, null, 3)
        ));

        long gradeId = gradeCreator.createGrade(gradeDefinition);
        assertEquals(1, readGradePg().size());

        ModelGrade grade = (ModelGrade) readGradePg().get(0);
        assertEquals(gradeId, grade.getId().longValue());
        assertEquals(UID, grade.getAuthorUid().longValue());
        assertEquals(GradeType.MODEL_GRADE, grade.getType());
        assertEquals(MODEL_ID, grade.getResourceId().longValue());
        assertEquals(UsageTime.MORE_THAN_A_YEAR, grade.getUsageTime());
        assertEquals("modelname", grade.getName());
        assertEquals(GradeState.LAST, grade.getState());
        assertEquals(ModState.APPROVED, grade.getModState());
        assertEquals("text", grade.getText());
        assertEquals("pro", grade.getPro());
        assertEquals("contra", grade.getContra());

        assertEquals(GradeCreator.DEFAULT_YANDEX_UID, readPgGradeYandexuid(gradeId));

        List<Photo> photos = photoService.getPhotosByGrade(gradeId);
        assertEquals(2, photos.size());
        photos.sort(Comparator.comparing(Photo::getImageName));
        assertEquals(gradeDefinition.getPhotos().get(0).getVal(), photos.get(0).getVal());
        assertEquals(gradeDefinition.getPhotos().get(1).getVal(), photos.get(1).getVal());

        List<GradeFactorValue> factors = factorService.getFactorValuesByGrade(gradeId);
        assertEquals(2, factors.size());
        factors.sort(Comparator.comparing(GradeFactor::getFactorId));
        assertEquals(2, factors.get(0).getFactorId());
        assertEquals(4, factors.get(0).getValue());
        assertEquals(3, factors.get(1).getFactorId());
        assertEquals(3, factors.get(1).getValue());

        assertEquals(1, pgJdbcTemplate.queryForObject(
            "select count(*) from security_data", Long.class).longValue());

        assertEquals(0, pgJdbcTemplate.queryForObject(
            "select count(*) from grade_vote", Long.class).longValue());

        gradeVoteService.createVote(gradeId, UID, GradeVoteKind.agree, null);
        gradeVoteService.createVote(gradeId, null, GradeVoteKind.agree, "yuid");

        assertEquals(2, pgJdbcTemplate.queryForObject(
            "select count(*) from grade_vote", Long.class).longValue());
    }

    @Test
    public void testModelGradePgCreationFactorUpdate() {
        ModelGrade gradeDefinition = GradeCreator.constructModelGrade(MODEL_ID, UID);
        gradeDefinition.setGradeFactorValues(List.of());
        long gradeId = gradeCreator.createGrade(gradeDefinition);
        assertEquals(1, readGradePg().size());

        List<GradeFactorValue> factors = factorService.getFactorValuesByGrade(gradeId);
        assertEquals(0, factors.size());

        gradeDefinition.setGradeFactorValues(List.of(
            new GradeFactorValue(2, null, null, 4),
            new GradeFactorValue(3, null, null, 3)
        ));

        // only factors changed
        long gradeIdUpdated = gradeCreator.createGrade(gradeDefinition);
        assertEquals(gradeIdUpdated, gradeId);

        factors = factorService.getFactorValuesByGrade(gradeId);
        assertEquals(2, factors.size());
        factors.sort(Comparator.comparing(GradeFactor::getFactorId));
        assertEquals(2, factors.get(0).getFactorId());
        assertEquals(4, factors.get(0).getValue());
        assertEquals(3, factors.get(1).getFactorId());
        assertEquals(3, factors.get(1).getValue());

        gradeDefinition.setGradeFactorValues(List.of(
            new GradeFactorValue(1, null, null, 4),
            new GradeFactorValue(3, null, null, 2)
        ));

        long gradeIdUpdatedMore = gradeCreator.createGrade(gradeDefinition);
        assertEquals(gradeIdUpdated, gradeIdUpdatedMore);

        factors = factorService.getFactorValuesByGrade(gradeId);
        assertEquals(2, factors.size());
        factors.sort(Comparator.comparing(GradeFactor::getFactorId));
        assertEquals(1, factors.get(0).getFactorId());
        assertEquals(4, factors.get(0).getValue());
        assertEquals(3, factors.get(1).getFactorId());
        assertEquals(2, factors.get(1).getValue());

        assertEquals(1, pgJdbcTemplate.queryForObject(
            "select count(*) from security_data", Long.class).longValue());
    }

    @Test
    public void testShopGradePgCreation() {
        ShopGrade gradeDefinition = GradeCreator.constructShopGrade(SHOP_ID, UID);
        gradeDefinition.fillShopGradeCreationFields("odrId", Delivery.INSTORE);
        gradeDefinition.setText("text");
        gradeDefinition.setPro("pro");
        gradeDefinition.setContra("contra");

        gradeDefinition.setPhotos(List.of(
            Photo.buildForTest("group", "image1", ModState.UNMODERATED),
            Photo.buildForTest("group", "image2", ModState.UNMODERATED)
        ));
        gradeDefinition.setGradeFactorValues(List.of(
            new GradeFactorValue(2, null, null, 4),
            new GradeFactorValue(3, null, null, 3)
        ));

        long gradeId = gradeCreator.createGrade(gradeDefinition);
        assertEquals(1, readGradePg().size());

        ShopGrade grade = (ShopGrade) readGradePg().get(0);
        assertEquals(gradeId, grade.getId().longValue());
        assertEquals(UID, grade.getAuthorUid().longValue());
        assertEquals(GradeType.SHOP_GRADE, grade.getType());
        assertEquals(SHOP_ID, grade.getResourceId().longValue());
        assertEquals(Delivery.INSTORE, grade.getDelivery());
        assertEquals("odrId", grade.getOrderId());
        assertEquals(GradeState.LAST, grade.getState());
        assertEquals(ModState.APPROVED, grade.getModState());
        assertEquals("text", grade.getText());
        assertEquals("pro", grade.getPro());
        assertEquals("contra", grade.getContra());

        assertEquals(GradeCreator.DEFAULT_YANDEX_UID, readPgGradeYandexuid(gradeId));

        List<Photo> photos = photoService.getPhotosByGrade(gradeId);
        assertEquals(2, photos.size());
        photos.sort(Comparator.comparing(Photo::getImageName));
        assertEquals(gradeDefinition.getPhotos().get(0).getVal(), photos.get(0).getVal());
        assertEquals(gradeDefinition.getPhotos().get(1).getVal(), photos.get(1).getVal());

        List<GradeFactorValue> factors = factorService.getFactorValuesByGrade(gradeId);
        assertEquals(2, factors.size());
        factors.sort(Comparator.comparing(GradeFactor::getFactorId));
        assertEquals(2, factors.get(0).getFactorId());
        assertEquals(4, factors.get(0).getValue());
        assertEquals(3, factors.get(1).getFactorId());
        assertEquals(3, factors.get(1).getValue());
    }

    @Test
    public void testGradeStatesChange() {
        long gradeId = gradeCreator.createModelGrade(MODEL_ID, UID);

        // mod_state changes
        // check orig
        assertEquals(ModState.APPROVED, readGradePg(gradeId).getModState());
        // change
        gradeAdminService.moderateGradeReplies(List.of(gradeId), DbGradeAdminService.FAKE_MODERATOR,
            ModState.REJECTED_BY_SHOP_CLAIM);
        // check changed
        assertEquals(ModState.REJECTED_BY_SHOP_CLAIM, readGradePg(gradeId).getModState());

        // grade_state changes

        // check orig
        assertNull(readGradePg(gradeId).getGradeState());
        // change
        gradeAdminService.setGradeState(List.of(gradeId), true);
        // check change
        assertEquals("0", readGradePg(gradeId).getGradeState());
        // change back
        gradeAdminService.setGradeState(List.of(gradeId), false);
        // check change back
        assertNull(readGradePg(gradeId).getGradeState());

        // cpa changes
        // check orig (null -> false)
        assertFalse(readGradePg(gradeId).getCpa());
        // change
        verifiedGradeService.setCpaInDB(List.of(gradeId), true);
        // check change
        assertTrue(readGradePg(gradeId).getCpa());
        // change back
        verifiedGradeService.setCpaInDB(List.of(gradeId), false);
        // check change back
        assertFalse(readGradePg(gradeId).getCpa());

        // verified changes
        // check orig (null -> false)
        assertFalse(readGradePg(gradeId).getVerified());
        // change verified
        verifiedGradeService.setVerified(List.of(gradeId), VerifiedType.BLUE);
        // check changed
        assertTrue(readGradePg(gradeId).getVerified());

        // is_advert changes
        // check orig
        assertFalse(readGradeAdvert(gradeId));
        // change
        gradeAdminService.markAdvert(List.of(gradeId), true);
        // check
        assertTrue(readGradeAdvert(gradeId));
        // change back
        gradeAdminService.markAdvert(List.of(gradeId), false);
        // check back
        assertFalse(readGradeAdvert(gradeId));

        // state changes
        // check current grade id is last
        assertEquals(GradeState.LAST, readGradePg(gradeId).getState());
        // change grade
        long gradeIdChanged = gradeCreator.createModelGrade(MODEL_ID, UID);
        // check changes
        assertNotEquals(gradeIdChanged, gradeId);
        assertEquals(GradeState.PREVIOUS, readGradePg(gradeId).getState());
        assertEquals(GradeState.LAST, readGradePg(gradeIdChanged).getState());

        // move model (simple)
        moveGradeService.moveGrades(GradeType.MODEL_GRADE, MODEL_ID, MODEL_ID + 1, 74829832L);
        assertEquals(MODEL_ID + 1, readGradePg(gradeId).getResourceId().longValue());
        assertEquals(MODEL_ID + 1, readGradePg(gradeIdChanged).getResourceId().longValue());

        // move model (transition)
        moveGradeService.moveGradesWithLog(Map.of(MODEL_ID + 1, MODEL_ID + 2), null, null);
        assertEquals(MODEL_ID + 2, readGradePg(gradeId).getResourceId().longValue());
        assertEquals(MODEL_ID + 2, readGradePg(gradeIdChanged).getResourceId().longValue());
    }

    public AbstractGrade readGradePg(long gradeId) {
        return pgJdbcTemplate.query(
            "select " + DbGradeService.GRADE_VIEW_FIELDS_PG + "\n" +
                "from v_grade_data\n" +
                "where id = ?",
            GradeParseUtils::parseGradeRow,
            gradeId
        ).stream()
            .findAny()
            .orElseThrow(() -> new IllegalStateException("No grade found, but expected"));
    }

    public Boolean readGradeAdvert(long gradeId) {
        Integer flag = pgJdbcTemplate.queryForObject(
            "select is_advert\n" +
                "from grade\n" +
                "where id = ?",
            Integer.class,
            gradeId
        );
        return flag == null ? null : flag != 0;
    }

    private String readPgGradeYandexuid(long gradeId) {
        return pgJdbcTemplate.queryForObject(
            "select yandexuid from grade where id = ?",
            String.class,
            gradeId);
    }

    public List<AbstractGrade> readGradePg() {
        return pgJdbcTemplate.query(
            "select " + DbGradeService.GRADE_VIEW_FIELDS_PG + "\n" +
                "from v_grade_data\n",
            GradeParseUtils::parseGradeRow
        );
    }
}
