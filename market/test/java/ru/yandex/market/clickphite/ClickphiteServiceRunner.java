package ru.yandex.market.clickphite;

import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

@org.junit.Ignore
public class ClickphiteServiceRunner {

    private ClickphiteService service;

    @Before
    public void setUp() throws Exception {
//        DOMConfigurator.configure("/Users/andreevdm/git/market-graphics/clickphite/src/script/log4j-config.xml");
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+3"));
        ApplicationContext cx = new FileSystemXmlApplicationContext("classpath:clickphite.xml");
        service = cx.getBean(ClickphiteService.class);
    }

    @Test
    public void testRun() throws Exception {
//        service.afterPropertiesSet();
        Thread.sleep(42000000);
    }
}
