package ru.yandex.market.logistics.util.client.tvm;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.logistics.util.client.tvm.config.TestConfiguration;

@ExtendWith(SpringExtension.class)
@MockBean({
    TvmClientApi.class
})
@SpringBootTest(
    classes = {
        TestConfiguration.class,
        JacksonAutoConfiguration.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
abstract class AbstractContextualTest {

    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Autowired
    protected AuthenticationManager authenticationManager;
    @Autowired
    protected TvmClientApi tvmClient;
}

