package ru.yandex.market.tsum.clients.staff;

import org.junit.Assert;
import org.junit.Test;

public class StaffQueryUtilsTest {

    @Test
    public void singleWordLatinQuery() throws Exception {
        String expected = "login==regex('user42','i') or name.first.en==regex('user42','i') or name.last.en==regex" +
            "('user42','i')";
        Assert.assertEquals(expected, StaffQueryUtils.patternToPersonQuery("user42"));
        Assert.assertEquals(expected, StaffQueryUtils.patternToPersonQuery("user42 "));
    }

    @Test
    public void singleWordCyrillicQuery() throws Exception {
        String expected = "name.first.ru==regex('Инфраструктрон','i') or name.last.ru==regex('Инфраструктрон','i')";
        Assert.assertEquals(expected, StaffQueryUtils.patternToPersonQuery("Инфраструктрон"));
    }

    @Test
    public void twoWordsLatinQuery() throws Exception {
        String expected = "(name.first.en==regex('Infra','i') and name.last.en==regex('Man','i')) or (name.first" +
            ".en==regex('Man','i') and name.last.en==regex('Infra','i'))";
        Assert.assertEquals(expected, StaffQueryUtils.patternToPersonQuery("Infra Man"));
    }

    @Test
    public void twoWordsCyrillicQuery() throws Exception {
        String expected = "(name.first.ru==regex('Инфраструктрон','i') and name.last.ru==regex('Маркетов','i')) or " +
            "(name.first.ru==regex('Маркетов','i') and name.last.ru==regex('Инфраструктрон','i'))";
        Assert.assertEquals(expected, StaffQueryUtils.patternToPersonQuery("Инфраструктрон Маркетов"));
    }


    @Test
    public void toRegexQueryParam() throws Exception {
        Assert.assertEquals("login==regex('loginValue','i')", StaffQueryUtils.toRegexFilter("login", "loginValue"));
    }
}
