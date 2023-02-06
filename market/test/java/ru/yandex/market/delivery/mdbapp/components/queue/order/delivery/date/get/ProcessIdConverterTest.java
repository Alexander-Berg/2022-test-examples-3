package ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.get;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.delivery.mdbapp.util.DeliveryDateUpdateReason;

public class ProcessIdConverterTest {
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    public static final Long PARCEL_ID = 12345L;
    public static final DeliveryDateUpdateReason REASON = DeliveryDateUpdateReason.DELIVERY_SERVICE_DELAYED;

    private final ProcessIdConverter converter = new ProcessIdConverter();

    @Test
    public void testWithParcelId() {
        String id = converter.toId(REASON, PARCEL_ID);
        softly.assertThat(converter.getParcelId(id)).isEqualTo(PARCEL_ID);
        softly.assertThat(converter.getReason(id)).isEqualTo(REASON);
    }

    @Test
    public void testNullParcelId() {
        String id = converter.toId(REASON, null);
        softly.assertThat(converter.getParcelId(id)).isNull();
        softly.assertThat(converter.getReason(id)).isEqualTo(REASON);
    }

    @Test
    public void testSupports() {
        softly.assertThat(converter.supports(null)).isFalse();
        softly.assertThat(converter.supports("")).isFalse();
        softly.assertThat(converter.supports("abcd")).isFalse();
        softly.assertThat(converter.supports(converter.LGW_PROCESS_CODE)).isTrue();
        softly.assertThat(converter.supports(converter.LGW_PROCESS_CODE + "anythingelse")).isTrue();
    }
}
