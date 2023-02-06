package ru.yandex.market.crm.platform.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

import ru.yandex.market.crm.platform.commons.CustomOptions;
import ru.yandex.market.crm.platform.config.impl.Configurations;
import ru.yandex.market.crm.platform.config.impl.FactFields;
import ru.yandex.market.crm.platform.config.raw.StorageType;
import ru.yandex.market.crm.platform.config.uid.UserIdsStrategy;
import ru.yandex.market.crm.util.ReflectionUtils;

public class TestConfigs {

    public static StorageConfig store() {
        return new StorageConfig(null, StorageType.HDD);
    }

    public static <T extends Message> Model model(Class<T> cls) {
        Parser<T> parser = ReflectionUtils.invokeStatic(cls, "parser");
        Descriptors.Descriptor descriptor = ReflectionUtils.invokeStatic(cls, "getDescriptor");
        Descriptors.FieldDescriptor ids = Configurations.getFirstFieldDescriptor(
                descriptor,
                CustomOptions.uid,
                true
        );

        return new Model(
                parser,
                descriptor,
                ids == null ? null : new UserIdsStrategy(ids),
                Configurations.getFirstFieldDescriptor(descriptor, CustomOptions.id, true),
                Configurations.getFirstFieldDescriptor(descriptor, CustomOptions.time, true),
                descriptor.findFieldByName(FactFields.RGB),
                null
        );
    }

    public static FactConfig factConfig(String id, Class<? extends Message> cls) {
        return new FactConfig(
                id,
                id,
                Collections.singletonList(InboxSourceConfig.INSTANCE),
                model(cls),
                null,
                null,
                List.of(),
                Map.of("hahn", store())
        );
    }
}
