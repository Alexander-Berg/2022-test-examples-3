package ru.yandex.market.crm.core.domain.trigger.variables.receipt;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author vtarasoff
 * @since 14.04.2022
 */
public class ReceiptsTableTest {
    @Test
    public void shouldReturnNotSentIds() {
        var table = new ReceiptsTable(Map.of(
                1L, new ReceiptEvent(1),
                2L, new ReceiptEvent(2),
                3L, new ReceiptEvent(3).setSent(true)
        ));

        Set<Long> ids = table.getNotSentIds();
        assertEquals(Set.of(1L, 2L), ids);
    }

    @Test
    public void shouldAddOnlyNewIds() {
        var table = new ReceiptsTable(Map.of(
                1L, new ReceiptEvent(1),
                2L, new ReceiptEvent(2).setSent(true)
        ));

        table.addIds(Set.of(2L, 3L));

        Set<Long> ids = table.getNotSentIds();
        assertEquals(Set.of(1L, 3L), ids);
    }

    @Test
    public void shouldAddOnlyNewId() {
        var table = new ReceiptsTable(Map.of(
                1L, new ReceiptEvent(1),
                2L, new ReceiptEvent(2).setSent(true)
        ));

        table.addIds(Set.of(2L));

        Set<Long> ids = table.getNotSentIds();
        assertEquals(Set.of(1L), ids);

        table.addIds(Set.of(3L));

        ids = table.getNotSentIds();
        assertEquals(Set.of(1L, 3L), ids);
    }

    @Test
    public void shouldMarkAllSent() {
        var table = new ReceiptsTable(Map.of(
                1L, new ReceiptEvent(1),
                2L, new ReceiptEvent(2),
                3L, new ReceiptEvent(3).setSent(true)
        ));

        table.markSentAll();

        Set<Long> ids = table.getNotSentIds();
        assertTrue(ids.isEmpty());
    }

    @Test
    public void shouldBeCheckedOnEmpty() {
        var table = new ReceiptsTable();
        assertTrue(table.isEmpty());

        table.addId(1);
        assertFalse(table.isEmpty());
    }
}
