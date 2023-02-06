package ru.yandex.market.common.test.transformer;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Строковый трансформер, который позволяет преобразовывать строки с применением лямбда-функции к шаблону.
 */
public class PatternFunctionalStringTransformer implements StringTransformer {

    private final Pattern pattern;
    private final Function<Matcher, String> replacer;

    public PatternFunctionalStringTransformer(final String pattern,
                                              final Function<Matcher, String> replacer) {
        this.pattern = Pattern.compile(pattern);
        this.replacer = replacer;
    }

    @Override
    public String transform(String string) {
        Matcher matcher = pattern.matcher(string);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, replacer.apply(matcher));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
