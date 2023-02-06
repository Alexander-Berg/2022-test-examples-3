package ru.yandex.market.partner;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DbUnitDataSet(before = "LinkVirtualShopToFFCommandTest.csv")
class LinkVirtualShopToFFCommandTest extends FunctionalTest {
    private static final String LINKS_COUNT =
            "select count(*) from shops_web.virtual_shop_ff_services " +
                    "where partner_id = :partnerId and service_id = :serviceId";
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private LinkVirtualShopToFFCommand linkVirtualShopToFFCommand;

    @BeforeEach
    void setUp() {
        linkVirtualShopToFFCommand = new LinkVirtualShopToFFCommand(namedParameterJdbcTemplate);
    }

    @Test
    void testAddNew() {
        String partnerId = "1";
        String serviceId = "88";
        Assertions.assertEquals(0, checkLinks(partnerId, serviceId));
        runCommand("save", partnerId, serviceId);
        Assertions.assertEquals(1, checkLinks(partnerId, serviceId));
    }

    @Test
    void testAddExisting() {
        String partnerId = "2";
        String serviceId = "49";
        Assertions.assertEquals(1, checkLinks(partnerId, serviceId));
        runCommand("save", partnerId, serviceId);
        Assertions.assertEquals(1, checkLinks(partnerId, serviceId));
    }

    @Test
    void testRemoveExisting() {
        String partnerId = "2";
        String serviceId = "49";
        Assertions.assertEquals(1, checkLinks(partnerId, serviceId));
        runCommand("remove", partnerId, serviceId);
        Assertions.assertEquals(0, checkLinks(partnerId, serviceId));
    }

    @Test
    void testRemoveNonexistent() {
        String partnerId = "1";
        String serviceId = "49";
        Assertions.assertEquals(0, checkLinks(partnerId, serviceId));
        runCommand("remove", partnerId, serviceId);
        Assertions.assertEquals(0, checkLinks(partnerId, serviceId));
    }

    private void runCommand(String... arguments) {
        CommandInvocation commandInvocation =
                new CommandInvocation("link-virtual-shop-to-ff", arguments, Collections.emptyMap());
        linkVirtualShopToFFCommand.executeCommand(commandInvocation, new Terminal(System.in, System.out) {
            @Override
            protected void onStart() {
            }

            @Override
            protected void onClose() {
            }

            @Override
            public boolean areYouSure() {
                return true;
            }
        });
    }

    private int checkLinks(String partnerId, String serviceId) {
        final MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("partnerId", Long.valueOf(partnerId))
                .addValue("serviceId", Long.valueOf(serviceId));
        return namedParameterJdbcTemplate.queryForObject(LINKS_COUNT, params, Integer.class);
    }
}
