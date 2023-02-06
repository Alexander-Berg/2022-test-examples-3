package ru.yandex.market.notification.notifications;

import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.notification.notifications.hidings.UCHiddenOffersNotification;
import ru.yandex.market.notification.notifications.hidings.UCHiddenOffersNotificationData;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Тесты для {@link UCHiddenOffersNotification}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class UCHiddenOffersNotificationTest extends FunctionalTest {

    @Autowired
    private UCHiddenOffersNotification ucHiddenOffersNotification;

    @Autowired
    @Qualifier("yqlNamedParameterJdbcTemplate")
    private NamedParameterJdbcTemplate yqlNamedParameterJdbcTemplate;

    @Test
    @DisplayName("Возвращаются только включенные партнеры")
    @DbUnitDataSet(before = "UCHiddenOffersNotificationTest.before.csv")
    void testGetPartners() {
        mockYqlResult(Map.of(
                100L, 10L,
                200L, 20L,
                300L, 30L,
                400L, 40L
        ));
        UCHiddenOffersNotificationData preparedData = ucHiddenOffersNotification.prepareData();
        Collection<Long> partnerIds = ucHiddenOffersNotification.getPartnerIds(preparedData);

        assertThat(partnerIds)
                .containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    @DisplayName("YT ничего не вернул, мы тоже ничего не вернули и не упали")
    @DbUnitDataSet(before = "UCHiddenOffersNotificationTest.before.csv")
    void testGetEmptyPartners() {
        mockYqlResult(Map.of());
        UCHiddenOffersNotificationData preparedData = ucHiddenOffersNotification.prepareData();
        Collection<Long> partnerIds = ucHiddenOffersNotification.getPartnerIds(preparedData);

        assertThat(partnerIds)
                .isEmpty();
    }

    @Test
    @DisplayName("YT вернул id, но все из них мертвые")
    @DbUnitDataSet(before = "UCHiddenOffersNotificationTest.before.csv")
    void testGetNotAlivePartners() {
        mockYqlResult(Map.of(
                10100L, 10L,
                10200L, 20L,
                10300L, 30L,
                10400L, 40L
        ));
        UCHiddenOffersNotificationData preparedData = ucHiddenOffersNotification.prepareData();
        Collection<Long> partnerIds = ucHiddenOffersNotification.getPartnerIds(preparedData);

        assertThat(partnerIds)
                .isEmpty();
    }

    @Test
    @DisplayName("Нотификация генерится корректно, со всеми нужными данными")
    @DbUnitDataSet(before = "UCHiddenOffersNotificationTest.before.csv")
    void testNotification() {
        mockYqlResult(Map.of(
                100L, 10L,
                200L, 20L,
                300L, 30L,
                400L, 40L
        ));
        UCHiddenOffersNotificationData preparedData = ucHiddenOffersNotification.prepareData();
        NotificationSendContext notification = ucHiddenOffersNotification.getPartnerNotification(100L, preparedData).orElseThrow();

        assertThat(notification.getShopId())
                .isEqualTo(100L);
        assertThat(notification.getData())
                .hasSize(3);
        assertThat(notification.getData().get(0))
                .hasFieldOrPropertyWithValue("id", 100000L);
        assertThat(notification.getData().get(1))
                .hasFieldOrPropertyWithValue("internalName", "TestShop");
        assertThat(notification.getData().get(2))
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new NamedContainer("hidden-offers", 10L));

    }

    private void mockYqlResult(Map<Long, Long> data) {
        Mockito.when(yqlNamedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(ResultSetExtractor.class)))
                .thenReturn(new UCHiddenOffersNotificationData() {{
                    putAll(data);
                }});
    }
}
