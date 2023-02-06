package ru.yandex.market.abo.clch.regular.loaders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.tel.CallDirection;
import ru.yandex.market.abo.core.tel.CallSourceType;
import ru.yandex.market.abo.core.tel.CallerType;
import ru.yandex.market.abo.core.tel.PhoneCall;
import ru.yandex.market.abo.core.tel.PhoneCallRepo;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicket;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketService;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 */
class ShopForRegularClchByPhoneLoaderTest extends EmptyTest {

    private static final long SHOP_ID = 123L;

    private static final String OLD_PHONE = "8 800 999 33 55";
    private static final String NEW_PHONE = "8 800 111 33 55";

    private static final LocalDateTime PHONE_PREVIOUS_USAGE_TIME = LocalDateTime.now()
            .minusDays(ShopForRegularClchLoader.NEW_DATA_PERIOD_DAYS + 1);
    private static final LocalDateTime PHONE_CURRENT_USAGE_TIME = LocalDateTime.now()
            .minusDays(ShopForRegularClchLoader.NEW_DATA_PERIOD_DAYS).plusHours(1);


    @Autowired
    private ShopForRegularClchByPhoneLoader loader;
    @Autowired
    private PhoneCallRepo phoneCallRepo;
    @Autowired
    private RecheckTicketService recheckTicketService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void init() {
        jdbcTemplate.update("insert into shop(id, cpc_active_last_thirty_days) values (?, true)", SHOP_ID);
        savePhoneCall(OLD_PHONE, PHONE_PREVIOUS_USAGE_TIME);
    }

    @Test
    void testLoadShops__changedShopsNotExist() {
        savePhoneCall(OLD_PHONE, PHONE_CURRENT_USAGE_TIME);

        assertTrue(loader.loadShopsForClch().isEmpty());
    }

    @Test
    void testLoadShops__shopChanged() {
        savePhoneCall(NEW_PHONE, PHONE_CURRENT_USAGE_TIME);

        assertEquals(Set.of(SHOP_ID), loader.loadShopsForClch());
    }

    private void savePhoneCall(String phoneNumber, LocalDateTime creationTime) {
        var ticket = recheckTicketService.save(new RecheckTicket.Builder()
                .withShopId(SHOP_ID)
                .withType(RecheckTicketType.LITE_CPC)
                .build()
        );

        var phoneCall = PhoneCall.newBuilder(41L, CallSourceType.LITE, ticket.getId(), phoneNumber)
                .callDirection(CallDirection.OUT)
                .callerType(CallerType.COURIER)
                .creationTime(DateUtil.asDate(creationTime))
                .build();

        phoneCallRepo.save(phoneCall);

        flushAndClear();
    }
}
