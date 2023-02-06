package ru.yandex.market.tpl.dora.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

public abstract class BaseShallowTest {

    @Autowired
    protected MockMvc mockMvc;
}
