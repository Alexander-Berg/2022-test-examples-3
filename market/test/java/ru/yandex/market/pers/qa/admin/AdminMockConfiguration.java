package ru.yandex.market.pers.qa.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.framework.user.blackbox.BlackBoxService;
import ru.yandex.market.cataloger.CatalogerClient;
import ru.yandex.market.pers.qa.CoreMockConfiguration;
import ru.yandex.market.shopinfo.ShopInfoService;

@Configuration
public class AdminMockConfiguration extends CoreMockConfiguration {

    @Autowired
    private WebApplicationContext wac;

    @Bean
    @Qualifier("mockMvc")
    public MockMvc getMockMvc() {
        return MockMvcBuilders.webAppContextSetup(this.wac)
            .dispatchOptions(true).build();
    }

    @Qualifier("regular")
    @Bean
    public BlackBoxService blackBoxService() {
        return PersQaAdminMockFactory.blackBoxMock();
    }

    @Qualifier("internal")
    @Bean
    public BlackBoxService blackBoxServiceInternal() {
        return PersQaAdminMockFactory.blackBoxMock();
    }

    @Bean
    public ShopInfoService shopInfoService() {
        return PersQaAdminMockFactory.shopInfoServiceMock();
    }

    @Bean
    public CatalogerClient catalogerClient() {
        return PersQaAdminMockFactory.catalogerClientMock();
    }
}
