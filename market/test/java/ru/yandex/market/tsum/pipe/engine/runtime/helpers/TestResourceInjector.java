package ru.yandex.market.tsum.pipe.engine.runtime.helpers;

import com.google.common.base.Preconditions;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.definition.common.SourceCodeEntity;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResourceList;
import ru.yandex.market.tsum.pipe.engine.runtime.ReflectionUtils;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceInjector;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.AbstractResourceContainer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class TestResourceInjector implements ResourceInjector {

    public TestResourceInjector() {

    }

    @Override
    public void inject(SourceCodeEntity instance, AbstractResourceContainer resources) {
        try {
            List<Field> declaredFields = ReflectionUtils.getAllFields(new ArrayList<>(), instance.getClass());

            for (Field field : declaredFields) {
                WiredResource resourceAnnotation = field.getAnnotation(WiredResource.class);
                WiredResourceList resourceListAnnotation = field.getAnnotation(WiredResourceList.class);

                Preconditions.checkState(
                    resourceAnnotation == null || resourceListAnnotation == null,
                    "A field cannot have both @WiredResource and @WiredResourceList at the same time, " +
                        "executor class: " + instance.getClass().getName() + ", field: " + field.getName()
                );

                field.setAccessible(true);
                if (resourceAnnotation != null) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Resource> fieldType = (Class<? extends Resource>) field.getType();

                    if (resources.containsOfType(fieldType)) {
                        Resource resource = resources.getSingleOfType(fieldType);
                        field.set(instance, resource);
                    } else {
                        Preconditions.checkState(
                            resourceAnnotation.optional(), "Required resource not provided %s",
                            fieldType.getName()
                        );
                    }
                } else if (resourceListAnnotation != null) {
                    Class<? extends Resource> type = resourceListAnnotation.value();
                    List<? extends Resource> resource = resources.getOfType(type);
                    field.set(instance, resource);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to inject resources", e);
        }
    }
}
