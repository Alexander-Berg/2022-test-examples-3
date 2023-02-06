package ru.yandex.market.jmf.bcp.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.attributes.AttributesTestConfiguration;
import ru.yandex.market.jmf.bcp.BcpConfiguration;

@Configuration
@Import({
        BcpConfiguration.class,
        AttributesTestConfiguration.class
})
public class BcpTestConfiguration {
}
