package ru.yandex.market.mbi.util.db.jdbc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class TNumberTblTest {

    @Test
    public void ArrayTest() {
        // test the method
        Long[][] stringArrayArray = new Long[][]{
                {1L, 3L, 4L, 5L, 0L},
                {-9L, 12L, 0L, 24L},
                {}
        };
        String[] ans = {"{1,3,4,5,0}", "{-9,12,0,24}", "{}"};
        int num = 0;
        for (Long[] stringArray : stringArrayArray) {
            TNumberTbl a = new TNumberTbl(stringArray);
            String s = a.toString();
            Assertions.assertEquals(s, ans[num++]);
        }
    }
}
