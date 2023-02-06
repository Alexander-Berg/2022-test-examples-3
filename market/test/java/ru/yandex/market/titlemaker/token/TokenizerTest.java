package ru.yandex.market.titlemaker.token;

import org.junit.Test;

import static org.junit.Assert.*;

public class TokenizerTest {
    @Test
    public void testTokenize() throws Exception {
        String string = "Galaxy S4-LTE";

        Tokenizer tokenizer = new Tokenizer();
        TokenizedString tokenizedString = tokenizer.tokenize(string);
    }
}