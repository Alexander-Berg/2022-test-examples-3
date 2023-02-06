package ru.yandex.direct.i18n;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.i18n.dict.BundleDictionarySource;
import ru.yandex.direct.i18n.dict.DictionaryLoader;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@RunWith(Parameterized.class)
public class DictionaryLoaderTest {
    private final DictionaryLoader loader;
    private final URL url;
    private final BundleDictionarySource source;

    public DictionaryLoaderTest(String prefix, String url, BundleDictionarySource source)
            throws MalformedURLException {
        loader = new DictionaryLoader(prefix);
        this.source = source;
        this.url = new URL(url);
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> generateData() {
        return Arrays.asList(new Object[][]{
                {"/locale/", "file:/home/user/dir/production/api5/locale/ru/yandex/direct/api/v5/ApiFaultTranslations.ru.json",
                        new BundleDictionarySource("ru.yandex.direct.api.v5.ApiFaultTranslations", Language.RU, null)},
                {"/locale/", "jar:file:/home/user/dir/archive-1.0-snapshot.jar!/locale/ru/yandex/direct/i18n/ApiErrors.en.json",
                        new BundleDictionarySource("ru.yandex.direct.i18n.ApiErrors", Language.EN, null)}
        });
    }

    @Test
    public void load() throws Exception {
        DefaultCompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields();
        assertThat(loader.makeBundleDictionarySource(url), beanDiffer(source).useCompareStrategy(strategy));
    }
}
