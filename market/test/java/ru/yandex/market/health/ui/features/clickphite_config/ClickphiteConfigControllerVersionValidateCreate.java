package ru.yandex.market.health.ui.features.clickphite_config;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.health.ui.TestConfig;
import ru.yandex.market.health.ui.features.common.view_model.ValidationErrorsViewModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.health.ui.TestUtils.loadFromClasspath;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
public class ClickphiteConfigControllerVersionValidateCreate extends ClickphiteConfigControllerBaseTest {

    @Test
    public void checkAbsenceOfRequiredLabelCluster() throws Exception {
        test(
            "clickphite_config_group_version_solomon_without_label_cluster.json",
            new AbstractMap.SimpleEntry<String, Collection<String>>(
                "configs[0].graphiteSolomon.solomonSensors[0].labels",
                Collections.singletonList("метка cluster обязательна")
            )
        );
    }

    @Test
    public void checkAbsenceOfAdditionalLabel() throws Exception {
        test(
            "clickphite_config_group_version_solomon_without_label_sensor.json",
            new AbstractMap.SimpleEntry<String, Collection<String>>(
                "configs[0].graphiteSolomon.solomonSensors[0].labels",
                Collections.singletonList("Нужна как минимум ещё одна метка кроме project, service, cluster и period")
            )
        );
    }

    @Test
    public void checkRestrictedTable() throws Exception {
        test(
            "clickphite_config_with_restricted_table.json",
            new AbstractMap.SimpleEntry<String, Collection<String>>(
                "configs[0].table",
                Collections.singletonList("Конфиг содержит опасные изменения таблицы. Их могут активировать только " +
                    "разработчики Market Infra. Напишите в наш чат поддержки (ссылка в футере)")
            )
        );
    }

    @Test
    public void checkDefaultRestrictedTable() throws Exception {
        test(
            "clickphite_config_with_default_restricted_table.json",
            new AbstractMap.SimpleEntry<String, Collection<String>>(
                "defaultValues.table",
                Collections.singletonList("Конфиг содержит опасные изменения таблицы. Их могут активировать только " +
                    "разработчики Market Infra. Напишите в наш чат поддержки (ссылка в футере)")
            )
        );
    }

    private void test(String requestBodyResourceName, Map.Entry<?, ?>... expectedErrors) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/clickphite/config/version/validateCreate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loadFromClasspath(requestBodyResourceName)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andReturn();
        String response = new String(mvcResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        ValidationErrorsViewModel validationErrorsViewModel = jacksonMapper.readValue(response,
            new TypeReference<ValidationErrorsViewModel>() {
            });
        assertThat(validationErrorsViewModel)
            .isNotNull()
            .extracting(ValidationErrorsViewModel::getErrors)
            .isNotNull()
            .asInstanceOf(MAP)
            .contains(expectedErrors);
    }

}
