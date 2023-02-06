package ru.yandex.market.logistics.util.client.tvm;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.util.client.tvm.config.TestClientConfiguration;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    TestClientConfiguration.class,
})
@ParametersAreNonnullByDefault
public class AbstractClientTest {
    @RegisterExtension
    final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

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
