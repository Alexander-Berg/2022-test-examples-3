package ru.yandex.market.checkerxservice;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

public class CheckErxServiceTest {

    @Test
    public void test() {
        //Just a test example
        Assert.assertTrue(true);
    }

    static class MyData {
        Integer id;
        Integer itemId;

        MyData(Integer id, Integer itemId) {
            this.id = id;
            this.itemId = itemId;
        }
    }

    @Test
    public void someTest() {
        List<MyData> myDataList = List.of(new MyData(1, null), new MyData(2, 2));
        final MyData anyReceiptItem = myDataList.stream()
                .filter(item -> item.itemId != null) // исключаем доставку
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not detect orderId from receipt items"));
        Assert.assertTrue(anyReceiptItem != null);
    }
}
