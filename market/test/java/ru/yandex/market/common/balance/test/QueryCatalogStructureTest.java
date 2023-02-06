package ru.yandex.market.common.balance.test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.balance.xmlrpc.model.QueryCatalogStructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryCatalogStructureTest {
    @Test
    void testMapFromColumns() {
        QueryCatalogStructure structure = new QueryCatalogStructure(
                Arrays.asList("t_person.id", "t_person.type"),
                Arrays.asList(
                        Arrays.asList("1001", "ph"),
                        Arrays.asList("1002", "ph"),
                        Arrays.asList("1004", "j")
                )
        );
        List<PersonData> personDataList = structure.mapFromColumns(Arrays.asList("t_person.type", "t_person.id"),
                l -> new PersonData(
                        Long.parseLong(l.get(1)),
                        l.get(0).equals("ph")));
        personDataList.sort(Comparator.comparingLong(pd -> pd.personId));
        assertEquals(3, personDataList.size());
        assertEquals(1001L, personDataList.get(0).personId);
        assertTrue(personDataList.get(0).isIndividialResident);
        assertEquals(1002L, personDataList.get(1).personId);
        assertTrue(personDataList.get(1).isIndividialResident);
        assertEquals(1004L, personDataList.get(2).personId);
        assertFalse(personDataList.get(2).isIndividialResident);
    }

    private static class PersonData {
        private final long personId;
        private final boolean isIndividialResident;

        private PersonData(long personId, boolean isIndividialResident) {
            this.personId = personId;
            this.isIndividialResident = isIndividialResident;
        }
    }
}
