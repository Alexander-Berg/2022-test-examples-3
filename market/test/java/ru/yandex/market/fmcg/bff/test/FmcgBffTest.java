package ru.yandex.market.fmcg.bff.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.fmcg.bff.config.BffInternalConfig;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 22/05/2018
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    BffInternalConfig.class,
    FmcgBffMockConfig.class
})
@WebAppConfiguration
@TestPropertySource("classpath:test.properties")
public abstract class FmcgBffTest {

    @Autowired
    MockRestServiceServer shopIntegrationMockServer;

    @BeforeEach
    public void resetMocks() {
        FmcgBffMockFactory.resetMocks();
        shopIntegrationMockServer.reset();
    }

}
