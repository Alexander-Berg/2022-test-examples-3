package ru.yandex.direct.core.entity.internalads.restriction;

import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;

public class RestrictionTextLengthMinTest {
    private static final RestrictionTextLengthMin CHECK_LENGTH_MIN_ONE = new RestrictionTextLengthMin(true, 1);

    @Test
    public void check_NullInput_NullResult() {
        Defect result = CHECK_LENGTH_MIN_ONE.check(null);
        assertThat(result).isNull();
    }

    @Test
    public void check_EmptyString_DefectResult() {
        Defect result = CHECK_LENGTH_MIN_ONE.check("");
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(InternalAdRestrictionDefects.Length.TEXT_TOO_SHORT,
                        new InternalAdRestrictionDefects.TextLengthParam(1, null));
    }

    @Test
    public void check_EmptyStringWithSpecialChars_NullResult() {
        var restrictionTextLengthMin = new RestrictionTextLengthMin(true, 2);
        Defect result = restrictionTextLengthMin.check("<br>&nbsp;");
        assertThat(result).isNull();
    }

    @Test
    public void check_EmptyStringWithAllowedTags_NullResult() {
        Defect result = CHECK_LENGTH_MIN_ONE.check("<strong><br><login></strong>");
        assertThat(result).isNull();
    }

    @Test
    public void check_EmptyStringWithSpecialChars_DefectResult() {
        var restrictionTextLengthMin = new RestrictionTextLengthMin(true, 3);
        Defect result = restrictionTextLengthMin.check("<br>&nbsp;");
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(InternalAdRestrictionDefects.Length.TEXT_TOO_SHORT,
                        new InternalAdRestrictionDefects.TextLengthParam(restrictionTextLengthMin.getLength(), null));
    }

    @Test
    public void check_ValidLengthString_NullResult() {
        Defect result = CHECK_LENGTH_MIN_ONE.check("a");
        assertThat(result).isNull();
    }

    @Test
    public void check_StringStartsWithSpace_DefectResult() {
        Defect result = CHECK_LENGTH_MIN_ONE.check(" a");
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId)
                .isEqualTo(InternalAdRestrictionDefects.Text.TEXT_HAS_SPACES_AROUND);
    }

    @Test
    public void check_StringEndsWithSpace_DefectResult() {
        Defect result = CHECK_LENGTH_MIN_ONE.check("a ");
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId)
                .isEqualTo(InternalAdRestrictionDefects.Text.TEXT_HAS_SPACES_AROUND);
    }
}
