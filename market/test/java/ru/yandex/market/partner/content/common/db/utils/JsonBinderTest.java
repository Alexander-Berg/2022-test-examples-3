package ru.yandex.market.partner.content.common.db.utils;


import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonBinderTest {

    @Test
    public void serializeDeserializeNotNumbersForDouble() {
        JsonBinder<DoubleValue> binder = JsonBinder.protoFromJsonb(DoubleValue.class);
        DoubleValue.Builder builder = DoubleValue.newBuilder().setValue(Double.NaN);
        String mes = (String) binder.converter().to(builder.build());
        DoubleValue doubleValue = binder.converter().from(mes);
        assertThat(doubleValue.getValue()).isNaN();
    }

    @Test
    public void serializeDeserializeNotNumbersForFloat() {
        JsonBinder<FloatValue> binder = JsonBinder.protoFromJsonb(FloatValue.class);
        FloatValue.Builder builder = FloatValue.newBuilder().setValue(Float.NaN);
        String mes = (String) binder.converter().to(builder.build());
        FloatValue floatValue = binder.converter().from(mes);
        assertThat(floatValue.getValue()).isNaN();
    }

}