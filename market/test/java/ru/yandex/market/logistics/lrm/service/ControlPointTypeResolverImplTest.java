package ru.yandex.market.logistics.lrm.service;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.config.properties.FeatureProperties;
import ru.yandex.market.logistics.lrm.model.entity.enums.ControlPointType;

class ControlPointTypeResolverImplTest extends LrmTest {

    private static final long WH_ALLOWED_TO_USE_RETURN_SC = 123908L;
    private static final long WH_NOT_ALLOWED_TO_USE_RETURN_SC = 178982L;
    private final FeatureProperties featureProperties = Mockito.mock(FeatureProperties.class);
    private final ControlPointTypeResolver controlPointTypeResolver
        = new ControlPointTypeResolverImpl(featureProperties);

    @BeforeEach
    void setupMocks() {
        Mockito
            .when(featureProperties.getWarehousesAllowedToUseReturnSc())
            .thenReturn(Set.of(WH_ALLOWED_TO_USE_RETURN_SC));
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Вычисление типа первой контрольной точки")
    void resolveFirstControlPointType(
        @SuppressWarnings("unused") String displayName,
        long whPartnerId,
        boolean isDropoff,
        ControlPointType expectedType
    ) {
        softly
            .assertThat(controlPointTypeResolver.resolveFirstControlPointType(whPartnerId, isDropoff))
            .isEqualTo(expectedType);
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Вычисление типа следующей контрольной точки")
    void resolveNextControlPointType(
        @SuppressWarnings("unused") String displayName,
        Set<ControlPointType> previousTypes,
        ControlPointType expectedType
    ) {
        softly
            .assertThat(controlPointTypeResolver.resolveNextControlPointType(previousTypes, 1L))
            .isEqualTo(expectedType);
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Ошибка при вычислении типа следующей контрольной точки")
    void exceptionResolveNextControlPointType(
        @SuppressWarnings("unused") String displayName,
        Set<ControlPointType> previousTypes,
        String message
    ) {
        softly
            .assertThatCode(() -> controlPointTypeResolver.resolveNextControlPointType(previousTypes, 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(message);
    }

    @Nonnull
    private static Stream<Arguments> resolveFirstControlPointType() {
        return Stream.of(
            Arguments.of(
                "Дрофофф использующий ВСЦ",
                WH_ALLOWED_TO_USE_RETURN_SC,
                true,
                ControlPointType.SHORT_TERM_STORAGE
            ),
            Arguments.of(
                "СЦ использующий ВСЦ",
                WH_ALLOWED_TO_USE_RETURN_SC,
                false,
                ControlPointType.SHORT_TERM_STORAGE
            ),
            Arguments.of(
                "Дропофф не использующий ВСЦ",
                WH_NOT_ALLOWED_TO_USE_RETURN_SC,
                true,
                ControlPointType.SHORT_TERM_STORAGE
            ),
            Arguments.of(
                "СЦ не использующий ВСЦ",
                WH_NOT_ALLOWED_TO_USE_RETURN_SC,
                false,
                ControlPointType.EXTRA_LONG_TERM_STORAGE
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> resolveNextControlPointType() {
        return Stream.of(
            Arguments.of(
                "Было кратковременное хранение",
                Set.of(ControlPointType.SHORT_TERM_STORAGE),
                ControlPointType.LONG_TERM_STORAGE
            ),
            Arguments.of(
                "Было долговременное хранение",
                Set.of(
                    ControlPointType.SHORT_TERM_STORAGE,
                    ControlPointType.LONG_TERM_STORAGE
                ),
                ControlPointType.UTILIZATION
            ),
            Arguments.of(
                "Было очень долгое хранение",
                Set.of(
                    ControlPointType.EXTRA_LONG_TERM_STORAGE
                ),
                ControlPointType.UTILIZATION
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> exceptionResolveNextControlPointType() {
        return Stream.of(
            Arguments.of(
                "Пустой список предыдущих типов",
                Set.of(),
                "Previous control point types must not be empty for return=1"
            ),
            Arguments.of(
                "Утилизатор уже был",
                Set.of(
                    ControlPointType.EXTRA_LONG_TERM_STORAGE,
                    ControlPointType.UTILIZATION
                ),
                "UTILIZATION control point already exists for return=1"
            )
        );
    }
}
