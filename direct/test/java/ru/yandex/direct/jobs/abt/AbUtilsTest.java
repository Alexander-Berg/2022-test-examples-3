package ru.yandex.direct.jobs.abt;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
public class AbUtilsTest {
    @Test
    public void getPathesForNumberOfDays() {
        Date date = new GregorianCalendar(2020, Calendar.NOVEMBER, 1).getTime();
        String prefix = "prefix/";
        String tableName = "name";
        List<String> pathes = AbUtils.getPathesForNumberOfDays(date.toInstant(), 5, prefix, tableName);
        assertThat(pathes).isEqualTo(List.of(
                prefix + "2020-11-01/" + tableName,
                prefix + "2020-10-31/" + tableName,
                prefix + "2020-10-30/" + tableName,
                prefix + "2020-10-29/" + tableName,
                prefix + "2020-10-28/" + tableName));
    }
}
