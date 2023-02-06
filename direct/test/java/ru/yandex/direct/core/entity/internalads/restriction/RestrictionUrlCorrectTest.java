package ru.yandex.direct.core.entity.internalads.restriction;

import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;

public class RestrictionUrlCorrectTest {
    private static final RestrictionUrlCorrect CHECK_CORRECT_URL = new RestrictionUrlCorrect(true, false);
    private static final RestrictionUrlCorrect CHECK_CORRECT_URL_HTTPS_ONLY = new RestrictionUrlCorrect(true, true);

    @Test
    public void check_NullInput_NullResult() {
        Defect result = CHECK_CORRECT_URL.check(null);
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectUrl_NullResult() {
        Defect result = CHECK_CORRECT_URL.check("http://yandex.ru");
        assertThat(result).isNull();
    }

    @Test
    public void check_CorrectUrlWithHttpsRequired_NullResult() {
        Defect result = CHECK_CORRECT_URL_HTTPS_ONLY.check("https://yandex.ru");
        assertThat(result).isNull();
    }

    @Test
    public void check_EmptyString_DefectResult() {
        Defect result = CHECK_CORRECT_URL.check("");
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(InternalAdRestrictionDefects.Url.URL_NOT_VALID, new InternalAdRestrictionDefects.UrlCorrectParam(false));
    }

    @Test
    public void check_UrlWithoutScheme_DefectResult() {
        Defect result = CHECK_CORRECT_URL.check("yandex.ru");
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(InternalAdRestrictionDefects.Url.URL_NOT_VALID, new InternalAdRestrictionDefects.UrlCorrectParam(false));
    }

    @Test
    public void check_CorrectUrlWithHttpButHttpsIsRequired_DefectResult() {
        Defect result = CHECK_CORRECT_URL_HTTPS_ONLY.check("http://yandex.ru");
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(InternalAdRestrictionDefects.Url.URL_NOT_VALID, new InternalAdRestrictionDefects.UrlCorrectParam(true));
    }

    @Test
    public void check_IncorrectUrl_DefectResult() {
        Defect result = CHECK_CORRECT_URL.check("blablabla");
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId, Defect::params)
                .containsExactly(InternalAdRestrictionDefects.Url.URL_NOT_VALID, new InternalAdRestrictionDefects.UrlCorrectParam(false));
    }

    @Test
    public void check_IncorrectUrlWithSpace_DefectResult() {
        Defect result = CHECK_CORRECT_URL.check("http://y andex.ru");
        assertThat(result).isNotNull();
        assertThat(result)
                .extracting(Defect::defectId)
                .isEqualTo(InternalAdRestrictionDefects.UrlWithSpaces.URL_HAS_SPACES);
    }


}
