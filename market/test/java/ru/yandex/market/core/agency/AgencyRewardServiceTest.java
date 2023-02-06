package ru.yandex.market.core.agency;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.agency.config.RewardQuarterConfig;
import ru.yandex.market.core.agency.program.quarter.Quarter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link AgencyRewardService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = "csv/AgencyRewardServiceTest.before.csv")
class AgencyRewardServiceTest extends FunctionalTest {
    private static final Quarter FIRST_QUARTER = Quarter.of(2017, 1);
    private static final Quarter SECOND_QUARTER = Quarter.of(2017, 2);
    private static final Quarter QUARTER_2021_1 = Quarter.of(2021, 1);

    @Autowired
    private AgencyRewardService agencyRewardService;

    private static Stream<Arguments> testGetSummaryData() {
        return Stream.of(
                Arguments.of(
                        "Нет итоговой премии. Мало качества и активности",
                        11L,
                        FIRST_QUARTER,
                        0.6842105263157895,
                        0.8078947368421052,
                        null
                ),
                Arguments.of(
                        "Нет итоговой премии. Мало качества",
                        33L,
                        FIRST_QUARTER,
                        0.6842105263157895,
                        0.95,
                        null
                ),
                Arguments.of(
                        "Нет итоговой премии. Мало активности",
                        44L,
                        FIRST_QUARTER,
                        0.8421052631578947,
                        0.8078947368421052,
                        null
                ),
                Arguments.of(
                        "Есть итоговая",
                        55L,
                        FIRST_QUARTER,
                        0.8421052631578947,
                        0.95,
                        3.58
                )
        );
    }

    private static Stream<Arguments> testGetRewardSettingsData() {
        return Stream.of(
                Arguments.of(
                        "Нет такой настройки. Плохая дата у квартала",
                        Quarter.of(2013, 1),
                        null
                ),
                Arguments.of(
                        "Первый конфиг. Середина",
                        Quarter.of(2016, 3),
                        RewardQuarterConfig.REWARD_SETTINGS.get(0)
                ),
                Arguments.of(
                        "Первый конфиг. Конец",
                        Quarter.of(2015, 2),
                        RewardQuarterConfig.REWARD_SETTINGS.get(0)
                ),
                Arguments.of(
                        "Второй конфиг. Начало",
                        Quarter.of(2019, 2),
                        RewardQuarterConfig.REWARD_SETTINGS.get(1)
                ),
                Arguments.of(
                        "Второй конфиг. Середина",
                        Quarter.of(2019, 3),
                        RewardQuarterConfig.REWARD_SETTINGS.get(1)
                )
        );
    }

