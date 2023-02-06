package ru.yandex.market.hrms.tms.manager.outstaff;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.OutstaffCalendarItemManager;

@DbUnitDataSet(schema = "public", before = "OutstaffCalendarItemTest.before.csv")
public class OutstaffCalendarItemTest extends AbstractTmsTest {
    @Autowired
    private OutstaffCalendarItemManager outstaffCalendarItemManager;

    @Test
    @DbUnitDataSet(schema = "public", after = "OutstaffCalendarItemTest.after.csv")
    void shouldPreprocessSof() {
        mockClock(LocalDate.of(2021, 8, 5));

        outstaffCalendarItemManager.processData();
    }
}
