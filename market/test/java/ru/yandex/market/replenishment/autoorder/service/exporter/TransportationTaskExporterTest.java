package ru.yandex.market.replenishment.autoorder.service.exporter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.delivery.transport_manager.model.dto.StockKeepingUnitDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationTaskDto;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.client.TMClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
public class TransportationTaskExporterTest extends FunctionalTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2021, 3, 30, 10, 55);

    @Autowired
    private TransportationTaskExporter exporter;

    @Autowired
    private TMClient tmClient;

    @Before
    public void mockTime() {
        setTestTime(NOW);
    }

    @Test
    @DbUnitDataSet(before = "TransportationTaskExporterTest.before.csv",
            after = "TransportationTaskExporterTest.after.csv")
    public void testExport() throws Exception {
        ArgumentCaptor<TransportationTaskDto> taskCaptor = ArgumentCaptor.forClass(TransportationTaskDto.class);
        when(tmClient.createTransportationTask(taskCaptor.capture())).thenReturn(101L);

        exporter.export();

        TransportationTaskDto task = taskCaptor.getValue();
        assertNotNull(task);

        assertNotNull(task.getLogisticPointFrom());
        assertEquals(145L, task.getLogisticPointFrom().longValue());

        assertNotNull(task.getLogisticPointTo());
        assertEquals(147L, task.getLogisticPointTo().longValue());

        Long movementId = task.getExternalId();
        assertNotNull(movementId);
        assertEquals(1L, movementId.longValue());

        List<StockKeepingUnitDto> units = task.getRegister();
        assertNotNull(units);
        assertEquals(4, units.size());
        units.sort(Comparator.comparing(StockKeepingUnitDto::getSsku));

        StockKeepingUnitDto unit = units.get(0);
        assertNotNull(unit);

        assertEquals("000420.100510", unit.getSsku());
        assertEquals("465852", unit.getSupplierId());
        assertEquals("000420", unit.getRealSupplierId());
        assertEquals(10L, unit.getCount());

        unit = units.get(1);
        assertNotNull(unit);

        assertEquals("000420.100511", unit.getSsku());
        assertEquals("465852", unit.getSupplierId());
        assertEquals("000420", unit.getRealSupplierId());
        assertEquals(10L, unit.getCount());

        unit = units.get(2);
        assertNotNull(unit);

        assertEquals("100521", unit.getSsku());
        assertEquals("451", unit.getSupplierId());
        assertNull(unit.getRealSupplierId());
        assertEquals(15L, unit.getCount());

        unit = units.get(3);
        assertNotNull(unit);

        assertEquals("100522", unit.getSsku());
        assertEquals("451", unit.getSupplierId());
        assertNull(unit.getRealSupplierId());
        assertEquals(15L, unit.getCount());
    }
}
