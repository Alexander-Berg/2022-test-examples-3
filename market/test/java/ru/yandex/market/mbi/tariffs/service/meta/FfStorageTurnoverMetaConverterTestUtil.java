package ru.yandex.market.mbi.tariffs.service.meta;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.mbi.tariffs.model.ServiceTypeEnum;

import static ru.yandex.market.mbi.tariffs.service.meta.MetaConverterServiceTest.UNKNOWN_BILLING_UNIT;
import static ru.yandex.market.mbi.tariffs.service.meta.MetaConverterServiceTest.fieldNotNull;

/**
 * Утилитный класс тестовых данных для {@link ServiceTypeEnum#FF_STORAGE_TURNOVER}
 */
public class FfStorageTurnoverMetaConverterTestUtil {
    private static final String FF_STORAGE_TURNOVER_FAILED_DATA = "ffStorageTurnover";
    /**
     * Данные для успешной сериализации данных
     */
    public static Arguments deserializeMetaData() {
        return Arguments.of(
                ServiceTypeEnum.FF_STORAGE_TURNOVER,
                /*language=json*/ """
                          [{
                            "amount": 0,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "item",
                            "categoryId": 198119,
                            "turnoverFrom" : 0,
                            "turnoverTo": 120
                          }, {
                            "amount": 20,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "item",
                            "categoryId": 198119,
                            "turnoverFrom" : 120,
                            "turnoverTo": 150
                          }, {
                            "amount": 45,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "item",
                            "categoryId": 198119,
                            "turnoverFrom" : 150
                          }, {
                            "amount": 0,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "item",
                            "turnoverFrom" : 0,
                            "turnoverTo": 150
                          }, {
                            "amount": 45,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "item",
                            "turnoverFrom" : 150
                          }]
                        """
        );
    }

    public static Stream<Arguments> deserializeFailed() {
        return Stream.of(
                Arguments.of(ServiceTypeEnum.FF_STORAGE_TURNOVER,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "relative",
                                    "currency": "RUB",
                                    "billingUnit": "shift"
                                }]
                                """,
                        "Type must be ABSOLUTE",
                        FF_STORAGE_TURNOVER_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.FF_STORAGE_TURNOVER,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "shift"
                                }]
                                """,
                        UNKNOWN_BILLING_UNIT,
                        FF_STORAGE_TURNOVER_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.FF_STORAGE_TURNOVER,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "item"
                                }]
                                """,
                        fieldNotNull("turnoverFrom"),
                        FF_STORAGE_TURNOVER_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.FF_STORAGE_TURNOVER,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "item",
                                    "turnoverFrom": -10
                                }]
                                """,
                        "The field 'turnoverFrom' must be not negative",
                        FF_STORAGE_TURNOVER_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.FF_STORAGE_TURNOVER,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "item",
                                    "turnoverFrom": 0,
                                    "turnoverTo": -10
                                }]
                                """,
                        "The field 'turnoverTo' must be bigger than zero",
                        FF_STORAGE_TURNOVER_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.FF_STORAGE_TURNOVER,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "item",
                                    "turnoverFrom": 10,
                                    "turnoverTo": 9
                                }]
                                """,
                        "TurnoverTo must be greater than turnoverFrom",
                        FF_STORAGE_TURNOVER_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.FF_STORAGE_TURNOVER,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "item",
                                    "turnoverFrom": 0,
                                    "turnoverTo": 150,
                                    "categoryId" : 123
                                }]
                                """,
                        "The category [123] doesnt exists",
                        FF_STORAGE_TURNOVER_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.FF_STORAGE_TURNOVER,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "item",
                                    "turnoverFrom": 0,
                                    "turnoverTo": 150,
                                    "categoryId" : 90607
                                }]
                                """,
                        "CategoryId must be department on first level (child of 90401)",
                        FF_STORAGE_TURNOVER_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.FF_STORAGE_TURNOVER,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "item",
                                    "turnoverFrom": 0,
                                    "turnoverTo": 150,
                                    "categoryId" : 198119
                                }, {
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "item",
                                    "turnoverFrom": 120,
                                    "categoryId" : 198119
                                }]
                                """,
                        "Found intersection on category 198119: [0, 150) intersect with [120, null)",
                        FF_STORAGE_TURNOVER_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.FF_STORAGE_TURNOVER,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "item",
                                    "turnoverFrom": 0,
                                    "turnoverTo": 150
                                }, {
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "item",
                                    "turnoverFrom": 120
                                }]
                                """,
                        "Found intersection on category null: [0, 150) intersect with [120, null)",
                        FF_STORAGE_TURNOVER_FAILED_DATA
                )
        );
    }
}
