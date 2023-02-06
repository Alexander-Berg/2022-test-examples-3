package ru.yandex.market.checkout.application;

import org.springframework.test.web.servlet.MockMvc;

/**
 * @author mkasumov
 */
public class AbstractWebTestHelper {

    protected MockMvc mockMvc;

    public AbstractWebTestHelper(AbstractWebTestBase test) {
        this.mockMvc = test.getMockMvc();

        test.getWebApplicationContext().getAutowireCapableBeanFactory().autowireBean(this);
    }
}
