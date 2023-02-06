package ru.yandex.market.abo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.abo.web.AboDispatcherServlet;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * @author artemmz
 * @date 22.03.18.
 */
public class MainMockMvcFactory {
    @Autowired
    private AboDispatcherServlet aboDispatcherServlet;

    public MockMvc getMockMvc() {
        WebApplicationContext webAppContext = aboDispatcherServlet.getWebApplicationContext();
        return MockMvcBuilders.webAppContextSetup(webAppContext)
                .apply(springSecurity())
                .build();
    }
}
