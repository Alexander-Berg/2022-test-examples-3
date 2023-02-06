package ru.yandex.market.deepdive;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.deepdive.configuration.IntegrationTestConfiguration;

@ExtendWith(SpringExtension.class)
@Slf4j
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = IntegrationTestConfiguration.class)
@AutoConfigureMockMvc(secure = false)
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
public class AbstractClientTest {
    @Value("${pvz-int.api.url:https://pvz-int.tst.vs.market.yandex.net/}")
    protected String pvzIntUrl;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected RestTemplate clientRestTemplate;

    protected MockRestServiceServer mock;

    @BeforeEach
    public void setUp() {
        mock = MockRestServiceServer.createServer(clientRestTemplate);
    }

    @AfterEach
    public void tearDown() {
        mock.verify();
    }
}
