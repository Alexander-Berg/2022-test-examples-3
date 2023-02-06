package ru.yandex.direct.core.entity.internalads.restriction;

import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;

public class RestrictionTextLengthMaxTest {
    private static final RestrictionTextLengthMax CHECK_LENGTH_MAX_ONE = new RestrictionTextLengthMax(true, 1);

    @Test
    public void check_NullInput_NullResult() {
        Defect result = CHECK_LENGTH_MAX_ONE.check(null);
        assertThat(result).isNull();
    }

    @Test
    public void check_EmptyString_NullResult() {
        Defect result = CHECK_LENGTH_MAX_ONE.check("");
        assertThat(result).isNull();
    }

    @Test
    public void check_ValidLengthString_NullResult() {
        Defect result = CHECK_LENGTH_MAX_ONE.check("a");
        assertThat(result).isNull();
    }

    @Test
    public void check_ValidLengthStringWithSpecialChars_NullResult() {
        Defect result = CHECK_LENGTH_MAX_ONE.check("&nbsp;");
        assertThat(result).isNull();
    }

    @Test
    public void check_ValidLengthStringWithSpecialChars_DefectResult() {
        Defect result = CHECK_LENGTH_MAX_ONE.check("<br>&nbsp;");
        assertThat(result).isNotNull();
    }

    @Test
    public void check_ValidLengthStringWithAllowedTags_NullResult() {
        Defect result = CHECK_LENGTH_MAX_ONE.check("<strong><br><login></strong>");
        assertThat(result).isNull();
    }

    @Test
    public void check_TooLongString_DefectResult() {
        Defect result = CHECK_LENGTH_MAX_ONE.check("aa");
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(InternalAdRestrictionDefects.Length.TEXT_TOO_LONG,
                        new InternalAdRestrictionDefects.TextLengthParam(null, 1));
    }

    @Test
    public void check_StringStartsWithSpace_DefectResult() {
        Defect result = CHECK_LENGTH_MAX_ONE.check(" a");
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId)
                .isEqualTo(InternalAdRestrictionDefects.Text.TEXT_HAS_SPACES_AROUND);
    }

    @Test
    public void check_StringEndsWithSpace_DefectResult() {
        Defect result = CHECK_LENGTH_MAX_ONE.check("a ");
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId)
                .isEqualTo(InternalAdRestrictionDefects.Text.TEXT_HAS_SPACES_AROUND);
    }
}
