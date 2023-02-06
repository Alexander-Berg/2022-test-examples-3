package ru.yandex.market.tsum.pipelines.common.jobs.hbf_close_dc;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.dclocation.DcLocation;
import ru.yandex.market.tsum.clients.startrek.NotificationUtils;
import ru.yandex.market.tsum.core.notify.common.ContextBuilder;
import ru.yandex.market.tsum.pipelines.sre.resources.HBFCloseDCEvent;

public class CreateHBFCloseAndOpenDCEventIssueJobTest {

    @Test
    public void createDescription() throws Exception {
        String[] actions = new String[]{"Close", "Open"};
        Map<String, String> subj = Map.of("Close", "Учения в Сасово (закрытие ДЦ)", "Open", "Учения в Сасово " +
            "(открытие ДЦ)");

        for (String action : actions) {
            String templateName = "templates/HBF" + action + "DCDescription.txt";
            String expected = IOUtils.toString(this.getClass().getResourceAsStream(
                "/CreateHBFCloseAndOpenDCEventIssueJobTest/HBF" + action + "DCIssue.txt"),
                StandardCharsets.UTF_8.name());

            Instant fromInstant =
                new SimpleDateFormat("hh:mm dd.MM.yyyy z").parse("14:00 22.12.2022 GMT+05:00").toInstant();
            Instant toInstant =
                new SimpleDateFormat("hh:mm dd.MM.yyyy z").parse("15:00 22.12.2022 GMT+05:00").toInstant();
            HBFCloseDCEvent closeDCEvent = new HBFCloseDCEvent(fromInstant, toInstant, DcLocation.SAS,
                subj.get(action), "Проверка генерации тикета учений",
                "TEST-98489", "http://aaaa.bbb.cc", action);

            String result = NotificationUtils.render(templateName, CreateHBFCloseAndOpenDCEventIssueJob.class,
                ContextBuilder.create()
                    .with("context", closeDCEvent)
                    .build()
            );

            Assert.assertEquals(expected, result);
        }
    }


}
