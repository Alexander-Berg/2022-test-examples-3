package ru.yandex.market.rg.agency;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.agency.report.AgencyCheckerReportData;
import ru.yandex.market.core.agency.report.XlsxAgencyCheckerReportGenerator;
import ru.yandex.market.rg.config.ClickhouseFunctionalTest;
import ru.yandex.market.rg.config.FunctionalTest;

import static ru.yandex.market.core.agency.AgencyRewardXlsxReportTestUtil.checkXlsx;

/**
 * Функциональный тесты для {@link XlsxAgencyCheckerReportGenerator}.
 * Ходят за данными в кх.
 */
class XlsxAgencyCheckerReportGeneratorTest extends ClickhouseFunctionalTest {

    private static final String CLICK_HOUSE = "clickHouseDataSource";
    private static final long AGENCY_ID = 2233719L;

    @Autowired
    private AgencyCheckerReportDao agencyCheckerReportDao;

    @Nonnull
    private static ByteArrayOutputStream getActualDataStream(final XlsxAgencyCheckerReportGenerator generator) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        generator.generate(os);
        return os;
    }

    @Test
    @DisplayName("Проверяем кейс, когда данные есть.")
    @DbUnitDataSet(dataSource = CLICK_HOUSE, before = "csv/AgencyCheckerReportDaoTest.getDataTest.before.csv")
    void test() throws IOException {
        final LocalDate month = LocalDate.of(2020, 1, 1);
        List<AgencyCheckerReportData> reportData = agencyCheckerReportDao.getReportData(AGENCY_ID, month);
        final XlsxAgencyCheckerReportGenerator generator = new XlsxAgencyCheckerReportGenerator(reportData);
        final ByteArrayOutputStream actualDataStream = getActualDataStream(generator);
        checkXlsx(actualDataStream, "checker.csv", getClass());
    }

    @Test
    @DisplayName("Проверяем кейс, когда данных по агенству нет.")
    void testEmpty() throws IOException {
        final LocalDate month = LocalDate.of(2020, 1, 1);
        List<AgencyCheckerReportData> reportData = agencyCheckerReportDao.getReportData(-1, month);
        final XlsxAgencyCheckerReportGenerator generator = new XlsxAgencyCheckerReportGenerator(reportData);
        final ByteArrayOutputStream actualDataStream = getActualDataStream(generator);
        checkXlsx(actualDataStream, "empty_checker.csv", getClass());
    }


}
