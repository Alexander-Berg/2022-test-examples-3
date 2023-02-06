package ru.yandex.canvas.service.video;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.canvas.model.video.addition.AdditionElement;

import static org.junit.Assert.assertTrue;

// Проверяем, что для всех типов элементов есть конфиг
@RunWith(Parameterized.class)
public class VideoAdditionValidationServiceAllElementsValidateTest {
    @Parameterized.Parameter
    public AdditionElement.ElementType elementType;

    @Parameterized.Parameters(name = "ElementType {0}")
    public static Collection<AdditionElement.ElementType> types() {
        return Arrays.asList(AdditionElement.ElementType.values());
    }

    @Test
    public void configTypeToElementTypeMapIsComplete() {
        assertTrue("There is a config which points to this ElementType",
                VideoAdditionValidationService.getConfigToElementTypeMap().values().contains(elementType));
    }

}
