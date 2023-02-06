package ru.yandex.market.tsum.test_utils;

import java.util.NoSuchElementException;

import org.mockito.Mockito;

import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.clients.sandbox.TaskInputDto;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 19.03.18
 */
public class SandboxTestUtils {
    private SandboxTestUtils() {
    }

    public static SandboxTask successfulTaskDto(long taskId) {
        SandboxTask taskDto = Mockito.mock(SandboxTask.class);
        Mockito.when(taskDto.getId()).thenReturn(taskId);
        Mockito.when(taskDto.getStatus()).thenReturn(SandboxTask.SANDBOX_SUCCESS_STATUS);
        return taskDto;
    }

    public static Object getParameterValue(TaskInputDto taskInputDto, String fieldName) {
        return taskInputDto.getCustomFields().stream()
            .filter(f -> f.getName().equals(fieldName))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Unable to find parameter " + fieldName))
            .getValue();
    }

    public static Object getParameterValueOrDefault(TaskInputDto taskInputDto, String fieldName) {
        return taskInputDto.getCustomFields().stream()
            .filter(f -> f.getName().equals(fieldName))
            .findFirst()
            .map(TaskInputDto.TaskFieldValidateItem::getValue)
            .orElse(null);
    }
}
