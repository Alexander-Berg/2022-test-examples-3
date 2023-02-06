package ru.yandex.direct.utils.text;

import com.google.common.base.CharMatcher;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringModifierTest {
    @Test
    public void makeReplacements_success() {
        StringModifier stringModifier = new StringModifier.Builder()
                .withCustomRule(StringUtils::normalizeSpace)
                .withRegexpReplaceAllRule("[abc]+", "-")
                .build();

        String actual = stringModifier.makeReplacements("10  cba 15 \t");
        assertThat(actual).isEqualTo("10 - 15");
    }

    @Test
    public void makeReplacements_keepOrder() {
        StringModifier stringModifierBeginWithRemoveLetter = new StringModifier.Builder()
                .withRegexpReplaceAllRule("[abc]+", "")
                .withCustomRule(StringUtils::normalizeSpace)
                .build();
        StringModifier stringModifierBeginWithNormalizeSpaces = new StringModifier.Builder()
                .withCustomRule(StringUtils::normalizeSpace)
                .withRegexpReplaceAllRule("[abc]+", "")
                .build();

        String text = "a b c";
        String actualFirst = stringModifierBeginWithRemoveLetter.makeReplacements(text);
        String actualSecond = stringModifierBeginWithNormalizeSpaces.makeReplacements(text);
        // проверяем лишь то, что порядок условий важен, и в нашем случае даёт разный результат
        assertThat(actualFirst).isNotEqualTo(actualSecond);
    }

    @Test
    public void collapseAnyOf_test() throws Exception {
        StringModifier modifier = new StringModifier.Builder()
                .withCollapseAnyOf(" \t\n", ' ')
                .build();
        assertThat(modifier.makeReplacements(" asdf \n\t asdf \n"))
                .isEqualTo(" asdf asdf ");
    }

    @Test
    public void collapseCharMatcher_test() throws Exception {
        StringModifier modifier = new StringModifier.Builder()
                .withCharMatcherCollapse(CharMatcher.whitespace(), ' ')
                .build();
        assertThat(modifier.makeReplacements(" asdf \n\r asdf \n"))
                .isEqualTo(" asdf asdf ");
    }

    @Test
    public void removeAnyOf_test() throws Exception {
        StringModifier modifier = new StringModifier.Builder()
                .withRemoveAnyOf(" \t\n")
                .build();
        assertThat(modifier.makeReplacements(" asdf \n\t asdf \n"))
                .isEqualTo("asdfasdf");
    }

    @Test
    public void removeCharMatcher_test() throws Exception {
        StringModifier modifier = new StringModifier.Builder()
                .withCharMatcherRemove(CharMatcher.whitespace())
                .build();
        assertThat(modifier.makeReplacements(" asdf \n\t asdf \n"))
                .isEqualTo("asdfasdf");
    }

    @Test
    public void builderCanBeUsedMoreThanOnce() throws Exception {
        StringModifier.Builder builder = new StringModifier.Builder()
                .withCharMatcherRemove(CharMatcher.whitespace());

        StringModifier m1 = builder.build();
        StringModifier m2 = builder.withRemoveAnyOf("a").build();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(m1.makeReplacements(" asdf \n\t asdf \n"))
                .isEqualTo("asdfasdf");
        soft.assertThat(m2.makeReplacements(" asdf \n\t asdf \n"))
                .isEqualTo("sdfsdf");
        soft.assertAll();
    }
}
