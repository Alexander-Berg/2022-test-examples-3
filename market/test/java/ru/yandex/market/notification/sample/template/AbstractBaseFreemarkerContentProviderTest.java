package ru.yandex.market.notification.sample.template;

import java.io.IOException;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.Ignore;

/**
 * Базовый абстрактный класс для тестирования контент провайдеров на движке Freemarker.
 *
 * @author avetokhin 16/06/16.
 */
@Ignore
public abstract class AbstractBaseFreemarkerContentProviderTest {

    private static final Configuration CONFIGURATION = createConfiguration();


    Template getTemplate(final String name) throws IOException {
        return CONFIGURATION.getTemplate(name);
    }


    private static Configuration createConfiguration() {
        final ClassTemplateLoader templateLoader =
            new ClassTemplateLoader(AbstractBaseFreemarkerContentProviderTest.class, "/freemarker");

        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_21);
        configuration.setTemplateLoader(templateLoader);
        return configuration;
    }

}
