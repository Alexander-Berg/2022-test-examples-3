package ru.yandex.ir.modelsclusterizer.be;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Aydar Gilmullin
 */
public class BarcodeStorageTest {
    @Test
    public void parseStringParser() {
        BarcodeStorage barcodeStorage = BarcodeStorage.parseFromString("0123412341234");
        assertEquals(barcodeStorage.getCodes().size(), 1);
        assertEquals(123412341234l, (long) barcodeStorage.getCodes().get(0));

        barcodeStorage = BarcodeStorage.parseFromString("");
        assertEquals(barcodeStorage.getCodes().size(), 0);

        barcodeStorage = BarcodeStorage.parseFromString("12341234a43211234");
        assertEquals(2, barcodeStorage.getCodes().size());
        assertEquals(12341234l, (long) barcodeStorage.getCodes().get(0));
        assertEquals(43211234l, (long) barcodeStorage.getCodes().get(1));

        barcodeStorage = BarcodeStorage.parseFromString("09874321-12344321a43214321");
        assertEquals(3, barcodeStorage.getCodes().size());
        assertEquals(9874321l, (long) barcodeStorage.getCodes().get(0));
        assertEquals(12344321l, (long) barcodeStorage.getCodes().get(1));
        assertEquals(43214321l, (long) barcodeStorage.getCodes().get(2));

        barcodeStorage = BarcodeStorage.parseFromString("12345678901234567890"); //greater than max_long
        assertEquals(0, barcodeStorage.getCodes().size());
    }

    @Test
    public void testToString() {
        BarcodeStorage barcodeStorage = BarcodeStorage.parseFromString("21341232,12431234a");
        assertEquals("21341232,12431234", barcodeStorage.toString());

        barcodeStorage = BarcodeStorage.parseFromString("21341232,12431234");
        assertEquals("21341232,12431234", barcodeStorage.toString());

        barcodeStorage = BarcodeStorage.parseFromString("21341232");
        assertEquals("21341232", barcodeStorage.toString());

        barcodeStorage = BarcodeStorage.parseFromString("");
        assertEquals("", barcodeStorage.toString());
    }

//    @Test
//    public void testToStringList() {
//        BarcodeStorage barcodeStorage = BarcodeStorage.parseFromString("234,134");
//        List<String> codes = barcodeStorage.toStringList();
//        assertEquals(2, codes.size());
//        assertEquals("234", codes.get(0));
//        assertEquals("134", codes.get(1));
//
//        barcodeStorage = BarcodeStorage.parseFromString("");
//        codes = barcodeStorage.toStringList();
//        assertEquals(0, codes.size());
//    }

    @Test
    public void testAddCode() {
        BarcodeStorage barcodeStorage = new BarcodeStorage();
        barcodeStorage.addCode(42424242l);
        assertEquals(1, barcodeStorage.getCodes().size());
        assertEquals(42424242l, (long) barcodeStorage.getCodes().get(0));
    }

    @Test
    public void testAddCodes() {
        BarcodeStorage barcodeStorage = new BarcodeStorage();
        BarcodeStorage addedCodes = BarcodeStorage.parseFromString("12312312,32132132");
        barcodeStorage.addCodes(addedCodes);
        assertEquals("12312312,32132132", barcodeStorage.toString());

        barcodeStorage = new BarcodeStorage();
        String addedCodesString = "12312312,32132132";
        barcodeStorage.addCodes(addedCodesString);
        assertEquals("12312312,32132132", barcodeStorage.toString());

        barcodeStorage = new BarcodeStorage();
        LongList addedCodesList = new LongArrayList(new long[]{12312312, 32132132});
        barcodeStorage.addCodes(addedCodesList);
        assertEquals("12312312,32132132", barcodeStorage.toString());
    }

}