package ru.yandex.market.core.notification.converter;

import org.junit.Test;

import ru.yandex.market.core.notification.context.NotificationContext;
import ru.yandex.market.core.notification.context.impl.BusinessNotificationContext;
import ru.yandex.market.core.notification.context.impl.DualNotificationContext;
import ru.yandex.market.core.notification.context.impl.EmptyNotificationContext;
import ru.yandex.market.core.notification.context.impl.ShopNotificationContext;
import ru.yandex.market.core.notification.context.impl.UidableNotificationContext;
import ru.yandex.market.notification.common.model.destination.MbiDestination;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Тест для {@link MbiDestinationToContextConverter}.
 */
public class MbiDestinationToContextConverterTest {

    private final MbiDestinationToContextConverter converter = new MbiDestinationToContextConverter();

    @Test
    public void testEmpty() {
        final NotificationContext context = converter.convert(MbiDestination.create(null, null, null));
        assertThat(context, notNullValue());
        assertThat(context, instanceOf(EmptyNotificationContext.class));
    }

    @Test
    public void testNull() {
        final NotificationContext context = converter.convert(null);
        assertThat(context, nullValue());
    }

    @Test
    public void testShop() {
        final NotificationContext context = converter.convert(MbiDestination.create(50L, null, null));
        assertThat(context, notNullValue());
        assertThat(context, instanceOf(ShopNotificationContext.class));
        assertThat(((ShopNotificationContext) context).getShopId(), equalTo(50L));
    }

    @Test
    public void testBusiness() {
        final NotificationContext context = converter.convert(MbiDestination.create(null,500L, null, null));
        assertThat(context, notNullValue());
        assertThat(context, instanceOf(BusinessNotificationContext.class));
        assertThat(((BusinessNotificationContext) context).getBusinessId(), equalTo(500L));
    }

    @Test
    public void testUser() {
        final NotificationContext context = converter.convert(MbiDestination.create(null, 50L, null));
        assertThat(context, notNullValue());
        assertThat(context, instanceOf(UidableNotificationContext.class));
        assertThat(((UidableNotificationContext) context).getUid(), equalTo(50L));
    }

    @Test
    public void testDual() {
        final NotificationContext context = converter.convert(MbiDestination.create(50L, 60L, null));
        assertThat(context, notNullValue());
        assertThat(context, instanceOf(DualNotificationContext.class));
        assertThat(((DualNotificationContext) context).getShopId(), equalTo(50L));
        assertThat(((DualNotificationContext) context).getUid(), equalTo(60L));
    }

}
