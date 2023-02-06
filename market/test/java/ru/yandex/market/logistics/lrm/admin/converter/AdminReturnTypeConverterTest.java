package ru.yandex.market.logistics.lrm.admin.converter;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnType;
import ru.yandex.market.logistics.lrm.model.entity.ReturnEntity;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSource;

@ParametersAreNonnullByDefault
@DisplayName("Конвертация типа возврата")
class AdminReturnTypeConverterTest extends LrmTest {

    private final AdminReturnTypeConverter returnTypeConverter = new AdminReturnTypeConverter();

    @MethodSource
    @ParameterizedTest
    @DisplayName("Конвертация возврата в тип возврата")
    void toReturnType(ReturnSource returnSource, Long logisticPointFromId, AdminReturnType expectedReturnType) {
        softly.assertThat(returnTypeConverter.toReturnType(
                new ReturnEntity().setSource(returnSource).setLogisticPointFromId(logisticPointFromId))
            )
            .isEqualTo(expectedReturnType);
    }

    @Nonnull
    private static Stream<Arguments> toReturnType() {
        return Stream.of(
            Arguments.of(ReturnSource.CLIENT, null, AdminReturnType.CLIENT_COURIER),
            Arguments.of(ReturnSource.CLIENT, 1L, AdminReturnType.CLIENT_PICKUP),
            Arguments.of(ReturnSource.CANCELLATION, null, AdminReturnType.CANCELLATION),
            Arguments.of(ReturnSource.COURIER, null, AdminReturnType.FASHION),
            Arguments.of(ReturnSource.PICKUP_POINT, null, AdminReturnType.FASHION)
        );
    }
}
