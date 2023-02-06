package ru.yandex.market.pers.history.controller.takeout;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.history.RGB;
import ru.yandex.market.pers.history.Type;
import ru.yandex.market.pers.history.controller.AbstractHistoryControllerTest;
import ru.yandex.market.pers.history.dj.DjClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

public class TakeoutControllerTest extends AbstractHistoryControllerTest {

    private static final long MOD_RESOURCE_ID = 1;
    private static final long BOOK_RESOURCE_ID = 2;

    @Autowired
    private DjClient djClient;

    public void prepareData() {
        Mockito.when(djClient.getHistory(any(), anyString(), eq(RGB.green)))
            .thenReturn(List.of(
                createItem(Type.MODEL, RESOURCE_ID, "model_name"),
                createItem(Type.GROUP, ANOTHER_RESOURCE_ID),
                createItem(Type.CLUSTER, YET_ANOTHER_RESOURCE_ID), createItem(Type.MODIFICATION, MOD_RESOURCE_ID, "modification_name"),
                createItem(Type.BOOK, BOOK_RESOURCE_ID)));

        Mockito.when(djClient.getHistory(any(), anyString(), eq(RGB.blue)))
            .thenReturn(List.of(
                createBlueItem(RESOURCE_ID, SKU),
                createBlueItem(ANOTHER_RESOURCE_ID, ANOTHER_MODEL_SKU, "some name")));

        Mockito.when(djClient.getHistory(any(), anyString(), eq(RGB.red)))
            .thenReturn(Collections.emptyList());
    }

    @Test
    public void testGetWhiteTakeoutData() throws Exception {
        prepareData();
        JSONAssert.assertEquals(fileToString("/data/white_takeout.json"), getHistoryForTakeout("white"),
            new CustomComparator(JSONCompareMode.NON_EXTENSIBLE,
                new Customization("data.history[*].crTime", (o1, o2) -> true)));
    }

    @Test
    public void testGetRedTakeoutData() throws Exception {
        prepareData();
        JSONAssert.assertEquals(fileToString("/data/red_takeout.json"), getHistoryForTakeout("red"), JSONCompareMode.STRICT);
    }

    @Test
    public void testGetBlueTakeoutData() throws Exception {
        prepareData();
        JSONAssert.assertEquals(fileToString("/data/blue_takeout.json"), getHistoryForTakeout("blue"),
            new CustomComparator(JSONCompareMode.NON_EXTENSIBLE,
                new Customization("data.history[*].crTime", (o1, o2) -> true)));
    }

    private String getHistoryForTakeout(String color) throws Exception {
        return invokeAndCheckResponse("/takeout", ImmutableMap.of("uid", String.valueOf(UID), "color", color));
    }
}
