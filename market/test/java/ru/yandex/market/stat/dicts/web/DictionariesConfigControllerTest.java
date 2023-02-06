package ru.yandex.market.stat.dicts.web;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

public class DictionariesConfigControllerTest {

    @Test
    public void testConf() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        DictionariesConfigController controller = new DictionariesConfigController();
        mapper.readTree(controller.getConfigAsJson("anaplan"));
    }
}