package ru.yandex.direct.utils;

import com.google.common.base.Strings;
import com.google.common.base.Utf8;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static ru.yandex.direct.utils.StringUtils.cutUtf8ToLength;

public class StringUtilsTest {
    @Test
    public void russianLettersAreCut() {
        String source = Strings.repeat("я", 40000);
        assertThat(cutUtf8ToLength(source, 40000).length(), lessThan(40000));
    }

    @Test
    public void englishLettersAreNotCut() {
        String source = Strings.repeat("z", 40000);
        assertThat(cutUtf8ToLength(source, 40000).length(), equalTo(40000));
    }

    @Test
    public void russianLettersBorderCaseAreCutCorrectly() {
        String source = Strings.repeat("я", 40000);
        assertThat(cutUtf8ToLength(source, 79999).length(), equalTo(39999));
    }

    @Test
    public void russianLettersMethodSatisfiesItsContract() {
        int maxLength = 23101;
        String source = Strings.repeat("я", 40000);
        String result = cutUtf8ToLength(source, maxLength);
        assertThat(Utf8.encodedLength(result), lessThanOrEqualTo(maxLength));
    }
}
