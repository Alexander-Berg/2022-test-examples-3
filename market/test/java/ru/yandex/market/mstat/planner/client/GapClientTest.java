package ru.yandex.market.mstat.planner.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.mockito.Mockito;

import java.io.OutputStream;
import java.net.HttpURLConnection;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.mstat.planner.util.RestUtil.parseLocalDateYMDFormat;

public class GapClientTest extends TestCase {
    AbstractClient.URLFactory urlFactory = Mockito.mock(AbstractClient.URLFactory.class);
    GapClient client = new GapClient(urlFactory, "111");


    @SneakyThrows
    public void testGetGaps() {
        String testGaps = "{\"persons\": {\"orphie\": [{\"id\": 430968, \"workflow\": \"vacation\", \"date_from\": \"2016-05-23T00:00:00\", \"date_to\": \"2016-05-24T00:00:00\", \"comment\": \"\", \"work_in_absence\": false}]}, \"workflows\": [{\"type\": \"absence\", \"color\": \"#ffc136\", \"verbose_name\": {\"ru\": \"Отсутствие\", \"en\": \"Absence\"}}]}";

        HttpURLConnection mockHttpConnection = Mockito.mock(HttpURLConnection.class);

        Mockito.when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(mockHttpConnection.getInputStream()).thenReturn(IOUtils.toInputStream(testGaps));
        OutputStream outputStream = Mockito.mock(OutputStream.class);
        Mockito.when(mockHttpConnection.getOutputStream()).thenReturn(outputStream);
        final AbstractClient.UrlWrapper mockURLSecond = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLSecond.openConnection()).thenReturn(mockHttpConnection);

        Mockito.when(urlFactory.createUrl("https://staff.yandex-team.ru/gap-api/api/export_gaps")).thenReturn(mockURLSecond);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(testGaps);

        assertEquals(jsonNode, client.getGaps(parseLocalDateYMDFormat("2016-05-23"), parseLocalDateYMDFormat("2016-05-24"),
                ImmutableList.<String>builder().add("orphie").build()));
    }
}
