package ru.yandex.market.mbi.tariffs.service.meta;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.mbi.tariffs.model.ServiceTypeEnum;

import static ru.yandex.market.mbi.tariffs.service.meta.MetaConverterServiceTest.UNKNOWN_BILLING_UNIT;
import static ru.yandex.market.mbi.tariffs.service.meta.MetaConverterServiceTest.fieldNotNull;

/**
 * Утилитный класс тестовых данных для {@link ServiceTypeEnum#COURIER_FINE}
 */
public class CourierFineMetaConverterTestUtil {

    private static final String COURIER_FINE_FAILED_DATA = "courierFine";

    /**
     * Данные для успешной сериализации данных
     */
    public static Arguments deserializeMetaData() {
        return Arguments.of(ServiceTypeEnum.COURIER_FINE,
                /*language=json*/ """
                          [{
                            "amount": 3000,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "shift",
                            "fineType": "unknown",
                            "courierType" : "PARTNER",
                            "shiftType": "SMALL_GOODS",
                            "isFine" : true
                          }, {
                            "amount": 3000,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "shift",
                            "fineType": "ADDITIONAL_PAYMENT",
                            "courierType" : "SELF_EMPLOYED",
                            "isFine" : false
                          }]
                        """
        );
    }

    /**
     * Данные для не успешной сериализации данных
     */
    public static Stream<Arguments> deserializeFailed() {
        return Stream.of(
                Arguments.of(ServiceTypeEnum.COURIER_FINE,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "item"
                                }]
                                """,
                        UNKNOWN_BILLING_UNIT,
                        COURIER_FINE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_FINE,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "shift"
                                }]
                                """,
                        fieldNotNull("fineType"),
                        COURIER_FINE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_FINE,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "shift",
                                    "fineType": "unknown"
                                }]
                                """,
                        fieldNotNull("courierType"),
                        COURIER_FINE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_FINE,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "shift",
                                    "fineType": "unknown",
                                    "courierType": "PARTNER"
                                }]
                                """,
                        fieldNotNull("isFine"),
                        COURIER_FINE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_FINE,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "shift",
                                    "fineType": "unknown",
                                    "courierType": "PARTNER",
                                    "isFine": true,
                                    "shiftType": "WITHDRAW"
                                }]
                                """,
                        "ShiftType must be one of : ",
                        COURIER_FINE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_FINE,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "shift",
                                    "fineType": "unknown",
                                    "courierType": "PARTNER",
                                    "isFine": true,
                                    "shiftType": "SMALL_GOODS"
                                }, {
                                    "amount": 100,
                                    "type": "relative",
                                    "currency": "RUB",
                                    "billingUnit": "shift",
                                    "fineType": "unknown",
                                    "courierType": "PARTNER",
                                    "isFine": true,
                                    "shiftType": "SMALL_GOODS"
                                }]
                                """,
                        "The meta contains duplicated fields (courier type + fine type + shift type) : [(PARTNER,unknown,SMALL_GOODS)]",
                        COURIER_FINE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_FINE,
                        /*language=json*/ """
                                [{
                                    "amount": 100,
                                    "type": "absolute",
                                    "currency": "RUB",
                                    "billingUnit": "shift",
                                    "fineType": "fineType1",
                                    "courierType": "PARTNER",
                                    "isFine": true,
                                    "shiftType": "BULKY_CARGO"
                                }, {
                                    "amount": 100,
                                    "type": "relative",
                                    "currency": "RUB",
                                    "billingUnit": "shift",
                                    "fineType": "fineType1",
                                    "courierType": "PARTNER",
                                    "isFine": false,
                                    "shiftType": "SMALL_GOODS"
                                }]
                                """,
                        "The fine types contains flag 'isFine' not the same state: [fineType1]",
                        COURIER_FINE_FAILED_DATA
                )
        );
    }
}
