package ru.yandex.market.common.balance.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.balance.xmlrpc.model.PersonStructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonStructureTest {
    @Test
    void idShouldBeReadFromBothFields() {
        // given
        Map rawData = new HashMap();
        rawData.put("ID", "123");
        PersonStructure p = new PersonStructure(rawData);

        // when-then
        assertEquals(123L, p.getPersonId());
        p.setPersonId(1L);
        assertNull(rawData.get("ID"), "детали в Balance2Wrapper.CreatePerson");
        assertEquals("1", rawData.get("PERSON_ID"));
        assertEquals(1L, p.getPersonId());
    }

    @Test
    void getShouldReturnSameAsSet() {
        PersonStructure p = new PersonStructure();

        assertEquals(PersonStructure.PERSON_ID_NEW, p.getPersonId());
        p.setPersonId(1L);
        assertEquals(1L, p.getPersonId());

        // теперь например пытаемся создать нового на основе данных существующего
        p.setPersonId(PersonStructure.PERSON_ID_NEW);
        assertEquals(PersonStructure.PERSON_ID_NEW, p.getPersonId(), "константа требует особенного внимания");

        assertEquals(0L, p.getClientId());
        p.setClientId(2L);
        assertEquals(2L, p.getClientId());

        assertFalse(p.getIsPartner());
        p.setIsPartner(true);
        assertTrue(p.getIsPartner());
    }
}
