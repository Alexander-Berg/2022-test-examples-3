package ru.yandex.direct.grid.processing.service.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.grid.model.jsonsettings.GdSetJsonSettings;
import ru.yandex.direct.grid.model.jsonsettings.GdUpdateJsonSettingsUnion;
import ru.yandex.direct.grid.model.jsonsettings.IdType;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class GridValidationServiceJsonSettingsTest {

    @Mock
    private GridValidationResultConversionService validationResultConversionService;

    @InjectMocks
    private GridValidationService service;

    @Parameterized.Parameter
    public ValidationPair<?> validationPair;
    @Parameterized.Parameter(1)
    public boolean expectException;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        pair(new GdSetJsonSettings()
                                        .withIdType(IdType.CLIENT_ID)
                                        .withUpdateItems(List.of(new GdUpdateJsonSettingsUnion()
                                                .withJsonPath("$")
                                                .withNewValue(Map.of("123", 123))
                                        )),
                                GridValidationService::validateSetJsonSettings),
                        false,
                },
                {
                        pair(new GdSetJsonSettings()
                                        .withIdType(IdType.CLIENT_ID)
                                        .withUpdateItems(List.of(new GdUpdateJsonSettingsUnion()
                                                .withJsonPath("?br0ken...jsonpath")
                                                .withNewValue(emptyMap())
                                        )),
                                GridValidationService::validateSetJsonSettings),
                        true,
                },
                {
                        pair(new GdSetJsonSettings()
                                        .withIdType(IdType.CLIENT_ID)
                                        .withUpdateItems(List.of(new GdUpdateJsonSettingsUnion()
                                                .withJsonPath("$")
                                                .withNewValue(Map.of("123", 123))
                                        )),
                                GridValidationService::validateSetJsonSettings),
                        false,
                },
                {
                        pair(new GdSetJsonSettings()
                                        .withIdType(IdType.CLIENT_ID)
                                        .withUpdateItems(List.of(new GdUpdateJsonSettingsUnion()
                                                .withJsonPath("$.key")
                                                .withNewValue(123123)
                                        )),
                                GridValidationService::validateSetJsonSettings),
                        false,
                },
                {
                        pair(new GdSetJsonSettings()
                                        .withIdType(IdType.CLIENT_ID)
                                        .withUpdateItems(List.of(new GdUpdateJsonSettingsUnion()
                                                .withJsonPath("$.key.array[100]")
                                                .withNewValue(emptyMap())
                                        )),
                                GridValidationService::validateSetJsonSettings),
                        false,
                },
        });
    }

    private static <T> ValidationPair<T> pair(@Nullable T value, BiConsumer<GridValidationService, T> validator) {
        return new ValidationPair<>(value, validator);
    }

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);
        doReturn(new GdValidationResult())
                .when(validationResultConversionService).buildGridValidationResult(any(), any());
    }

    @Test
    public void checkValidation() {
        if (!expectException) {
            validationPair.check(service);
        } else {
            assertThatThrownBy(() -> validationPair.check(service))
                    .isInstanceOf(GridValidationException.class);
        }
    }

    private static class ValidationPair<T> {
        private final T value;
        private final BiConsumer<GridValidationService, T> validator;

        private ValidationPair(T value, BiConsumer<GridValidationService, T> validator) {
            this.value = value;
            this.validator = validator;
        }

        private void check(GridValidationService service) {
            validator.accept(service, value);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
