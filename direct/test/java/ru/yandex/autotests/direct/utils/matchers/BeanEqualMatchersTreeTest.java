package ru.yandex.autotests.direct.utils.matchers;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.autotests.irt.testutils.beans.BeanHelper;
import ru.yandex.autotests.direct.utils.beans.ChildBean;
import ru.yandex.autotests.direct.utils.beans.ParentBean;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class BeanEqualMatchersTreeTest {
    private ParentBean expectedBean;
    private ParentBean actualBean;

    @Before
    public void before() {
        expectedBean = new ParentBean();
        expectedBean.setField1("parent field1 value");
        expectedBean.setField2("parent field2 value");

        ChildBean childBeanExpected = new ChildBean();
        childBeanExpected.setField1("child field1 value");
        childBeanExpected.setField2("child field2 value");
        expectedBean.setChildBean(childBeanExpected);

        actualBean = new ParentBean();
        BeanHelper.copyProperties(actualBean, expectedBean);

        ChildBean childBeanActual = new ChildBean();
        BeanHelper.copyProperties(childBeanActual, childBeanExpected);
        actualBean.setChildBean(childBeanActual);
    }

    @Test(expected = AssertionError.class)
    public void shouldMatchChildBeans() {
        BeanCompareStrategy beanCompareStrategy = new BeanCompareStrategy();
        expectedBean.getChildBean().setField1("child new value for field1");
        beanCompareStrategy.putFieldMatcher("childBean", beanEquals(expectedBean.getChildBean()));

        assertThat("", actualBean, beanEquals(expectedBean).accordingStrategy(beanCompareStrategy));
    }
}
