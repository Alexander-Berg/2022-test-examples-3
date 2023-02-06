package ru.yandex.market.logistics.nesu.jobs;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.executor.YtUpdateDropoffDayoffExecutor;
import ru.yandex.market.logistics.nesu.utils.CommonsConstants;

import static org.mockito.Mockito.doReturn;

@DatabaseSetup("/jobs/executors/update_dropoff_dayoff/before/availabilities.xml")
@DisplayName("Выгрузка дэйоффов и выходных дропшипов из yt.")
class YtUpdateDropoffDayoffExecutorTest extends AbstractContextualTest {
    private static final String PARSE_DAY_TEMPLATE = "$parse_day = DateTime::Parse('%Y-%m-%dT%H:%M:%SZ'); ";
    private static final String SELECT_DAYS_TEMPLATE = ""
        + "$dropships_lps = "
        + "    SELECT "
        + "        lp.id AS id, "
        + "        lp.calendar_id AS calendar_id, "
        + "        lp.market_calendar_id AS market_calendar_id, "
        + "        lp.dayoff_calendar AS dayoff_calendar, "
        + "        lp.partner_id AS partner_id"
        + "    FROM `logistics_point_path` AS lp"
        + "    INNER JOIN `logistic_point_availability_path` AS lpa "
        + "        ON lpa.logistic_point_id = lp.id "
        + "    WHERE lpa.partner_type = 'DROPSHIP' "
        + "      AND lpa.enabled = true; "
        + "$lp_merged_calendars = "
        + "    SELECT lp.id AS id, calendar.id AS c_id, calendar.parent_id AS cp_id "
        + "    FROM $dropships_lps AS lp "
        + "    JOIN `calendar_path` AS calendar "
        + "        ON calendar.id = lp.calendar_id "
        + "    UNION ALL "
        + "    SELECT lp.id AS id, calendar.id AS c_id, calendar.parent_id AS cp_id "
        + "    FROM $dropships_lps AS lp "
        + "    JOIN `calendar_path` AS calendar "
        + "        ON calendar.id = lp.market_calendar_id "
        + "    UNION ALL "
        + "    SELECT lp.id AS id, calendar.id AS c_id, calendar.parent_id AS cp_id "
        + "    FROM $dropships_lps AS lp "
        + "    JOIN `calendar_path` AS calendar "
        + "        ON calendar.id = lp.dayoff_calendar; "
        + "$partner_calendars = "
        + "    SELECT p.id AS id, p.calendar_id AS calendar_id, calendar.parent_id AS parent_id "
        + "    FROM `partner_path` AS p "
        + "    INNER JOIN `calendar_path` AS calendar "
        + "        ON p.calendar_id = calendar.id; "
        + "$partner_days_from_partner_calendars = "
        + "    SELECT pc.id AS partner_id, cd.day AS day "
        + "    FROM $partner_calendars AS pc "
        + "    INNER JOIN `calendar_day_path` AS cd "
        + "        ON cd.calendar_id = pc.calendar_id "
        + "    WHERE cd.day IS NOT NULL "
        + "      AND cd.is_holiday = true "
        + "    UNION ALL "
        + "    SELECT pc.id AS partner_id, cd.day AS day "
        + "    FROM $partner_calendars AS pc "
        + "    INNER JOIN `calendar_day_path` AS cd "
        + "        ON cd.calendar_id = pc.parent_id "
        + "    WHERE cd.day IS NOT NULL "
        + "      AND cd.is_holiday = true; "
        + "$partner_days_from_partner_capacity_dayoff = "
        + "    SELECT p.id AS partner_id, pcdo.day AS day "
        + "    FROM `partner_path` AS p "
        + "    INNER JOIN `partner_capacity_path` AS pc "
        + "        ON p.id = pc.partner_id "
        + "    INNER JOIN `partner_capacity_day_off_path` AS pcdo "
        + "        ON pc.id = pcdo.capacity_id "
        + "    WHERE pcdo.day IS NOT NULL; "
        + "$partner_days = "
        + "    SELECT * FROM $partner_days_from_partner_calendars "
        + "    UNION ALL "
        + "    SELECT * FROM $partner_days_from_partner_capacity_dayoff; "
        + "$lp_days_by_partner_id = "
        + "    SELECT lp.id AS id, CAST(DateTime::MakeDatetime($parse_day(pd.day)) AS Date) AS day "
        + "    FROM $dropships_lps AS lp "
        + "    INNER JOIN $partner_days AS pd "
        + "        ON lp.partner_id = pd.partner_id; "
        + "$lp_days = "
        + "    SELECT lpc.id AS id, CAST(DateTime::MakeDatetime($parse_day(cd.day)) AS Date) AS day "
        + "    FROM ( "
        + "        SELECT lpmc.id AS id, lpmc.c_id AS calendar "
        + "        FROM $lp_merged_calendars AS lpmc "
        + "        UNION ALL "
        + "        SELECT lpmc.id AS id, lpmc.cp_id AS calendar "
        + "        FROM $lp_merged_calendars AS lpmc"
        + "    ) AS lpc "
        + "    INNER JOIN `calendar_day_path` AS cd "
        + "        ON lpc.calendar = cd.calendar_id "
        + "    WHERE cd.day IS NOT NULL "
        + "      AND cd.is_holiday = true; "
        + "SELECT DISTINCT id, CAST(day AS String) as day "
        + "FROM ( "
        + "    SELECT * "
        + "    FROM $lp_days_by_partner_id AS lpd_bp "
        + "    UNION ALL "
        + "    SELECT * "
        + "    FROM $lp_days AS lpd"
        + ") "
        + "WHERE day >= CurrentUtcDate() "
        + "  AND day <= CurrentUtcDate() + DateTime::IntervalFromDays(6)";

    private static final String YQL_QUERY = PARSE_DAY_TEMPLATE + SELECT_DAYS_TEMPLATE;

    @Autowired
    private JdbcTemplate yqlJdbcTemplate;

    @Autowired
    private YtUpdateDropoffDayoffExecutor ytUpdateDropoffDayoffExecutor;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-11-22T17:00:00Z"), CommonsConstants.MSK_TIME_ZONE);
    }

    @Test
    @DisplayName("Успешное обновление дэйоффов")
    @ExpectedDatabase(
        value = "/jobs/executors/update_dropoff_dayoff/after/yt_non_empty.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateDayoffsSuccess() {
        doReturn(List.of(
            Map.of(
                "id", 1000L,
                "day", "2022-01-01".getBytes()
            ),
            Map.of(
                "id", 2000L,
                "day", "2022-01-02".getBytes()
            ),
            Map.of(
                "id", 3000L,
                "day", "2022-01-02".getBytes()
            ),
            Map.of(
                "id", 4000L,
                "day", "2022-01-03".getBytes()
            )
        ))
            .when(yqlJdbcTemplate).queryForList(YQL_QUERY);
        ytUpdateDropoffDayoffExecutor.doJob(null);
    }

    @Test
    @DisplayName("Пустая выдача из yt, но дни до сегодня все равно удаляются.")
    @ExpectedDatabase(
        value = "/jobs/executors/update_dropoff_dayoff/after/yt_empty.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateDayoffsYtEmpty() {
        doReturn(List.of()).when(yqlJdbcTemplate).queryForList(YQL_QUERY);
        ytUpdateDropoffDayoffExecutor.doJob(null);
    }
}
