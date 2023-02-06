package ru.yandex.autotests.httpclient.metabeanprocessor.beans;

public class FourthTestBeanBuilder {
    private String email;

    public FourthTestBeanBuilder setEmail(String email) {
        this.email = email;
        return this;
    }

    public FourthTestBean createFourthTestBean() {
        return new FourthTestBean(email);
    }
}