package ru.yandex.market.clickphite.config.validation;

import java.util.regex.Pattern;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 27.12.16
 */
public class TokenPattern {
    private final TokenType type;
    private final Pattern pattern;

    public TokenPattern(TokenType type, Pattern pattern) {
        this.type = type;
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public TokenType getType() {
        return type;
    }
}
