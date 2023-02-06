package ru.yandex.autotests.direct.httpclient.util.beanmapper;

import org.dozer.classmap.ClassMap;
import org.dozer.classmap.MappingFileData;
import org.dozer.loader.api.BeanMappingBuilder;

import java.lang.reflect.Field;

/**
 * Created by shmykov on 09.02.15.
 */
public abstract class HierarchicBeanMappingBuilder extends BeanMappingBuilder {

    @Override
    public MappingFileData build() {
        MappingFileData data = super.build();
        completeMappingFileData(data);
        return data;
    }
    /**
     * Метод для связывания вложенных сущностей BeanMappingBuilder.
     * Например, пусть есть бин Banner, содержащий бин Phrase, и для обоих сущностей определены маппинг билдеры
     * BannerMappingBuilder и PhraseMappingBuilder соответственно.
     * Для того, чтобы при вызове BeanMapper.map() с BannerMappingBuilder в качестве параметра
     * в DozerBeanMapper добавились еще и параметры PhraseMappingBuilder,
     * нужно в BannerMappingBuilder объявить PhraseMappingBuilder с аннотацией @InnerBuilder.
     *
     * Работает в глубину.
     */
    protected void completeMappingFileData(MappingFileData data) {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(InnerBuilder.class)) {
                if (BeanMappingBuilder.class.isAssignableFrom(field.getType())) {
                        addDataFromBeanMappingBuilder(field.getType(), data);
                } else {
                    throw new InnerBuilderAnnotationClassError(field.getName());
                }
            }
        }
    }

    private void addDataFromBeanMappingBuilder(Class builderClazz, MappingFileData data) {
        BeanMappingBuilder builder;
        try {
            builder = (BeanMappingBuilder) builderClazz.newInstance();
        } catch (Exception e) {
            throw new Error("Cannot create new instance of bean " + builderClazz.getName());
        }
        for (ClassMap map : builder.build().getClassMaps()) {
            data.addClassMap(map);
        }
    }
}