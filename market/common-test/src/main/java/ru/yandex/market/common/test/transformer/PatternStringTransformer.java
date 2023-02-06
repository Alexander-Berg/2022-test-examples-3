package ru.yandex.market.common.test.transformer;

import org.intellij.lang.annotations.Language;

import java.util.regex.Pattern;

import ru.yandex.market.common.test.transformer.StringTransformer;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class PatternStringTransformer implements StringTransformer {

    private final Pattern pattern;
    private final String replacement;

    public PatternStringTransformer(@Language("RegExp") String pattern, String replacement) {
        this.pattern = Pattern.compile(pattern,
                Pattern.MULTILINE |
                        Pattern.CASE_INSENSITIVE |
                        Pattern.UNICODE_CASE |
                        Pattern.DOTALL);
        this.replacement = replacement;
    }

    @Override
    public String transform(String string) {
        if (string == null) {
            throw new IllegalArgumentException("Input string is null");
        }
        return pattern.matcher(string).replaceAll(replacement);
    }
}
