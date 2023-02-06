package ru.yandex.market.crm.operatorwindow.checkout;

import java.util.Objects;
import java.util.function.Function;

import com.google.common.base.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.tracking.CheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackStatus;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubstatus;
import ru.yandex.market.crm.operatorwindow.http.controller.api.orders.converter.CheckpointStatusViewConverter;
import ru.yandex.market.crm.operatorwindow.http.controller.api.orders.converter.HistoryEventTypeViewConverter;
import ru.yandex.market.crm.operatorwindow.http.controller.api.orders.converter.PaymentStatusDescription;
import ru.yandex.market.crm.operatorwindow.http.controller.api.orders.converter.PaymentSubstatusDescription;
import ru.yandex.market.crm.operatorwindow.http.controller.api.orders.converter.TrackStatusDescription;

/**
 * TODO: завести справочники и убрать ручную механику
 */
public class CheckouterEnumsConvertersTest {

    private static final String EMPTY_VALUE = "";

    @Test
    public void checkpointStatusViewConverter() {
        assertNonTrivialValuesAreExistsForAllEnumValues(
                CheckpointStatus.values(),
                CheckpointStatus.UNKNOWN,
                CheckpointStatusViewConverter::convert);
    }

    @Test
    public void historyEventTypeViewConverter() {
        assertNonTrivialValuesAreExistsForAllEnumValues(
                HistoryEventType.values(),
                HistoryEventType.UNKNOWN,
                HistoryEventTypeViewConverter::convert,
                "Use HistoryEventType.ApiModelProperty to get text representation for HistoryEventTypeViewConverter");
    }

    @Test
    public void paymentStatusViewConverter() {
        assertNonTrivialValuesAreExistsForAllEnumValues(
                PaymentStatus.values(),
                PaymentStatus.UNKNOWN,
                x -> new PaymentStatusDescription(x).getText());
    }

    @Test
    public void paymentSubstatusViewConverter() {
        assertNonTrivialValuesAreExistsForAllEnumValues(
                PaymentSubstatus.values(),
                PaymentSubstatus.UNKNOWN,
                x -> new PaymentSubstatusDescription(x).getText());
    }

    @Test
    public void trackStatusViewConverter() {
        assertNonTrivialValuesAreExistsForAllEnumValues(
                TrackStatus.values(),
                TrackStatus.UNKNOWN,
                x -> new TrackStatusDescription(x).getText());
    }

    private <T extends Enum> void assertNonTrivialValuesAreExistsForAllEnumValues(T[] enumValues,
                                                                                  T unknownValue,
                                                                                  Function<T, String> converter) {
        assertNonTrivialValuesAreExistsForAllEnumValues(enumValues,
                unknownValue,
                converter,
                "");
    }

    private <T extends Enum> void assertNonTrivialValuesAreExistsForAllEnumValues(T[] enumValues,
                                                                                  T unknownValue,
                                                                                  Function<T, String> converter,
                                                                                  String source) {
        Assertions.assertEquals(EMPTY_VALUE, converter.apply(null));
        String sourceMessage = Strings.isNullOrEmpty(source)
                ? ""
                : String.format("Use this source to define missing values: %s", source);
        for (T value : enumValues) {
            if (Objects.equals(unknownValue, value)) {
                continue;
            }
            String convertedValue = converter.apply(value);
            Assertions.assertFalse(
                    Strings.isNullOrEmpty(convertedValue), String.format("cannot convert item = %s", value));
            Assertions.assertFalse(
                    convertedValue.equalsIgnoreCase(value.name()), String.format(
                            "enum name instead of non trivial, explicitly converted value was found for %s. %s",
                            value,
                            sourceMessage));
        }
    }
}
