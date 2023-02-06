package ru.yandex.direct.i18n.localization;

import org.junit.Test;

public class LocalizationMapperBuilderTest {

    class TestObj {
        String s;

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }
    }

    @Test(expected = NullPointerException.class)
    public void nullGetter_Error() {
        LocalizationMapper.builder()
                .addEnTranslation(null);
    }

    @Test(expected = NullPointerException.class)
    public void nullSetter_Error() {
        LocalizationMapper.builder()
                .addEnTranslation(TestObj::getS)
                .translateTo(null);
    }

    @Test(expected = NullPointerException.class)
    public void nullCreator_Error() {
        LocalizationMapper.builder()
                .addEnTranslation(TestObj::getS)
                .translateTo(TestObj::setS)
                .createBy(null);
    }

    @Test(expected = NullPointerException.class)
    public void nullCopyFunction_Error() {
        LocalizationMapper.builder()
                .addEnTranslation(TestObj::getS)
                .translateTo(TestObj::setS)
                .copyBy(null);
    }
}
