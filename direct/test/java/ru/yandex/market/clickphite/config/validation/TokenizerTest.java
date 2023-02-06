package ru.yandex.market.clickphite.config.validation;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 27.12.16
 */
public class TokenizerTest {
    @Test
    public void baseCase() {
        Assert.assertEquals(
            Arrays.asList(
                new Token(TokenType.WORD, "count"),
                new Token(TokenType.BRACKET, "("),
                new Token(TokenType.BRACKET, ")")
            ),
            Tokenizer.getTokens("count()")
        );
    }

    @Test
    public void keywordIgnoreCase() throws Exception {
        Assert.assertEquals(
            Arrays.asList(
                new Token(TokenType.NUMBER, "1"),
                new Token(TokenType.KEYWORD, "OR"),
                new Token(TokenType.NUMBER, "1")
            ),
            Tokenizer.getTokens("1 OR 1")
        );
    }

    @Test
    public void complexCase() throws Exception {
        Assert.assertEquals(
            Arrays.asList(
                new Token(TokenType.BRACKET, "("),
                new Token(TokenType.WORD, "countIf"),
                new Token(TokenType.BRACKET, "("),
                new Token(TokenType.WORD, "http_code"),
                new Token(TokenType.OTHER, ">"),
                new Token(TokenType.OTHER, "="),
                new Token(TokenType.NUMBER, "500"),
                new Token(TokenType.KEYWORD, "and"),
                new Token(TokenType.WORD, "http_code"),
                new Token(TokenType.OTHER, "<"),
                new Token(TokenType.OTHER, "="),
                new Token(TokenType.NUMBER, "599"),
                new Token(TokenType.BRACKET, ")"),
                new Token(TokenType.OTHER, "/"),
                new Token(TokenType.WORD, "count"),
                new Token(TokenType.BRACKET, "("),
                new Token(TokenType.BRACKET, ")"),
                new Token(TokenType.OTHER, "*"),
                new Token(TokenType.NUMBER, "100"),
                new Token(TokenType.BRACKET, ")")
            ),
            Tokenizer.getTokens("(countIf(http_code >= 500 and http_code <= 599) / count() * 100)")
        );
    }
}