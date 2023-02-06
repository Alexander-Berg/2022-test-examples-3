package ru.yandex.market.pricelabs.bindings.csv;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import ru.yandex.common.util.csv.CSVRowMapper;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.impl.ytree.object.YTreeObjectField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.YTreeSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.AbstractYTreeDateSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.YTreeArraySerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.YTreeListSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.YTreeObjectSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeBooleanSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeDoubleSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeDurationSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeEnumSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeFloatSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeIntEnumSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeIntegerSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeJavaInstantSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeLocalDateTimeSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeLongSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeStringEnumSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeStringSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.simple.YTreeUnsignedLongSerializer;
import ru.yandex.market.pricelabs.bindings.yt.YTStyledPriceFieldSerializer;
import ru.yandex.market.pricelabs.bindings.yt.YTreeHyperlinkSerializer;
import ru.yandex.market.pricelabs.exports.format.ObjectFormatter;
import ru.yandex.market.pricelabs.generated.server.pub.model.ExportFileParams.FileTypeEnum;
import ru.yandex.market.pricelabs.misc.PricelabsRuntimeException;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.misc.enums.IntEnum;
import ru.yandex.misc.enums.IntEnumResolver;
import ru.yandex.yt.ytclient.object.MappedRowSerializer;

import static ru.yandex.market.pricelabs.exports.ExportMapper.getDoubleFormat;
import static ru.yandex.market.pricelabs.exports.ExportMapper.getNameToCode;
import static ru.yandex.market.pricelabs.exports.ExportMapper.getTimestampFormat;

@Slf4j
public class CSVMapper<T> {

    private static final Map<YTreeObjectSerializer<?>, CSVMapper<?>> MAPPER_CACHE = new ConcurrentHashMap<>();

    private final YTreeObjectSerializer<T> serializer;
    private final FieldDeserializer[] fields;

    private CSVMapper(CSVMapperBuilder<T> builder) {
        this.serializer = builder.serializer;
        this.fields = builder.fields.values().toArray(new FieldDeserializer[0]);
    }

    public CSVRowMapper<T> csvRowMapper() {
        return new CSVRowMapper<>() {
            @Override
            public void onHeaders(Set<String> headers) {

            }

            @Override
            public T mapRow(Map<String, String> fieldsByNames) {
                return map(fieldsByNames);
            }
        };
    }

    public T map(Map<String, String> row) {
        YTreeBuilder treeBuilder = YTree.builder();
        treeBuilder.beginMap();

        for (FieldDeserializer field : fields) {
            treeBuilder.onKeyedItem(field.key);
            String value = row.get(field.csvKey);
            if (value == null) {
                treeBuilder.onEntity();
            } else {
                try {
                    field.treeBuilder.accept(value, treeBuilder);
                } catch (Exception e) {
                    throw new PricelabsRuntimeException("Unable to map field [" + field.key
                            + "] to value [" + value + "]", e);
                }
            }
        }
        treeBuilder.endMap();
        return serializer.deserialize(treeBuilder.build());
    }


    private static BiConsumer<String, YTreeBuilder> mapSerializer(YTreeObjectField<?> field) {
        YTreeSerializer<?> serializer = MappedRowSerializer.unwrap(field.serializer);
        if (serializer instanceof YTreeIntegerSerializer || serializer instanceof YTreeLongSerializer
                || serializer instanceof YTreeDurationSerializer) {
            return longConsumer(Long::parseLong);
        } else if (serializer instanceof YTreeIntEnumSerializer) {
            YTreeIntEnumSerializer<?> intEnumSerializer = (YTreeIntEnumSerializer<?>) serializer;
            IntEnumResolver<? extends IntEnum> resolver = (IntEnumResolver) intEnumSerializer.getResolver();
            return longConsumer(value -> (long) resolver.valueOf(value).value());
        } else if (serializer instanceof YTreeJavaInstantSerializer) {
            return longConsumer(s -> Utils.parseDateTimeAsInstantUTC(s.replace(' ', 'T')).toEpochMilli());
        } else if (serializer instanceof YTreeDoubleSerializer || serializer instanceof YTreeFloatSerializer
                || serializer instanceof YTStyledPriceFieldSerializer) {
            return doubleConsumer(CSVMapper::parseDouble);
        } else if (serializer instanceof YTreeUnsignedLongSerializer) {
            return (value, builder) ->
                    builder.onUnsignedInteger(Utils.isEmpty(value)
                            ? 0
                            : Long.parseLong(value));
        } else if (serializer instanceof YTreeBooleanSerializer) {
            return (value, builder) ->
                    builder.onBoolean(Boolean.parseBoolean(value));
        } else if (serializer instanceof YTreeStringEnumSerializer || serializer instanceof YTreeStringSerializer
                || serializer instanceof YTreeEnumSerializer || serializer instanceof AbstractYTreeDateSerializer
                || serializer instanceof YTreeLocalDateTimeSerializer
                || serializer instanceof YTreeHyperlinkSerializer) {
            return (value, builder) ->
                    builder.onString(value);
        } else if (serializer instanceof YTreeArraySerializer) {
            var arraySerializer = (YTreeArraySerializer<?, ?>) serializer;
            return arrayConsumer(arraySerializer.getElementWrapperClass(), true);
        } else if (serializer instanceof YTreeListSerializer<?>) {
            var listSerializer = (YTreeListSerializer<?>) serializer;
            return arrayConsumer((Class<?>) listSerializer.getElementType(), false);
        } else {
            return (value, builder) -> {
                byte[] bytes = value.getBytes();
                builder.onString(bytes, 0, bytes.length);
            };
        }
    }

