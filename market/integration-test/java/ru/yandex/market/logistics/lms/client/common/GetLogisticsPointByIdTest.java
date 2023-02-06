package ru.yandex.market.logistics.lms.client.common;

import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lms.client.utils.EntityNotFoundTestCases;
import ru.yandex.market.logistics.lms.client.utils.LmsLomClientLogUtils;
import ru.yandex.market.logistics.lom.lms.model.LogisticsPointLightModel;
import ru.yandex.market.logistics.lom.lms.model.logging.enums.LmsLomLoggingCode;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@DisplayName("Получение логистической точки по идентификатору")
public class GetLogisticsPointByIdTest extends LmsLomLightCommonClientMockedAbstractTest {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final Optional<LogisticsPointLightModel> EMPTY_RESPONSE = Optional.empty();

    private static final long POINT_ID = 1L;
    private static final String METHOD = "getLogisticsPointById id = " + POINT_ID;

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Получение логистической точки по идентификатору")
    void checkEntityNotFoundLogs(
        @SuppressWarnings("unused") String displayName,
        EntityNotFoundTestCases<Optional<LogisticsPointLightModel>> entityNotFoundTestCase
    ) {
        mockClients(entityNotFoundTestCase);
        softly.assertThat(mockedLmsLomLightCommonClient.getLogisticsPoint(POINT_ID))
            .usingRecursiveComparison()
            .isEqualTo(entityNotFoundTestCase.getExpectedClientResponse());
        String logs = backLogCaptor.getResults().toString();

        verify(mockedRedisClient).getLogisticsPoint(POINT_ID);
        if (entityNotFoundTestCase.isExistsInRedis()) {
            return;
        }

        verify(mockedRedisClient).getLoggingCode();
        softly.assertThat(logs)
            .contains(LmsLomClientLogUtils.getEntityNotFoundLog(LmsLomLoggingCode.LMS_LOM_REDIS, METHOD));

        verify(mockedYtClient).getLogisticsPoint(POINT_ID);
        if (entityNotFoundTestCase.isExistsInYt()) {
            return;
        }

        verify(mockedYtClient).getLoggingCode();
        softly.assertThat(logs)
            .contains(LmsLomClientLogUtils.getEntityNotFoundLog(LmsLomLoggingCode.LMS_LOM_YT, METHOD));

        verify(mockedLmsFallbackClient).getLogisticsPoint(POINT_ID);
    }

    @Nonnull
    private static Stream<Arguments> checkEntityNotFoundLogs() {
        return Stream.of(
            Arguments.of(
                "Точка не найдена нигде",
                EntityNotFoundTestCases.<Optional<LogisticsPointLightModel>>builder()
                    .expectedClientResponse(EMPTY_RESPONSE)
                    .build()
            ),
            Arguments.of(
                "Точка найдена в ЛМС",
                EntityNotFoundTestCases.<Optional<LogisticsPointLightModel>>builder()
                    .expectedClientResponse(pointLightModel())
                    .build()
            ),
            Arguments.of(
                "Точка найдена в redis",
                EntityNotFoundTestCases.<Optional<LogisticsPointLightModel>>builder()
                    .existsInRedis(true)
                    .expectedClientResponse(pointLightModel())
                    .build()
            ),
            Arguments.of(
                "Точка найдена в yt",
                EntityNotFoundTestCases.<Optional<LogisticsPointLightModel>>builder()
                    .existsInYt(true)
                    .expectedClientResponse(pointLightModel())
                    .build()
            )
        );
    }

    private void mockClients(EntityNotFoundTestCases<Optional<LogisticsPointLightModel>> entityNotFoundTestCase) {
        doReturn(entityNotFoundTestCase.isExistsInRedis() ? pointLightModel() : EMPTY_RESPONSE)
            .when(mockedRedisClient).getLogisticsPoint(POINT_ID);
        doReturn(entityNotFoundTestCase.isExistsInYt() ? pointLightModel() : EMPTY_RESPONSE)
            .when(mockedYtClient).getLogisticsPoint(POINT_ID);
        doReturn(entityNotFoundTestCase.getExpectedClientResponse().isPresent() ? pointLightModel() : EMPTY_RESPONSE)
            .when(mockedLmsFallbackClient).getLogisticsPoint(POINT_ID);
    }

    @Nonnull
    private static Optional<LogisticsPointLightModel> pointLightModel() {
        return Optional.of(LogisticsPointLightModel.build(LogisticsPointResponse.newBuilder().build()));
    }
}
