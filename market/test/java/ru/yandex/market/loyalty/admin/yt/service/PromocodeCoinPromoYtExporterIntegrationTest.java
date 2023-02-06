package ru.yandex.market.loyalty.admin.yt.service;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import ru.yandex.market.juggler.JugglerEvent;
import ru.yandex.market.loyalty.admin.monitoring.AdminMonitorType;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.mapper.exporter.PromocodeCoinMapperFactory;
import ru.yandex.market.loyalty.admin.yt.model.PromoForYt;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.monitoring.PushMonitor;
import ru.yandex.market.loyalty.monitoring.juggler.JugglerInternalPushMonitor;
import ru.yandex.market.loyalty.monitoring.juggler.LoyaltyJugglerEvent;
import Market.Promo.Promo.BudgetSourceType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

@Ignore
public class PromocodeCoinPromoYtExporterIntegrationTest extends MarketLoyaltyAdminMockedDbTest {

    @Value("classpath:sql/promocode-yt-exporter-sample.sql")
    private Resource backwardCompatibilityScript;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PromocodeCoinMapperFactory coinMapperFactory;
    @Autowired
    private PromoService promoService;
    @Autowired
    private CoinDao coinDao;
    @Autowired
    private PushMonitor pushMonitor;

    private PromocodeCoinPromoYtExporter promoYtExporter;

    @Before
    public void configure() {
        jdbcTemplate.execute((Connection con) -> {
            ScriptUtils.executeSqlScript(con, backwardCompatibilityScript);
            return null;
        });

        promoYtExporter = new PromocodeCoinPromoYtExporter(
                null, coinDao, "//tmp", null, null, promoService,
                coinMapperFactory, null, pushMonitor,
                configurationService);
    }

    @Test
    public void shouldLoadPromocodePromos() {
        List<PromoForYt> promocodes = promoYtExporter.loadAndMapPromocodeCoinPromo(new ArrayList<>());

        assertThat(promocodes, hasSize(1));
    }

    @Test
    public void shouldNotLoadDuplicatedPromos() throws IOException {
        promoYtExporter.loadAndMapPromocodeCoinPromo(new ArrayList<>());

        AtomicReference<Collection<LoyaltyJugglerEvent>> events = new AtomicReference<>();
        ((JugglerInternalPushMonitor) pushMonitor).pushEvents(events::set);

        List<LoyaltyJugglerEvent> criticals = events.get().stream()
                .filter(loyaltyJugglerEvent -> loyaltyJugglerEvent.getStatus() == JugglerEvent.Status.WARN)
                .collect(Collectors.toList());

        criticals.stream()
                .filter(e -> e.getService() == AdminMonitorType.DUPLICATED_PROMOCODE_TO_YT)
                .findFirst()
                .orElseThrow(() -> new AssertionError("There is not DUPLICATED_PROMOCODE_TO_YT error"));
    }

    @Test
    public void shouldLoadBudgetSources() {
        List<PromoForYt> promoForYts = promoYtExporter.loadAndMapPromocodeCoinPromo(new ArrayList<>());
        List<BudgetSourceType> budgetSourceTypeList =
                promoForYts.get(0).getPromo().getBudgetSources().getBudgetSourceTypeList();
        assertEquals(1, budgetSourceTypeList.size());
        assertEquals("VENDOR", budgetSourceTypeList.get(0).name());
    }
}
