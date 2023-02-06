package ru.yandex.direct.core.entity.internalads.restriction;

import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;

public class RestrictionRegexpTest {
    private static final RestrictionRegexp CHECK_CORRECT_COUNTER = new RestrictionRegexp(true, "^[0-9a-zA-Z_-]*$");

    @Test
    public void check_NullCounter_NullResult() {
        Defect result = CHECK_CORRECT_COUNTER.check(null);
        assertThat(result).isNull();
    }

    @Test
    public void check_ValidCounter_NullResult() {
        Defect result = CHECK_CORRECT_COUNTER.check("sdfDfdF123123-_sdfs24");
        assertThat(result).isNull();
    }

    @Test
    public void check_CounterWithInvalidSymbol_DefectResult() {
        Defect result = CHECK_CORRECT_COUNTER.check("453$%#%&");
        assertThat(result)
                .isNotNull()
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(InternalAdRestrictionDefects.Gen.FORMAT_INVALID, null);
    }
}
