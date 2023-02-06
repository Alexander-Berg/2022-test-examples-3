package ru.yandex.market.pers.tms.moderation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.cleanweb.dto.CleanWebResponseDto;
import ru.yandex.market.cleanweb.dto.VerdictDto;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.core.GradeSource;
import ru.yandex.market.pers.grade.core.model.core.GradeValue;
import ru.yandex.market.pers.grade.core.model.core.ModReason;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.service.GradeCleanWebService;
import ru.yandex.market.pers.grade.core.service.VerifiedGradeService;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.tms.moderation.AbstractAutomoderation.EXP_ORDER_OR_CPC_ONLY;
import static ru.yandex.market.pers.tms.moderation.ShopGradesAutomoderation.FEEDBACK_GRADES_BATCH_SIZE_KEY;

public class ShopGradesAutomoderationTest extends AbstractAutoModerationTest {
    private static final long SHOP_ID = 1L;
    private static final Logger log = Logger.getLogger(ShopGradesAutomoderationTest.class);

    public static final String NORMAL_TEXT = "Пример вполне хорошего отзыва, не слишком короткого";
    public static final String TEXT_TO_READY = "latin text for grade review";

    @Autowired
    private ShopGradesAutomoderation processor;

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private VerifiedGradeService verifiedGradeService;

    @Before
    public void setUp() {
        processor.setNeedDelayPositive(false);
    }

    @Test
    public void testCleanWebAutoFilter() {
        checkCleanWebAutoFilter(GradeValue.TERRIBLE, false, ModState.READY);
        checkCleanWebAutoFilter(GradeValue.EXCELLENT, false, ModState.APPROVED);

        checkCleanWebAutoFilter(GradeValue.EXCELLENT, true, ModState.AUTOMATICALLY_REJECTED);
        checkCleanWebAutoFilter(GradeValue.GOOD, true, ModState.AUTOMATICALLY_REJECTED);
        checkCleanWebAutoFilter(GradeValue.NORMAL, true, ModState.AUTOMATICALLY_REJECTED);
        checkCleanWebAutoFilter(GradeValue.BAD, true, ModState.AUTOMATICALLY_REJECTED);
        checkCleanWebAutoFilter(GradeValue.TERRIBLE, true, ModState.AUTOMATICALLY_REJECTED);
    }

