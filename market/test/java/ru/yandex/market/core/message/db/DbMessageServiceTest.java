package ru.yandex.market.core.message.db;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.framework.pager.Pager;
import ru.yandex.market.core.message.model.AgencyMessageAccess;
import ru.yandex.market.core.message.model.HeaderFilter;
import ru.yandex.market.core.message.model.UserMessageAccess;
import ru.yandex.market.core.message.service.ShopMessageAccessService;
import ru.yandex.market.notification.safe.composer.NotificationComposer;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link DbMessageService}.
 *
 * @author avetokhin 15/12/16.
 */
public class DbMessageServiceTest extends FunctionalTest {
    private static final Date SENT_TIME = new Date(2022 - 1900, 4, 17);
    private static final HeaderFilter FILTER_REGULAR = HeaderFilter.newBuilder()
            .withDateTo(new Date(SENT_TIME.getTime() + TimeUnit.DAYS.toMillis(1L)))
            .withDateFrom(new Date(SENT_TIME.getTime() - TimeUnit.DAYS.toMillis(1L)))
            .withThemeId(5L)
            .build();
    private static final UserMessageAccess USER_ACCESS_1 = new UserMessageAccess(1L, null);
    private static final UserMessageAccess USER_ACCESS_2 = new UserMessageAccess(2L, 2L);
    private static final UserMessageAccess USER_ACCESS_3 = new UserMessageAccess(3L, null);

    private static final AgencyMessageAccess AGENCY_ACCESS_1 = new AgencyMessageAccess(1L, 1L);
    private static final AgencyMessageAccess AGENCY_ACCESS_2 = new AgencyMessageAccess(2L, 2L);
    private static final AgencyMessageAccess AGENCY_ACCESS_3 = new AgencyMessageAccess(3L, 3L);
    private static final AgencyMessageAccess AGENCY_ACCESS_4 = new AgencyMessageAccess(4L, 4L);

    private static final Set<UserMessageAccess> USER_MESSAGE_ACCESS_SET = Stream.of(
            USER_ACCESS_1, USER_ACCESS_2, USER_ACCESS_3
    ).collect(Collectors.toSet());

    private static final Set<AgencyMessageAccess> AGENCY_MESSAGE_ACCESS_SET = Stream.of(
            AGENCY_ACCESS_1, AGENCY_ACCESS_2, AGENCY_ACCESS_3, AGENCY_ACCESS_4
    ).collect(Collectors.toSet());

    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    NotificationComposer notificationComposer;

    private ShopMessageAccessService shopMessageAccessService;
    private DbMessageService dbMessageService;

    @BeforeEach
    void setUp() {
        shopMessageAccessService = mock(ShopMessageAccessService.class);
        dbMessageService = new DbMessageService(
                jdbcTemplate,
                null,
                shopMessageAccessService,
                null,
                mock(BalanceService.class),
                notificationComposer
        );
    }

    @Test
    public void getFilteredAgencyShopsNoFilterTest() {
        var result = DbMessageService.getFilteredAgencyShops(AGENCY_MESSAGE_ACCESS_SET, null);
        assertThat(result).containsExactlyElementsOf(AGENCY_MESSAGE_ACCESS_SET.stream()
                .map(UserMessageAccess::getShopId).collect(Collectors.toSet()));
    }

    @Test
    public void getFilteredAgencyShopsEmptyFilterTest() {
        var filter = HeaderFilter.newBuilder().build();

        var result = DbMessageService.getFilteredAgencyShops(AGENCY_MESSAGE_ACCESS_SET, filter);
        assertThat(result).containsExactlyElementsOf(AGENCY_MESSAGE_ACCESS_SET.stream()
                .map(UserMessageAccess::getShopId).collect(Collectors.toSet()));
    }

    @Test
    public void getFilteredAgencyShopsWithFilterByShopTest() {
        var filter = HeaderFilter.newBuilder()
                .withShopId(2L)
                .build();

        var result = DbMessageService.getFilteredAgencyShops(AGENCY_MESSAGE_ACCESS_SET, filter);
        assertThat(result).containsExactly(AGENCY_ACCESS_2.getShopId());
    }

    @Test
    public void getFilteredAgencyShopsWithFilterByClientTest() {
        var filter = HeaderFilter.newBuilder()
                .withClientId(1L)
                .build();

        var result = DbMessageService.getFilteredAgencyShops(AGENCY_MESSAGE_ACCESS_SET, filter);
        assertThat(result).containsExactly(AGENCY_ACCESS_1.getShopId());
    }

    @Test
    public void getFilteredContactAccessSetNoFilterTest() {
        var result = DbMessageService.getFilteredContactAccessSet(USER_MESSAGE_ACCESS_SET, null);
        assertThat(result).containsExactlyInAnyOrder(USER_ACCESS_1, USER_ACCESS_2, USER_ACCESS_3);
    }


