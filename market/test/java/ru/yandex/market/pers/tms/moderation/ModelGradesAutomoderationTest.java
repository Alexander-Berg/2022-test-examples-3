package ru.yandex.market.pers.tms.moderation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.core.ModReason;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.service.VerifiedGradeService;
import ru.yandex.market.pers.grade.core.ugc.PhotoService;
import ru.yandex.market.pers.grade.core.ugc.model.Photo;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.report.model.Prices;
import ru.yandex.market.report.model.Vendor;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author vvolokh
 * 08.10.2018
 */
public class ModelGradesAutomoderationTest extends AbstractAutoModerationTest {
    private static final long MODEL_ID = 1L;
    private static final long SUSPICIOUS_MODEL_ID = 100501L;
    private static final String COMMENT_TEXT =
        "чрезвычайно длинный комментарий, несомненно больше, чем положено настройками автоматической модерации. " +
            "К сожалению, в нём есть немного \"мат\"-ерных слов. С этим придётся как-то смириться.";
    private static final Long TEST_VENDOR_ID = 1L;
    private static final double GOOD_PRICE = 1000.0;
    public static final String NORMAL_TEXT = "Пример вполне хорошего отзыва, не слишком коротного";
    public static final String TEXT_TO_READY = "мал";

    @Autowired
    private ReportService reportService;

    @Autowired
    private ModelGradesAutomoderation processor;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private VerifiedGradeService verifiedGradeService;

    @Before
    public void setUp() {
        processor.setNeedDelayPositive(false);
    }

