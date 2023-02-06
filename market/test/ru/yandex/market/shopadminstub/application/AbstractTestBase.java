package ru.yandex.market.shopadminstub.application;

import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.shopadminstub.context.MockContext;

@ActiveProfiles("test")
@SpringBootTest(classes = {MockContext.class})
// Работает не за счет ru.yandex.market.application.AppPropertyPlaceholderConfigurer, а за счет SpringBoot.
@TestPropertySource(value = {"classpath:00_servant.properties", "classpath:int-test.properties"})
@WebAppConfiguration
@ExtendWith(SpringExtension.class)
public abstract class AbstractTestBase {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CuratorFramework curatorFramework;

    @Autowired
    private List<WireMockServer> wireMockList;

    @Value("${shopadminstub.feed.dispatcher.blacklist.path}")
    private String blackListPath;

    @Autowired
    private TestableClock clock;

    protected MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    public void tearDownBase() throws Exception {
        wireMockList.forEach(WireMockServer::resetAll);

        for (String directory : new String[]{"/shopadminstub/regions", "/shopadminstub/paymentmethods",
                blackListPath}) {
            for (String shop : curatorFramework.getChildren().forPath(directory)) {
                curatorFramework.delete()
                        .deletingChildrenIfNeeded()
                        .forPath(directory + "/" + shop);
            }
        }

        clock.clearFixed();
    }
}
