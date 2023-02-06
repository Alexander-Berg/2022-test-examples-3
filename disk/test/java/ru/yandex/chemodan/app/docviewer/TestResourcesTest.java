package ru.yandex.chemodan.app.docviewer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.misc.test.Assert;

@RunWith(Parameterized.class)
public class TestResourcesTest {
    private static final Logger logger = LoggerFactory.getLogger(TestResourcesTest.class);

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<>();
        for (Field field : TestResources.class.getFields()) {
            if (!Modifier.isPublic(field.getModifiers())
                    || !Modifier.isStatic(field.getModifiers())
                    || !URL.class.equals(field.getType()))
                continue;
            try {
                final URL url = (URL) field.get(null);
                data.add(new Object[] { field.getName(), url });
            } catch (Exception exc) {
                logger.error("Unable to add URL from field '" + field + "': " + exc, exc);
            }
        }
        return data;
    }

    private final String fieldName;

    private final URL url;

    public TestResourcesTest(String fieldName, URL url) {
        this.fieldName = fieldName;
        this.url = url;
    }

    @Test
    public void test() {
        Assert.assertNotNull("Field '" + fieldName
                + "' of TestResources points to missing resource", url);
    }
}
