package ru.yandex.market.tsum.tms.tasks.duty.callstatsduty;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.core.duty.DutyManager;
import ru.yandex.market.tsum.tms.tasks.duty.switchduty.SwitchDutyTaskTestConfig;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = SwitchDutyTaskTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class CallStatsDutyUtilsTest {

    @Autowired
    public DutyManager dutyManager;

    @Test
    public void renderSqlForWholeDay() throws IOException {
        String minusTwoDay = LocalDateTime.ofInstant((new Date()).toInstant(), ZoneId.of("Europe/Moscow"))
            .minusDays(2)
            .toLocalDate()
            .toString();

        String expection = "use hahn;\n" +
            "\n" +
            "$method = \"phone_escalation\";\n" +
            "$input_range = (SELECT * from LIKE(`statbox/juggler-banshee-log`, \"" + minusTwoDay + "\"));\n" +
            "\n" +
            "--------------------------------------------------\n" +
            "$date_format = DateTime::Format(\"%Y-%m-%d\");\n" +
            "$date_parse = DateTime::Parse(\"%Y-%m-%d %H:%M:%S\");\n" +
            "\n" +
            "SELECT allDates.iso_eventtime, count(phoneCallDates.login)\n" +
            "FROM (\n" +
            "        SELECT DISTINCT $date_format(DateTime::MakeDatetime($date_parse(iso_eventtime))) as " +
            "iso_eventtime\n" +
            "        FROM $input_range\n" +
            "        WHERE method = $method\n" +
            "            AND message like 'Call%'\n" +
            "    ) as allDates\n" +
            "    LEFT JOIN (\n" +
            "        SELECT $date_format(DateTime::MakeDatetime($date_parse(iso_eventtime))) as iso_eventtime, " +
            "login\n" +
            "        FROM $input_range\n" +
            "        WHERE method = $method\n" +
            "            AND login IN (\"vasya01\",\"vasya02\")\n" +
            "            AND message like 'Call%'\n" +
            "    ) as phoneCallDates\n" +
            "    ON allDates.iso_eventtime == phoneCallDates.iso_eventtime\n" +
            "GROUP BY allDates.iso_eventtime as iso_eventtime";

        List<String> logins = List.of("vasya01", "vasya02");
        Mockito.when(dutyManager.getStaffLoginsByAbcDuty(Mockito.any())).thenReturn(logins);
        String rendered = CallStatsDutyUtils.renderSqlForWholeDay(dutyManager, "test");
        assertEquals(expection, rendered);
    }

    @Test
    public void renderSql() throws IOException {
        String minusTwoDay = LocalDateTime.ofInstant((new Date()).toInstant(), ZoneId.of("Europe/Moscow"))
            .minusDays(2)
            .toLocalDate()
            .toString();

        String expection = "use hahn;\n" +
            "\n" +
            "$method = \"phone_escalation\";\n" +
            "$input_range = (SELECT * from LIKE(`statbox/juggler-banshee-log`, \"" + minusTwoDay + "\"));\n" +
            "\n" +
            "--------------------------------------------------\n" +
            "$date_format = DateTime::Format(\"%Y-%m-%d\");\n" +
            "$date_parse = DateTime::Parse(\"%Y-%m-%d %H:%M:%S\");\n" +
            "\n" +
            "SELECT allDates.iso_eventtime, count(phoneCallDates.login)\n" +
            "FROM (\n" +
            "        SELECT DISTINCT $date_format(DateTime::MakeDatetime($date_parse(iso_eventtime))) as " +
            "iso_eventtime\n" +
            "        FROM $input_range\n" +
            "        WHERE method = $method\n" +
            "            AND message like 'Call%'\n" +
            "    ) as allDates\n" +
            "    LEFT JOIN (\n" +
            "        SELECT $date_format(DateTime::MakeDatetime($date_parse(iso_eventtime))) as iso_eventtime, " +
            "login\n" +
            "        FROM $input_range\n" +
            "        WHERE method = $method\n" +
            "            AND login IN (\"vasya01\",\"vasya02\")\n" +
            "            AND message like 'Call%'\n" +
            "            AND (DateTime::GetHour($date_parse(iso_eventtime)) > 22\n" +
            "                OR DateTime::GetHour($date_parse(iso_eventtime)) < 10)\n" +
            "    ) as phoneCallDates\n" +
            "    ON allDates.iso_eventtime == phoneCallDates.iso_eventtime\n" +
            "GROUP BY allDates.iso_eventtime as iso_eventtime";

        List<String> logins = List.of("vasya01", "vasya02");
        Mockito.when(dutyManager.getStaffLoginsByAbcDuty(Mockito.any())).thenReturn(logins);
        String rendered = CallStatsDutyUtils.renderSql(dutyManager, "test", true, "10", "22");
        assertEquals(expection, rendered);
    }

    @Test
    public void buildTskv() {
        String exception1 = "tskv\tdate=2020-01-01\tcount=99\tnight=0";
        CallStatsTskv callStatsTskv1 = new CallStatsTskv("2020-01-01", 99, false);
        assertEquals(exception1, CallStatsDutyUtils.buildTskv(callStatsTskv1));

        String exception2 = "tskv\tdate=2020-01-02\tcount=9\tnight=1";
        CallStatsTskv callStatsTskv2 = new CallStatsTskv("2020-01-02", 9, true);
        assertEquals(exception2, CallStatsDutyUtils.buildTskv(callStatsTskv2));
    }
}
