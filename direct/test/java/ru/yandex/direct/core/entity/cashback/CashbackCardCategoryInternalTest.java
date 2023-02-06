package ru.yandex.direct.core.entity.cashback;

import java.math.BigDecimal;
import java.util.List;

import one.util.streamex.StreamEx;
import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.cashback.model.CashbackCardCategoryInternal;
import ru.yandex.direct.core.entity.cashback.model.CashbackCardProgramInternal;
import ru.yandex.direct.core.entity.cashback.model.CashbackCardsProgram;
import ru.yandex.direct.core.entity.cashback.model.CashbackCategory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CashbackCardCategoryInternalTest {
    private CashbackCardsProgram p1;
    private CashbackCardsProgram p2;
    private CashbackCardsProgram p3;
    private CashbackCardsProgram p4;
    private CashbackCardCategoryInternal category;
    private CashbackCategory dbCategoryData;
    private final String defaultLanguage = "RU";

    @Before
    public void init() {
        p1 = new CashbackCardsProgram()
                .withId(1L)
                .withIsNew(false)
                .withIsGeneral(true)
                .withIsTechnical(false)
                .withPercent(BigDecimal.valueOf(0.1))
                .withNameRu("программа")
                .withNameEn("program")
                .withTooltipInfoRu("информация в окне")
                .withTooltipInfoEn("tooltip info")
                .withTooltipLinkTextRu("текст ссылки в окне")
                .withTooltipLinkTextEn("tooltip link text");
        p2 = new CashbackCardsProgram()
                .withId(2L)
                .withIsNew(false)
                .withIsGeneral(true)
                .withIsTechnical(false)
                .withPercent(BigDecimal.valueOf(0.2));
        p3 = new CashbackCardsProgram()
                .withId(3L)
                .withIsNew(false)
                .withIsGeneral(true)
                .withIsTechnical(false)
                .withPercent(BigDecimal.valueOf(0.3));
        p4 = new CashbackCardsProgram()
                .withId(4L)
                .withIsNew(false)
                .withIsGeneral(true)
                .withIsTechnical(false)
                .withPercent(BigDecimal.valueOf(0.25));
        dbCategoryData = new CashbackCategory()
                .withNameRu("категория")
                .withNameEn("category")
                .withDescriptionRu("описание категории")
                .withDescriptionEn("category description")
                .withButtonTextRu("настроить кампании")
                .withButtonTextEn("manage campaigns");
        category = new CashbackCardCategoryInternal(dbCategoryData, defaultLanguage);
    }

    @Test
    public void testCalculateMaxPercent() {
        category.addProgram(new CashbackCardProgramInternal(p1, 3L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p2, 1L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p3, 2L, defaultLanguage));
        BigDecimal actual = category.calculateMaxPercent();
        BigDecimal expected = BigDecimal.valueOf(0.6);
        assertThat(actual.equals(expected)).isTrue();
    }

    @Test
    public void testCalculateMaxPercentWithEqualsOrder() {
        category.addProgram(new CashbackCardProgramInternal(p1, 2L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p2, 1L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p3, 1L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p4, 2L, defaultLanguage));
        BigDecimal actual = category.calculateMaxPercent();
        BigDecimal expected = BigDecimal.valueOf(0.55);
        assertThat(actual).isCloseTo(expected, Percentage.withPercentage(0.01));
    }

    @Test
    public void testCalculateIsGeneralOnGeneralPrograms() {
        category.addProgram(new CashbackCardProgramInternal(p1, 1L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p2, 2L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p3, 3L, defaultLanguage));
        boolean actual = category.calculateIsGeneral();
        assertThat(actual).isTrue();
    }

    @Test
    public void testCalculateIsGeneralOnPersonalProgram() {
        category.addProgram(new CashbackCardProgramInternal(p1, 1L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p2.withIsGeneral(false), 2L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p3, 3L, defaultLanguage));
        boolean actual = category.calculateIsGeneral();
        assertThat(actual).isFalse();
    }

    @Test
    public void testCalculateIsNewOnOldPrograms() {
        category.addProgram(new CashbackCardProgramInternal(p1, 1L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p2, 2L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p3, 3L, defaultLanguage));
        boolean actual = category.calculateIsNew();
        assertThat(actual).isFalse();
    }

    @Test
    public void testCalculateIsNewOnNewPrograms() {
        category.addProgram(new CashbackCardProgramInternal(p1.withIsNew(true), 1L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p2, 2L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p3, 3L, defaultLanguage));
        boolean actual = category.calculateIsNew();
        assertThat(actual).isTrue();
    }

    @Test
    public void testCalculateIsTechnicalOnNotTechnicalPrograms() {
        category.addProgram(new CashbackCardProgramInternal(p1, 1L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p2, 2L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p3, 3L, defaultLanguage));
        boolean actual = category.calculateIsTechnical();
        assertThat(actual).isFalse();
    }

    @Test
    public void testCalculateIsTechnicalOnTechnicalPrograms() {
        category.addProgram(new CashbackCardProgramInternal(p1, 1L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p2.withIsTechnical(true), 2L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p3, 3L, defaultLanguage));
        boolean actual = category.calculateIsTechnical();
        assertThat(actual).isTrue();
    }

    @Test
    public void testProgramsSortedByOrder() {
        category.addProgram(new CashbackCardProgramInternal(p1, 2L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p2, 1L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p3, 3L, defaultLanguage));
        category.addProgram(new CashbackCardProgramInternal(p4, 2L, defaultLanguage));
        category.prepareCategory();
        List<Long> actual = StreamEx.of(category.getPrograms()).map(CashbackCardProgramInternal::getProgramId).toList();
        List<Long> expected = List.of(2L, 1L, 4L, 3L);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testSelectLanguageRU() {
        String language = "RU";
        category = new CashbackCardCategoryInternal(dbCategoryData, language);
        category.addProgram(new CashbackCardProgramInternal(p1, 1L, language));
        assertThat(category.getName()).isEqualTo("категория");
        assertThat(category.getDescription()).isEqualTo("описание категории");
        assertThat(category.getButtonText()).isEqualTo("настроить кампании");
        assertThat(category.getPrograms().get(0).getName()).isEqualTo("программа");
        assertThat(category.getPrograms().get(0).getTooltipInfo()).isEqualTo("информация в окне");
        assertThat(category.getPrograms().get(0).getTooltipLinkText()).isEqualTo("текст ссылки в окне");
    }

    @Test
    public void testSelectLanguageEN() {
        String language = "EN";
        category = new CashbackCardCategoryInternal(dbCategoryData, language);
        category.addProgram(new CashbackCardProgramInternal(p1, 1L, language));
        assertThat(category.getName()).isEqualTo("category");
        assertThat(category.getDescription()).isEqualTo("category description");
        assertThat(category.getButtonText()).isEqualTo("manage campaigns");
        assertThat(category.getPrograms().get(0).getName()).isEqualTo("program");
        assertThat(category.getPrograms().get(0).getTooltipInfo()).isEqualTo("tooltip info");
        assertThat(category.getPrograms().get(0).getTooltipLinkText()).isEqualTo("tooltip link text");
    }

    @Test
    public void testSelectLanguageUnsupported() {
        String language = "KZ";
        assertThatThrownBy(() -> {
            CashbackCardCategoryInternal cat = new CashbackCardCategoryInternal(dbCategoryData, language);
        }).isInstanceOf(IllegalArgumentException.class).hasMessage("Unsupported language KZ");
    }
}
