package ru.yandex.travel.api.services.orders;

import org.junit.Test;

import ru.yandex.travel.api.services.hotels_booking_flow.models.CheckinCheckoutTime;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckinTimeTest {
    @Test
    public void testSameDay24h() {
        var res = CheckinCheckoutTime.fromStrings("12:00", "23:00");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("12:00");
        assertThat(res.getEnds()).isEqualTo("23:00");
        assertThat(res.isOvernight()).isFalse();
    }

    @Test
    public void testSameDay24hFromMorning() {
        var res = CheckinCheckoutTime.fromStrings("7:00", "23:00");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("07:00");
        assertThat(res.getEnds()).isEqualTo("23:00");
        assertThat(res.isOvernight()).isFalse();
    }

    @Test
    public void testMidnight24h() {
        var res = CheckinCheckoutTime.fromStrings("14:00", "0:00");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("14:00");
        assertThat(res.getEnds()).isEqualTo("00:00");
        assertThat(res.isOvernight()).isTrue();
    }

    @Test
    public void testNextDay24h() {
        var res = CheckinCheckoutTime.fromStrings("14:00", "4:00");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("14:00");
        assertThat(res.getEnds()).isEqualTo("04:00");
        assertThat(res.isOvernight()).isTrue();
    }

    @Test
    public void testSameDay12h() {
        var res = CheckinCheckoutTime.fromStrings("12:00 pm", "11:00 pm");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("12:00");
        assertThat(res.getEnds()).isEqualTo("23:00");
        assertThat(res.isOvernight()).isFalse();
    }

    @Test
    public void testSameDay12hFromMorning() {
        var res = CheckinCheckoutTime.fromStrings("7:00 am", "11:00 pm");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("07:00");
        assertThat(res.getEnds()).isEqualTo("23:00");
        assertThat(res.isOvernight()).isFalse();
    }

    @Test
    public void testMidnight12h() {
        var res = CheckinCheckoutTime.fromStrings("2:00 pm", "12:00 am");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("14:00");
        assertThat(res.getEnds()).isEqualTo("00:00");
        assertThat(res.isOvernight()).isTrue();
    }

    @Test
    public void testNextDay12h() {
        var res = CheckinCheckoutTime.fromStrings("2:00 pm", "4:00 am");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("14:00");
        assertThat(res.getEnds()).isEqualTo("04:00");
        assertThat(res.isOvernight()).isTrue();
    }

    @Test
    public void testNoMinutes() {
        var res = CheckinCheckoutTime.fromStrings("1", "10");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("01:00");
        assertThat(res.getEnds()).isEqualTo("10:00");
        assertThat(res.isOvernight()).isFalse();
    }

    @Test
    public void testNullAtMixedFormats() {
        var res = CheckinCheckoutTime.fromStrings("2:00 pm", "13:00");
        assertThat(res).isNull();
    }

    @Test
    public void testNullAtMixedFormatsIndistinguishable() {
        var res = CheckinCheckoutTime.fromStrings("2:00 pm", "8:00");
        assertThat(res).isNull();
    }

    @Test
    public void test12hPlusMidnight() {
        var res = CheckinCheckoutTime.fromStrings("2:00 pm", "полночь");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("14:00");
        assertThat(res.getEnds()).isEqualTo("00:00");
        assertThat(res.isOvernight()).isTrue();
    }

    @Test
    public void test24hPlusMidnight() {
        var res = CheckinCheckoutTime.fromStrings("14:00", "полночь");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("14:00");
        assertThat(res.getEnds()).isEqualTo("00:00");
        assertThat(res.isOvernight()).isTrue();
    }

    @Test
    public void testMiddayMidnight() {
        var res = CheckinCheckoutTime.fromStrings("полдень", "полночь");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("12:00");
        assertThat(res.getEnds()).isEqualTo("00:00");
        assertThat(res.isOvernight()).isTrue();
    }

    @Test
    public void testMiddayPlus12h() {
        var res = CheckinCheckoutTime.fromStrings("полдень", "8:00 pm");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("12:00");
        assertThat(res.getEnds()).isEqualTo("20:00");
        assertThat(res.isOvernight()).isFalse();
    }

    @Test
    public void testNulls() {
        assertThat(CheckinCheckoutTime.fromStrings("12:00", null)).isNull();
        assertThat(CheckinCheckoutTime.fromStrings(null, "12:00")).isNull();
        assertThat(CheckinCheckoutTime.fromStrings(null, null)).isNull();
    }

    @Test
    public void testFromSingleString() {
        var res = CheckinCheckoutTime.fromString("полдень");
        assertThat(res).isNotNull();
        assertThat(res.getBegins()).isEqualTo("12:00");
        assertThat(res.getEnds()).isEqualTo("12:00");
        assertThat(res.isOvernight()).isTrue();
    }
}
