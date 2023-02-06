package ru.yandex.market.delivery.transport_manager.converter.prefix;


import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.exception.IdWithPrefixCouldNotBeConvertedException;

class IdPrefixConverterTest {
    private final IdPrefixConverter idPrefixConverter = new IdPrefixConverter();

    @Test
    void testConvertWithoutPrefix() {
        Assertions.assertEquals(idPrefixConverter.getIdWithoutPrefix("TM100"), 100L);
        Assertions.assertEquals(idPrefixConverter.getIdWithoutPrefix("TMU1001"), 1001L);
        Assertions.assertEquals(idPrefixConverter.getIdWithoutPrefix("TMM10012"), 10012L);
        Assertions.assertEquals(idPrefixConverter.getIdWithoutPrefix("TMR100123"), 100123L);
        Assertions.assertEquals(idPrefixConverter.getIdWithoutPrefix("SOMETHING1001234"), 1001234L);
    }

    @Test
    void testFindWithoutPrefix() {
        Assertions.assertEquals(
            idPrefixConverter.findIdWithoutPrefix("TM100", ResourceIdType.TRANSPORTATION),
            Optional.of(100L)
        );
        Assertions.assertEquals(
            idPrefixConverter.findIdWithoutPrefix("TMU1001", ResourceIdType.TRANSPORTATION_UNIT),
            Optional.of(1001L)
        );
        Assertions.assertEquals(
            idPrefixConverter.findIdWithoutPrefix("TMM10012", ResourceIdType.MOVEMENT),
            Optional.of(10012L)
        );
        Assertions.assertEquals(
            idPrefixConverter.findIdWithoutPrefix("TMR100123", ResourceIdType.REGISTER),
            Optional.of(100123L)
        );
        Assertions.assertEquals(
            idPrefixConverter.findIdWithoutPrefix("TMT10", ResourceIdType.TRIP),
            Optional.of(10L)
        );
    }

    @Test
    void testExceptionThrown() {
        Assertions.assertThrows(
            IdWithPrefixCouldNotBeConvertedException.class,
            () -> idPrefixConverter.getIdWithoutPrefix("a1a")
        );

        Assertions.assertThrows(
            IdWithPrefixCouldNotBeConvertedException.class,
            () -> idPrefixConverter.getIdWithoutPrefix("fregerger")
        );
    }

    @Test
    void testNotFound() {
        Assertions.assertTrue(idPrefixConverter.findIdWithoutPrefix("a1a", ResourceIdType.TRANSPORTATION)
            .isEmpty());
        Assertions.assertTrue(idPrefixConverter.findIdWithoutPrefix("fregerger", ResourceIdType.TRANSPORTATION)
            .isEmpty());
    }

    @Test
    void testIncorrect() {
        Assertions.assertTrue(
            idPrefixConverter.findIdWithoutPrefix("SOMETHING1001234", ResourceIdType.TRANSPORTATION).isEmpty()
        );
    }

    @Test
    void testConvertWithPrefix() {
        Assertions.assertEquals(
            idPrefixConverter.getIdWithPrefix(100L, ResourceIdType.MOVEMENT),
            "TMM100"
        );
        Assertions.assertEquals(
            idPrefixConverter.getIdWithPrefix(100L, ResourceIdType.REGISTER),
            "TMR100"
        );
        Assertions.assertEquals(
            idPrefixConverter.getIdWithPrefix(100L, ResourceIdType.TRANSPORTATION),
            "TM100"
        );
        Assertions.assertEquals(
            idPrefixConverter.getIdWithPrefix(100L, ResourceIdType.TRANSPORTATION_UNIT),
            "TMU100"
        );
    }
}
