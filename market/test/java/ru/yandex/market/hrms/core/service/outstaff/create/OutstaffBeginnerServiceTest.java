package ru.yandex.market.hrms.core.service.outstaff.create;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.outstaff.OutstaffBeginnerShiftEnd;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffBeginnerService;
import ru.yandex.market.hrms.core.service.wms.WmsUserStateManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OutstaffBeginnerServiceTest extends AbstractCoreTest {

    @Autowired
    private OutstaffBeginnerService service;

    @MockBean
    private WmsUserStateManager wmsUserStateManager;

    @Captor
    private ArgumentCaptor<Map<Long, List<OutstaffBeginnerShiftEnd>>> argumentCaptor;

    @Test
    @DbUnitDataSet(
            before = "OutstaffBeginnerServiceTest.beginner_last_shift.before.csv",
            after = "OutstaffBeginnerServiceTest.beginner_last_shift.after.csv"
    )
    public void populateBeginnerLastShiftEnd() {
        service.populateBeginnerLastShiftEnd();
    }

    @Test
    @DbUnitDataSet(
            before = "OutstaffBeginnerServiceTest.beginner_last_shift_send_to_wms.before.csv"
    )
    public void sendBeginnerLastShiftEndToWms() {
        service.populateBeginnerLastShiftEnd();
        verify(wmsUserStateManager, times(1))
                .updateOutstaffBeginnerEndTime(argumentCaptor.capture());
        Map<Long, List<OutstaffBeginnerShiftEnd>> result = argumentCaptor.getValue();
        assertEquals(1, result.size(), "domains size");
        assertEquals(1, result.get(1L).size(), "beginners size");
        assertEquals("sof-test6", result.get(1L).get(0).getWmsLogin(), "wms login");
    }
}
