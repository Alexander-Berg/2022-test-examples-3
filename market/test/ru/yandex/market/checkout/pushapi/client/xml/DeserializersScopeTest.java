package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.yandex.common.util.xml.parser.StackableElementOrientedSAXHandler;

import java.util.Map;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(locations = {
        "classpath:ru/yandex/market/checkout/pushapi/client/http/PushApiHttpComponentsClientHttpRequestFactoryTest.xml",
        "classpath:WEB-INF/push-api-client.xml"
})
public class DeserializersScopeTest {

    @Autowired
    private ConfigurableApplicationContext context;


    @Test
    public void testSaxDeserializersScope() {
        Map<String, StackableElementOrientedSAXHandler> beans = context.getBeansOfType(StackableElementOrientedSAXHandler.class);
        beans.keySet().forEach(name -> {
                    String scope = context.getBeanFactory().getBeanDefinition(name).getScope();
                    Assertions.assertEquals(scope,"prototype","Bean '" + name + "' should have prototype scope for correct multithreading parsing.");
                }
        );
    }
}
