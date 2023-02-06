package ru.yandex.market.tpl.internal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;

@TplIntWebTest
public abstract class BaseTplIntWebTest {
    protected static final Long UID = 1L;

    @Autowired
    protected BlackboxClient blackboxClient;

    @Autowired
    protected MockMvc mockMvc;
}
