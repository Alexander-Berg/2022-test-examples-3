package ru.yandex.autotests.httpclient.metabeanprocessor.beans;

public class ThirdTestBeanBuilder {
    private FourthTestBean email;
    private String select;

    public ThirdTestBeanBuilder setEmail(FourthTestBean email) {
        this.email = email;
        return this;
    }

    public ThirdTestBeanBuilder setSelect(String select) {
        this.select = select;
        return this;
    }

    public ThirdTestBean createThirdTestBean() {
        return new ThirdTestBean(email, select);
    }
}