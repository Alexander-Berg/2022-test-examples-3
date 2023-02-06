package ru.yandex.market.health.ui.features.clickphite_config;

import de.bwaldvogel.mongo.backend.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.health.configs.clickphite.validation.query.BeautySQLFormatter;
import ru.yandex.market.health.ui.TestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.health.ui.TestUtils.loadFromClasspath;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
public class ClickphiteConfigControllerVersionQueryTest extends ClickphiteConfigControllerBaseTest {

    @Test
    public void checkAbsenceOfRequiredLabel_cluster() throws Exception {
        test("clickphite_config_group_version_solomon_without_label_cluster.json",
            "checkAbsenceOfRequiredLabel_cluster_response.json");
    }

    @Test
    public void checkAbsenceOfAdditionalLabel() throws Exception {
        test("clickphite_config_group_version_solomon_without_label_sensor.json",
            "checkAbsenceOfAdditionalLabel_response.json");
    }

    private void test(String requestBodyResourceName, String expectedResponseBodyResourceName) throws Exception {
        mockMvc.perform(post("/api/clickphite/config/version/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loadFromClasspath(requestBodyResourceName)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().json(loadFromClasspath(expectedResponseBodyResourceName)));
    }

    @Test
    public void checkFormatter() {
        String input = "SELECT multiply(intDiv(timestamp, 300), 300) AS metric_ts, countIf(http_code >=" +
            " 400 and http_code <= 499 and dynamic = 1) * 100 / countIf(dynamic = 1) AS value_0, dc FROM" +
            " market.nginx2 WHERE 1 AND date = today() AND (vhost = 'mbo.market.yandex-team.ru') GROUP BY" +
            " metric_ts, if(substring(host, 1, 3) in ('iva', 'man', 'vla', 'sas'), substring(host, 1, 3)," +
            " if(endsWith(host, 'yp-c.yandex.net'), substring(host, position(host, '.') + 1, 3), " +
            "if(dictGetString('dc', 'dc', sipHash64(host)) <> '', dictGetString('dc', 'dc', sipHash64(host))," +
            " 'undefined_dc'))) AS dc ORDER BY metric_ts LIMIT 10";
        String expectedOutput = "SELECT multiply(intDiv(timestamp, 300), 300) AS metric_ts, countIf(http_code" +
            " >= 400 and http_code <= 499 and dynamic = 1) * 100 / countIf(dynamic = 1) AS value_0, dc \nFROM" +
            " market.nginx2 \nWHERE 1 \n\tAND date = today() \n\tAND (vhost = 'mbo.market.yandex-team.ru') " +
            "\nGROUP BY metric_ts, if(substring(host, 1, 3) in ('iva', 'man', 'vla', 'sas'), substring(host, 1, 3)," +
            " if(endsWith(host, 'yp-c.yandex.net'), substring(host, position(host, '.') + 1, 3), " +
            "if(dictGetString('dc', 'dc', sipHash64(host)) <> '', dictGetString('dc', 'dc', sipHash64(host))," +
            " 'undefined_dc'))) AS dc \nORDER BY metric_ts \nLIMIT 10";
        Assert.equals(expectedOutput, BeautySQLFormatter.prettyQuery(input));
    }
}
