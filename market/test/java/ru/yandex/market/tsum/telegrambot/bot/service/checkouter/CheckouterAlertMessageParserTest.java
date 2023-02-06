package ru.yandex.market.tsum.telegrambot.bot.service.checkouter;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.telegrambot.bot.handlers.commands.startrek.checkouter.CheckouterAlertMessageModel;

public class CheckouterAlertMessageParserTest {

    @Test
    public void parseParameters_forExistingOnes_shouldReturnParsed() {
        List<String> expectedParameters = List.of("method = 'method'", "http_method = 'GET'", "resptime_ms >= 1000");
        String alertText = "some title text\n" +
            "Параметры:\n" +
            String.join("\n", expectedParameters) + "\n\n" +
            "bottom text";
        List<String> actualParameters = CheckouterAlertMessageParser.parseParameters(alertText);

        Assert.assertEquals(expectedParameters.size(), actualParameters.size());
        Assert.assertTrue(actualParameters.containsAll(expectedParameters));
    }

    @Test
    public void parseParameters_forNotExistingOnes_shouldReturnEmpty() {
        String alertText = "some title text\n\nother text\n\nbottom text";
        List<String> actualParameters = CheckouterAlertMessageParser.parseParameters(alertText);

        Assert.assertEquals(0, actualParameters.size());
    }

    @Test
    public void parseEnvironment_prestable() {
        String alertMessage = "ALARM: Timing one_min_0_99 on POST cart(prestable)\n\nbottom text";
        String environment = CheckouterAlertMessageParser.parseEnvironment(alertMessage);
        Assert.assertEquals("PRESTABLE", environment);
    }

    @Test
    public void parseEnvironment_production() {
        String alertMessage = "ALARM: Timing one_min_0_99 on POST cart(production)\n\nbottom text";
        String environment = CheckouterAlertMessageParser.parseEnvironment(alertMessage);
        Assert.assertEquals("PRODUCTION", environment);
    }

    @Test
    public void parseMessageModel_withParameters() {
        String parameters = "method = 'cart'\n" +
            "http_method = 'POST'\n" +
            "resptime_ms >= 12500.0";
        String alertMessage = "/newcheckouteralert a_1m_ch_p_mon_cart_post_timings_99 Timing one_min_0_99 on POST " +
            "cart(production)\n" +
            "<b>ALARM</b>: <a href=\"https://solomon.yandex-team" +
            ".ru/admin/projects/market-checkout/alerts/a_1m_ch_p_mon_cart_post_timings_99 \">Timing one_min_0_99 on " +
            "POST cart(production)</a>\n" +
            "\n" +
            "Описание: 3 значений 0_99 персентиля превысили порог 12500ms на ручке /cart(cart)\n" +
            "\n" +
            "Параметры:\n" + parameters;

        CheckouterAlertMessageModel actualModel = CheckouterAlertMessageParser.parseMessageModel(alertMessage);

        Assert.assertEquals(1, actualModel.getTags().size());
        Assert.assertEquals("a_1m_ch_p_mon_cart_post_timings_99", actualModel.getTags().get(0));
        Assert.assertEquals("Timing one_min_0_99 on POST cart(production)", actualModel.getTitle());
        Assert.assertEquals(alertMessage.split("\n", 2)[1], actualModel.getDescription());
        var expectedParameters = Arrays.asList(parameters.split("\n"));
        Assert.assertEquals(expectedParameters.size(), actualModel.getParameters().size());
        Assert.assertTrue(actualModel.getParameters().containsAll(expectedParameters));
    }

    @Test
    public void parseMessageModel_withoutParameters() {
        String alertMessage = "/newcheckouteralert a_1m_ch_p_mon_cart_post_timings_99 Timing one_min_0_99 on POST " +
            "cart(production)\n" +
            "<b>ALARM</b>: <a href=\"https://solomon.yandex-team" +
            ".ru/admin/projects/market-checkout/alerts/a_1m_ch_p_mon_cart_post_timings_99 \">Timing one_min_0_99 on " +
            "POST cart(production)</a>\n" +
            "\n" +
            "Описание: 3 значений 0_99 персентиля превысили порог 12500ms на ручке /cart(cart)\n";

        CheckouterAlertMessageModel actualModel = CheckouterAlertMessageParser.parseMessageModel(alertMessage);

        Assert.assertEquals(1, actualModel.getTags().size());
        Assert.assertEquals("a_1m_ch_p_mon_cart_post_timings_99", actualModel.getTags().get(0));
        Assert.assertEquals("Timing one_min_0_99 on POST cart(production)", actualModel.getTitle());
        Assert.assertEquals(alertMessage.split("\n", 2)[1], actualModel.getDescription());
        Assert.assertEquals(0, actualModel.getParameters().size());
    }
}
