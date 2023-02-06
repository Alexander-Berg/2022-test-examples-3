package ru.yandex.common.magic;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.common.magic.defender.DefenderException;
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
public class DefenderTest {

    @Autowired
    private MockCounterAspect counterInterceptor;

    @Autowired
    private MagicServiceFinder magicServiceFinder;

    @Test(expected = DefenderException.class)
    public void testBillGates() {
        Hello service = (Hello) magicServiceFinder.findService("hello");
        service.sayHello("Bill Gates");
    }

    @Test
    public void testAlexeyShevenkov() {
        Hello service = (Hello) magicServiceFinder.findService("hello");
        service.sayHello("Alexey Shevenkov");
    }

    @Test
    public void testCounter() {
        counterInterceptor.setCalled(false);
        counterInterceptor.setGotcha(false);

        Hello service = (Hello) magicServiceFinder.findService("hello");
        service.callHello("Alexey Shevenkov");

        Assert.assertTrue(counterInterceptor.isCalled());
        Assert.assertTrue(counterInterceptor.isGotcha());
    }
}
