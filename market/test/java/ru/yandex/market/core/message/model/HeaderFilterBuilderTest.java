package ru.yandex.market.core.message.model;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.simple.model.type.NotificationPriority;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit тесты для {@link ru.yandex.market.core.message.model.HeaderFilter.HeaderFilterBuilder}.
 *
 * @author avetokhin 21/11/16.
 */
public class HeaderFilterBuilderTest {
    private static final Long THEME_ID = 1L;
    private static final Long CLIENT_ID = 2L;
    private static final Long SHOP_ID = 3L;

    private static final Collection<NotificationPriority> PRIORITIES = List.of(
            NotificationPriority.HIGH,
            NotificationPriority.NORMAL
    );
    private static final List<Long> GROUP_IDS = List.of(1L, 2L, 3L);

    private static final long FROM_MILLIS = 1479721269;
    private static final long TO_MILLIS = 1479727269;

    @Test
    public void testNullValues() {
        HeaderFilter filter = HeaderFilter.newBuilder()
                .withThemeId(null).withClientId(null).withShopId(null)
                .withDateFrom(null).withDateTo(null)
                .withPriorities(null).withGroupIds(null).build();

        assertThat(filter.getThemeId(), equalTo(Optional.empty()));
        assertThat(filter.getClientId(), equalTo(Optional.empty()));
        assertThat(filter.getShopId(), equalTo(Optional.empty()));
        assertThat(filter.getDateFrom(), equalTo(Optional.empty()));
        assertThat(filter.getDateTo(), equalTo(Optional.empty()));
        assertThat(filter.getPriorities(), containsInAnyOrder(NotificationPriority.values()));
        assertThat(filter.getGroupIds(), empty());
    }

    @Test
    public void testValidValues() {
        HeaderFilter filter = HeaderFilter.newBuilder()
                .withThemeId(THEME_ID).withClientId(CLIENT_ID).withShopId(SHOP_ID)
                .withDateFrom(new Date(FROM_MILLIS)).withDateTo(new Date(TO_MILLIS))
                .withPriorities(PRIORITIES).withGroupIds(GROUP_IDS).build();

        assertThat(filter.getThemeId(), equalTo(Optional.of(THEME_ID)));
        assertThat(filter.getClientId(), equalTo(Optional.of(CLIENT_ID)));
        assertThat(filter.getShopId(), equalTo(Optional.of(SHOP_ID)));
        assertThat(filter.getDateFrom(), equalTo(Optional.of(Instant.ofEpochMilli(FROM_MILLIS))));
        assertThat(filter.getDateTo(), equalTo(Optional.of(Instant.ofEpochMilli(TO_MILLIS))));
        assertTrue(CollectionUtils.isEqualCollection(filter.getPriorities(), PRIORITIES));
        assertTrue(CollectionUtils.isEqualCollection(filter.getGroupIds(), GROUP_IDS));
    }

}