    private static void checkRatio(Double expected, Double actual) {
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testGetSummaryData")
    @DisplayName("Получение коэффициентов для агентства")
    @DbUnitDataSet(before = "csv/AgencyRewardService.get_summary.before.csv")
    void testGetSummary(String name, long agencyId, Quarter quarter, double expectedQuality, double expectedActivity, Double expectedTotal) {
        AgencyRewardSummary actual = agencyRewardService.getRewardSummary(agencyId, quarter);
        assertThat(actual).isNotNull();
        checkRatio(expectedQuality, actual.getQualityRatio());
        checkRatio(expectedActivity, actual.getActivityRatio());
        checkRatio(expectedTotal, actual.getTotalRatio());
    }

    @Test
    @DisplayName("Получение коэффициентов для агентства без данных")
    void testGetSummaryWithoutData() {
        AgencyRewardSummary withoutData = agencyRewardService.getRewardSummary(9999L, SECOND_QUARTER);
        assertThat(withoutData).isNull();
    }

    @Test
    @DisplayName("Генерация xlsx-отчета агентства без данных")
    @DbUnitDataSet(before = "csv/AgencyRewardService.xlsx.only_quarter.before.csv")
    void testEmptyQuarterXlsxReport() throws Exception {
        checkXlsx(11L, FIRST_QUARTER, "empty_report.csv");
        checkXlsx(11L, SECOND_QUARTER, "empty_report.csv");
        checkXlsx(9999L, FIRST_QUARTER, "empty_report.csv");
        checkXlsx(9999L, SECOND_QUARTER, "empty_report.csv");
    }

    @Test
    @DisplayName("Генерация xlsx-отчета агентства только с текущим кварталом")
    @DbUnitDataSet(before = "csv/AgencyRewardService.xlsx.current_quarter_data.before.csv")
    void testCurrentQuarterXlsxReport() throws Exception {
        checkXlsx(11L, FIRST_QUARTER, "only_current.csv");
    }

    @Test
    @DisplayName("Генерация xlsx-отчета агентства. Нет итогового коэффициента в текущем квартале")
    @DbUnitDataSet(before = "csv/AgencyRewardService.xlsx.current_without_total.before.csv")
    void testCurrentQuarterWithoutTotalXlsxReport() throws Exception {
        checkXlsx(11L, FIRST_QUARTER, "only_current_without_total.csv");
    }

    @Test
    @DisplayName("Генерация xlsx-отчета агентства. С фильтрацией предыдущего, тк еще не прошел месяц")
    @DbUnitDataSet(before = {
            "csv/AgencyRewardService.xlsx.current_quarter_data.before.csv",
            "csv/AgencyRewardService.xlsx.without_prev.before.csv"
    })
    void testWithoutPrevQuarterXlsxReport() throws Exception {
        checkXlsx(11L, FIRST_QUARTER, "only_current.csv");
    }

    @Test
    @DisplayName("Генерация xlsx-отчета агентства. И текущий квартал, и прошлые")
    @DbUnitDataSet(before = {
            "csv/AgencyRewardService.xlsx.current_quarter_data.before.csv",
            "csv/AgencyRewardService.xlsx.with_prev.before.csv"
    })
    void testWithPrevQuarterXlsxReport() throws Exception {
        checkXlsx(11L, FIRST_QUARTER, "with_prev.csv");
    }

    @Test
    @DisplayName("Генерация xlsx-отчета агентства для кварталов после 2021Q1")
    @DbUnitDataSet(before = "csv/AgencyRewardService.xlsx.2021_1_data.before.csv")
    void test2021Q1XlsxReport() throws Exception {
        checkXlsx(11L, QUARTER_2021_1, "2021Q1.csv");
    }

    @Test
    @DisplayName("Настройки расчета упорядочены по fromDate")
    void testAgencyRewardSettingsOrder() {
        List<AgencyRewardSettings> rewardSettings = RewardQuarterConfig.REWARD_SETTINGS;
        assertThat(rewardSettings).isNotEmpty();

        for (int i = 1; i < rewardSettings.size(); ++i) {
            AgencyRewardSettings prev = rewardSettings.get(i - 1);
            AgencyRewardSettings cur = rewardSettings.get(i);
            assertThat(prev.getFromQuarter().isBefore(cur.getFromQuarter())).isTrue();
        }
    }

    @Test
    @DisplayName("Настройки расчета не дублируются для одного квартала")
    void testAgencyRewardSettingsUnique() {
        Set<Quarter> dates = RewardQuarterConfig.REWARD_SETTINGS.stream()
                .map(AgencyRewardSettings::getFromQuarter)
                .collect(Collectors.toSet());

        assertThat(RewardQuarterConfig.REWARD_SETTINGS).hasSameSizeAs(dates);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testGetRewardSettingsData")
    @DisplayName("Получение настроек расчета премии")
    @DbUnitDataSet(before = "csv/AgencyRewardService.get_reward_settings.before.csv")
    void testGetRewardSettings(String name, Quarter quarter, AgencyRewardSettings expected) {
        AgencyRewardSettings rewardSettings = agencyRewardService.getRewardSettings(quarter);
        assertThat(rewardSettings).isEqualTo(expected);
    }

    private void checkXlsx(long agencyId, Quarter quarter, String expectedFile) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        agencyRewardService.getXlsReport(agencyId, quarter, os);
        AgencyRewardXlsxReportTestUtil.checkXlsx(os, expectedFile, getClass());
    }
}
