package ru.yandex.market.core.param;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.param.db.ParamValueDao;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.DateParamValue;
import ru.yandex.market.core.param.model.NumberParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;
import ru.yandex.market.core.param.model.StringParamValue;
import ru.yandex.market.core.param.validator.ParamValueValidatorsRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author sergey-fed
 */
class DefaultParamServiceTest {
    private static final String ETALON_STRING_VALUE = "ETALON_STRING_VALUE";
    private static final BigDecimal ETALON_NUMBER_VALUE = new BigDecimal(123);
    private DefaultParamService paramService;
    @Mock
    private HistoryService historyService;
    @Mock
    private ParamValueValidatorsRegistry validatorsRegistry;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TransactionTemplate transactionTemplate;

    private static StringParamValue createStringValue() {
        return new StringParamValue(ParamType.NULL, 1, ETALON_STRING_VALUE);
    }

    private static NumberParamValue createNumberValue() {
        return new NumberParamValue(ParamType.NULL, 1, ETALON_NUMBER_VALUE);
    }

    private static BooleanParamValue createBooleanParamValue() {
        return new BooleanParamValue(ParamType.NULL, 1, true);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        paramService = new DefaultParamService();
        paramService.setHistoryService(historyService);
        paramService.setParamValueValidatorsRegistry(validatorsRegistry);
        paramService.setTransactionTemplate(transactionTemplate);
        paramService.setParamValueDao(new ParamValueDao(jdbcTemplate));
    }

    static Stream<Arguments> testThatUpdateOfTheSameParamValueIsSkippedData() {
        return Stream.of(
                Arguments.of(createStringValue(), createStringValue()),
                Arguments.of(createNumberValue(), createNumberValue()),
                Arguments.of(createBooleanParamValue(), createBooleanParamValue())
        );
    }

    @ParameterizedTest
    @MethodSource("testThatUpdateOfTheSameParamValueIsSkippedData")
    void testThatUpdateOfTheSameParamValueIsSkipped(
            ParamValue oldParamValue,
            ParamValue newParamValue

    ) {
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(ResultSetExtractor.class)))
                .thenReturn(Collections.singletonList(oldParamValue));

        paramService.setParam(newParamValue, 1);

        verifyNoInteractions(validatorsRegistry, transactionTemplate); // update skipped
    }

    @Test
    void testGetParams() {
        long entityId = 1L;
        Set<ParamType> types = new HashSet<>();
        types.add(ParamType.AGENCY_CLIENT_WHITE_LIST);
        types.add(ParamType.MARKET_PRICE_MULTIPLIER);
        types.add(ParamType.SHOP_DELIVERY_CURRENCY);
        types.add(ParamType.CPA_LATEST_ACTIVITY);

        List<ParamValue> paramValues = new ArrayList<>();
        BooleanParamValue boolValue = new BooleanParamValue(
                ParamType.AGENCY_CLIENT_WHITE_LIST,
                entityId,
                true
        );
        NumberParamValue decimalValue = new NumberParamValue(
                ParamType.MARKET_PRICE_MULTIPLIER,
                entityId,
                BigDecimal.valueOf(3.21)
        );
        StringParamValue stringValue = new StringParamValue(
                ParamType.SHOP_DELIVERY_CURRENCY,
                entityId,
                "testString"
        );
        DateParamValue dataValue = new DateParamValue(
                ParamType.CPA_LATEST_ACTIVITY,
                entityId,
                new Date(123456)
        );
        paramValues.add(boolValue);
        paramValues.add(decimalValue);
        paramValues.add(stringValue);
        paramValues.add(dataValue);

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(ResultSetExtractor.class)))
                .thenReturn(paramValues);

        MultiMap<ParamType, ParamValue> params = paramService.getParams(entityId, types);

        assertThat(params.keySet()).hasSize(4);
        assertThat(params.get(ParamType.AGENCY_CLIENT_WHITE_LIST)).first().isSameAs(boolValue);
        assertThat(params.get(ParamType.MARKET_PRICE_MULTIPLIER)).first().isEqualTo(decimalValue);
        assertThat(params.get(ParamType.SHOP_DELIVERY_CURRENCY)).first().isEqualTo(stringValue);
        assertThat(params.get(ParamType.CPA_LATEST_ACTIVITY)).first().isEqualTo(dataValue);
    }
}
