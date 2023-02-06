package ru.yandex.market.partner.security.checker;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


import ru.yandex.market.core.security.BusinessUidable;
import ru.yandex.market.core.security.Campaignable;
import ru.yandex.market.core.security.DefaultBusinessUidable;
import ru.yandex.market.core.security.DefaultCampaignable;
import ru.yandex.market.security.model.Authority;

public class ByPercentOfObjectCheckerTest {

    private final ByPercentOfCampaignChecker byPercentOfCampaignChecker = new ByPercentOfCampaignChecker();
    private final ByPercentOfBusinessChecker byPercentOfBusinessChecker = new ByPercentOfBusinessChecker();

    private static Stream<Arguments> invalidAndBoundaryData() {
        return Stream.of(
                Arguments.of("-1", false), //отрицательный процент
                Arguments.of("0", false), //процент 0
                Arguments.of("20.4", false), //не целый процент
                Arguments.of("100", true), //процент 100
                Arguments.of("101", false), //процент больше 100
                Arguments.of("20,4", false) //токенов в параметрах больше 2 (запятая - разделитель токенов)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAndBoundaryData")
    public void testInvalidAndBoundaryData(String value, boolean expectedResult) {
        Campaignable campaignable = new DefaultCampaignable(1, 2, 3);
        Authority authority = new Authority("name", value + ",market-partner:html:business-payouts-frequency:get");
        boolean result = byPercentOfCampaignChecker.checkTyped(campaignable, authority);
        Assertions.assertEquals(expectedResult, result);
    }

    private static Stream<Arguments> data() {
        //взял 10 последних campaign id на момент написания теста
        return Stream.of(
                Arguments.of(26411196, "market-partner:html:business-payouts-frequency:get", false),
                Arguments.of(26411193, "market-partner:html:business-payouts-frequency:get", false),
                Arguments.of(26411192, "market-partner:html:business-payouts-frequency:get", false),
                Arguments.of(26411189, "market-partner:html:business-payouts-frequency:get", true),
                Arguments.of(26411182, "market-partner:html:business-payouts-frequency:get", false),
                Arguments.of(26411172, "market-partner:html:business-payouts-frequency:get", false),
                Arguments.of(26411161, "market-partner:html:business-payouts-frequency:get", false),
                Arguments.of(26411138, "market-partner:html:business-payouts-frequency:get", false),
                Arguments.of(26411137, "market-partner:html:business-payouts-frequency:get", false),
                Arguments.of(26411130, "market-partner:html:business-payouts-frequency:get", false)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testByPercentOfCampaignChecker(long campaignId, String salt, boolean expectedResult) {
        Campaignable campaignable = new DefaultCampaignable(campaignId, 2, 3);
        Authority authority = new Authority("name", "10," + salt);
        boolean result = byPercentOfCampaignChecker.checkTyped(campaignable, authority);
        Assertions.assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testByPercentOfBusinessChecker(long businessId, String salt, boolean expectedResult) {
        BusinessUidable businessUidable = new DefaultBusinessUidable(businessId, 2, 3);
        Authority authority = new Authority("name", "10," + salt);
        boolean result = byPercentOfBusinessChecker.checkTyped(businessUidable, authority);
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void stats() {
        Authority authority = new Authority("name", "10,market-partner:html:business-payouts-frequency:get");
        long count = IntStream.range(0, 1000000)
                .mapToObj(operand -> {
                    Campaignable campaignable = new DefaultCampaignable(operand, 2, 3);
                    return byPercentOfCampaignChecker.checkTyped(campaignable, authority);
                })
                .filter(result -> result)
                .count();
        Assertions.assertEquals(99950L, count);
    }

}