    private static BiConsumer<String, YTreeBuilder> arrayElementConsumer(Class<?> wrapperClass, boolean mandatory) {
        if (wrapperClass == Integer.class) {
            return (value, builder) -> builder.onInteger(Integer.parseInt(value));
        } else if (wrapperClass == Double.class) {
            return (value, builder) -> builder.onDouble(parseDouble(value));
        } else if (wrapperClass == Long.class) {
            return (value, builder) -> builder.onInteger(Long.parseLong(value));
        } else {
            if (mandatory) {
                throw new IllegalArgumentException("Unsupported array type " + wrapperClass);
            } else {
                return null;
            }
        }
    }

    private static <A, E> BiConsumer<String, YTreeBuilder> arrayConsumer(Class<?> wrapperClass, boolean mandatory) {
        @Nullable var element = arrayElementConsumer(wrapperClass, mandatory);
        if (element == null) {
            return (value, builder) -> {
                byte[] bytes = value.getBytes();
                builder.onString(bytes, 0, bytes.length);
            };
        }
        return (value, builder) -> {
            builder.beginList();
            for (String item : value.split(",")) {
                item = item.trim();
                if (Utils.isNonEmpty(item)) {
                    element.accept(item, builder);
                }
            }
            builder.endList();

        };
    }

    private static BiConsumer<String, YTreeBuilder> longConsumer(Function<String, Long> parser) {
        return (value, builder) ->
                builder.onInteger(Utils.isEmpty(value) ? 0 : parser.apply(value));
    }

    private static BiConsumer<String, YTreeBuilder> doubleConsumer(Function<String, Double> parser) {
        return (value, builder) ->
                builder.onDouble(Utils.isEmpty(value) ? 0 : parser.apply(value));
    }

    private static double parseDouble(String value) {
        try {
            return getDoubleFormat().parse(value.replace('.', ',')).doubleValue();
        } catch (ParseException e) {
            throw new PricelabsRuntimeException("Unable to parse " + value + " with " + getDoubleFormat(), e);
        }
    }

    public static <T> CSVMapperBuilder<T> builder(Class<T> clazz) {
        return new CSVMapperBuilder<>(YTBinder.getBinder(clazz).getSerializer());
    }

    public static <T> CSVMapper<T> mapper(Class<T> clazz) {
        return mapper(YTBinder.getBinder(clazz));
    }

    public static <T> CSVMapper<T> mapper(YTBinder<T> binder) {
        return mapper(binder.getSerializer());
    }

    @SuppressWarnings("unchecked")
    private static <T> CSVMapper<T> mapper(YTreeObjectSerializer<T> serializer) {
        return (CSVMapper<T>) MAPPER_CACHE.computeIfAbsent(serializer, s ->
                new CSVMapperBuilder<>(serializer).build());
    }

    @AllArgsConstructor
    private static class FieldDeserializer {

        @NonNull
        private final String csvKey;
        @NonNull
        private final String key;
        @NonNull
        private final BiConsumer<String, YTreeBuilder> treeBuilder;

    }


    public static class CSVMapperBuilder<T> {
        private final YTreeObjectSerializer<T> serializer;
        private final Map<String, FieldDeserializer> fields = new LinkedHashMap<>();

        CSVMapperBuilder(@NonNull YTreeObjectSerializer<T> serializer) {
            this.serializer = serializer;

            var clazz = serializer.getClazz();
            var formats = ObjectFormatter.getInstance().getFormat(clazz, FileTypeEnum.csv);

            for (YTreeObjectField<?> field : this.serializer.getFieldMap().values()) {
                String csvKey = field.key;
                var format = formats.get(field.field.getName());

                BiConsumer<String, YTreeBuilder> treeBuilder = null;

                if (format == null && !formats.isEmpty()) {
                    continue; // ---
                }
                if (format != null) {
                    if (Utils.isNonEmpty(format.getTitle())) {
                        csvKey = format.getTitle();
                    }
                    switch (format.getFormat()) {
                        case LONG_TO_TIMESTAMP:
                            treeBuilder = (value, builder) -> builder.onInteger(Utils.nvl(timestampToLong(value)));
                            break;
                        case YYYYMM_TO_MONTH:
                            treeBuilder = (value, builder) -> builder.onString(monthToYyyymm(value));
                            break;
                        default:
                            //
                    }
                }
                if (treeBuilder == null) {
                    treeBuilder = mapSerializer(field);
                }
                fields.put(field.key, new FieldDeserializer(csvKey, field.key, treeBuilder));
            }
        }

        public CSVMapper<T> build() {
            return new CSVMapper<>(this);
        }

    }

    private static Long timestampToLong(String timestamp) {
        try {
            return Utils.isEmpty(timestamp) ? null :
                    getTimestampFormat().parse(timestamp).getTime();
        } catch (ParseException e) {
            throw new PricelabsRuntimeException("Unable to parse " + timestamp, e);
        }
    }

    private static String monthToYyyymm(String monthYear) {
        if (Utils.isEmpty(monthYear)) {
            return "";
        } else {
            int pos = monthYear.indexOf(' ');
            if (pos <= 0) {
                throw new PricelabsRuntimeException("Unable to split " + monthYear);
            }
            var month = monthYear.substring(0, pos);
            var code = getNameToCode().get(month);
            if (code == null) {
                throw new PricelabsRuntimeException("Unable to find a code for month " + month);
            }
            return monthYear.substring(pos + 1) + "-" + code;
        }
    }
}
