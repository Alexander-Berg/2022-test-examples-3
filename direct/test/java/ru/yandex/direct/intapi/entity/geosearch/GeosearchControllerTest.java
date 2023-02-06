package ru.yandex.direct.intapi.entity.geosearch;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.configuration.IntapiConfiguration;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@IntApiTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {IntapiConfiguration.class})
public class GeosearchControllerTest {
    @Autowired
    private GeosearchController geosearchController;

    private MockMvc mockMvc;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(geosearchController).build();
    }

    @Test
    @Ignore
    public void getGeosearchAddressSmokeTest() throws Exception {
        mockMvc.perform(get("/geosearch/address")
                .param("lang", "RU").param("text", "Россия Москва Красная площадь 1")
        ).andExpect(json().isEqualTo("["
                + "{\"administrativeArea\":" + "null" +
                ",\"city\":" + "\"Москва\"" +
                ",\"components\":" + "[{\"kind\":\"COUNTRY\",\"name\":\"Россия\"},"
                    + "{\"kind\":\"PROVINCE\",\"name\":\"Центральный федеральный округ\"},"
                    + "{\"kind\":\"PROVINCE\",\"name\":\"Москва\"},"
                    + "{\"kind\":\"LOCALITY\",\"name\":\"Москва\"},"
                    + "{\"kind\":\"STREET\",\"name\":\"Красная площадь\"}"
                    + ",{\"kind\":\"HOUSE\",\"name\":\"1\"}]" +
                ",\"country\":" + "\"Россия\"" +
                ",\"geoId\":" + String.valueOf(213) +
                ",\"house\":" + "\"1\"" +
                ",\"kind\":" + "\"HOUSE\"" +
                ",\"precision\":" + "\"EXACT\"" +
                ",\"street\":" + "\"Красная площадь\"" +
                ",\"text\":" + "\"Россия, Москва, Красная площадь, 1\"" +
                ",\"x\":" + String.valueOf(37.617716) +
                ",\"x1\":" + String.valueOf(37.613611) +
                ",\"x2\":" + String.valueOf(37.621821) +
                ",\"y\":" + String.valueOf(55.755322) +
                ",\"y1\":" + String.valueOf(55.753007) +
                ",\"y2\":" + String.valueOf(55.757637) + "}"
                + "]"));
    }
}
