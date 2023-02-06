package ru.yandex.direct.bshistory;

import java.math.BigInteger;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

class HistoryUtilsTest {

    @Test
    void convertHistoryForBannerTest() {
        String expected = "B13379847,40981572,B2028558,40981572";
        History history = History.parse("P40981572;16107453:13379847,2028558");
        String bannerHistory = HistoryUtils.convertHistoryForBanner(history);
        assertThat("get correct banner history", bannerHistory, Matchers.equalTo(expected));
    }

    @Test
    void convertHistoryForBannerTestWithEmptyHistory() {
        History history = History.parse("");
        String bannerHistory = HistoryUtils.convertHistoryForBanner(history);
        assertThat("get correct banner history", bannerHistory, Matchers.equalTo(null));
    }

    @Test
    void convertHistoryForBannerTestWithHistoryWithSkippedBannerId() {
        String phraseIdHistory =
                "O17208788;G2947883827;P1668811195,1668811195;:4871622287;4814709126:4871870749;im:4871622288";
        History history = History.parse(phraseIdHistory);
        String bannerHistory = HistoryUtils.convertHistoryForBanner(history);

        // ожидаемое значение взято из референсной perl-реализации
        String expected = "B4871870749,1668811195,B4871870749,1668811195";
        assertThat("get correct banner history", bannerHistory, Matchers.equalTo(expected));
    }

    @Test
    void convertHistoryForAdGroupTest() {
        String expected = "G1509031,40981572,G1509031,5587828,G1509032,40981572,G1509032,5587828";
        History history = History.parse("G1509031;P40981572,5587828");
        String adGroupHistory = HistoryUtils.convertHistoryForAdGroup(history, 1509032);
        assertThat("get correct adgroup history", adGroupHistory, Matchers.equalTo(expected));
    }

    @Test
    void convertHistoryForAdGroupTestWithEmptyHistory() {
        History history = History.parse("");
        String adGroupHistory = HistoryUtils.convertHistoryForAdGroup(history, 1509032);
        assertThat("get correct adgroup history", adGroupHistory, Matchers.equalTo(null));
    }

    @Test
    void convertHistoryForAdGroupTestWithHistoryWithoutAdGroupId() {
        History history = History.parse("O6577522;P965861848;1277537924:903984361;1726412063:1263295983");
        String adGroupHistory = HistoryUtils
                .convertHistoryForAdGroup(history, 10101);
        assertThat("get correct adgroup history", adGroupHistory, Matchers.equalTo("G10101,965861848"));
    }

    @Test
    void convertHistoryForAdGroupTestWithHistoryWithSkippedBannerId() {
        String phraseIdHistory =
                "O17208788;G2947883827;P1668811195,1668811195;:4871622287;4814709126:4871870749;im:4871622288";
        History history = History.parse(phraseIdHistory);
        String adGroupHistory = HistoryUtils.convertHistoryForAdGroup(history, 1234567);

        String expected = "G2947883827,1668811195,G2947883827,1668811195,G1234567,1668811195,G1234567,1668811195";
        assertThat("get correct adgroup history", adGroupHistory, Matchers.equalTo(expected));
    }

    @Test
    void prependHistoryForBannerTest() {
        String expected = "B2028558,40981572,B13379847,40981572";
        String bannerHistory = HistoryUtils.prependHistoryForBanner(
                "B13379847,40981572", 2028558L, BigInteger.valueOf(40981572L));
        assertThat("get correct banner history", bannerHistory, Matchers.equalTo(expected));
    }

    @Test
    void prependHistoryForBannerTestWithEmptyHistory() {
        String expected = "B2028558,40981572";
        String bannerHistory = HistoryUtils.prependHistoryForBanner(
                "", 2028558L, BigInteger.valueOf(40981572L));
        assertThat("get correct banner history", bannerHistory, Matchers.equalTo(expected));
    }

    @Test
    void prependHistoryForAdGroupTest() {
        String expected = "G1509031,40981572,G1509031,5587828";
        String adGroupHistory = HistoryUtils.prependHistoryForAdGroup(
                "G1509031,5587828", 1509031L, BigInteger.valueOf(40981572L));
        assertThat("get correct adgroup history", adGroupHistory, Matchers.equalTo(expected));
    }

    @Test
    void prependHistoryForAdGroupTestWithEmptyHistory() {
        String expected = "G1509031,40981572";
        String adGroupHistory = HistoryUtils.prependHistoryForAdGroup(
                "", 1509031L, BigInteger.valueOf(40981572L));
        assertThat("get correct adgroup history", adGroupHistory, Matchers.equalTo(expected));
    }

}
