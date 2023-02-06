package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class IdfaTest {
    @Test
    public void testIsValid() {
        Idfa test1 = new Idfa("");
        Idfa test2 = new Idfa("F1D2AEE4-FDE7-4E18-A612-4EAB70DC2FCF");
        Idfa test3 = new Idfa("F1D2AEE4-fDE7-4E18-A612-4EAB70DC2FCF");
        Idfa test4 = new Idfa("F1D2AEE4-FDE7-4E18-A612-4EAB70DC2FCFS");
        Idfa test5 = new Idfa("F1D2AEE4-FDE7-4E18-A612-4EA2FCFF1D2-0DC2FCF");
        Idfa test6 = new Idfa("F1D2AEE4-0000-4E18-A612-4EAB70DC2FCF");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "F1D2AEE4-FDE7-4E18-A612-4EAB70DC2FCF");
        assertEquals(test2.isValid(), true);
        assertEquals(test3.getValue(), "F1D2AEE4-fDE7-4E18-A612-4EAB70DC2FCF");
        assertEquals(test3.isValid(), true);
        assertEquals(test4.getValue(), "F1D2AEE4-FDE7-4E18-A612-4EAB70DC2FCFS");
        assertEquals(test4.isValid(), false);
        assertEquals(test5.getValue(), "F1D2AEE4-FDE7-4E18-A612-4EA2FCFF1D2-0DC2FCF");
        assertEquals(test5.isValid(), false);
        assertEquals(test6.getValue(), "F1D2AEE4-0000-4E18-A612-4EAB70DC2FCF");
        assertEquals(test6.isValid(), true);
    }

    @Test
    public void testgetType() {
        Idfa test = new Idfa("");
        assertEquals(test.getType(), EIdType.IDFA);
    }
}