    @Test
    public void getFilteredContactAccessSetEmptyFilterTest() {
        var filter = HeaderFilter.newBuilder().build();

        var result = DbMessageService.getFilteredContactAccessSet(USER_MESSAGE_ACCESS_SET, filter);
        assertThat(result).containsExactly(USER_ACCESS_1, USER_ACCESS_2, USER_ACCESS_3);
    }

    @Test
    public void getFilteredContactAccessSetFilterByShopTest() {
        var filter = HeaderFilter.newBuilder()
                .withShopId(1L).build();

        var result = DbMessageService.getFilteredContactAccessSet(USER_MESSAGE_ACCESS_SET, filter);
        assertThat(result).containsExactly(USER_ACCESS_1);
    }

    @Test
    void getGroupHeadersEmpty() {
        var filter = HeaderFilter.newBuilder()
                .withShopId(1L)
                .withGroupIds(Set.of(101L, 102L))
                .build();
        var pager = new Pager(0, 1);
        var headers = dbMessageService.getMessageHeaders(0L, (Long) null, filter, pager);
        assertThat(headers).hasSize(pager.getItemCount()).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "DbMessageServiceTest.before.csv")
    void getGroupHeadersFound() {
        var filter = HeaderFilter.newBuilder()
                .withShopId(1L)
                .withGroupIds(Set.of(101L, 102L))
                .build();
        var pager = new Pager(0, 2);
        var headers = dbMessageService.getMessageHeaders(0L, (Long) null, filter, pager);
        assertThat(pager.getItemCount()).isEqualTo(2);
        assertThat(headers)
                .satisfiesExactly(
                        h -> {
                            assertThat(h.getId()).isEqualTo(3L);
                            assertThat(h.getImportance()).isEqualTo(NotificationPriority.LOW);
                            assertThat(h.getSentTime()).isEqualTo(SENT_TIME);
                        },
                        h -> {
                            assertThat(h.getId()).isEqualTo(2L);
                            assertThat(h.getImportance()).isEqualTo(NotificationPriority.NORMAL);
                            assertThat(h.getSentTime()).isEqualTo(SENT_TIME);
                        }
                );
    }

    @Test
    void getMessageHeadersForAgencyEmpty() {
        var userId = 0L;
        var agencyId = 1L;
        var clientId = 4444L;
        when(shopMessageAccessService.getMessageAccessSetForAgency(agencyId))
                .thenReturn(Set.of(new AgencyMessageAccess(1L, clientId)));

        var pager = new Pager(0, 3);
        var headers = dbMessageService.getMessageHeaders(
                userId,
                new ClientInfo(clientId, ClientType.PHYSICAL, true, agencyId),
                FILTER_REGULAR,
                pager
        );
        assertThat(headers).hasSize(pager.getItemCount()).isEmpty();
    }

    @Test
    void getMessageHeadersForContactEmpty() {
        var userId = 100500L;
        when(shopMessageAccessService.getMessageAccessSetForContact(userId))
                .thenReturn(Set.of(new UserMessageAccess(1L, null)));

        var pager = new Pager(0, 1);
        var headers = dbMessageService.getMessageHeaders(
                userId,
                (Long) null,
                FILTER_REGULAR,
                pager
        );
        assertThat(headers).hasSize(pager.getItemCount()).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "DbMessageServiceTest.before.csv")
    void getMessageHeadersForContactNonEmtpy() {
        var userId = 100500L;
        when(shopMessageAccessService.getMessageAccessSetForContact(userId))
                .thenReturn(Set.of(new UserMessageAccess(1L, null)));

        var pager = new Pager(0, 1);
        var headers = dbMessageService.getMessageHeaders(
                userId,
                (Long) null,
                FILTER_REGULAR,
                pager
        );
        assertThat(pager.getItemCount())
                .as("total found")
                .isEqualTo(2);
        assertThat(headers)
                .as("paged result")
                .hasSize(1)
                .satisfiesExactly(
                        h -> {
                            assertThat(h.getId()).isEqualTo(1L);
                            assertThat(h.getImportance()).isEqualTo(NotificationPriority.HIGH);
                            assertThat(h.getSentTime()).isEqualTo(SENT_TIME);
                        }
                );
    }

    @Test
    @DbUnitDataSet(before = "DbMessageServiceTest.before.csv")
    void getMessage() {
        var message = dbMessageService.getNotificationMessage(1L, 100500L);
        assertThat(message).get().satisfies(m -> {
            assertThat(m.getId()).isEqualTo(1L);
            assertThat(m.getShopId()).isEqualTo(1L);
            assertThat(m.getImportance()).isEqualTo(NotificationPriority.HIGH);
            assertThat(m.getSentDate()).isEqualTo(SENT_TIME);
        });
    }
}
