package ru.yandex.market.checkout.checkouter.config.web.serialization;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.ser.PropertyWriter;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.tvm.TvmAuthorization;
import ru.yandex.market.checkout.checkouter.tvm.TvmAuthorizationType;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderDeliveryViewModel;
import ru.yandex.market.checkout.common.json.jackson.DefaultJsonPropertyFilter;
import ru.yandex.market.checkout.common.trace.TvmAuthorizationContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertySerializationConfigTest {

    @Test
    public void testWithOutTvm() throws Exception {
        PropertyWriter pw = mock(PropertyWriter.class);
        when(pw.getName()).thenReturn("shipment");
        PropertySerializationConfig config = new PropertySerializationConfig();
        CheckouterProperties properties = new CheckouterPropertiesImpl();
        properties.setDeliveryWhiteTvmClientIds(Collections.emptySet());

        DefaultJsonPropertyFilter<OrderDeliveryViewModel> filter = config.deliveryViewFilter(properties);

        OrderDeliveryViewModel model = new OrderDeliveryViewModel();
        filter.serializeAsField(model, null, null, pw);

        verify(pw).serializeAsField(eq(model), any(), any());
    }


    @Test
    public void testWithTvm() throws Exception {
        try {
            TvmAuthorizationContextHolder.setTvmAuthorization(new TvmAuthorization(TvmAuthorizationType.NOT_AUTHORIZED,
                    10L));
            PropertyWriter pw = mock(PropertyWriter.class);
            when(pw.getName()).thenReturn("shipment");
            PropertySerializationConfig config = new PropertySerializationConfig();
            CheckouterProperties properties = new CheckouterPropertiesImpl();
            properties.setDeliveryWhiteTvmClientIds(Collections.emptySet());

            DefaultJsonPropertyFilter<OrderDeliveryViewModel> filter = config.deliveryViewFilter(properties);

            OrderDeliveryViewModel model = new OrderDeliveryViewModel();
            filter.serializeAsField(model, null, null, pw);

            verify(pw, never()).serializeAsField(eq(model), any(), any());
        } finally {
            TvmAuthorizationContextHolder.setTvmAuthorization(TvmAuthorization.notAuthorized());
        }
    }

    @Test
    public void testWithTvmAndWhiteList() throws Exception {
        try {
            TvmAuthorizationContextHolder.setTvmAuthorization(new TvmAuthorization(TvmAuthorizationType.NOT_AUTHORIZED,
                    10L));
            PropertyWriter pw = mock(PropertyWriter.class);
            when(pw.getName()).thenReturn("shipment");
            PropertySerializationConfig config = new PropertySerializationConfig();
            CheckouterProperties properties = new CheckouterPropertiesImpl();
            properties.setDeliveryWhiteTvmClientIds(Set.of(10L));

            DefaultJsonPropertyFilter<OrderDeliveryViewModel> filter = config.deliveryViewFilter(properties);

            OrderDeliveryViewModel model = new OrderDeliveryViewModel();
            filter.serializeAsField(model, null, null, pw);

            verify(pw).serializeAsField(eq(model), any(), any());
        } finally {
            TvmAuthorizationContextHolder.setTvmAuthorization(TvmAuthorization.notAuthorized());
        }
    }
}
