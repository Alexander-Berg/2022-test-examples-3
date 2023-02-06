package ru.yandex.market.pvz.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import ru.yandex.market.pvz.internal.config.PvzIntInternalConfiguration;
import ru.yandex.market.tpl.common.web.config.TplJettyConfiguration;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@Import({
        PvzIntInternalConfiguration.class,
        TplJettyConfiguration.class
})
@ComponentScan(basePackages = {
        "ru.yandex.market.tpl.common.db.test"
})
public @interface PvzIntTest {
}