    @Test
    public void testOldGradeOnAutomoderation() {
        long gradeIdNoText = createModelGrade(AUTHOR_ID, null);
        makeGradeOlder(gradeIdNoText);

        processor.process();

        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeIdNoText));
    }

    @Test
    public void testNegativeObsceneCleanWebAutoFilter() {
        String pro = "Какое-то количество текста, содержащее мат внутри";
        String text = "Немного текста, чтобы всё было хорошо с длиной";
        String contra = "И ещё чуть-чуть";
        long gradeId = createModelGrade(text, pro, contra);

        mockCleanWebVerdict(gradeId, "true");

        processor.process();

        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeId));
        assertEquals(ModReason.RUDE.forModel(), getGradeModReason(gradeId));
        assertEquals("Contains obscene words, cw", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testNegativeSpecificRegexpFilter() {
        String pro = "facebook";
        String text = "Немного текста, чтобы всё было хорошо с длиной";
        String contra = "И ещё чуть-чуть";
        long gradeId = createModelGrade(text, pro, contra);

        processor.process();

        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeId));
        //assertEquals(ModReason.RUDE.forModel(), getGradeModReason(gradeId));
        assertEquals("Contains specifix regexp", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testPositiveObsceneCleanWebAutoFilter() {
        String pro = "Какое-то количество текста, содержащее мат внутри";
        String text = "Немного текста, чтобы всё было хорошо с длиной";
        String contra = "И ещё чуть-чуть";
        long gradeId = createModelGrade(text, pro, contra);

        mockCleanWebVerdict(gradeId, "false");

        processor.process();

        // filter is not triggered - so grade is fine
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testEmptyObsceneCleanWebAutoFilter() {
        String pro = "Какое-то количество текста, содержащее мат внутри";
        String text = "Немного текста, чтобы всё было хорошо с длиной";
        String contra = "И ещё чуть-чуть";
        long gradeId = createModelGrade(text, pro, contra);

        mockCleanWebVerdict(gradeId);

        processor.process();

        // filter is not triggered - so grade is fine
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testNegativeShortTextFilter() {
        long gradeIdNoText = createModelGrade(AUTHOR_ID, null);
        long gradeIdShortText = createModelGrade(AUTHOR_ID + 1, TEXT_TO_READY);

        processor.process();

        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeIdNoText));
        assertNull(getGradeModReason(gradeIdNoText));
        assertEquals("There is no text", getGradeFailedFilterDescription(gradeIdNoText));

        assertEquals(ModState.READY, getGradeModState(gradeIdShortText));
        assertNull(getGradeModReason(gradeIdShortText));
        assertEquals("Text length less than 5 symbols", getGradeFailedFilterDescription(gradeIdShortText));
    }

    @Test
    public void testNegativeSpammerFilter() {
        List<String> spammers = Stream.of(AUTHOR_ID).map(Object::toString).collect(Collectors.toList());

        // mark as spammer
        authorService.markAsSpammmers(
            spammers,
            AbstractAutomoderation.FAKE_MODERATOR,
            "For auto-test purposes");

        long gradeId = createModelGrade("Не слишком короткий текст, но достаточный, чтобы его считать ОК");

        processor.process();

        assertEquals(ModState.SPAMMER, getGradeModState(gradeId));
        assertNull(getGradeModReason(gradeId));

        // not not spammer
        authorService.markAsNotSpammmers(
            spammers,
            AbstractAutomoderation.FAKE_MODERATOR,
            "For auto-test purposes");

        gradeId = createModelGrade(
            "Не слишком короткий текст, но достаточный, чтобы его считать ОК. Но теперь совсем другой");

        processor.process();

        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
        assertNull(getGradeModReason(gradeId));
    }

    @Test
    public void testNegativeExcludedModelFilter() {
        long gradeIdNewModel = createModelGrade(AUTHOR_ID, MODEL_ID, "Отзыв на новую модель");
        // should be filtered because of new model filter, not empty text filter
        long gradeIdNewModelWithoutText = createModelGrade(AUTHOR_ID + 1, MODEL_ID, null);
        long gradeIdOldModel = createModelGrade(AUTHOR_ID, MODEL_ID + 1, "Отзыв на старую модель");

        pgJdbcTemplate.execute("INSERT INTO EXCLUDE_MODEL VALUES (" + MODEL_ID + ", 'Some new model')");

        processor.process();

        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeIdNewModel));
        assertEquals(ModReason.NOT_RELEASED.forModel(), getGradeModReason(gradeIdNewModel));
        assertEquals("Model in exclude list", getGradeFailedFilterDescription(gradeIdNewModel));

        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeIdNewModelWithoutText));
        assertEquals(ModReason.NOT_RELEASED.forModel(), getGradeModReason(gradeIdNewModelWithoutText));
        assertEquals("Model in exclude list", getGradeFailedFilterDescription(gradeIdNewModelWithoutText));

        assertEquals(ModState.APPROVED, getGradeModState(gradeIdOldModel));
        assertNull(getGradeModReason(gradeIdOldModel));
    }

    @Test
    public void testNegativeReportModelFilter() {
        //given:
        long gradeIdNoModelInReport = createModelGrade(AUTHOR_ID, MODEL_ID, COMMENT_TEXT);
        long gradeIdNoReleaseDateInReport = createModelGrade(AUTHOR_ID, MODEL_ID + 1, COMMENT_TEXT);
        long gradeIdModelIsNew = createModelGrade(AUTHOR_ID, MODEL_ID + 2, COMMENT_TEXT);
        long gradeIdModelIsOld = createModelGrade(AUTHOR_ID, MODEL_ID + 3, COMMENT_TEXT);
        long gradeIdModelIsReleasesToday = createModelGrade(AUTHOR_ID, MODEL_ID + 4, COMMENT_TEXT);

        mockReportService(
            mockReportModel(MODEL_ID + 1, null, TEST_VENDOR_ID),
            mockReportModel(MODEL_ID + 2, TEST_VENDOR_ID, LocalDate.now().plus(1L, ChronoUnit.DAYS)),
            mockReportModel(MODEL_ID + 3, TEST_VENDOR_ID, LocalDate.now().minus(1L, ChronoUnit.DAYS)),
            mockReportModel(MODEL_ID + 4, TEST_VENDOR_ID, LocalDate.now())
        );

        mockShopNames4Moderation("shop");

        //when:
        processor.process();

        //then:
        assertEquals("No model found in report", ModState.APPROVED, getGradeModState(gradeIdNoModelInReport));
        assertEquals("No release date in report", ModState.APPROVED, getGradeModState(gradeIdNoReleaseDateInReport));
        assertEquals("Model is new", ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeIdModelIsNew));
        assertEquals("Model not released yet", getGradeFailedFilterDescription(gradeIdModelIsNew));
        assertEquals("Model is old", ModState.APPROVED, getGradeModState(gradeIdModelIsOld));
        assertEquals("Model releases today", ModState.APPROVED, getGradeModState(gradeIdModelIsReleasesToday));
    }

    @Test
    public void testPositiveAutomoderation() {
        //given:
        long gradeId = createModelGrade(COMMENT_TEXT);
        mockReportService(MODEL_ID, GOOD_PRICE, TEST_VENDOR_ID);
        mockShopNames4Moderation("shop");

        //when:
        processor.process();

        //then:
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testPositiveAutomoderationWithoutCpa() {
        //given:
        long gradeId = createModelGrade(AUTHOR_ID, MODEL_ID, COMMENT_TEXT, 5, false);
        mockReportService(MODEL_ID, GOOD_PRICE, TEST_VENDOR_ID);
        mockShopNames4Moderation("shop");

        //when:
        processor.process();

        //then:
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testPositiveAutomoderationNegativeGrade() {
        //given:
        long gradeId = createModelGrade(MODEL_ID, COMMENT_TEXT, 1);
        mockReportService(MODEL_ID, GOOD_PRICE, TEST_VENDOR_ID);
        mockShopNames4Moderation("shop");

        //when:
        processor.process();

        //then:
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testPositiveWithPhoto() {
        long gradeId = createModelGrade(MODEL_ID, COMMENT_TEXT, 1);
        Photo photo1 = Photo.buildForTest("group_id", "img_name", ModState.UNMODERATED);
        Photo photo2 = Photo.buildForTest("group_id_2", "img_name_2", ModState.UNMODERATED);
        photoService.saveGradePhotosWithCleanup(gradeId, Arrays.asList(photo1, photo2));
        List<Photo> savedPhotos = photoService.getPhotosByGrade(gradeId);
        mockReportService(MODEL_ID, GOOD_PRICE, TEST_VENDOR_ID);
        mockShopNames4Moderation("shop");

        //when:
        processor.process();

        //then:
        assertEquals(ModState.AWAITS_PHOTO_MODERATION, getGradeModState(gradeId));
        savedPhotos.forEach(photo -> assertEquals(ModState.UNMODERATED.value(), getPhotoModState(photo.getId())));
    }

    @Test
    public void testFailByLength() {
        //given:
        long gradeId = createModelGrade(TEXT_TO_READY);
        mockReportService(MODEL_ID, GOOD_PRICE, TEST_VENDOR_ID);
        mockShopNames4Moderation("shop");

        //when:
        processor.process();

        //then:
        assertEquals(ModState.READY, getGradeModState(gradeId));
        assertEquals("Text length less than 5 symbols", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testFailBecauseBlue() {
        //given:
        long gradeId1 = createModelGrade(AUTHOR_ID, MODEL_ID, "Негативный отзыв оставленный на Беру!", 1);
        pgJdbcTemplate.execute("UPDATE GRADE.GRADE SET real_source = 'marketplace;bla;bla' WHERE id = " + gradeId1);
        long gradeId2 = createModelGrade(AUTHOR_ID + 1, MODEL_ID, "Позитивный отзыв оставленный на Беру!", 3);
        pgJdbcTemplate.execute("UPDATE GRADE.GRADE SET real_source = 'marketplace;bla;bla' WHERE id = " + gradeId2);
        mockReportService(MODEL_ID, GOOD_PRICE, TEST_VENDOR_ID);
        mockShopNames4Moderation("shop");

        //when:
        processor.process();

        //then:
        assertEquals(ModState.READY, getGradeModState(gradeId1));
        assertEquals(ModState.APPROVED, getGradeModState(gradeId2));
        assertEquals("Grade came from blue", getGradeFailedFilterDescription(gradeId1));
    }

    @Test
    public void testFailPrivacy() {
        //given:
        long[] gradeIdsBan = {
            createModelGrade(AUTHOR_ID, MODEL_ID, "Куплю рояль", 1),
            createModelGrade(AUTHOR_ID + 1, MODEL_ID, "Купим вашего сына в обмен на кукурузное поле", 1),
            createModelGrade(AUTHOR_ID + 2, MODEL_ID, "Скупая слеза стекла по моей щеке, когда я его открыл", 1),
            createModelGrade(AUTHOR_ID + 3, MODEL_ID, "Продаю геймпад. Недорого", 1),
        };
        long[] gradeIdsOk = {
            createModelGrade(AUTHOR_ID + 100, MODEL_ID, "Никогда не куплю больше", 1),
            createModelGrade(AUTHOR_ID + 101, MODEL_ID, "Никому не продаю, и не спрашивайте", 1),
            createModelGrade(AUTHOR_ID + 102, MODEL_ID, "Презираю всех, кто скупает видеокарты, и не играет на них", 1),
            createModelGrade(AUTHOR_ID + 4, MODEL_ID, "А ведь покупатель всегда прав, как тебе такое, яндекс?", 1),
        };

        mockReportService(MODEL_ID, GOOD_PRICE, TEST_VENDOR_ID);
        mockShopNames4Moderation("shop");

        //when:
        processor.process();

        //then:
        for (long gradeId : gradeIdsBan) {
            assertEquals(ModState.READY, getGradeModState(gradeId));
        }

        for (long gradeId : gradeIdsOk) {
            assertEquals(ModState.APPROVED, getGradeModState(gradeId));
        }
    }

    @Test
    public void testFailByEnglish() {
        //given:
        long gradeId = createModelGrade("Вроде начинает комментарий хорошо, but then suddenly switches to english and" +
            " can't stop any more. Moreover - there are spoilers! Jon Snow will kill Dani with his bare hands. Just " +
            "as Ned Stark taught his heirs to deliver punishment rightly.");
        mockReportService(MODEL_ID, GOOD_PRICE, TEST_VENDOR_ID);
        mockShopNames4Moderation("shop");

        //when:
        processor.process();

        //then:
        assertEquals(ModState.READY, getGradeModState(gradeId));
        assertEquals("More than 50% is NON-CYRILLIC", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testFailByReportChecks() {
        //given:
        long gradeIdFilteredByPrice = createModelGrade(AUTHOR_ID, MODEL_ID, COMMENT_TEXT);
        long gradeIdNoModelInReport = createModelGrade(AUTHOR_ID, MODEL_ID + 1, COMMENT_TEXT);
        long gradeIdNoPriceInReport = createModelGrade(AUTHOR_ID, MODEL_ID + 2, COMMENT_TEXT);
        long gradeIdModelIsNew = createModelGrade(AUTHOR_ID, MODEL_ID + 3, COMMENT_TEXT);

        mockReportService(
            mockReportModel(MODEL_ID, (double) (ModelGradesAutomoderation.PRICE_LIMIT + 1), TEST_VENDOR_ID),
            mockReportModel(MODEL_ID + 1, GOOD_PRICE, TEST_VENDOR_ID),
            mockReportModel(MODEL_ID + 2, null, TEST_VENDOR_ID),
            mockReportModel(MODEL_ID + 3, 1000.0, TEST_VENDOR_ID, true)
        );

        mockShopNames4Moderation("shop");

        //when:
        processor.process();

        //then:
        assertEquals("Model has too high price", ModState.READY, getGradeModState(gradeIdFilteredByPrice));
        assertEquals("Price, excluded vendors and 'New' badge check",
            getGradeFailedFilterDescription(gradeIdFilteredByPrice));
        assertEquals("No model found in report", ModState.APPROVED, getGradeModState(gradeIdNoModelInReport));
        assertEquals("No model found in report", ModState.APPROVED, getGradeModState(gradeIdNoPriceInReport));
        assertEquals("Model is new", ModState.READY, getGradeModState(gradeIdModelIsNew));
        assertEquals("Price, excluded vendors and 'New' badge check",
            getGradeFailedFilterDescription(gradeIdModelIsNew));
    }

    @Test
    public void testFailByObscenityFilter() {
        // creates obscene grade in READY state
        // this test is required to seal auto-moderation contract: do not ban obscene grades if they are happened to
        // become READY.
        // positive filter works always after negative, so grades with obscene words should be already banned
        // obscene check are too complex, so there is no need to re-run them in positive filters

        //given:
        long gradeId = createModelGrade(COMMENT_TEXT);
        pgJdbcTemplate.update("update grade set mod_state = ? where id = ?", ModState.READY.value(), gradeId);

        mockCleanWebVerdict(gradeId, "true");

        mockReportService(MODEL_ID, GOOD_PRICE, TEST_VENDOR_ID);
        mockShopNames4Moderation("shop");

        //when:
        processor.process();

        //then:
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testFailByLinksFilter() {
        //given:
        long gradeId = createModelGrade(
            "достаточно длинный текст, чтобы его можно было считать совершенно валидным комментарием, но, к сожалению" +
                " он содержит ссылку: ya.ru - это фиаско");
        mockReportService(MODEL_ID, GOOD_PRICE, TEST_VENDOR_ID);
        mockShopNames4Moderation("shop");

        //when:
        processor.process();

        //then:
        assertEquals(ModState.READY, getGradeModState(gradeId));
        assertEquals("Contains links (Model)", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testFailByWordLengthFilter() {
        long gradeId = createModelGrade("Длина слова диоксометилтетрагидропиримидин ровно 30 символов, удивительно!");
        processor.process();

        assertEquals(ModState.READY, getGradeModState(gradeId));
        assertEquals("Contains long words: > 30 symbols", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testWordLengthFilter() {
        long gradeId = createModelGrade("А вот длина слова клиентоориентированныймагазин ровно 29 символов!");
        processor.process();

        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testPositivePaidFilterSimple() {
        long gradeIdPaid = createModelGrade(AUTHOR_ID, MODEL_ID, NORMAL_TEXT);
        long gradeId = createModelGrade(AUTHOR_ID + 1, MODEL_ID, NORMAL_TEXT);

        pgJdbcTemplate.update("insert into paid_grade(grade_id, paid_fl) values (?,1)", gradeIdPaid);

        processor.process();

        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
        assertEquals(ModState.READY, getGradeModState(gradeIdPaid));
    }

    @Test
    public void testPositiveFilterWithSuspiciousModel() {
        long gradeIdSuspicious = createModelGrade(AUTHOR_ID, SUSPICIOUS_MODEL_ID, NORMAL_TEXT);
        long gradeId = createModelGrade(AUTHOR_ID, MODEL_ID, NORMAL_TEXT);

        pgJdbcTemplate.update("insert into suspicious_model(model_id) values (?)", SUSPICIOUS_MODEL_ID);

        processor.process();

        assertEquals(ModState.READY, getGradeModState(gradeIdSuspicious));
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testExpDisableFlags() {
        long gradeId = createModelGrade(AUTHOR_ID, MODEL_ID, NORMAL_TEXT);

        processor.setExpFlag(AbstractAutomoderation.EXP_NEG_DISABLED, true);
        processor.setExpFlag(AbstractAutomoderation.EXP_POS_DISABLED, true);

        processor.process();
        assertEquals(ModState.UNMODERATED, getGradeModState(gradeId));

        processor.setExpFlag(AbstractAutomoderation.EXP_NEG_DISABLED, false);
        processor.process();
        assertEquals(ModState.UNREADY, getGradeModState(gradeId));

        processor.setExpFlag(AbstractAutomoderation.EXP_POS_DISABLED, false);
        processor.process();
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testExpCpaOnlyFlag() {
        long gradeId = createModelGrade(AUTHOR_ID, MODEL_ID, TEXT_TO_READY);
        long gradeIdNonCpa = createModelGrade(AUTHOR_ID + 1, MODEL_ID, TEXT_TO_READY);

        verifiedGradeService.setCpaInDB(List.of(gradeIdNonCpa), false);

        processor.setExpFlag(AbstractAutomoderation.EXP_POS_CPA_ONLY, true);

        processor.process();
        assertEquals(ModState.READY, getGradeModState(gradeId));
        assertEquals(ModState.UNREADY, getGradeModState(gradeIdNonCpa));

        processor.setExpFlag(AbstractAutomoderation.EXP_POS_CPA_ONLY, false);
        processor.process();
        assertEquals(ModState.READY, getGradeModState(gradeId));
        assertEquals(ModState.READY, getGradeModState(gradeIdNonCpa));
    }

    @Test
    public void testExpManualOnly() {
        long gradeId = createModelGrade(AUTHOR_ID, MODEL_ID, NORMAL_TEXT);

        processor.setExpFlag(AbstractAutomoderation.EXP_POS_MANUAL_ONLY, true);

        processor.process();
        assertEquals(ModState.READY, getGradeModState(gradeId));

        processor.setExpFlag(AbstractAutomoderation.EXP_POS_MANUAL_ONLY, false);
        processor.process();
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testExpNonCpaManual() {
        long gradeId = createModelGrade(AUTHOR_ID, MODEL_ID, NORMAL_TEXT);
        long gradeIdNonCpa = createModelGrade(AUTHOR_ID + 1, MODEL_ID, NORMAL_TEXT);

        verifiedGradeService.setCpaInDB(List.of(gradeIdNonCpa), false);

        processor.setExpFlag(AbstractAutomoderation.EXP_POS_NON_CPA_MANUAL, true);

        processor.process();
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
        assertEquals(ModState.READY, getGradeModState(gradeIdNonCpa));

        processor.setExpFlag(AbstractAutomoderation.EXP_POS_NON_CPA_MANUAL, false);
        processor.process();
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
        assertEquals(ModState.APPROVED, getGradeModState(gradeIdNonCpa));
    }

    @Test
    public void testRejectByModeratorRepeatedGrades() {
        doTestRepeatedRejectedGrades(ModState.REJECTED);
    }

    @Test
    public void testRejectByAutomoderatorRepeatedGrades() {
        doTestRepeatedRejectedGrades(ModState.AUTOMATICALLY_REJECTED);
    }

    @Test
    public void testDoNotBanRepeatedGrade() {
        //given:
        long prevGradeId = createModelGrade(AUTHOR_ID, MODEL_ID, COMMENT_TEXT);
        dbGradeAdminService.moderateGradeReplies(singletonList(prevGradeId), 1L, ModState.READY);
        long gradeId = createModelGrade(AUTHOR_ID, MODEL_ID, COMMENT_TEXT);

        //when:
        processor.process();

        //then:
        // в тесте не заданы никакие негативные и позитивные фильтры - все принимаем
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testFiltersAfterBanRepeated() {
        //given:
        long prevGradeId1 = createModelGrade(AUTHOR_ID, MODEL_ID, COMMENT_TEXT);
        Long modReason1 = ModReason.SPAM.forModel();
        dbGradeAdminService.moderateGradeReplies(
            singletonMap(prevGradeId1, modReason1),
            prevGradeId1, ModState.REJECTED);
        long gradeId1 = createModelGrade(AUTHOR_ID, MODEL_ID, COMMENT_TEXT + 1);

        String rudeWord = "мат";
        mockCleanWebVerdict(gradeId1, "true");
        long prevGradeId2 = createModelGrade(AUTHOR_ID + 1, MODEL_ID + 1, COMMENT_TEXT + rudeWord);
        Long modReason2 = ModReason.NOT_RESTORED.forModel();
        dbGradeAdminService.moderateGradeReplies(
            singletonMap(prevGradeId2, modReason2),
            prevGradeId2, ModState.REJECTED);
        long gradeId2 = createModelGrade(AUTHOR_ID + 1, MODEL_ID + 1, COMMENT_TEXT + rudeWord + 1);
        mockCleanWebVerdict(gradeId2, "true");

        //when:
        processor.process();

        //then:
        // позитивный фильтр не заапрувил этот отзыв
        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeId1));
        assertEquals(modReason1, getGradeModReason(gradeId1));

        // должна сохранится причина модерации и не исправиться на RUDE
        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeId2));
        assertEquals(modReason2, getGradeModReason(gradeId2));
    }


    @Test
    public void testHasPreviousRejectedGrade() {
        long oldGrade = createModelGrade(123L, 123L, "Просто какой-то текст, который в любом случае зареджекчен");
        dbGradeAdminService.moderateGradeReplies(List.of(oldGrade), 1L, ModState.REJECTED);

        long newGrade = createModelGrade(123L, 123L, "Просто какой-то текст, который должен был бы зааппрувиться, но " +
            "имеет старый отзыв");
        processor.process();

        assertEquals(ModState.READY, getGradeModState(newGrade));
        assertEquals("Has previous version with ModState 4 or 7", getGradeFailedFilterDescription(newGrade));
    }

    @Test
    public void testHasPreviousApprovedGrade() {
        long oldGrade = createModelGrade(123L, 123L, "Просто какой-то текст, который уже заапрувлен");
        dbGradeAdminService.moderateGradeReplies(List.of(oldGrade), 1L, ModState.APPROVED);

        long newGrade = createModelGrade(123L, 123L, "Просто какой-то текст, который должен был бы зааппрувиться");
        processor.process();

        assertEquals(ModState.APPROVED, getGradeModState(newGrade));
    }

    @Test
    public void testFakeRegexps() {
        int i = 0;
        Long goodGrade = createModelGrade(AUTHOR_ID + i++, "Тестируем, что заданные слова проходят через фильтр: " +
            "флиБУстьер, производитель большой ОРИГИНАЛ, восПАЛЕНие, запах ПАЛЁНой пластмассы");

        String filler = "Тестируем, что фильтр срабатывает на слово из списка: ";
        List<String> badWords = List.of("подделка", "поддельный", "реплика", "б\\у", "бывшие  в     употреблении",
            "контрафакт", "копия", "не   в    оригинале", "неоригинальный", "пародия", "фейковый", "палёнка",
            "поюзанный", "бу-шная");

        List<Long> gradesToFail = new ArrayList<>();
        for (String word : badWords) {
            Long modelGrade = createModelGrade(AUTHOR_ID + i++, filler + word);
            gradesToFail.add(modelGrade);
        }

        processor.process();

        checkGradeModState(ModState.APPROVED, goodGrade);

        gradesToFail.forEach(grade -> {
            checkGradeModState(ModState.READY, grade);
            assertEquals("Grade about fake goods", getGradeFailedFilterDescription(grade));
        });
    }

    @Test
    public void testModTimeExecution() {
        long gradeId = createModelGrade(NORMAL_TEXT);

        processor.setNeedDelayPositive(true);
        mockCleanWebVerdict(gradeId, "false");
        processor.process();

        //after this process positive filter not run, because mod_time is not enough
        assertEquals(ModState.UNREADY, getGradeModState(gradeId));

        makeModerationOlder(gradeId);
        processor.process();

        //now mod_time is enough for positive filter
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testDoNotConvertModStateFromReadyToReady() {
        long gradeId = createModelGrade(TEXT_TO_READY);

        processor.process();
        assertEquals(ModState.READY, getGradeModState(gradeId));

        processor.process();
        assertEquals(ModState.READY, getGradeModState(gradeId));

        @SuppressWarnings("ConstantConditions")
        long readyCnt = pgJdbcTemplate.queryForObject(
            "select count(*) from mod_grade where grade_id = ? and mod_state = ?",
            Long.class, gradeId, ModState.READY.value()
        );
        assertEquals(1L, readyCnt);
    }

    @Test
    public void testDoNotConvertModStateFromReadyToReadyWithPosManualFlag() {
        long gradeId = createModelGrade(NORMAL_TEXT);
        processor.setExpFlag(AbstractAutomoderation.EXP_POS_MANUAL_ONLY, true);

        processor.process();
        assertEquals(ModState.READY, getGradeModState(gradeId));

        processor.process();
        assertEquals(ModState.READY, getGradeModState(gradeId));

        @SuppressWarnings("ConstantConditions")
        long readyCnt = pgJdbcTemplate.queryForObject(
            "select count(*) from mod_grade where grade_id = ? and mod_state = ?",
            Long.class, gradeId, ModState.READY.value()
        );
        assertEquals(1L, readyCnt);
    }

    @Test
    public void testUnpublishedGrades() {
        long gradeId = createModelGrade(AUTHOR_ID, MODEL_ID, NORMAL_TEXT + " для братков");

        expFlagService.setFlag(AbstractAutomoderation.EXP_DISABLE_AUTO_PUBLISH_MODEL_GRADE_KEY, true);
        verifiedGradeService.setCpaInDB(List.of(gradeId), true);
        processor.process();

        checkGradeModState(ModState.READY_TO_PUBLISH, gradeId);
        validateIndexingQueueIsEmpty(gradeId);
    }

    @Test
    public void testModerationImportedGrades() {
        long gradeId = createModelGrade(AUTHOR_ID, MODEL_ID, "Негативный отзыв оставленный на Беру!", 1);
        pgJdbcTemplate.execute("UPDATE GRADE.GRADE SET real_source = 'import;bla;bla' WHERE id = " + gradeId);

        processor.process();

        assertEquals(ModState.READY, getGradeModState(gradeId));
        assertEquals("Imported grades", getGradeFailedFilterDescription(gradeId));
    }


    public void doTestRepeatedRejectedGrades(ModState rejectedModState) {
        List<ModReason> rejectedModReasons = Arrays.asList(
            ModReason.SPAM,
            ModReason.NOT_RESTORED
        );

        //given:
        HashMap<Long, Long> rejectedGrades = new HashMap<>();
        List<Long> otherGradeIds = new ArrayList<>();
        int i = 0;
        for (ModReason modReason : getModReasonsForModel()) {
            i++;
            long prevGradeId = createModelGrade(AUTHOR_ID + i, MODEL_ID + i, COMMENT_TEXT + i);
            Long modReasonValue = modReason.forModel();
            dbGradeAdminService.moderateGradeReplies(
                singletonMap(prevGradeId, modReasonValue), prevGradeId,
                rejectedModState);
            long gradeId = createModelGrade(AUTHOR_ID + i, MODEL_ID + i, COMMENT_TEXT + i + i);
            // только отзывы, у которых предыдущий отклонен по одной из этих причин - отклоняется
            if (rejectedModReasons.contains(modReason)) {
                rejectedGrades.put(gradeId, modReasonValue);
            } else {
                otherGradeIds.add(gradeId);
            }
        }

        //when:
        processor.process();

        //then:
        rejectedGrades.forEach((gradeId, modReason) -> {
                assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeId));
                assertEquals(modReason, getGradeModReason(gradeId));
            }
        );

        // в тесте не заданы никакие негативные и позитивные фильтры - все принимаем
        // поменял логику, теперь если есть отклоненный предыдущий отзыв оставляем на ручную модерацию
        if (rejectedModState == ModState.REJECTED) {
            otherGradeIds.forEach(gradeId -> assertEquals(ModState.READY, getGradeModState(gradeId)));
        } else {
            otherGradeIds.forEach(gradeId -> assertEquals(ModState.APPROVED, getGradeModState(gradeId)));
        }
    }

    private int getPhotoModState(long photoId) {
        return pgJdbcTemplate.queryForObject("SELECT mod_state FROM photo WHERE id_seq=" + photoId, Integer.class);
    }

    protected Long getGradeModReason(long gradeId) {
        return pgJdbcTemplate.queryForList(
                "SELECT mod_reason FROM grade WHERE id =" + gradeId, Long.class)
            .stream()
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private void mockReportService(Long modelId, Double price, Long vendorId) {
        mockReportService(modelId, price, vendorId, false);
    }

    private void mockReportService(Long modelId, Double price, Long vendorId, boolean isNew) {
        Map<Long, Model> reportResult = new HashMap<>();
        Model reportModel = mockReportModel(modelId, price, vendorId, isNew);
        mockReportService(reportModel);
    }

    private Model mockReportModel(Long modelId, Long vendorId, LocalDate date) {
        Model model = mockReportModel(modelId, null, vendorId, false);
        model.setSaleBeginDate(date);
        return model;
    }

    private Model mockReportModel(Long modelId, Double price, Long vendorId) {
        return mockReportModel(modelId, price, vendorId, false);
    }

    private Model mockReportModel(Long modelId, Double price, Long vendorId, boolean isNew) {
        Map<Long, Model> reportResult = new HashMap<>();
        Model reportModel = new Model();
        reportModel.setId(modelId);
        if (price != null) {
            reportModel.setPrices(new Prices(price, price, price, new BigDecimal(1), Currency.RUR));
        }
        reportModel.setVendor(new Vendor(vendorId, "Test vendor"));
        reportModel.setNew(isNew);
        return reportModel;
    }

    private void mockReportService(Model... models) {
        when(reportService.getModelsByIds(any())).thenReturn(
            Arrays.stream(models)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                    Model::getId,
                    x -> x,
                    (x, y) -> y
                ))
        );
    }

    private long createModelGrade(String text, String pro, String contra) {
        ModelGrade grade = GradeCreator.constructModelGrade(MODEL_ID, AUTHOR_ID);
        grade.setText(text);
        grade.setPro(pro);
        grade.setContra(contra);
        grade.setModState(ModState.UNMODERATED);
        grade.setAverageGrade(5);
        grade.setPhotos(List.of());
        grade.setCpa(true);
        final long gradeId = gradeCreator.createGrade(grade);
        makeGradeOlder(gradeId);
        return gradeId;
    }

    private long createModelGrade(String text) {
        return createModelGrade(MODEL_ID, text, 5);
    }

    private long createModelGrade(long userId, String text) {
        return createModelGrade(userId, MODEL_ID, text, 5);
    }

    private long createModelGrade(long userId, long modelId, String text) {
        return createModelGrade(userId, modelId, text, 5);
    }

    private long createModelGrade(long modelId, String comment, int gradeValue) {
        return createModelGrade(AUTHOR_ID, modelId, comment, gradeValue);
    }

    private long createModelGrade(long userId, long modelId, String comment, int gradeValue) {
        return createModelGrade(userId, modelId, comment, gradeValue, true);
    }

    private long createModelGrade(long userId, long modelId, String comment, int gradeValue, boolean cpa) {
        ModelGrade grade = GradeCreator.constructModelGrade(modelId, userId);
        grade.setText(comment);
        grade.setPro(null);
        grade.setContra(null);
        grade.setModState(ModState.UNMODERATED);
        grade.setAverageGrade(gradeValue);
        grade.setPhotos(List.of());
        grade.setCpa(cpa);

        long gradeId = gradeCreator.createGrade(grade);
        makeGradeOlder(gradeId);
        return gradeId;
    }

    public static List<ModReason> getModReasonsForModel() {
        return Stream.of(ModReason.values()).filter(it -> it.forModel() != null).collect(Collectors.toList());
    }

}
