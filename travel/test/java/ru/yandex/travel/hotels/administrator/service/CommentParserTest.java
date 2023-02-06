package ru.yandex.travel.hotels.administrator.service;

import org.junit.Assert;
import org.junit.Test;

public class CommentParserTest {

    private final CommentParser commentParser = new CommentParser();

    @Test
    public void validTest() {
        CommentParser.BalanceData balanceData = commentParser.extractBalanceDataFromComment("clientId: 123; contractId: 321");
        Assert.assertEquals(123L, balanceData.getClientId().longValue());
        Assert.assertEquals(321, balanceData.getContractId().longValue());
    }

    @Test
    public void validIgnoreCaseTest() {
        CommentParser.BalanceData balanceData = commentParser.extractBalanceDataFromComment("CLIENTID: 123; CONTRACTID: 321");
        Assert.assertEquals(123L, balanceData.getClientId().longValue());
        Assert.assertEquals(321, balanceData.getContractId().longValue());
    }

    @Test
    public void validIgnorePrefixSuffixTest() {
        CommentParser.BalanceData balanceData = commentParser.extractBalanceDataFromComment("prefix clientId: 123 ; contractId: 321; suffix");
        Assert.assertEquals(123L, balanceData.getClientId().longValue());
        Assert.assertEquals(321, balanceData.getContractId().longValue());
    }

    @Test
    public void invalidStringTest() {
        try {
            commentParser.extractBalanceDataFromComment("invalidField: 123 invalidField2: 321");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void invalidClientIdTest() {
        try {
            commentParser.extractBalanceDataFromComment("clientId: 123123123123123123123123123123123; contractId: 321");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof NumberFormatException);
        }
    }
}
