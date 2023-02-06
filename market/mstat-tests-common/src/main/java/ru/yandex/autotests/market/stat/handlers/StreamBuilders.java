package ru.yandex.autotests.market.stat.handlers;

import org.beanio.builder.GroupBuilder;
import org.beanio.builder.StreamBuilder;
import java.time.LocalDateTime;

/**
 * Created by entarrion on 14.01.15.
 */
public class StreamBuilders {

    public static StreamBuilder jsonStreamBuilderWithDay(Class<?> mappingClass) {
        return getJsonStreamBuilder(mappingClass)
            .addTypeHandler(Handlers.EVENTTIME_HANDLER, LocalDateTime.class, new DateHandler())
            .addTypeHandler(Handlers.EVENTTIME_HANDLER, LocalDateTime.class, new RawFilesTimeHandler());
    }

    private static StreamBuilder getJsonStreamBuilder(Class<?> mappingClass) {
        String streamMappingName = mappingName(mappingClass);
        return new StreamBuilder(streamMappingName)
            .format("json")
            .addGroup(new GroupBuilder("records").addRecord(mappingClass));
    }

    public static String mappingName(Class<?> mappingClass) {
        return mappingClass.toString();
    }
}
