package ru.yandex.market.mbo.gwt.client.widgets.fileupload;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.market.mbo.gwt.client.widgets.image.UploadResponseUnmarshaller;

/**
 * @author s-ermakov
 */
@RunWith(Parameterized.class)
@SuppressWarnings("checkstyle:VisibilityModifier")
public abstract class BaseUploadResponseUnmarshallerTest<T> {

    @Parameterized.Parameter(0)
    public String given;

    @Parameterized.Parameter(1)
    public T expected;

    @Parameterized.Parameter(2)
    public Class<? extends Exception> expectedException;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private UploadResponseUnmarshaller<T> unmarshaller;

    @Before
    public void setUp() throws Exception {
        unmarshaller = getUnmarshaller();
    }

    protected abstract UploadResponseUnmarshaller<T> getUnmarshaller();

    @Test
    public void test() {
        if (expectedException != null) {
            thrown.expect(expectedException);
        }

        T actual = unmarshaller.unmarshal(given);

        assertEquals(expected, actual);
    }

    protected void assertEquals(T expected, T actual) {
        Assert.assertEquals(expected, actual);
    }

    protected static Object[] createExpectedExeption(String given, Class<? extends Exception> expectedException) {
        return new Object[]{given, null, expectedException};
    }

    protected static Object[] createParams(String given, Object expected) {
        return new Object[]{given, expected, null};
    }
}
