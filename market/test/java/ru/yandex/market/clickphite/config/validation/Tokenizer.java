package ru.yandex.market.clickphite.config.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 27.12.16
 */
public class Tokenizer {

    private Tokenizer() {
    }

    public static List<Token> getTokens(String fieldExpression) {
        List<Token> tokens = new ArrayList<>();
        List<TokenPattern> patterns = Arrays.asList(
            new TokenPattern(TokenType.KEYWORD, Pattern.compile("^(and|or|not|like|between|in|global|case|when|then" +
                "|end|desc|asc)\\b", CASE_INSENSITIVE)),
            new TokenPattern(TokenType.NUMBER, Pattern.compile("^[0-9]+(e[0-9]+)?(\\.[0-9]+)?")),
            new TokenPattern(TokenType.WORD, Pattern.compile("^[a-zA-Z_0-9]+")),
            new TokenPattern(TokenType.BRACKET, Pattern.compile("^[)(]")),
            new TokenPattern(TokenType.OTHER, Pattern.compile("^."))
        );

        while (!fieldExpression.isEmpty()) {
            for (TokenPattern pattern : patterns) {
                Matcher matcher = pattern.getPattern().matcher(fieldExpression);
                if (matcher.find()) {
                    String group = matcher.group();
                    fieldExpression = fieldExpression.substring(group.length()).trim();
                    tokens.add(new Token(pattern.getType(), group));
                    break;
                }
            }
        }

        return tokens;
    }
}
