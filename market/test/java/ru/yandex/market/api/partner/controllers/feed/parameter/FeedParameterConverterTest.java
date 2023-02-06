package ru.yandex.market.api.partner.controllers.feed.parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.api.partner.apisupport.ApiInvalidRequestException;
import ru.yandex.market.api.partner.apisupport.ErrorRestModel;
import ru.yandex.market.api.partner.apisupport.ErrorRestModelCode;
import ru.yandex.market.api.partner.controllers.feed.FeedParameterConverter;
import ru.yandex.market.api.partner.controllers.feed.model.FeedParameterDTO;
import ru.yandex.market.api.partner.controllers.feed.model.FeedParametersDTO;
import ru.yandex.market.core.feed.model.FeedInfo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.api.partner.apisupport.ErrorRestModelCode.BAD_REQUEST;
import static ru.yandex.market.api.partner.apisupport.ErrorRestModelCode.INVALID_FEED_REPARSE_INTERVAL_MINUTES;
import static ru.yandex.market.api.partner.apisupport.ErrorRestModelCode.UNKNOWN_PARAMETER;
import static ru.yandex.market.api.partner.controllers.feed.FeedParameterConverter.DUPLICATED_PARAMETERS;
import static ru.yandex.market.api.partner.controllers.feed.FeedParameterConverter.FIELD_REPARSE_INTERVAL_MINUTES;
import static ru.yandex.market.api.partner.controllers.feed.FeedParameterConverter.NEGATIVE_REPARSE_INTERVAL_VALUE;
import static ru.yandex.market.api.partner.controllers.feed.FeedParameterConverter.NOT_SINGLE_REPARSE_INTERVAL_VALUE;
import static ru.yandex.market.api.partner.controllers.feed.FeedParameterConverter.TOO_BIG_REPARSE_INTERVAL_VALUE;
import static ru.yandex.market.api.partner.controllers.feed.FeedParameterConverter.TOO_SMALL_REPARSE_INTERVAL_VALUE;
import static ru.yandex.market.api.partner.controllers.feed.FeedParameterConverter.UNKNOWN_PARAMETER_MESSAGE;
import static ru.yandex.market.api.partner.controllers.feed.FeedParameterConverter.WRONG_REPARSE_INTERVAL_VALUE;

/**
 * Тесты для {@link FeedParameterConverter}.
 */
public class FeedParameterConverterTest {

    private static Stream<Arguments> okParams() {
        return Stream.of(
                Arguments.of(prepareFeedParametersDTO(FIELD_REPARSE_INTERVAL_MINUTES, Collections.singletonList(33), true),
                        prepareFeedInfo(null)),
                Arguments.of(prepareFeedParametersDTO(FIELD_REPARSE_INTERVAL_MINUTES, Collections.singletonList(33), false),
                        prepareFeedInfo(33))
        );
    }

    private static FeedInfo prepareFeedInfo(Integer reparseIntervalMinutes) {
        FeedInfo currentFeedInfo = new FeedInfo();
        currentFeedInfo.setDatasourceId(774L);
        currentFeedInfo.setReparseIntervalMinutes(reparseIntervalMinutes);
        currentFeedInfo.setEnabled(true);
        return currentFeedInfo;
    }

    private static Stream<Arguments> wrongData() {
        String newParamName = "some new parameter";
        return Stream.of(Arguments.of(prepareFeedParametersDTO(null), NOT_SINGLE_REPARSE_INTERVAL_VALUE, INVALID_FEED_REPARSE_INTERVAL_MINUTES),
                Arguments.of(prepareFeedParametersDTO(-100), NEGATIVE_REPARSE_INTERVAL_VALUE, INVALID_FEED_REPARSE_INTERVAL_MINUTES),
                Arguments.of(prepareFeedParametersDTO(10), TOO_SMALL_REPARSE_INTERVAL_VALUE, INVALID_FEED_REPARSE_INTERVAL_MINUTES),
                Arguments.of(prepareFeedParametersDTO(999990), TOO_BIG_REPARSE_INTERVAL_VALUE, INVALID_FEED_REPARSE_INTERVAL_MINUTES),
                Arguments.of(prepareFeedParametersDTO("some text"), WRONG_REPARSE_INTERVAL_VALUE, INVALID_FEED_REPARSE_INTERVAL_MINUTES),
                Arguments.of(duplicateParameters(prepareFeedParametersDTO(123)), DUPLICATED_PARAMETERS, BAD_REQUEST),
                Arguments.of(prepareFeedParametersDTO(newParamName, Collections.singletonList("bla-bla"), false),
                        UNKNOWN_PARAMETER_MESSAGE + newParamName, UNKNOWN_PARAMETER),
                Arguments.of(prepareFeedParametersDTO(FIELD_REPARSE_INTERVAL_MINUTES, Arrays.asList(33, 44), false),
                        NOT_SINGLE_REPARSE_INTERVAL_VALUE, INVALID_FEED_REPARSE_INTERVAL_MINUTES));
    }

    private static FeedParametersDTO prepareFeedParametersDTO(Object value) {
        return prepareFeedParametersDTO(FIELD_REPARSE_INTERVAL_MINUTES, value == null ? null : Collections.singletonList(value), false);
    }

    private static FeedParametersDTO prepareFeedParametersDTO(String paramName, List<Object> values, boolean deleted) {
        FeedParameterDTO feedParameterDTO = new FeedParameterDTO();
        feedParameterDTO.setName(paramName);
        feedParameterDTO.setValues(values);
        if (deleted) {
            feedParameterDTO.setDeleted(true);
        }

        List<FeedParameterDTO> feedParameterDTOList = new ArrayList<>();
        feedParameterDTOList.add(feedParameterDTO);

        FeedParametersDTO feedParametersDTO = new FeedParametersDTO();
        feedParametersDTO.setParams(feedParameterDTOList);
        return feedParametersDTO;
    }

    private static FeedParametersDTO duplicateParameters(FeedParametersDTO feedParametersDTO) {
        feedParametersDTO.getParams().add(feedParametersDTO.getParams().get(0));
        return feedParametersDTO;
    }

    /**
     * Проверяем валидные сочетания параметров для установки и удаления reparseIntervalMinutes.
     */
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("okParams")
    void testWrongReparseIntervalInputData(FeedParametersDTO newFeedParametersDTO, FeedInfo expectedFeedInfo) {
        FeedInfo currentFeedInfo = prepareFeedInfo(55);
        FeedInfo newFeedInfo = FeedParameterConverter.modifyParamsValue(newFeedParametersDTO, currentFeedInfo);

        ReflectionAssert.assertReflectionEquals(expectedFeedInfo, newFeedInfo);
    }

    /**
     * Проверяем реакцию на невалидное значение параметра reparseIntervalMinutes.
     */
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("wrongData")
    void testWrongReparseIntervalInputData(final FeedParametersDTO params, final String expectedMessage, ErrorRestModelCode expectedCode) {
        check(params, expectedMessage, expectedCode);
    }

    private void check(FeedParametersDTO feedParametersDTO,
                       String expectedMessage,
                       ErrorRestModelCode expectedCode) {
        ApiInvalidRequestException exception = assertThrows(ApiInvalidRequestException.class,
                () -> FeedParameterConverter.modifyParamsValue(feedParametersDTO, null));
        ReflectionAssert.assertReflectionEquals(new ErrorRestModel(expectedCode, expectedMessage), exception.getErrors().get(0));

    }
}
