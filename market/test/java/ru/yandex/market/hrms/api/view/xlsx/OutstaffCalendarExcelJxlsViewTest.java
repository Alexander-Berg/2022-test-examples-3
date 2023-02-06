package ru.yandex.market.hrms.api.view.xlsx;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Map;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.AbstractRandomizer;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ru.yandex.market.hrms.model.outstaff.excel.OutstaffCalendarExcel;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

class OutstaffCalendarExcelJxlsViewTest {

    private static final int SEED = 1;
    private OutstaffCalendarExcelJxlsView view;
    private EnhancedRandom random;

    @BeforeEach
    void setUp() {
        view = new OutstaffCalendarExcelJxlsView();
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .seed(SEED)
                .randomize(LocalDateInterval.class, new AbstractRandomizer<LocalDateInterval>(SEED) {
                    @Override
                    public LocalDateInterval getRandomValue() {
                        var start = LocalDate.EPOCH.plusDays(random.nextInt(1000));
                        var end = start.plusDays(random.nextInt(100));
                        return new LocalDateInterval(start, end);
                    }
                })
                .build();
    }

    @Test
    void testRandom() throws Exception {
        var excel = random.nextObject(OutstaffCalendarExcel.class);

        var response = new MockHttpServletResponse();
        view.render(Map.of("a", excel), new MockHttpServletRequest(), response);

        // check, that you can open workbook
        var workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()));
        workbook.close();
    }
}
