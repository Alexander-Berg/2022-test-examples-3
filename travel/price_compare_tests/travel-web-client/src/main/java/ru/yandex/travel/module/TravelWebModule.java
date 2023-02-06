package ru.yandex.travel.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.junit.rules.TestRule;
import ru.yandex.travel.junit.RuleChainModule;
import ru.yandex.travel.webdriver.WebDriverModule;
import ru.yandex.travel.webdriver.WebDriverResource;

public class TravelWebModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);
        rulesBinder.addBinding().to(WebDriverResource.class);

        install(new WebDriverModule());
        install(new RuleChainModule());
    }
}
