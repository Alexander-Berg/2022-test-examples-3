package ru.yandex.autotests.direct.utils.matchers;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.autotests.irt.testutils.beans.BeanHelper;
import ru.yandex.autotests.direct.utils.beans.SomeBean;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;
import static ru.yandex.autotests.irt.testutils.matchers.NumberApproximatelyEqual.approxEqualTo;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class BeanEqualMatcherTest {
    private SomeBean expectedBean;
    private SomeBean actualBean;

    @Before
    public void before() {
        expectedBean = new SomeBean("stringVal");
        expectedBean.setIntValue(99);
        expectedBean.setIntegerValue(321);
        expectedBean.setDoubleValue(1234.56);
        expectedBean.setEnumField(SomeBean.Enum.FIRST);

        actualBean = new SomeBean();
        BeanHelper.copyProperties(actualBean, expectedBean);
    }

    @Test
    public void beansAssert() {
        assertThat(actualBean, beanEquals(expectedBean));
    }

    @Test(expected = AssertionError.class)
    public void beansAssertShouldFail() {
        actualBean.setStringValue("actual string val");
        actualBean.setEnumField(SomeBean.Enum.SECOND);

        assertThat(actualBean, beanEquals(expectedBean));
    }

    /**
     * Матчер сравнивает только простые поля(primitiveType, String, enum, T extends Number)
     */
    @Test
    public void matcherNotCompareNotSimpleFields() {
        expectedBean.setSomeBeanField(new SomeBean("expected string"));
        expectedBean.setArray(new String[]{"exp1", "exp2"});
        expectedBean.setList(Arrays.asList(expectedBean.getArray()));

        actualBean.setSomeBeanField(new SomeBean("actual string"));
        actualBean.setArray(new String[]{"act1", "act2"});
        actualBean.setList(Arrays.asList(actualBean.getArray()));

        assertThat(actualBean, beanEquals(expectedBean));
    }

    /**
     * Кастомные матчеры для полей бина
     *
     * Дефолтный матчер для полей: equalTo(expectedValue)
     */
    @Test
    public void beansAssertWithStrategy() {
        actualBean.setIntValue(101);
        actualBean.setStringValue("actual string");

        BeanCompareStrategy beanCompareStrategy = new BeanCompareStrategy();
        beanCompareStrategy.putFieldMatcher("intValue", approxEqualTo(expectedBean.getIntValue()).withDifference(5));
        beanCompareStrategy.putFieldMatcher("stringValue", not(equalTo(expectedBean.getStringValue())));

        assertThat(actualBean, beanEquals(expectedBean).accordingStrategy(beanCompareStrategy));
    }

    /**
     * Матчер не сравнивает поля значение которых null в ожидаемом бине
     */
    @Test
    public void matcherNotCompareFieldsWithNullInExpectedBean() {
        expectedBean.setStringValue(null);
        expectedBean.setEnumField(null);

        assertThat(actualBean, beanEquals(expectedBean));
    }

    /**
     * Проверка поля актуального бина на null
     */
    @Test(expected = AssertionError.class)
    public void checkNullFieldsInActualBean() {
        expectedBean.setStringValue(null);

        BeanCompareStrategy beanCompareStrategy = new BeanCompareStrategy();
        beanCompareStrategy.putFieldMatcher("stringValue", equalTo(null));

        assertThat(actualBean, beanEquals(expectedBean).accordingStrategy(beanCompareStrategy));
    }

    /**
     * Матчер не сравнивает игнорируемые поля
     */
    @Test
    public void matcherNotCompareIgnoredFields() {
        expectedBean.setStringValue("new value");
        expectedBean.setIntValue(1635);

        assertThat(actualBean, beanEquals(expectedBean).ignoreFields("stringValue", "intValue"));
    }

    /**
     * Матчер игнорирует все поля кроме указанных
     */
    @Test
    public void matcherCompareOnlyNeededFields() {
        expectedBean.setStringValue("new value");
        expectedBean.setIntValue(1635);

        assertThat(actualBean, beanEquals(expectedBean).byFields("doubleValue", "enumField"));
    }
}
