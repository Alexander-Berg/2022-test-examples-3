package ru.yandex.direct.web.entity.crypta.service;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.configuration.CommonConfiguration;
import ru.yandex.direct.web.configuration.DirectWebConfiguration;
import ru.yandex.direct.web.core.entity.inventori.service.CryptaService;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {CommonConfiguration.class, DirectWebConfiguration.class})
public class CryptaGoalsTest {

    @Autowired
    private CryptaService cryptaService;

    @Test
    @Ignore("only for manual runs, because it connects to real database")
    public void getSegments() {
        cryptaService.getSegments();
    }
}
