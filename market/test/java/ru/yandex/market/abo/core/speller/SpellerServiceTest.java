package ru.yandex.market.abo.core.speller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.speller.model.SpellError;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpellerServiceTest {

    @InjectMocks
    private SpellerService spellerService;
    @Mock
    private ConfigurationService aboConfigurationService;
    @Mock
    private SpellerClient spellerClient;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest(name = "checkText_{index}")
    @MethodSource("checkTextMethodSource")
    void checkText(String expectedError, List<SpellError> errors) {
        when(aboConfigurationService.getValueAsInt(CoreConfig.USE_SPELLER_API.getId())).thenReturn(1);
        when(spellerClient.checkText(any(), any())).thenReturn(errors);
        assertEquals(expectedError, spellerService.checkText("some"));
    }

    private static Stream<Arguments> checkTextMethodSource() {
        return Stream.of(
                Arguments.of("", Collections.emptyList()),
                Arguments.of("обшибка -> ошибка", List.of(
                        createError("обшибка", "ошибка"))),
                Arguments.of("стуль -> стул", List.of(
                        createError("стуль", "стул", "стулья"))),
                Arguments.of("машына -> ?", List.of(createError("машына"))),
                Arguments.of("обшибка -> ошибка\nмашына -> машина", List.of(
                        createError("обшибка", "ошибка"), createError("машына", "машина")))
        );
    }

    private static SpellError createError(String word, String... suggestion) {
        return new SpellError(word, Arrays.asList(suggestion));
    }

    @Test
    void ifServiceDisabledInCoreConfig() {
        when(aboConfigurationService.getValueAsInt(CoreConfig.USE_SPELLER_API.getId())).thenReturn(0);
        assertEquals("", spellerService.checkText("машына"));
        verify(spellerClient, never()).checkText(any(), any());
    }
}
