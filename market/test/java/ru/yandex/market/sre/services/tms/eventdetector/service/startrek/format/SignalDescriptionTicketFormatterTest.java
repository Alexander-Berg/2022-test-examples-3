package ru.yandex.market.sre.services.tms.eventdetector.service.startrek.format;

import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.sre.services.tms.eventdetector.dao.entity.Event;
import ru.yandex.market.sre.services.tms.eventdetector.dao.entity.ServiceIndicator;
import ru.yandex.market.sre.services.tms.eventdetector.enums.AlertPriority;
import ru.yandex.market.sre.services.tms.eventdetector.enums.IndicatorType;
import ru.yandex.market.sre.services.tms.eventdetector.enums.StartrekComponent;
import ru.yandex.market.sre.services.tms.eventdetector.enums.StartrekTicketType;
import ru.yandex.market.sre.services.tms.eventdetector.enums.TicketClass;
import ru.yandex.market.sre.services.tms.eventdetector.model.Alert;
import ru.yandex.market.sre.services.tms.eventdetector.model.core.FlapSettings;
import ru.yandex.market.sre.services.tms.eventdetector.model.core.GrafanaTicketBlock;
import ru.yandex.market.sre.services.tms.eventdetector.model.core.Period;
import ru.yandex.market.sre.services.tms.eventdetector.model.core.StartrekIssueSettings;
import ru.yandex.market.sre.services.tms.eventdetector.service.TimeFormatterService;

public class SignalDescriptionTicketFormatterTest {

    ServiceIndicator indicator() {
        FlapSettings frontFlapSettings;
        GrafanaTicketBlock grafana;

        ServiceIndicator indicator = new ServiceIndicator();
        indicator.setServiceId("1");
        indicator.setId("WHITE_TOUCH_NGINX_TTLB");
        indicator.setName("White Touch Nginx TTLB");
        indicator.setType(IndicatorType.TTLB);
        indicator.setRawSignalName("one_min.market-front-touch.timings-dynamic.ALL.0_99");
        indicator.setTargets(Arrays.asList("color(constantLine(1.0)%2C%22red%22)", "divideSeries(movingAverage" +
                "(one_min.market-front-touch.timings-dynamic.ALL.0_99,%2730minute%27),sumSeries(minSeries(timeStack" +
                "(movingAverage(one_min.market-front-touch.timings-dynamic.ALL.0_99,%2730minute%27),%27-1week%27,1,4)" +
                "),stddevSeries(timeStack(movingAverage(one_min.market-front-touch.timings-dynamic.ALL.0_99," +
                "%2730minute%27),%27-1week%27,1,4),movingAverage(one_min.market-front-touch.timings-dynamic.ALL.0_99," +
                "%2730minute%27)),stdev(one_min.market-front-touch.timings-dynamic.ALL.0_99,60,0)))"));
        indicator.setBatchMaxSeconds(4 * 60 * 60);
        indicator.setStartTimeOffsetSeconds(30 * 60);

        frontFlapSettings = new FlapSettings();
        frontFlapSettings.setStableTimeSeconds(90 * 60);
        frontFlapSettings.setNormalWindowSeconds(2 * 60 * 60);
        frontFlapSettings.setCriticalWindowSeconds(2 * 60 * 60);
        indicator.setFlap(frontFlapSettings);

        StartrekIssueSettings settings = new StartrekIssueSettings();
        settings.setRegisterIncident(false);
        settings.setDowntimeNormalSeconds(30 * 60);
        settings.setDowntimeCriticalSeconds(1 * 60 * 60);
        settings.setAlarmQueue("MARKETFRONT");
        settings.setAlarmTicketType(StartrekTicketType.BUG);
        settings.setEventLowMinSeconds(30 * 60);
        settings.setTags(Arrays.asList("frontech_speed_alert", "Контур#Скорость", "белый"));
        settings.setComponent(StartrekComponent.FRONT_TOUCH);

        grafana = new GrafanaTicketBlock();
        grafana.setIframe("https://grafana.yandex-team.ru/d-solo/aTpYl6kGk/market-front-speed-lab?panelId" +
                "=10003&var-mov_avg=30m&var-nginxTable=market-front-touch&var-timersTable=market_front_touch&var" +
                "-retroFactor=4&var-retroScale=1w");
        grafana.setFullscreen("https://grafana.yandex-team" +
                ".ru/d/aTpYl6kGk/market-front-speed-lab?fullscreen&panelId=10003&var-mov_avg=30m&var-nginxTable" +
                "=market-front-touch&var-timersTable=market_front_touch&var-retroFactor=4&var-retroScale=1w");
        settings.setGrafana(grafana);
        indicator.setAlerting(settings);

        return indicator;
    }

    @Ignore
    @Test
    public void format() {
        SignalDescriptionTicketFormatter formatter = new SignalDescriptionTicketFormatter(new TimeFormatterService());
        Event event = new Event();
        event.setPeriod(new Period(1586784660L, 1586788260L));
        event.setPeriod(new Period(1586784660L, null));
        Alert alert = new Alert(event, indicator(), AlertPriority.CRITICAL);
        String result = formatter.format(alert, TicketClass.EVENT, "");
        System.out.println(result);
    }
}