    @Test
    public void testEmptyObsceneCleanWebAutoFilter() {
        String text = "Немного текста, чтобы всё было хорошо с длиной";
        long gradeId = createShopGrade(text);

        CleanWebResponseDto dto = new CleanWebResponseDto(String.valueOf(gradeId), new VerdictDto[]{});
        configurationService.tryGetOrMergeVal(GradeCleanWebService.ENABLE_KEY, Boolean.class, true);
        when(cleanWebClient.sendContent(anyList(), anyBoolean())).thenReturn(new CleanWebResponseDto[]{dto});

        processor.process();

        // filter is not triggered - so grade is fine
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    private void checkCleanWebAutoFilter(GradeValue value, boolean isTriggered, ModState modState) {
        String text = "Немного текста, чтобы всё было хорошо с длиной";
        long gradeId = createShopGrade(text, value);

        VerdictDto verdictDto = new VerdictDto();
        verdictDto.setKey(String.valueOf(gradeId));
        verdictDto.setName("text_auto_obscene");
        verdictDto.setValue(String.valueOf(isTriggered));
        CleanWebResponseDto dto = new CleanWebResponseDto(String.valueOf(gradeId), new VerdictDto[]{verdictDto});
        configurationService.tryGetOrMergeVal(GradeCleanWebService.ENABLE_KEY, Boolean.class, true);
        when(cleanWebClient.sendContent(anyList(), anyBoolean())).thenReturn(new CleanWebResponseDto[]{dto});

        processor.process();

        if (isTriggered) {
            assertEquals(modState, getGradeModState(gradeId));
            assertEquals(ModReason.RUDE.forShop(), getGradeModReason(gradeId));
            assertEquals("Contains obscene words, cw", getGradeFailedFilterDescription(gradeId));
        } else {
            assertEquals(modState, getGradeModState(gradeId));
        }
    }

    @Test
    public void testToxicCleanWebAutoFilter() {
        String text = "Немного текста, чтобы всё было хорошо с длиной";
        long gradeId = createShopGrade(text, GradeValue.GOOD);

        VerdictDto verdictDto1 = new VerdictDto();
        verdictDto1.setKey(String.valueOf(gradeId));
        verdictDto1.setName("text_auto_toxic");
        verdictDto1.setValue(String.valueOf(true));

        VerdictDto verdictDto2 = new VerdictDto();
        verdictDto2.setKey(String.valueOf(gradeId));
        verdictDto2.setName("text_auto_obscene");
        verdictDto2.setValue(String.valueOf(true));

        VerdictDto verdictDto3 = new VerdictDto();
        verdictDto3.setKey(String.valueOf(gradeId));
        verdictDto3.setName("text_auto_common_toloka_no_sense");
        verdictDto3.setValue(String.valueOf(true));

        CleanWebResponseDto dto = new CleanWebResponseDto(String.valueOf(gradeId),
            new VerdictDto[]{verdictDto1, verdictDto2, verdictDto3});
        configurationService.tryGetOrMergeVal(GradeCleanWebService.ENABLE_KEY, Boolean.class, true);
        when(cleanWebClient.sendContent(anyList(), anyBoolean())).thenReturn(new CleanWebResponseDto[]{dto});

        processor.process();

        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeId));
        assertEquals(ModReason.BY_FILTER_SILENT.forShop(), getGradeModReason(gradeId));
        assertEquals("Contains obscene words, cw", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testNoSenceCleanWebAutoFilter() {
        String text = "Немного текста, чтобы всё было хорошо с длиной";
        long gradeId = createShopGrade(text, GradeValue.GOOD);

        VerdictDto verdictDto = new VerdictDto();
        verdictDto.setKey(String.valueOf(gradeId));
        verdictDto.setName("text_auto_common_toloka_no_sense");
        verdictDto.setValue(String.valueOf(true));

        CleanWebResponseDto dto = new CleanWebResponseDto(String.valueOf(gradeId), new VerdictDto[]{verdictDto});
        configurationService.tryGetOrMergeVal(GradeCleanWebService.ENABLE_KEY, Boolean.class, true);
        when(cleanWebClient.sendContent(anyList(), anyBoolean())).thenReturn(new CleanWebResponseDto[]{dto});

        processor.process();

        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeId));
        assertEquals(ModReason.UNINFORMATIVE.forShop(), getGradeModReason(gradeId));
        assertEquals("Contains obscene words, cw", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testNegativeObsceneAutoFilter() {
        checkNegativeObsceneAutoFilter(GradeValue.TERRIBLE, ModState.AUTOMATICALLY_REJECTED);
        checkNegativeObsceneAutoFilter(GradeValue.BAD, ModState.AUTOMATICALLY_REJECTED);
        checkNegativeObsceneAutoFilter(GradeValue.NORMAL, ModState.AUTOMATICALLY_REJECTED);
        checkNegativeObsceneAutoFilter(GradeValue.GOOD, ModState.AUTOMATICALLY_REJECTED);
        checkNegativeObsceneAutoFilter(GradeValue.EXCELLENT, ModState.AUTOMATICALLY_REJECTED);
    }

    @Test
    public void testNegativeShortTextFilterGoodGrade() {
        checkNegativeShortTextFilter(GradeValue.TERRIBLE, ModState.AUTOMATICALLY_REJECTED);
        checkNegativeShortTextFilter(GradeValue.BAD, ModState.AUTOMATICALLY_REJECTED);
        checkNegativeShortTextFilter(GradeValue.NORMAL, ModState.AUTOMATICALLY_REJECTED);
        checkNegativeShortTextFilter(GradeValue.GOOD, ModState.AUTOMATICALLY_REJECTED);
        checkNegativeShortTextFilter(GradeValue.EXCELLENT, ModState.AUTOMATICALLY_REJECTED);
    }

    @Test
    public void testLengthFilter() {
        Long gradeId = createShopGrade("Не слишком короткий текст, но достаточный, чтобы его считать ОК");
        processor.process();

        checkGradeModState(ModState.APPROVED, gradeId);
    }

    @Test
    public void testGoodGradeWithEmptyText() {
        ShopGrade grade = GradeCreator.constructShopGradeNoText(
            GradeCreator.rndShop(), GradeCreator.rndUid(), ModState.UNMODERATED);
        grade.setPro("Не слишком короткий текст, но достаточный, чтобы его считать ОК");
        grade.setAverageGrade(GradeValue.EXCELLENT.toAvgGrade());
        Long gradeId = createShopGrade(grade);

        processor.process();

        checkGradeModState(ModState.APPROVED, gradeId);
    }

    @Test
    public void testWordLengthFilter() {
        Long gradeId = createShopGrade("Это слово было придумано специально для этого теста: " +
            "verylongwordspeciallyfortestingpurpose");
        processor.process();

        checkGradeModState(ModState.READY, gradeId);
        assertEquals("Contains long words: > 30 symbols", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testWordLengthFilterWithLessThan30Chars() {
        Long gradeId = createShopGrade("длина слова клиентоориентированный меньше, чем 30 символов");
        processor.process();

        checkGradeModState(ModState.APPROVED, gradeId);
    }

    @Test
    public void testCapsFilter() {
        Long gradeId = createShopGrade("КАПС НИКТО НЕ ЛЮБИТ, НИКТО НЕ БУДЕТ ОБАЩАТЬСЯ С ТОБОЙ - только я");
        processor.process();

        checkGradeModState(ModState.AUTOMATICALLY_REJECTED, gradeId);
        assertEquals("More than 30% CAPSLOCK", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testLinksFilter() {
        Long gradeId = createShopGrade("Кажется, кто-то упоминал в этом отзыве такие ссылки, как ya.ru и beru.ru");
        processor.process();

        checkGradeModState(ModState.READY, gradeId);
        assertEquals("Contains links", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testYandexMentionsFilter() {
        Long gradeId = createShopGrade("Почему-то модератор яндекса злится, если люди упоминают его в отзывах, " +
            "давайте не будем его злить");
        processor.process();

        checkGradeModState(ModState.APPROVED, gradeId);
    }

    @Test
    public void testPrivacyDataFilter() {
        Long gradeId = createShopGrade("Это специальный фильтр, сделанный для того, чтобы нельзя было запостить фио " +
            "или данные плательщика");
        processor.process();

        checkGradeModState(ModState.READY, gradeId);
        assertEquals("Contains privacy data (db)", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testAnalyzeRegexp() {
        String text =
            "Лучшие цены в Перми. Отличный интернет-магазин. Позвонила, заказала телефон, подвезли очень быстро, не " +
                "пришлось ждать. Курьер все объяснил и показал. Спасибо. Однозначно еще воспользуюсь.Большой выбор " +
                "товаров, удобный сайт, короткие сроки доставки товаров, возможность выбрать из нескольких заказанных" +
                " товаров.Нет. ";
        String regexp = "\\b[А-Я].*(ин|ина|ов|ова|ев|ева)\\b";
        List<String> allMatches = new ArrayList<String>();
        Matcher matcher = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        while (matcher.find()) {
            allMatches.add(matcher.group());
        }
        if (allMatches.isEmpty()) {
            log.debug("Text doesn't match this pattern");
        } else {
            log.debug(allMatches);
        }
    }

    @Test
    public void testVeryVeryLongSumText() {
        ShopGrade grade = GradeCreator.constructShopGradeNoText(
            GradeCreator.rndShop(), GradeCreator.rndUid(), ModState.UNMODERATED);
        final String text = "Это очень длинный текст для тестирования того, что автомодерация на больших текстах не " +
            "падает. 12345 ";
        grade.setText(StringUtils.repeat(text, 20));
        grade.setPro(StringUtils.repeat(text, 20));
        grade.setContra(StringUtils.repeat(text, 20));
        grade.setAverageGrade(GradeValue.EXCELLENT.toAvgGrade());
        long gradeId = createShopGrade(grade);
        processor.process();

        checkGradeModState(ModState.APPROVED, gradeId);
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
    public void testFiltersAfterBanRepeated() {
        //given:
        long prevGradeId1 = createShopGrade(AUTHOR_ID, SHOP_ID, COMMENT_TEXT);
        Long modReason1 = ModReason.SPAM.forShop();
        dbGradeAdminService.moderateGradeReplies(
            singletonMap(prevGradeId1, modReason1),
            prevGradeId1, ModState.REJECTED);
        long gradeId1 = createShopGrade(AUTHOR_ID, SHOP_ID, COMMENT_TEXT + 1);

        String rudeWord = "мат";
        mockCleanWebVerdict(gradeId1, "true");
        long prevGradeId2 = createShopGrade(AUTHOR_ID + 1, SHOP_ID + 1, COMMENT_TEXT + rudeWord);
        Long modReason2 = ModReason.NOT_RESTORED.forShop();
        dbGradeAdminService.moderateGradeReplies(
            singletonMap(prevGradeId2, modReason2),
            prevGradeId2, ModState.REJECTED);
        long gradeId2 = createShopGrade(AUTHOR_ID + 1, SHOP_ID + 1, COMMENT_TEXT + rudeWord + 1);
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
    public void testPrivacyRegexps() {
        Long goodGrade = createShopGrade("Тестируем, что заданные слова проходят через фильтр: " +
            "баул., имярек, шина, iPhone, марка, букв., фиолет, 12345ааааа");
        Long gradeWithPhone = createShopGrade("Тестируем, что фильтр срабатывает на что-то вроде номера телефона: " +
            "+7 322 223 33 22");
        Long gradeWithName = createShopGrade("Тестируем, что фильтр срабатывает на имя из списка: Сергей");
        Long gradeWithStopWord1 = createShopGrade("Тестируем, что фильтр срабатывает на слово из списка: имя");
        Long gradeWithStopWord2 = createShopGrade("Тестируем, что фильтр срабатывает на слово из списка: кв.");
        Long gradeWithStopWord3 = createShopGrade("Тестируем, что фильтр срабатывает на слово из списка: ул.");
        Long gradeWithStopWord4 = createShopGrade("Тестируем, что фильтр срабатывает на слово из списка: фио");
        processor.process();

        checkGradeModState(ModState.APPROVED, goodGrade);

        checkGradeModState(ModState.READY, gradeWithPhone);
        assertEquals("Contains privacy data (db)", getGradeFailedFilterDescription(gradeWithPhone));
        checkGradeModState(ModState.READY, gradeWithName);
        assertEquals("Contains privacy data (db)", getGradeFailedFilterDescription(gradeWithName));
        checkGradeModState(ModState.READY, gradeWithStopWord1);
        assertEquals("Contains privacy data (db)", getGradeFailedFilterDescription(gradeWithStopWord1));
        checkGradeModState(ModState.READY, gradeWithStopWord2);
        assertEquals("Contains privacy data (db)", getGradeFailedFilterDescription(gradeWithStopWord2));
        checkGradeModState(ModState.READY, gradeWithStopWord3);
        assertEquals("Contains privacy data (db)", getGradeFailedFilterDescription(gradeWithStopWord3));
        checkGradeModState(ModState.READY, gradeWithStopWord4);
        assertEquals("Contains privacy data (db)", getGradeFailedFilterDescription(gradeWithStopWord4));
    }

    @Test
    public void testHasPreviousRejectedGrade() {
        long oldGrade = createShopGrade(123L, 123L, "Просто какой-то текст, который в любом случае зареджекчен");
        dbGradeAdminService.moderateGradeReplies(List.of(oldGrade), 1L, ModState.REJECTED);

        long grade1 = createShopGrade(123L, 123L, "Просто какой-то текст, который должен был бы зааппрувиться, но " +
            "имеет старый отзыв");
        processor.process();

        checkGradeModState(ModState.READY, grade1);
        assertEquals("Has previous version with ModState 4 or 7", getGradeFailedFilterDescription(grade1));
    }

    @Test
    public void testHasPreviousApprovedGrade() {
        long oldGrade = createShopGrade(123L, 123L, "Просто какой-то текст, который уже заапрувлен");
        dbGradeAdminService.moderateGradeReplies(List.of(oldGrade), 1L, ModState.APPROVED);


        long grade1 = createShopGrade(123L, 123L, "Просто какой-то текст, который должен был бы зааппрувиться");
        processor.process();

        checkGradeModState(ModState.APPROVED, grade1);
    }

    @Test
    public void testClusterFilter() {
        Long grade1 = createShopGrade("Просто какой-то грейд, которого нет в кластере");
        Long grade2 = createShopGrade("Просто какой-то грейд, который есть в кластере");
        pgJdbcTemplate.execute("INSERT INTO cluster_grade_color VALUES (" + grade2 + ", 123)");
        Long grade3 = createShopGrade("Просто какой-то грейд, который есть в кластере со значением -1");
        pgJdbcTemplate.execute("INSERT INTO cluster_grade_color VALUES (" + grade3 + ", -1)");

        processor.process();

        checkGradeModState(ModState.APPROVED, grade1);
        checkGradeModState(ModState.AUTOMATICALLY_REJECTED, grade2);
        checkGradeModState(ModState.APPROVED, grade3);
    }

    @Test
    public void testFeedbackGrades() {
        ShopGrade grade1 = GradeCreator.constructShopGradeNoText(
            GradeCreator.rndShop(), GradeCreator.rndUid(), ModState.UNREADY);
        grade1.setText("некий текст для модерации, проверим, что там.");

        ShopGrade grade2 = GradeCreator.constructShopGradeNoText(
            GradeCreator.rndShop(), GradeCreator.rndUid(), ModState.UNREADY);
        grade2.setText("другой текст для модерации, чтобы пройти фильтр, отзыв из фидбека");
        grade2.setSource(GradeSource.FEEDBACK.value());

        Long gradeId1 = createShopGrade(grade1);
        Long gradeId2 = createShopGrade(grade2);

        processor.process();
        checkGradeModState(ModState.READY, gradeId1);
        checkGradeModState(ModState.APPROVED, gradeId2);
    }

    @Test
    public void testSwitchedOffFeedbackGradesProcessing() {

        configurationService.mergeValue(FEEDBACK_GRADES_BATCH_SIZE_KEY, 0L);

        ShopGrade grade1 = GradeCreator.constructShopGradeNoText(
            GradeCreator.rndShop(), GradeCreator.rndUid(), ModState.READY);
        grade1.setText("некий текст для модерации, проверим, что там.");
        grade1.setAverageGrade(GradeValue.BAD.toAvgGrade());

        ShopGrade grade2 = GradeCreator.constructShopGradeNoText(
            GradeCreator.rndShop(), GradeCreator.rndUid(), ModState.UNMODERATED);

        grade2.setText(
            "другой текст для модерации, чтобы пройти фильтр, отзыв из фидбека, но с отключенным автоаппрувом");
        grade2.setAverageGrade(GradeValue.BAD.toAvgGrade());
        grade2.setSource(GradeSource.FEEDBACK.value());

        Long gradeId1 = createShopGrade(grade1);
        Long gradeId2 = createShopGrade(grade2);

        processor.process();
        checkGradeModState(ModState.READY, gradeId1);
        checkGradeModState(ModState.UNREADY, gradeId2);
    }

    @Test
    public void testExpDisableFlags() {
        long gradeId = createShopGrade(AUTHOR_ID, SHOP_ID, NORMAL_TEXT);

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
        long gradeId = createShopGrade(AUTHOR_ID, SHOP_ID, TEXT_TO_READY);
        long gradeIdNonCpa = createShopGrade(AUTHOR_ID + 1, SHOP_ID, TEXT_TO_READY);

        verifiedGradeService.setCpaInDB(List.of(gradeId), true);
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
        long gradeId = createShopGrade(AUTHOR_ID, SHOP_ID, NORMAL_TEXT);

        processor.setExpFlag(AbstractAutomoderation.EXP_POS_MANUAL_ONLY, true);

        processor.process();
        assertEquals(ModState.READY, getGradeModState(gradeId));

        processor.setExpFlag(AbstractAutomoderation.EXP_POS_MANUAL_ONLY, false);
        processor.process();
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testExpNonCpaManual() {
        long gradeId = createShopGrade(AUTHOR_ID, SHOP_ID, NORMAL_TEXT);
        long gradeIdNonCpa = createShopGrade(AUTHOR_ID + 1, SHOP_ID, NORMAL_TEXT);

        verifiedGradeService.setCpaInDB(List.of(gradeId), true);
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
    public void testModTimeExecution() {
        long gradeId = createShopGrade(NORMAL_TEXT);

        processor.setNeedDelayPositive(true);
        processor.process();

        //after this process positive filter not run, because mod_time is not enough
        assertEquals(ModState.UNREADY, getGradeModState(gradeId));

        makeModerationOlder(gradeId);
        processor.process();

        //now mod_time is enough for positive filter
        assertEquals(ModState.APPROVED, getGradeModState(gradeId));
    }

    @Test
    public void testNotConvertModStateFromReadyToReady() {
        long gradeId = createShopGrade(AUTHOR_ID, SHOP_ID, TEXT_TO_READY);

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
        long gradeId = createShopGrade(NORMAL_TEXT);
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
    public void testSpecifixRegexp() {
        Long gradeId = createShopGrade("просто прекрасный текст отзыва, если бы не экстремисты из facebook");
        processor.process();

        checkGradeModState(ModState.AUTOMATICALLY_REJECTED, gradeId);
        assertEquals("Contains specifix regexp", getGradeFailedFilterDescription(gradeId));
    }

    @Test
    public void testModerateShopGradesWithOrderFilter() {
        expFlagService.setFlag(EXP_ORDER_OR_CPC_ONLY, true);
        prepareCpcBusinessMapping();

        long gradeId = createShopGrade(AUTHOR_ID, SHOP_ID, NORMAL_TEXT + " нормальный отзыв");
        long gradeIdWithoutText = createShopGrade(AUTHOR_ID + 4, SHOP_ID + 2, null);
        long gradeIdNonCpa = createShopGrade(AUTHOR_ID + 1, SHOP_ID, NORMAL_TEXT + " отзыв не СПА но и не СПС");
        long gradeIdCpc = createShopGrade(AUTHOR_ID + 1, 111L, NORMAL_TEXT + " отзыв СПС");

        verifiedGradeService.setCpaInDB(List.of(gradeId), true);
        verifiedGradeService.setCpaInDB(List.of(gradeIdNonCpa), false);
        verifiedGradeService.setCpaInDB(List.of(gradeIdWithoutText), false);
        verifiedGradeService.setCpaInDB(List.of(gradeIdCpc), false);

        processor.process();

        checkGradeModState(ModState.AUTOMATICALLY_REJECTED, gradeIdNonCpa);
        assertEquals(ModReason.WITHOUT_ORDER.forShop(), getGradeModReason(gradeIdNonCpa));
        checkGradeModState(ModState.AUTOMATICALLY_REJECTED, gradeIdWithoutText);
        assertEquals(ModReason.WITHOUT_ORDER.forShop(), getGradeModReason(gradeIdWithoutText));

        checkGradeModState(ModState.APPROVED, gradeId);
        checkGradeModState(ModState.APPROVED, gradeIdCpc);
    }

    @Test
    public void testUnpublishedGrades() {
        long gradeId = createShopGrade(AUTHOR_ID, SHOP_ID, NORMAL_TEXT + " для братков");

        expFlagService.setFlag(AbstractAutomoderation.EXP_DISABLE_AUTO_PUBLISH_SHOP_GRADE_KEY, true);
        verifiedGradeService.setCpaInDB(List.of(gradeId), true);
        processor.process();

        checkGradeModState(ModState.READY_TO_PUBLISH, gradeId);
        validateIndexingQueueIsEmpty(gradeId);
    }

    private void prepareCpcBusinessMapping() {
        pgJdbcTemplate.execute("insert into grade.ext_shop_business_id (shop_id, business_id, type) " +
            "values" +
            "(111, 121, 2)");
        pgJdbcTemplate.execute("insert into grade.ext_mbi_cpc_mapping " +
            "(business_id) values" +
            "(121)");
    }

    public void doTestRepeatedRejectedGrades(ModState rejectedModState) {
        List<ModReason> rejectedModReasons = Arrays.asList(
            ModReason.DIFFERENT_SHOP,
            ModReason.DUPLICATED,
            ModReason.SPAM,
            ModReason.NOT_RESTORED
        );

        //given:
        HashMap<Long, Long> rejectedGrades = new HashMap<>();
        List<Long> otherGradeIds = new ArrayList<>();
        int i = 0;
        for (ModReason modReason : getModReasonsForShops()) {
            i++;
            long prevGradeId = createShopGrade(AUTHOR_ID + i, SHOP_ID + i, COMMENT_TEXT + i);
            Long modReasonValue = modReason.forShop();
            dbGradeAdminService.moderateGradeReplies(
                singletonMap(prevGradeId, modReasonValue), prevGradeId,
                rejectedModState);
            long gradeId = createShopGrade(AUTHOR_ID + i, SHOP_ID + i, COMMENT_TEXT + i + i);
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

    private void checkNegativeObsceneAutoFilter(GradeValue value, ModState expectedState) {
        ShopGrade grade = GradeCreator.constructShopGradeRnd();
        grade.setPro("Какое-то количество текста, содержащее мат внутри");
        grade.setText("Немного текста, чтобы всё было хорошо с длиной");
        grade.setContra("И ешё чуть-чуть");
        grade.setModState(ModState.UNMODERATED);
        grade.setAverageGrade(value.toAvgGrade());
        long gradeId = createShopGrade(grade);

        makeGradeOlder(gradeId);
        mockCleanWebVerdict(gradeId, "true");

        processor.process();

        checkGradeModState(expectedState, gradeId);
        assertEquals(ModReason.RUDE.forShop(), getGradeModReason(gradeId));
        assertEquals("Contains obscene words, cw", getGradeFailedFilterDescription(gradeId));
    }

    private Long createShopGrade(String text) {
        return createShopGrade(text, GradeValue.EXCELLENT);
    }

    private long createShopGrade(long userId, long shopId, String comment) {
        ShopGrade grade = GradeCreator.constructShopGradeNoText(shopId, userId, ModState.UNMODERATED);
        grade.setText(comment);
        grade.setAverageGrade(GradeValue.GOOD.toAvgGrade());
        return createShopGrade(grade);
    }

    private Long createShopGrade(String text, GradeValue value) {
        ShopGrade grade = GradeCreator.constructShopGradeRnd();
        grade.setText(text);
        grade.setPro(null);
        grade.setContra(null);
        grade.setAverageGrade(value.toAvgGrade());
        grade.setModState(ModState.UNMODERATED);
        return createShopGrade(grade);
    }

    private Long createShopGrade(ShopGrade param) {
        Long gradeId = gradeCreator.createGrade(param);
        verifiedGradeService.setCpaInDB(List.of(gradeId), new Random().nextBoolean());
        makeGradeOlder(gradeId);
        return gradeId;
    }

    public Long getGradeModReason(long gradeId) {
        return pgJdbcTemplate.queryForList(
            "SELECT mod_reason FROM grade WHERE id =" + gradeId, Long.class)
            .stream()
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private void checkNegativeShortTextFilter(GradeValue value, ModState expectedState) {
        long gradeIdNoText = createShopGrade(null, value);
        long gradeIdShortText = createShopGrade("короткий", value);

        makeGradeOlder(gradeIdNoText);
        makeGradeOlder(gradeIdShortText);

        processor.process();

        checkGradeModState(ModState.AUTOMATICALLY_REJECTED, gradeIdNoText);
        assertNull(getGradeModReason(gradeIdNoText));

        checkGradeModState(expectedState, gradeIdShortText);

        String expected = "Text length less than " + (value.toGr0() > 0 ? "20" : "10") + " symbols";

        if (expectedState == ModState.AUTOMATICALLY_REJECTED) {
            assertEquals(ModReason.UNINFORMATIVE.forShop(), getGradeModReason(gradeIdShortText));
            assertEquals(expected, getGradeFailedFilterDescription(gradeIdShortText));
        } else {
            assertNull(getGradeModReason(gradeIdShortText));
        }
    }

    public static List<ModReason> getModReasonsForShops() {
        return Stream.of(ModReason.values()).filter(it -> it.forShop() != null).collect(Collectors.toList());
    }

}
