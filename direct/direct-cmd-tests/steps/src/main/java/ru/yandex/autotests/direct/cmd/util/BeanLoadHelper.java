package ru.yandex.autotests.direct.cmd.util;

import ru.yandex.autotests.direct.utils.beans.MongoBeanLoader;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;

public class BeanLoadHelper {

    public static <T> T loadCmdBean(String templateName, Class<T> beanClass) {
        String templatesCollection = DirectTestRunProperties.getInstance().getDirectCmdMongoTemplatesCollection();
        MongoBeanLoader<T> loader = new MongoBeanLoader<>(beanClass, templatesCollection);
        return loader.getBean(templateName);
    }

    public static <T> void saveCmdBean(String templateName, T bean, Class<T> beanClass) {
        String templatesCollection = DirectTestRunProperties.getInstance().getDirectCmdMongoTemplatesCollection();
        MongoBeanLoader<T> loader = new MongoBeanLoader<>(beanClass, templatesCollection);
        loader.saveBean(bean, templateName);
    }

    public static <T> void removeCmdBean(String templateName, Class<T> beanClass) {
        String templatesCollection = DirectTestRunProperties.getInstance().getDirectCmdMongoTemplatesCollection();
        MongoBeanLoader<T> loader = new MongoBeanLoader<>(beanClass, templatesCollection);
        loader.removeBean(templateName);
    }
}
