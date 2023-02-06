package ru.yandex.market.common.test.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.MapField;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.RecursiveComparisonAssert;

import ru.yandex.market.common.test.json.JsonTransformer;

public class ProtoTestUtil {

    private final static List<String> IGNORED_PROTO_FIELDS = ImmutableList.of(
            ".*bitField0_.*",
            ".*memoizedHashCode.*",
            ".*memoizedIsInitialized.*"
    );

    private static final JsonTransformer JSON_TRANSFORMER = new JsonTransformer();

    private ProtoTestUtil() {
        throw new UnsupportedOperationException("Cannot instantiate an util class");
    }

    /**
     * Получить protobuf-сообщение по его json-представлению.
     *
     * @param messageClass класс protobuf-сообщения, экземпляр которого должен быть возвращен
     * @param jsonPath     путь до ресурса с json-файлом
     * @param contextClass класс, чей classloader должен загрузить json-файл
     * @return сообщение заданного класса, построенное по заданному json-предствалению.
     */
    public static <T extends Message> T getProtoMessageByJson(Class<T> messageClass,
                                                              String jsonPath,
                                                              Class<?> contextClass) {
        String rawJsonFile = StringTestUtil.getString(contextClass, jsonPath);
        return getProtoMessageByJson(messageClass, ExtensionRegistry.getEmptyRegistry(), rawJsonFile);
    }

    /**
     * Получить protobuf-сообщение по его json-представлению.
     *
     * @param messageClass класс protobuf-сообщения, экземпляр которого должен быть возвращен
     * @param jsonData     строка с json данными
     * @return сообщение заданного класса, построенное по заданному json-предствалению.
     */
    public static <T extends Message> T getProtoMessageByJson(Class<T> messageClass,
                                                              String jsonData) {
        return getProtoMessageByJson(messageClass, ExtensionRegistry.getEmptyRegistry(), jsonData);
    }

    /**
     * Получить protobuf-сообщение по его json-представлению.
     *
     * @param messageClass      класс protobuf-сообщения, экземпляр которого должен быть возвращен
     * @param extensionRegistry расширения из proto файла. Будут распознаны в json, только если они добавлены в
     *                          extensionRegistry
     * @param jsonPath          путь до ресурса с json-файлом
     * @param contextClass      класс, чей classloader должен загрузить json-файл
     * @return сообщение заданного класса, построенное по заданному json-предствалению.
     */
    public static <T extends Message> T getProtoMessageByJson(Class<T> messageClass,
                                                              ExtensionRegistry extensionRegistry,
                                                              String jsonPath,
                                                              Class<?> contextClass) {
        String rawJsonFile = StringTestUtil.getString(contextClass, jsonPath);
        return getProtoMessageByJson(messageClass, extensionRegistry, rawJsonFile);
    }

    /**
     * Получить protobuf-сообщение по его json-представлению.
     *
     * @param messageClass      сообщение заданного класса, построенное по заданному json-предствалению.
     * @param extensionRegistry расширения из proto файла. Будут распознаны в json, только если они добавлены в
     *                          extensionRegistry
     * @param jsonData          строка с json данными
     * @return сообщение заданного класса, построенное по заданному json-предствалению.
     */
    private static <T extends Message> T getProtoMessageByJson(Class<T> messageClass,
                                                               ExtensionRegistry extensionRegistry,
                                                               String jsonData) {
        try {
            final Message.Builder builder = (Message.Builder) messageClass.getMethod("newBuilder").invoke(null);
            final String transformedJsonFile = JSON_TRANSFORMER.transform(jsonData);

            JsonFormat.merge(transformedJsonFile, extensionRegistry, builder);
            return messageClass.cast(builder.build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Получить Assert, который специально преднастроен для сравнения протобуфок.
     *
     * @param actual актуальное значение
     */
    public static <T extends Message> RecursiveComparisonAssert<?> assertThat(T actual) {
        return Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .ignoringFieldsMatchingRegexes(IGNORED_PROTO_FIELDS.toArray(new String[0]))

                // В прото мапа может храниться тремя разными способами: в виде мапы, в виде списка, оба одновременно.
                // Из-за этого возникают спецэффекты во время сравнения.
                // Можно либо принудительно поменять тип хранения, но это ресурсы, либо сравнивать мапы самим.
                .withEqualsForType((a, b) -> {
                    return (a == null || a.getMap().isEmpty())
                            ? (b == null || b.getMap().isEmpty())
                            : Objects.equals(a.getMap(), b.getMap());
                }, MapField.class);
    }

    /**
     * Получить список полей для игнорирования во время сравнения.
     * Стандартные для прото поля + список дополнительных.
     */
    public static String[] getIgnoredFields(String... additionalFields) {
        int resultLength = IGNORED_PROTO_FIELDS.size() + additionalFields.length;
        String[] result = Arrays.copyOfRange(additionalFields, 0, resultLength);

        Iterator<String> it = IGNORED_PROTO_FIELDS.iterator();
        for (int i = additionalFields.length; it.hasNext(); ++i) {
            result[i] = it.next();
        }

        return result;
    }
}
