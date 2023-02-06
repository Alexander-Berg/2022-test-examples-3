package ru.yandex.autotests.direct.httpclient.util.beanmapper;

/**
 * Created by shmykov on 10.02.15.
 */
public class InnerBuilderAnnotationClassError extends Error {

    public InnerBuilderAnnotationClassError(String fieldName) {
        super("Поле " + fieldName + " с аннотацией @InnerBuilder должно быть наследником класса BeanMappingBuilder");
    }
}
