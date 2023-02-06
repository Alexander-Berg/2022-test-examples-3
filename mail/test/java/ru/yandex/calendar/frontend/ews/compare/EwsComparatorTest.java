package ru.yandex.calendar.frontend.ews.compare;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class EwsComparatorTest {

    // For details, see: https://jira.yandex-team.ru/browse/CAL-3198
    @Test
    public void ignoreWhenPrefix() {
        Assert.A.equals("XYZ", EwsComparator.discardWhenPrefix("Сегодня в 05:30 \"XYZ\""));
        Assert.A.equals("XYZ", EwsComparator.discardWhenPrefix("Завтра в 07:30 \"XYZ\""));
        Assert.A.equals("XYZ", EwsComparator.discardWhenPrefix("04 мая в 10:00 \"XYZ\""));
        Assert.A.equals("Название", EwsComparator.discardWhenPrefix("30 апреля в 18:00 \"Название\""));

        Assert.A.equals("Название события", EwsComparator.discardWhenPrefix("Название события"));
        Assert.A.equals("ЗАВТРА в 07:30 \"XYZ\"", EwsComparator.discardWhenPrefix("ЗАВТРА в 07:30 \"XYZ\""));
        Assert.A.equals("Послезавтра в 07:30 \"XYZ\"", EwsComparator.discardWhenPrefix("Послезавтра в 07:30 \"XYZ\""));
        Assert.A.equals("Вчера в 07:30 \"XYZ\"", EwsComparator.discardWhenPrefix("Вчера в 07:30 \"XYZ\""));
    }

}
