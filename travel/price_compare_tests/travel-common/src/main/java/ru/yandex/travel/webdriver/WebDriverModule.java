package ru.yandex.travel.webdriver;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.aeonbits.owner.ConfigFactory;
import ru.yandex.travel.guice.CustomScopes;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class WebDriverModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(WebDriverManager.class).to(DefaultWebDriverManager.class).in(CustomScopes.THREAD);
    }

    @Provides
    public WebDriverConfig provideConfig() {
        return ConfigFactory.newInstance().create(WebDriverConfig.class);
    }

}
