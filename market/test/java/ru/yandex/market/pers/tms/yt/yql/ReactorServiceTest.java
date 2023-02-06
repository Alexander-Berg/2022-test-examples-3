package ru.yandex.market.pers.tms.yt.yql;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.core.util.reactor.ReactorClient;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.yt.yql.reactor.ReactorService;

import static org.mockito.Mockito.verify;

public class ReactorServiceTest extends MockedPersTmsTest {
    @Autowired
    private ReactorClient reactorClient;

    @Autowired
    private ReactorService reactorService;

    @Test
    public void testDeprecateYesterdayAndCreateNewCurrentDate() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String today = now.format(dateTimeFormatter);
        String yesterday = now.minus(1, ChronoUnit.DAYS).format(dateTimeFormatter);

        reactorService.deprecateYesterdayAndCreateNewToday(
            "hahn",
            "//tmp/market/development/pers-grade/content-rank/question/1653565204957"
        );

        ArgumentCaptor<String> method = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(reactorClient).sendPostRequest(method.capture(), json.capture());

        String expectedJson = String.format(IOUtils.toString(
            getClass().getResourceAsStream("/testdata/reactor/request.json"),
            StandardCharsets.UTF_8
        ), yesterday, yesterday, today);

        Assert.assertEquals("/api/v1/a/i/deprecate", method.getValue());
        Assert.assertEquals(expectedJson, json.getValue());
    }

}
