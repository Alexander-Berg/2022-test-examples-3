package ru.yandex.autotests.direct.utils.matchers;

import org.junit.Test;
import ru.yandex.autotests.direct.utils.beans.SomeBean;

import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;
import static ru.yandex.autotests.direct.utils.matchers.BeanEqualsAssert.assertThat;

/**
 * User: xy6er
 * Date: 29.11.13
 * Time: 7:24
 */

public class DifferentBeansAssertTest {

    /**
     * Матчер умеет сравнивать бины разных классов
     *  для этого надо использовать кастомный BeanEqualsAssert
     */
    @Test
    public void differentBeansAssert() {
        SomeBean actualBean = new SomeBean("stringVal");
        actualBean.setIntValue(99);
        actualBean.setDoubleValue(1234.56);
        actualBean.setEnumField(SomeBean.Enum.FIRST);

        OtherBean otherBean = new OtherBean();
        otherBean.setIntValue(actualBean.getIntValue());
        otherBean.setStringValue(actualBean.getStringValue());

        assertThat(actualBean, beanEquals(otherBean));
    }

    public static class OtherBean {
        private int intValue;
        private String stringValue;
        private String otherField;

        public int getIntValue() {
            return intValue;
        }

        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public String getOtherField() {
            return otherField;
        }

        public void setOtherField(String otherField) {
            this.otherField = otherField;
        }
    }

}
