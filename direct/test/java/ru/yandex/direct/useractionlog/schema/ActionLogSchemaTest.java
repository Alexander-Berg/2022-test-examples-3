package ru.yandex.direct.useractionlog.schema;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.TimeZone;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.tracing.data.DirectTraceInfo;
import ru.yandex.direct.useractionlog.ClientId;

import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class ActionLogSchemaTest {
    private static final TimeZone INITIAL_TIME_ZONE = TimeZone.getDefault();

    @After
    public void tearDown() {
        TimeZone.setDefault(INITIAL_TIME_ZONE);
    }

    /**
     * LocalDateTime и LocalDate не содержат информацию о часовом поясе.
     * java.sql.Date и java.sql.Timestamp содержат такую информацию. Неявное преобразование из Local* в java.sql
     * может привести к неправильным датам, отправляемым в БД.
     * Тест проверяет, что toSqlObject в ActionLogSchema переводит в sql-типы те же даты, что в ActionLogRecord, с
     * установленным часовым поясом UTC.
     */
    @Parameters({
            "Antarctica/Troll",
            "Antarctica/Vostok",
            "Asia/Novosibirsk",
            MOSCOW_TIMEZONE,
            "UTC",
    })
    @Test
    public void javaSqlTimeZones(String timeZoneName) {
        final long unixTimestamp20180101 = 1514764800L;
        for (int hour = 0; hour < 24; ++hour) {
            for (int minute = 0; minute < 60; minute += 5) {
                TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
                Date expectedDate = new Date(unixTimestamp20180101 * 1000L);
                Timestamp expectedTimestamp =
                        new Timestamp((unixTimestamp20180101 + hour * 3600L + 60L * minute) * 1000L);
                TimeZone.setDefault(TimeZone.getTimeZone(timeZoneName));

                ActionLogRecord record = ActionLogRecord.builder()
                        .withDateTime(LocalDateTime.of(2018, 1, 1, hour, minute))
                        .withDb("ignored")
                        .withDirectTraceInfo(DirectTraceInfo.empty())
                        .withGtid("ignored:0")
                        .withNewFields(FieldValueList.empty())
                        .withOldFields(FieldValueList.empty())
                        .withOperation(Operation.INSERT)
                        .withPath(new ObjectPath.ClientPath(new ClientId(0)))
                        .withQuerySerial(0)
                        .withRecordSource(RecordSource.makeDaemonRecordSource())
                        .withRowSerial(0)
                        .withType("ignored")
                        .build();
                Assertions.assertThat(ActionLogSchema.DATE.getType().toSqlObject(record.getDateTime().toLocalDate()))
                        .describedAs(
                                "2018-01-01 %02d:%02d:00 should be converted to sql date as UTC", hour, minute)
                        .isEqualTo(expectedDate);
                Assertions.assertThat(ActionLogSchema.DATETIME.getType().toSqlObject(record.getDateTime()))
                        .describedAs(
                                "2018-01-01 %02d:%02d:00 should be converted to sql timestamp as UTC", hour, minute)
                        .isEqualTo(expectedTimestamp);
            }
        }
    }
}
