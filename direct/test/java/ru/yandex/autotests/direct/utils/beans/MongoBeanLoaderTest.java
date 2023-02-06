package ru.yandex.autotests.direct.utils.beans;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.autotests.direct.utils.DirectUtilsException;

import java.util.List;

import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;
import static ru.yandex.autotests.direct.utils.matchers.BeanEqualsAssert.assertThat;

/**
 * User: xy6er
 * Date: 14.10.13
 * Time: 7:43
 */

public class MongoBeanLoaderTest {
    private static final String TEMPLATE_NAME = "SOME_BEAN";
    private static final String COLLECTION_NAME = "testCollection";
    private MongoBeanLoader<SomeBean> loader;
    private SomeBean expectedBean;

    @Before
    public void before() {
        loader = new MongoBeanLoader<>(SomeBean.class, COLLECTION_NAME);
        expectedBean = new SomeBean("stringVal");
        expectedBean.setIntValue(14);
        expectedBean.setIntegerValue(123);
        expectedBean.setDoubleValue(1234.5);
        expectedBean.setEnumField(SomeBean.Enum.FIRST);
        expectedBean.setSomeBeanField(new SomeBean("yooo"));
        expectedBean.setArray(new String[]{"1111", "22222"});
    }

    @After
    public void after() {
        loader.removeBean(TEMPLATE_NAME);
    }

    @Test
    public void canSaveBean() {
        loader.saveBean(expectedBean, TEMPLATE_NAME);
    }
    @Test
    public void canSaveBeanWithNotTypifiedMongoBeanLoader() {
        MongoBeanLoader<SomeBean> beanLoader = new MongoBeanLoader<>(SomeBean.class, COLLECTION_NAME);
        beanLoader.saveBean(expectedBean, TEMPLATE_NAME);
    }

    @Test
    public void checkGetBean() {
        loader.saveBean(expectedBean, TEMPLATE_NAME);
        SomeBean actualBean = loader.getBean(TEMPLATE_NAME);
        assertThat(actualBean, beanEquals(expectedBean));
    }
    @Test
    public void checkGetBeanWithNotTypifiedMongoBeanLoader() {
        MongoBeanLoader<SomeBean> beanLoader = new MongoBeanLoader<>(SomeBean.class, COLLECTION_NAME);
        beanLoader.saveBean(expectedBean, TEMPLATE_NAME);
        SomeBean actualBean = beanLoader.getBean(TEMPLATE_NAME);
        assertThat(actualBean, beanEquals(expectedBean));
    }

    @Test
    public void canRemoveBean() {
        loader.saveBean(expectedBean, TEMPLATE_NAME);
        loader.removeBean(TEMPLATE_NAME);
    }
    @Test(expected = DirectUtilsException.class)
    public void checkRemoveBean() {
        loader.saveBean(expectedBean, TEMPLATE_NAME);
        loader.removeBean(TEMPLATE_NAME);
        loader.getBean(TEMPLATE_NAME);
    }

    @Test
    public void loadBeansTest() {
        loader.saveBean(expectedBean, TEMPLATE_NAME);
        List<SomeBean> beans = loader.getBeans(TEMPLATE_NAME);
    }

}
