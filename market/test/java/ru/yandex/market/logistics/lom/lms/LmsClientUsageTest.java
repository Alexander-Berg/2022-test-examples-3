package ru.yandex.market.logistics.lom.lms;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.management.client.LMSClient;

@ParametersAreNonnullByDefault
@DisplayName("Проверка использования вызовов лмс")
public class LmsClientUsageTest extends AbstractTest {

    // Добавлять только по согласованию с командой LOM из Новосибирска и писать причину
    private static final List<String> USAGE_EXCLUSIONS = List.of(
        //фолбек в лмс
        "ru.yandex.market.logistics.lom.lms.client.LmsFallbackClient",
        //использование будет удалено после полного переключения невыкупов
        "ru.yandex.market.logistics.lom.service.partner.LogisticSegmentsServiceImpl"
    );

    @Test
    @DisplayName("Запрещено использование LmsClient")
    void lmsClientUsageIsForbidden() {
        Reflections reflections = new Reflections(
            "ru.yandex.market.logistics.lom",
            new SubTypesScanner(false)
        );

        reflections.getSubTypesOf(Object.class)
            .forEach(this::checkForLmsClientUsage);
    }

    @SuppressWarnings("unchecked")
    private void checkForLmsClientUsage(Class<?> lomClass) {
        String className = lomClass.getName();
        if (className.contains("Test") || USAGE_EXCLUSIONS.contains(className)) {
            return;
        }

        boolean hasLmsClientField = ReflectionUtils.getAllFields(lomClass).stream()
            .anyMatch(field -> LMSClient.class.isAssignableFrom(field.getType()));
        softly.assertThat(hasLmsClientField)
            .as(
                "Class " + className + " has LmsClient usage, you need to remove it. "
                    + "Use LmsLomLightClient instead."
            )
            .isFalse();
    }
}
