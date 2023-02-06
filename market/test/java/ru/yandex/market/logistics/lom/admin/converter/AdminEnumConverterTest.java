package ru.yandex.market.logistics.lom.admin.converter;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.EnumUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.front.library.dto.FrontEnum;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.admin.enums.AdminCancellationOrderReason;
import ru.yandex.market.logistics.lom.admin.enums.AdminOrderStatus;
import ru.yandex.market.logistics.lom.admin.enums.AdminSegmentStatus;
import ru.yandex.market.logistics.lom.converter.EnumConverter;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;

@DisplayName("Конвертация админских перечислений")
@SuppressWarnings({"rawtypes", "unchecked", "UnstableApiUsage"})
class AdminEnumConverterTest extends AbstractTest {
    private static final String BASE_PACKAGE = "ru.yandex.market.logistics.lom";
    private static final Set<String> MODEL_ENUMS_PACKAGES = Set.of(
        "ru.yandex.market.logistics.lom.model.enums",
        "ru.yandex.market.logistics.lom.entity.enums"
    );
    private static final Set<Class> ADMIN_ENUMS_BLACK_LIST = Set.of(
        // см. AdminOrderStatusConverterTest
        AdminOrderStatus.class,
        AdminSegmentStatus.class,
        QueueType.class,
        //см. AdminCancellationOrderReasonConverterTest
        AdminCancellationOrderReason.class
    );
    private static final Set<String> ENUM_VALUES_BLACK_LIST = Set.of(
        "UNKNOWN"
    );

    private final EnumConverter enumConverter = new EnumConverter();

    @Test
    @DisplayName("Для каждого админского enum'а есть соответствующий enum из модели с именем без префикса Admin")
    void assertAllAdminEnumsHaveCorrespondingModelEnums() {
        List<Class<?>> adminEnums = getAdminEnums();
        Set<String> adminEnumNames = Set.copyOf(truncatePrefix(getSimpleNames(adminEnums)));
        Set<String> modelEnumNames = Set.copyOf(getSimpleNames(getModelEnums().values()));
        softly.assertThat(Sets.difference(adminEnumNames, modelEnumNames)).isEmpty();
    }

    @DisplayName("Для каждого элемента перечисления модели существует соответствующее значение в админском enum'е")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("arguments")
    void assertAllAdminEnumValuesHaveCorrespondingModelEnumValues(Class<Enum> adminEnum, Class<Enum> modelEnum) {
        List<Enum> adminEnumValues = getEnumValues(adminEnum);
        List<Enum> modelEnumValues = getEnumValues(modelEnum);

        Set<Enum> convertedEnumValues = modelEnumValues.stream()
            .filter(value -> !ENUM_VALUES_BLACK_LIST.contains(value.name()))
            .map(value -> enumConverter.convert(value, adminEnum))
            .collect(Collectors.toSet());

        Set<Enum> expectedEnumValues = adminEnumValues.stream()
            .filter(value -> !ENUM_VALUES_BLACK_LIST.contains(value.name()))
            .collect(Collectors.toSet());

        softly.assertThat(convertedEnumValues).containsExactlyElementsOf(expectedEnumValues);
    }

    @Nonnull
    private static Stream<Arguments> arguments() {
        List<Class<?>> adminEnums = getAdminEnums();
        Map<String, Class<?>> modelEnums = getModelEnums();
        return StreamEx.of(adminEnums)
            .mapToEntry(adminEnum -> modelEnums.get(truncatePrefix(adminEnum.getSimpleName())))
            .mapKeyValue(Arguments::of);
    }

    @Nonnull
    private List<Enum> getEnumValues(Class<Enum> enumClass) {
        List<Enum> adminEnumValues = EnumUtils.getEnumList(enumClass);
        return adminEnumValues.stream().sorted(Comparator.comparing(Enum::name)).collect(Collectors.toList());
    }

    @Nonnull
    @SneakyThrows
    private static List<Class<?>> getAdminEnums() {
        return ClassPath.from(Thread.currentThread().getContextClassLoader())
            .getTopLevelClassesRecursive(BASE_PACKAGE)
            .stream()
            .map(ClassPath.ClassInfo::load)
            .filter(FrontEnum.class::isAssignableFrom)
            .filter(c -> !ADMIN_ENUMS_BLACK_LIST.contains(c))
            .sorted(Comparator.comparing(Class::getSimpleName))
            .collect(Collectors.toList());
    }

    @Nonnull
    private static Map<String, Class<?>> getModelEnums() {
        return StreamEx.of(getAllClassesFromPackages(MODEL_ENUMS_PACKAGES))
            .mapToEntry(Class::getSimpleName, Function.identity())
            .toMap((a, b) -> a.getCanonicalName().startsWith("ru.yandex.market.logistics.lom.entity.enums") ? a : b);
    }

    @Nonnull
    @SneakyThrows
    private static Set<Class<?>> getAllClassesFromPackages(Set<String> packages) {
        ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
        return packages.stream()
            .map(classPath::getTopLevelClassesRecursive)
            .flatMap(Set::stream)
            .map(ClassPath.ClassInfo::load)
            .collect(Collectors.toSet());
    }

    @Nonnull
    private static Collection<String> getSimpleNames(Collection<Class<?>> classes) {
        return classes.stream().map(Class::getSimpleName).collect(Collectors.toSet());
    }

    @Nonnull
    private static Collection<String> truncatePrefix(Collection<String> adminClasses) {
        return adminClasses.stream()
            .map(AdminEnumConverterTest::truncatePrefix)
            .collect(Collectors.toSet());
    }

    @Nonnull
    private static String truncatePrefix(String adminClass) {
        if (adminClass.startsWith("Admin")) {
            return adminClass.substring(5);
        }
        return adminClass;
    }
}
