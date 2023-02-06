package ru.yandex.autotests.direct.httpclient.util;

import ru.yandex.autotests.direct.utils.beans.MongoBeanLoader;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 22.09.14
 */
public class PropertyLoader<T> {

    private Class<T> beanClass;
    private T bean;

    public PropertyLoader(Class<T> beanClass) {
        this.beanClass = beanClass;
        instantiateBean();
    }

    private void instantiateBean() {
        try {
            this.bean = beanClass.newInstance();
        } catch (Exception e) {
            throw new Error("Cannot create new instance of bean " + bean.getClass().getName());
        }
    }

    private MongoBeanLoader<T> getHttpLoader() {
        return new MongoBeanLoader<>(beanClass, "DirectHttpTemplates");
    }

    public T getHttpBean(String templateName) {
        return getHttpLoader().getBean(templateName);
    }

    public void saveHttpBean(T bean, String templateName) {
        getHttpLoader().saveBean(bean, templateName);
    }

    private MongoBeanLoader<T> getDirectWebLoader() {
        return new MongoBeanLoader<>(beanClass, DirectTestRunProperties.getInstance()
                .getDirectWebMongoTemplatesCollection());
    }

    public T getDirectWebBean(String templateName) {
        return getDirectWebLoader().getBean(templateName);
    }

    public void saveDirectWebBean(T bean, String templateName) {
                getDirectWebLoader().saveBean(bean, templateName);
    }

}
