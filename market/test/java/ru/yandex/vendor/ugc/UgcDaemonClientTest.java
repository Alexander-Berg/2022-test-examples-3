package ru.yandex.vendor.ugc;

import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.vendor.util.IRestClient;

import java.io.InputStream;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UgcDaemonClientTest {

    private UgcDaemonClient ugcDaemonClient;

    @Mock
    IRestClient restClient;

    @Test
    public void testParseListOfCommentsForJson() {
        ugcDaemonClient = new UgcDaemonClient(restClient);
        InputStream inputStream = getInputStreamResource("/testParseListOfCommentsForJson/inputUgcDaemon.json");
        Map<Long, List<UgcComment>> modelOpinionComments = UgcCommentParser.parseCommentsForest(inputStream);
        ArrayList<Long> keys = new ArrayList<>();
        modelOpinionComments.forEach((key, value) -> {
            keys.add(key);
            List<Long> valueIds = new ArrayList<>();
            value.forEach(v -> valueIds.add(v.getId()));
            if (key == 96671360L) {
                assertTrue(valueIds.contains(102457263L));
                assertEquals(1, keys.size());
                assertEquals(0, value.get(0).getComments().size());
                assertEquals(102457263L, value.get(0).getId().longValue());
                assertEquals(15300046, value.get(0).getBrandId().longValue());
                assertEquals(1130000027083368L, value.get(0).getUser().getUid().longValue());
                assertEquals(1572949845000L, value.get(0).getUpdateTime().atZone(TimeUtil.LOCAL).toInstant().toEpochMilli());
                assertEquals("111", value.get(0).getText());
            } else {
                assertTrue(valueIds.contains(102351791L));
                assertTrue(valueIds.contains(102303050L));
                assertEquals(2, keys.size());
                assertEquals(2, value.size());
                ArrayList<Long> subComments = new ArrayList<>();
                subComments.add(value.get(0).getComments().get(0).getId());
                subComments.add(value.get(1).getComments().get(0).getId());
                assertTrue(subComments.contains(102363062L));
                assertTrue(subComments.contains(102304707L));
                assertEquals(102351791L,value.get(0).getId().longValue());
                assertEquals(value.get(0).getId(), value.get(0).getComments().get(0).getParentId());
            }
        });

        assertTrue(keys.contains(96671360L));
        assertTrue(keys.contains(94450556L));
        assertEquals(2, keys.size());
    }

    private InputStream getInputStreamResource(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        return classLoader.getResourceAsStream("ru/yandex/vendor/ugc/" + getClass().getSimpleName() + filename);
    }
}
