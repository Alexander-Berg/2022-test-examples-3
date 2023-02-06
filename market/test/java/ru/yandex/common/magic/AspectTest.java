package ru.yandex.common.magic;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.common.magic.hello.Hello;
import ru.yandex.common.magic.service.MagicServiceFinder;

/**
 * @author ashevenkov
 * @author amaslak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:common-magic/common-magic.xml",
        "classpath:common-magic/hello-service.xml"
})
public class AspectTest {

    @Autowired
    private MagicServiceFinder magicServiceFinder;

    @Test
    public void testAspects() {
        Hello service = (Hello) magicServiceFinder.findService("hello");
        String result = service.sayHello("operationName");
        Assert.assertEquals(result, "Hello my dear friend, operationName");
    }
}
