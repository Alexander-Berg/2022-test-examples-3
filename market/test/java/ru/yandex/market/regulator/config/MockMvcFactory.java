package ru.yandex.market.regulator.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

/**
 * @author Anastasiya Emelianova / orphie@ / 10/11/21
 */
public class MockMvcFactory {
    @Autowired
    private WebApplicationContext wac;

    public MockMvc getMockMvc() {
        return MockMvcBuilders.webAppContextSetup(this.wac).alwaysDo(log()).build();
    }
}
