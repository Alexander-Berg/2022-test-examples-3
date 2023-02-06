package ru.yandex.direct.internaltools.core.input;

import java.util.Collections;
import java.util.List;

import ru.yandex.direct.internaltools.core.bootstrap.InternalToolInputBootstrap;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;

public class InternalToolInputTestUtil {
    private static final int SHARDS_NUM = 8;

    private InternalToolInputTestUtil() {
    }

    public static <T extends InternalToolParameter> InternalToolInput<T, ?> getInput(Class<T> cls, String name)
            throws NoSuchFieldException {
        return getInput(cls, Collections.emptyList(), name);
    }

    public static <T extends InternalToolParameter> InternalToolInput<T, ?> getInput(Class<T> cls,
                                                                                     List<InternalToolInputPreProcessor<?>> preProcessors, String name)
            throws NoSuchFieldException {
        return InternalToolInputBootstrap
                .inputFromField(cls, cls.getDeclaredField(name), preProcessors, SHARDS_NUM);
    }

    public static <T extends InternalToolParameter> InternalToolInputGroup<T> getGroup(Class<T> cls, String name)
            throws NoSuchFieldException {
        return InternalToolInputBootstrap
                .groupFromField(cls.getDeclaredField(name));
    }
}
