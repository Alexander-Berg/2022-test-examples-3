package ru.yandex.market.sre.services.tms.eventdetector.service.startrek;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import ru.yandex.market.sre.services.tms.eventdetector.enums.StartrekComponent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class StartrekIncidentServiceTest {

    @Test
    public void patternTest() {
        String text = "==**Первичная информация**\n" +
                "<[**Создано автоматически**\n" +
                "Тайминги превысили SLO в компоненте Blue main report\n" +
                "Сигнал: one_min.blue-market-report-nginx.upstream_resp_time.env.production.loc.ALL.cluster.ALL.place" +
                ".ALL.0_99\n" +
                "Максимальное превышение SLO за 10 мин: 2мин\n" +
                "Максимальное превышение SLO за 30 мин: 6мин\n" +
                "0x0:https://market-graphite.yandex-team.ru/render?from=23:58_20200507&target=color(constantLine(900)" +
                "%2C%22red%22)&target=one_min.blue-market-report-nginx.upstream_resp_time.env.production.loc.ALL" +
                ".cluster.ALL.place.ALL.0_99&lineMode=staircase&\n" +
                "((https://market-graphite.yandex-team.ru/?from=23:58_20200507&target=color(constantLine(900)" +
                "%2C%22red%22)&target=one_min.blue-market-report-nginx.upstream_resp_time.env.production.loc.ALL" +
                ".cluster.ALL.place.ALL.0_99 График))\n" +
                "{{iframe frameborder=\"0\" width=\"100%\" height=\"100%\" src=\"https://charts.yandex-team" +
                ".ru/preview/editor/xilr4scvzevat?startTime=2020-05-08 00:03&endTime=2020-05-08 " +
                "05:49&_embedded=1\"}}\n" +
                "<{Другие метрики компонента\n" +
                "0x0:https://market-graphite.yandex-team.ru/render?from=23:58_20200507&target=color(constantLine(0.1)" +
                "%2C%22red%22)&target=one_min.blue-market-report-nginx.5xx_error_percentage.env.production.loc.ALL" +
                ".cluster.ALL&lineMode=staircase&\n" +
                "((https://market-graphite.yandex-team.ru/?from=23:58_20200507&target=color(constantLine(0.1)" +
                "%2C%22red%22)&target=one_min.blue-market-report-nginx.5xx_error_percentage.env.production.loc.ALL" +
                ".cluster.ALL График))\n" +
                "}>\n" +
                "]>\n" +
                "==Что сломалось и почему сломалось?\n" +
                "TBD (заполняется исполнителем)\n" +
                "\n" +
                "==**Задело платформы**\n" +
                "Беру \n" +
                "\n" +
                "==**Оценка приоритета**\n" +
                "<{Impact\n" +
                " \n" +
                "}>\n" +
                "<{Urgency\n" +
                "    \n" +
                "}> \n" +
                "\n" +
                "==**Влияние на деньги**\n" +
                "<{Графики [создано автоматически]\n" +
                "500x800:https://st-api.yandex-team.ru/v2/attachments/18665991/Clicks&\n" +
                "500x800:https://st-api.yandex-team.ru/v2/attachments/18665992/Orders&\n" +
                "}>\n" +
                "\n" +
                "\n" +
                "==**Хронология событий**\n" +
                "<{Что тут должно быть?\n" +
                "Важно отразить следующие моменты:\n" +
                "- когда проблема появилась\n" +
                "- когда мы обнаружили проблему\n" +
                "- когда была восстановлена работа сервиса\n" +
                "- когда устранили полностью неисправность\n" +
                "}>\n" +
                "  \n" +
                "TBD (заполняется исполнителем)\n" +
                "[2020-05-08 00:03] Первое превышение SLO TESTMARKETINCID-1791\n" +
                "[2020-05-08 01:53] Регистрация инцидента\n" +
                "[dd.mm hh:mm] - ...\n" +
                "[dd.mm hh:mm] - ...\n" +
                "\n" +
                "==**Что было/будет сделано**\n" +
                "Тут должен быть перечень конкретных тасок, которые уже сделаны или будут сделаны.\n" +
                "__Чтобы не повторилось:__\n" +
                "* если нужно\n" +
                "\n" +
                "__Чтобы быстрее обнаружить:__\n" +
                "* если нужно\n" +
                "\n" +
                "__Чтобы быстрее диагностировать:__\n" +
                "* если нужно\n" +
                "\n" +
                "__Чтобы быстрее восстановить работу:__\n" +
                "* если нужно";
        Pattern p = Pattern.compile("<\\[\\*\\*Создано автоматически[\\s\\S]+?\\]>");
        Matcher m = p.matcher(text);
        if (m.find()) {
            System.out.println(m.replaceFirst("none"));
        }
    }

    @Test
    public void patternTest2() {
        Pattern p = Pattern.compile("Исходное событие: (.+)");
        String text = "==**Первичная информация**\n" +
                "<[**Создано автоматически**\n" +
                "Исходное событие: MARKETALARMS-3\n" +
                "Тайминги превысили SLO в компоненте FAPI\n" +
                "Сигнал: one_min.market-front-white-api.timings-dynamic.ALL.0_99\n" +
                "Максимальное превышение SLO за 10 мин: 10мин\n" +
                "Максимальное превышение SLO за 30 мин: 19мин\n" +
                "0x0:https://market-graphite.yandex-team.ru/render?from=17:49_20200512&target=color(constantLine" +
                "(1500)%2C%22red%22)&target=one_min.market-front-white-api.timings-dynamic.ALL" +
                ".0_99&lineMode=staircase&\n" +
                "((https://market-graphite.yandex-team.ru/?from=17:49_20200512&target=color(constantLine(1500)" +
                "%2C%22red%22)&target=one_min.market-front-white-api.timings-dynamic.ALL.0_99 График))\n" +
                "{{iframe frameborder=\"0\" width=\"100%\" height=\"100%\" src=\"https://charts.yandex-team" +
                ".ru/preview/editor/xilr4scvzevat?startTime=2020-05-12 " +
                "17:54&endTime=&key=MARKETALARMS-3&_embedded=1\"}}\n" +
                "<{Другие метрики компонента\n" +
                "0x0:https://market-graphite.yandex-team.ru/render?from=17:49_20200512&target=alias(color" +
                "(constantLine(1500)%2C%22red%22)%2C%22нормально%201500%22)&target=one_min.market-front-blue-api" +
                ".timings-dynamic.ALL.0_99&lineMode=staircase&\n" +
                "((https://market-graphite.yandex-team.ru/?from=17:49_20200512&target=alias(color(constantLine(1500)" +
                "%2C%22red%22)%2C%22нормально%201500%22)&target=one_min.market-front-blue-api.timings-dynamic.ALL" +
                ".0_99 График))\n" +
                "0x0:https://market-graphite.yandex-team.ru/render?from=17:49_20200512&target=alias(color" +
                "(constantLine(0.1)%2C%22red%22)&target=one_min.market-front-white-api.5xx-percent" +
                ".ALL&lineMode=staircase&\n" +
                "((https://market-graphite.yandex-team.ru/?from=17:49_20200512&target=alias(color(constantLine(0.1)" +
                "%2C%22red%22)&target=one_min.market-front-white-api.5xx-percent.ALL График))\n" +
                "}>\n" +
                "]>\n" +
                "==Что сломалось и почему сломалось?\n" +
                "TBD (заполняется исполнителем)\n" +
                "==**Задело платформы**\n" +
                "Маркет \n" +
                "==**Оценка приоритета**\n" +
                "<{Impact\n" +
                "\n" +
                "}>\n" +
                "<{Urgency\n" +
                "\n" +
                "}> \n" +
                "==**Влияние на деньги**\n" +
                "<{Графики [создано автоматически]\n" +
                "500x800:https://market-graphite.yandex-team" +
                ".ru/render?from=16:54_20200512&until=19:54_20200512&format=png&height=500&width=800&target" +
                "=cactiStyle(alias(timeShift(one_min.money.mstat.clicks.count.TOTAL%2C%227d%22)" +
                "%2C%22Total%20clicks%20week%20ago%22))&target=cactiStyle(alias(one_min.money.mstat.clicks.count" +
                ".TOTAL%2C%22Total%20clicks%20now%22))&colorList=blue%2Cred&title=Clicks%20Total%20vs%20week&\n" +
                "500x800:https://market-graphite.yandex-team" +
                ".ru/render?from=16:54_20200512&until=19:54_20200512&format=png&height=500&width=800&title=Blue" +
                "%20Orders%20Total%20vs%20week%20and%20day%20ago&target=cactiStyle(alias(timeShift(five_min" +
                ".checkouter.monitorings.BIZ.rgb.BLUE.order.new.TOTAL%2C%227d%22)%2C%22Orders%20week%20ago%22))" +
                "&target=cactiStyle(alias(timeShift(five_min.checkouter.monitorings.BIZ.rgb.BLUE.order.new" +
                ".TOTAL%2C%221d%22)%2C%22Orders%20day%20ago%22))&target=cactiStyle(alias(five_min.checkouter" +
                ".monitorings.BIZ.rgb.BLUE.order.new.TOTAL%2C%22Orders%20today%22))&\n" +
                "}>\n" +
                "==**Хронология событий**\n" +
                "<{Что тут должно быть?\n" +
                "Важно отразить следующие моменты:\n" +
                "- когда проблема появилась\n" +
                "- когда мы обнаружили проблему\n" +
                "- когда была восстановлена работа сервиса\n" +
                "- когда устранили полностью неисправность\n" +
                "}>\n" +
                "\n" +
                "TBD (заполняется исполнителем)\n" +
                "[2020-05-12 17:54] Первое превышение SLO MARKETALARMS-3\n" +
                "[2020-05-12 18:23] Регистрация инцидента\n" +
                "[dd.mm hh:mm] - ...\n" +
                "[dd.mm hh:mm] - ...\n" +
                "==**Что было/будет сделано**\n" +
                "Тут должен быть перечень конкретных тасок, которые уже сделаны или будут сделаны.\n" +
                "__Чтобы не повторилось:__\n" +
                "* если нужно\n" +
                "__Чтобы быстрее обнаружить:__\n" +
                "* если нужно\n" +
                "__Чтобы быстрее диагностировать:__\n" +
                "* если нужно\n" +
                "__Чтобы быстрее восстановить работу:__\n" +
                "* если нужно";
        Matcher m = p.matcher(text);
        if (m.find()) {
            assertEquals("MARKETALARMS-3", m.group(1));
        } else {
            fail();
        }
    }

    @Test
    public void patternTest3() {
        String text = "==**Первичная информация**\n" +
                "<[**Создано автоматически**\n" +
                "Тайминги превысили SLO в компоненте Beru front desktop\n" +
                "Сигнал: one_min.market-front-blue-desktop.timings-dynamic.ALL.0_99\n" +
                "Продолжительность: c 2020-05-08 00:00 до 2020-05-08 00:29 (29мин)\n" +
                "Суммарное превышение SLO: 19мин\n" +
                "Максимальное превышение SLO за 10 мин: 10мин\n" +
                "Максимальное превышение SLO за 30 мин: 19мин\n" +
                "0x0:https://market-graphite.yandex-team.ru/render?from=23:55_20200507&target=color(constantLine" +
                "(1500)%2C%22red%22)&target=one_min.market-front-blue-desktop.timings-dynamic.ALL" +
                ".0_99&lineMode=staircase&until=00:34_20200508&\n" +
                "((https://market-graphite.yandex-team.ru/?from=23:55_20200507&until=00:34_20200508&target=color" +
                "(constantLine(1500)%2C%22red%22)&target=one_min.market-front-blue-desktop.timings-dynamic.ALL.0_99 " +
                "График))\n" +
                "{{iframe frameborder=\"0\" width=\"100%\" height=\"100%\" src=\"https://charts.yandex-team" +
                ".ru/preview/editor/xilr4scvzevat?startTime=2020-05-08 00:00&endTime=2020-05-08 " +
                "00:29&key=TESTMARKETINCID-1827&_embedded=1\"}}\n" +
                "<{Другие метрики компонента\n" +
                "0x0:https://market-graphite.yandex-team.ru/render?from=23:55_20200507&target=color(constantLine(0.1)" +
                "%2C%22red%22)&target=one_min.market-front-blue-desktop.5xx-percent" +
                ".ALL&lineMode=staircase&until=00:34_20200508&\n" +
                "((https://market-graphite.yandex-team.ru/?from=23:55_20200507&until=00:34_20200508&target=color" +
                "(constantLine(0.1)%2C%22red%22)&target=one_min.market-front-blue-desktop.5xx-percent.ALL График))\n" +
                "0x0:https://market-graphite.yandex-team.ru/render?from=23:55_20200507&target=color(constantLine" +
                "(2000)%2C%22red%22)&target=one_min.market-front-blue-touch.timings-dynamic.ALL" +
                ".0_99&lineMode=staircase&until=00:34_20200508&\n" +
                "((https://market-graphite.yandex-team.ru/?from=23:55_20200507&until=00:34_20200508&target=color" +
                "(constantLine(2000)%2C%22red%22)&target=one_min.market-front-blue-touch.timings-dynamic.ALL.0_99 " +
                "График))\n" +
                "0x0:https://market-graphite.yandex-team.ru/render?from=23:55_20200507&target=color(constantLine(0.1)" +
                "%2C%22red%22)&target=one_min.market-front-blue-touch.5xx-percent" +
                ".ALL&lineMode=staircase&until=00:34_20200508&\n" +
                "((https://market-graphite.yandex-team.ru/?from=23:55_20200507&until=00:34_20200508&target=color" +
                "(constantLine(0.1)%2C%22red%22)&target=one_min.market-front-blue-touch.5xx-percent.ALL График))\n" +
                "}>\n" +
                "]>\n" +
                "==Что сломалось и почему сломалось?\n" +
                "TBD (заполняется исполнителем)\n" +
                "\n" +
                "==**Задело платформы**\n" +
                "Беру \n" +
                "\n" +
                "==**Оценка приоритета**\n" +
                "<{Impact\n" +
                " \n" +
                "}>\n" +
                "<{Urgency\n" +
                "    \n" +
                "}> \n" +
                "\n" +
                "==**Влияние на деньги**\n" +
                "<{Графики [создано автоматически]\n" +
                "500x800:https://st-api.yandex-team.ru/v2/attachments/18692725/Clicks&\n" +
                "500x800:https://st-api.yandex-team.ru/v2/attachments/18692726/Orders&\n" +
                "}>\n" +
                "\n" +
                "\n" +
                "==**Хронология событий**\n" +
                "<{Что тут должно быть?\n" +
                "Важно отразить следующие моменты:\n" +
                "- когда проблема появилась\n" +
                "- когда мы обнаружили проблему\n" +
                "- когда была восстановлена работа сервиса\n" +
                "- когда устранили полностью неисправность\n" +
                "}>\n" +
                "  \n" +
                "TBD (заполняется исполнителем)\n" +
                "[2020-05-08 00:00] Первое превышение SLO TESTMARKETINCID-1827\n" +
                "[2020-05-08 18:04] Регистрация инцидента\n" +
                "[dd.mm hh:mm] - ...\n" +
                "[dd.mm hh:mm] - ...\n" +
                "\n" +
                "==**Что было/будет сделано**\n" +
                "Тут должен быть перечень конкретных тасок, которые уже сделаны или будут сделаны.\n" +
                "__Чтобы не повторилось:__\n" +
                "* если нужно\n" +
                "\n" +
                "__Чтобы быстрее обнаружить:__\n" +
                "* если нужно\n" +
                "\n" +
                "__Чтобы быстрее диагностировать:__\n" +
                "* если нужно\n" +
                "\n" +
                "__Чтобы быстрее восстановить работу:__\n" +
                "* если нужно";
        Pattern p = Pattern.compile("<\\[\\*\\*Создано автоматически[\\s\\S]+?\\]>");
        Matcher m = p.matcher(text);
        if (m.find()) {
            System.out.println(m.replaceFirst("Что то новое"));
        }
    }

    @Test
    public void collectComponents() {
        assertEquals("(Components: )", StartrekIncidentService.collectComponents(Collections.emptyList()));
        assertEquals("(Components: @Приборы)",
                StartrekIncidentService.collectComponents(Arrays.asList(StartrekComponent.FRONT_INDICATOR)));
        assertEquals("(Components: @Приборы AND Components: @Desktop)",
                StartrekIncidentService.collectComponents(Arrays.asList(StartrekComponent.FRONT_INDICATOR,
                        StartrekComponent.FRONT_DESKTOP)));
    }
}
