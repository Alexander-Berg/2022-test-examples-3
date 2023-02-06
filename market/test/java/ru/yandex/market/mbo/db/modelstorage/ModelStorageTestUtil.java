package ru.yandex.market.mbo.db.modelstorage;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import org.junit.Assert;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.params.ParameterProtoConverter;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.http.ModelStorage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Вспомогательный класс для тестирования эквивалентность объектов при двойном переводе:
 * java-object -> proto-object -> java-object.
 * <p>
 * Каждый тест, который использует данный класс должен делать 2 вещи:
 * - Передавать перед каждым тестом seed, чтобы не было влияния одного теста на другой
 * - Поставить @RepeatedTest(times)
 * Таким образом мы повышаем шансы отлова редкого бага и возможность последующего его воспроизведения.
 *
 * @author york
 * @since 04.04.2017
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ModelStorageTestUtil {
    private static final String LIST_COUNT_PREFIX = ".list_count";
    private static final String LIST_PREFIX = ".list";
    private static final int XSLS_COUNT = 10;
    private static final int LONG_MOD = 10000000;
    private static final int DAYS_IN_YEAR = 365;
    private static final int MIN_LIST_VALUES = 1;
    private static final int MAX_LIST_VALUES = 3;

    // TODO сделать передачу seed через сетер
    private static Random random = new Random(1);

    private ModelStorageTestUtil() { }

    /**
     * Generates fully filled valid proto model.
     */
    public static ModelStorage.Model generateModel() {
        return generateModel(getModelCustomGenerators());
    }

    /**
     * Generates fully filled valid proto model.
     * @param customGenerators custom generators for field values
     */
    public static ModelStorage.Model generateModel(Map<String, Supplier<Object>> customGenerators) {
        return generateRandomMessageGeneric(ModelStorage.Model.class, customGenerators);
    }

    public static <T extends Message> T generateRandomMessageGeneric(Class<T> protoClass) {
        return generateRandomMessageGeneric(protoClass, Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Message> T generateRandomMessageGeneric(Class<T> protoClass,
                                                                     Map<String, Supplier<Object>> customGenerators) {
        try {
            Method getDefaultInstanceMethod = protoClass.getMethod("getDefaultInstance");
            T defaultInstance = (T) getDefaultInstanceMethod.invoke(null);
            Message message = generateRandomMessage(defaultInstance, customGenerators);
            return (T) message;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to create class: " + e);
        }
    }

    public static ModelStorage.ParameterValue stringParamValue(Long id, String xslName, String... value) {
        return ModelStorage.ParameterValue.newBuilder()
            .setXslName(xslName)
            .setParamId(id)
            .setTypeId(Parameter.Type.STRING.ordinal())
            .setValueType(MboParameters.ValueType.STRING)
            .addAllStrValue(
                Arrays.stream(value).map(WordUtil::defaultWord).map(ModelProtoConverter::fromWord)
                    .collect(Collectors.toList())
             )
            .build();
    }

    /**
     * Custom fields generators to create valid model
     * (synthetic fields are skipped, dates are within last year, etc.).
     */
    public static Map<String, Supplier<Object>> getModelCustomGenerators() {
        Map<String, Supplier<Object>> customGenerators = new HashMap<>();

        //skipped (generated or deprecated fields)
        customGenerators.put("Market.Mbo.Models.Model.vendor_id", null);
        customGenerators.put("Market.Mbo.Models.Model.titles", null);
        customGenerators.put("Market.Mbo.Models.Model.aliases", null);
        customGenerators.put("Market.Mbo.Models.Model.descriptions", null);
        customGenerators.put("Market.Mbo.Models.Model.pinned_offers", null);
        customGenerators.put("Market.Mbo.Models.Model.throwed_offers", null);
        customGenerators.put("Market.Mbo.Models.Model.group_pictures", null);
        customGenerators.put("Market.Mbo.Models.Model.modif_pictures", null);
        customGenerators.put("Market.Mbo.Models.Model.group_size", null);
        customGenerators.put("Market.Mbo.Models.Model.check_date", null);
        customGenerators.put("Market.Mbo.Models.Model.shop_count", null);
        customGenerators.put("Market.Mbo.Models.Model.clusterizer_offer_ids", null);
        customGenerators.put("Market.Mbo.Models.Model.titleApproved", null);
        customGenerators.put("Market.Mbo.Models.Model.doubtful", null);
        customGenerators.put("Market.Mbo.Models.Model.published_on_market", null);
        customGenerators.put("Market.Mbo.Models.Model.param_model_properties", null);
        customGenerators.put("Market.Mbo.Models.Model.modif_pictures", null);
        customGenerators.put("Market.Mbo.Models.Model.offer_rules", null);
        customGenerators.put("Market.Mbo.Models.Model.broken", null);
        customGenerators.put("Market.Mbo.Models.Model.strict_checks_required", null);

        customGenerators.put("Market.Mbo.Models.Model.source_type", ModelStorageTestUtil::generateRandomSource);
        customGenerators.put("Market.Mbo.Models.Model.current_type", ModelStorageTestUtil::generateRandomSource);
        customGenerators.put("Market.Mbo.Models.LocalizedString.isoCode", Language.RUSSIAN::getIsoCode);

        customGenerators.put("Market.Mbo.Parameters.PickerImage.name_space", null);
        customGenerators.put("Market.Mbo.Parameters.PickerImage.namespace", null);

        //dates
        customGenerators.put("Market.Mbo.Models.OfferInfo.added_time", ModelStorageTestUtil::generateDate);
        customGenerators.put("Market.Mbo.Models.Model.created_date", ModelStorageTestUtil::generateDate);
        customGenerators.put("Market.Mbo.Models.Model.modified_ts", ModelStorageTestUtil::generateDate);
        customGenerators.put("Market.Mbo.Models.Model.expired_date", ModelStorageTestUtil::generateDate);
        customGenerators.put("Market.Mbo.Models.Model.deleted_date", ModelStorageTestUtil::generateDate);

        //parameter values
        customGenerators.put("Market.Mbo.Models.Model.parameter_values", ModelStorageTestUtil::generateParameterValues);
        customGenerators.put("Market.Mbo.Models.Model.parameter_value_links",
            ModelStorageTestUtil::generateParameterValues);
        customGenerators.put("Market.Mbo.Models.Picture.parameter_values",
            ModelStorageTestUtil::generateParameterValues);

        return Collections.unmodifiableMap(customGenerators);
    }

    public static String getListCountFieldName(String fullName) {
        return fullName + LIST_COUNT_PREFIX;
    }

    public static String getListFieldName(String fullName) {
        return fullName + LIST_PREFIX;
    }

    /**
     * Checks that all fields from the first message remained the same in the second one.
     * Fields with blank custom generators are skipped.
     *
     * @return empty string if everything is ok, string with errors otherwise
     */
    public static Diff generateDiff(Message messageBefore, Message messageAfter) {
        return generateDiff(messageBefore, messageAfter, Collections.emptyMap());
    }

    /**
     * Checks that all fields from the first message remained the same in the second one.
     * Fields with blank custom generators are skipped.
     *
     * @return empty string if everything is ok, string with errors otherwise
     */
    public static Diff generateDiff(Message messageBefore, Message messageAfter,
                                    Map<String, BiFunction<Object, Object, Diff>> customComparison) {
        return generateDiff(messageBefore, messageAfter, Collections.emptyMap(), customComparison);
    }

    /**
     * Checks that all fields from the first message remained the same in the second one.
     * Fields with blank custom generators are skipped.
     * @return empty string if everything is ok, string with errors otherwise
     */
    @SuppressWarnings("unchecked")
    public static Diff generateDiff(Message messageBefore, Message messageAfter,
                                    Map<String, Supplier<Object>> customGenerators,
                                    Map<String, BiFunction<Object, Object, Diff>> customComparators) {
        Map<Descriptors.FieldDescriptor, Object> before = new HashMap<>(messageBefore.getAllFields());
        Map<Descriptors.FieldDescriptor, Object> after = new HashMap<>(messageAfter.getAllFields());

        //filtering skipped fields
        Set<Descriptors.FieldDescriptor> keys = new HashSet<>(before.keySet());
        keys.addAll(after.keySet());
        keys.stream()
            .filter(fd -> customGenerators.containsKey(fd.getFullName())
                && customGenerators.get(fd.getFullName()) == null)
            .forEach(fd -> {
                before.remove(fd);
                after.remove(fd);
            });

        Diff diff = Diff.EMPTY;
        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : before.entrySet()) {
            Descriptors.FieldDescriptor fd = entry.getKey();
            Object beforeValue = entry.getValue();
            Object afterValue = after.get(fd);
            if (customComparators.containsKey(fd.getFullName())) {
                BiFunction<Object, Object, Diff> diffFunction = customComparators.get(fd.getFullName());
                if (diffFunction != null) {
                    diff = diffFunction.apply(beforeValue, afterValue);
                }
            } else
            if (afterValue == null) {
                diff = new Diff(fd.getFullName(), beforeValue, afterValue);
            } else
            if (fd.isRepeated()) {
                List<Object> listBefore = new ArrayList<>((List) beforeValue);
                List<Object> listAfter = new ArrayList<>((List) afterValue);
                boolean equals = equalsIgnoringOrder(listBefore, listAfter, customGenerators, customComparators);
                if (!equals) {
                    diff = new Diff(fd.getFullName(),
                        String.format("before-list (size: %d,\n%s\n)", listBefore.size(), listBefore),
                        String.format("after-list (size: %d,\n%s\n)", listAfter.size(), listAfter));
                }
            } else
            if (!beforeValue.equals(afterValue)) {
                //check if it is because repeated fields
                if (beforeValue instanceof Message) {
                    Diff subDiff = generateDiff((Message) beforeValue, (Message) afterValue, customGenerators,
                        customComparators);
                    if (!subDiff.isEmpty()) {
                        diff = new Diff(fd.getFullName(), subDiff);
                    }
                }
            }
        }
        return diff;
    }

    private static boolean equalsIgnoringOrder(List<Object> beforeList, List<Object> afterList,
                                               Map<String, Supplier<Object>> customGenerators,
                                               Map<String, BiFunction<Object, Object, Diff>> customComparison) {
        if (beforeList.size() != afterList.size()) {
            return false;
        }

        for (int i = 0, n = beforeList.size(); i < n; i++) {
            boolean equals = false;
            for (int j = 0; j < n; j++) {
                Object beforeValue = beforeList.get(i);
                Object afterValue = afterList.get(j);

                if (beforeValue.equals(afterValue)) {
                    equals = true;
                    break;
                }

                if (beforeValue instanceof Message && afterValue instanceof Message) {
                    Diff diff = generateDiff((Message) beforeValue, (Message) afterValue, customGenerators,
                        customComparison);
                    if (diff.isEmpty()) {
                        equals = true;
                        break;
                    }
                }
            }
            if (!equals) {
                return false;
            }
        }
        return true;
    }

    public static List<ModelStorage.ParameterValue> generateParameterValues() {
        try {
            AtomicInteger idCounter = new AtomicInteger();
            AtomicInteger xslCounter = new AtomicInteger();

            Map<String, Supplier<Object>> customGenerators = new HashMap<>();
            customGenerators.put("Market.Mbo.Models.ParameterValue.param_id",
                () -> idCounter.incrementAndGet());
            customGenerators.put("Market.Mbo.Models.ParameterValue.xsl_name",
                () -> Integer.toHexString(xslCounter.addAndGet(XSLS_COUNT)));
            customGenerators.put("Market.Mbo.Models.ParameterValue.modification_date",
                ModelStorageTestUtil::generateDate);
            customGenerators.put("Market.Mbo.Models.ParameterValue.numeric_value", () ->
                new BigDecimal(random.nextDouble(), MathContext.DECIMAL64).toString());
            customGenerators.put("Market.Mbo.Models.LocalizedString.isoCode", Language.RUSSIAN::getIsoCode);

            customGenerators = Collections.unmodifiableMap(customGenerators);

            List<ModelStorage.ParameterValue> result = new ArrayList<>();
            Iterator<MboParameters.ValueType> iterator = EnumSet.allOf(MboParameters.ValueType.class).iterator();
            // we want at least one of each type
            while (iterator.hasNext()) {
                ModelStorage.ParameterValue.Builder paramValueBuilder =
                    ModelStorage.ParameterValue.newBuilder().mergeFrom(
                        generateRandomMessage(ModelStorage.ParameterValue.getDefaultInstance(), customGenerators));

                paramValueBuilder.setValueType(iterator.next());
                iterator.remove();

                paramValueBuilder.setTypeId(
                    ParameterProtoConverter.convert(paramValueBuilder.getValueType()).ordinal());

                switch (paramValueBuilder.getValueType()) {
                    case STRING:
                        paramValueBuilder.clearBoolValue();
                        paramValueBuilder.clearNumericValue();
                        paramValueBuilder.clearOptionId();
                        paramValueBuilder.clearHypothesisValue();
                        break;
                    case NUMERIC:
                        paramValueBuilder.clearBoolValue();
                        paramValueBuilder.clearStrValue();
                        paramValueBuilder.clearOptionId();
                        paramValueBuilder.clearHypothesisValue();
                        break;
                    case NUMERIC_ENUM:
                    case ENUM:
                        paramValueBuilder.clearBoolValue();
                        paramValueBuilder.clearStrValue();
                        paramValueBuilder.clearNumericValue();
                        paramValueBuilder.clearHypothesisValue();
                        break;
                    case BOOLEAN:
                        paramValueBuilder.clearNumericValue();
                        paramValueBuilder.clearOptionId();
                        paramValueBuilder.clearStrValue();
                        paramValueBuilder.clearHypothesisValue();
                        break;
                    case HYPOTHESIS:
                        paramValueBuilder.clearStrValue();
                        paramValueBuilder.clearNumericValue();
                        paramValueBuilder.clearOptionId();
                        paramValueBuilder.clearBoolValue();
                        break;
                    default:
                        throw new IllegalStateException("Support new type " + paramValueBuilder.getValueType());
                }
                result.add(paramValueBuilder.build());
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Message generateRandomMessage(Message message, Map<String, Supplier<Object>> customGenerators) {
        Message.Builder builder = message.newBuilderForType();
        List<Descriptors.FieldDescriptor> fields = new ArrayList<>(message.getDescriptorForType().getFields());
        for (Descriptors.FieldDescriptor field : fields) {
            Object value = null;
            if (customGenerators.containsKey(field.getFullName())) {
                Supplier<Object> supplier = customGenerators.get(field.getFullName());
                if (supplier != null) {
                    value = supplier.get();
                }
            } else if (field.isRepeated()) {
                String listName = getListFieldName(field.getFullName());
                if (customGenerators.containsKey(listName)) {
                    Supplier<Object> listSupplier = customGenerators.get(listName);
                    if (listSupplier != null) {
                        value = listSupplier.get();
                    }
                } else {
                    value = listOf(field, customGenerators, MIN_LIST_VALUES, MAX_LIST_VALUES);
                }
            } else {
                value = generateRandomValue(field, customGenerators);
            }
            if (value != null) {
                try {
                    builder.setField(field, value);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to set field " + field.getFullName() + " with value " + value,
                        e);
                }
            }
        }
        return builder.build();
    }

    public static Object generateRandomValue(Descriptors.FieldDescriptor field,
                                             Map<String, Supplier<Object>> customGenerators) {
        switch (field.getJavaType()) {
            case MESSAGE:
                return generateRandomMessage(
                    DynamicMessage.getDefaultInstance(field.getMessageType()), customGenerators);
            case ENUM:
                List<Descriptors.EnumValueDescriptor> vals = field.getEnumType().getValues();
                return vals.get(random.nextInt(vals.size()));
            case BOOLEAN:
                return random.nextBoolean();
            case INT:
                return Math.abs(random.nextInt());
            case DOUBLE:
                return random.nextDouble();
            case FLOAT:
                return random.nextFloat();
            case LONG:
                return Math.abs(random.nextLong()) % LONG_MOD; // generate quite small longs
            case STRING:
                return Integer.toHexString(random.nextInt());
            default:
                throw new IllegalStateException("Unknown descriptor type " + field.getJavaType());
        }
    }

    private static String generateRandomSource() {
        return CommonModel.Source.values()[random.nextInt(CommonModel.Source.values().length)].name();
    }

    private static Long generateDate() {
        //like within 1 year
        return System.currentTimeMillis() - Math.abs(random.nextLong()) % TimeUnit.DAYS.toMillis(DAYS_IN_YEAR);
    }

    private static Object listOf(Descriptors.FieldDescriptor field, Map<String, Supplier<Object>> customGenerators,
                          int minSize, int maxSize) {
        String fullName = field.getFullName();
        String listCountFieldName = getListCountFieldName(fullName);
        Integer count;
        if (customGenerators.containsKey(listCountFieldName)) {
            Supplier<Object> listCountSupplier = customGenerators.get(listCountFieldName);
            count = listCountSupplier != null ? (Integer) listCountSupplier.get() : null;
        } else {
            int delta = maxSize - minSize;
            count = minSize + (delta > 0 ? random.nextInt(delta) : delta);
        }

        if (count == null) {
            return null;
        }

        List<Object> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(generateRandomValue(field, customGenerators));
        }
        return values;
    }

    public static class Diff {
        private static final Diff EMPTY = new Diff(null, null, null);

        private final String fieldName;
        private final String expected;
        private final String actual;

        public Diff(String fieldName, Object expected, Object actual) {
            this.fieldName = fieldName;
            this.expected = expected == null ? null : expected.toString();
            this.actual = actual == null ? null : actual.toString();
        }

        public Diff(String fieldName, Diff subDiff) {
            this(fieldName + " -> " + subDiff.fieldName, subDiff.expected, subDiff.actual);
        }

        public boolean isEmpty() {
            return expected == null && actual == null;
        }

        public void assertEquals() {
            if (!isEmpty()) {
                // assertEquals используется, так как IDEA очень красиво пишет дифф
                Assert.assertEquals("Corrupted conversion: " + fieldName, expected, actual);
            }
        }

        @Override
        public String toString() {
            return "Diff{" +
                "fieldName='" + fieldName + '\'' +
                ", expected='" + expected + '\'' +
                ", actual='" + actual + '\'' +
                '}';
        }
    }
}
