package ru.yandex.market.tsum.pipelines.common.jobs.datasource;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 15/10/2017
 */
public class DataSourcePropertyTest {
    @Test
    public void testTemplate() {
        checkTemplate(
            "${prefix}",
            "${key}",
            "${value}-${value}",
            ImmutableMap.of("key", "k1", "value", "v1", "prefix", "test-prefix"),
            "test-prefix",
            "k1",
            "v1-v1"
        );
    }

    @Test
    public void testDefaultValue() {
        checkTemplate(
            null,
            "key",
            "${missing:123}",
            ImmutableMap.of(),
            DataSourceProperty.COMMON_SECTION,
            "key",
            "123"
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoValues() {
        new DataSourceProperty.Template(DataSourceProperty.Type.JAVA, null, "key", "${missing}")
            .resolve(ImmutableMap.of());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyKey() {
        new DataSourceProperty.Template(DataSourceProperty.Type.JAVA, DataSourceProperty.COMMON_SECTION, "${empty}", "")
            .resolve(ImmutableMap.of("empty", ""));
    }

    private static void checkTemplate(String sectionTemplate, String keyTemplate, String valueTemplate,
                                      Map<String, Object> variables, String expectedSection,
                                      String expectedKey, String expectedValue) {
        DataSourceProperty property =
            new DataSourceProperty.Template(DataSourceProperty.Type.JAVA, sectionTemplate, keyTemplate, valueTemplate)
                .resolve(variables);
        Assert.assertEquals(expectedKey, property.getKey());
        Assert.assertEquals(expectedValue, property.getValue());
        Assert.assertEquals(expectedSection, property.getSection());
    }
}
