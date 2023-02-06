package ru.yandex.market.abo.tms.premod;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.CoreCounter;
import ru.yandex.market.abo.core.premod.PremodItemService;
import ru.yandex.market.abo.core.premod.PremodManager;
import ru.yandex.market.abo.core.premod.PremodTicketService;
import ru.yandex.market.abo.core.premod.helper.ShopDataItemHelper;
import ru.yandex.market.abo.core.premod.model.PremodCheckType;
import ru.yandex.market.abo.core.premod.model.PremodItem;
import ru.yandex.market.abo.core.premod.model.PremodItemType;
import ru.yandex.market.abo.core.premod.model.PremodTicketStatus;
import ru.yandex.market.abo.core.premod.model.PremoderationTicketRequest;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.mm.db.DbMailAccountService;
import ru.yandex.market.abo.mm.model.AccountType;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.mbi.api.client.entity.moderation.PremoderationReadyShopResponse;
import ru.yandex.market.mbi.api.client.entity.shops.ProgramState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.ANTI_FRAUD;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.AUTOORDER;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.CATEGORY_CHANGE;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.CLONE;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.CLONE_CHECKER;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.CUT_PRICE_CHECK;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.INFO_IN_PARTNER_COINCIDE;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.MONITORINGS;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.NO_PROBLEMS_WITH_ORDER;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.OFFERS_ON_MARKET_COINCIDE;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.OTHER;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.OTHER_UNCRITICAL;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.PHONE_ORDER;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.SHOP_INFO_COLLECTED;

/**
 * @author artemmz
 * created on 14.09.16.
 */
public class PremodProcessorIntegrationTest extends EmptyTest {
    private static final long SHOP_ID = 774L;
    private static final AccountType ACCOUNT_TYPE = AccountType.AUTO_PREMOD;
    private static final Set<PremodItemType> EXPECTED_ITEM_TYPES = Set.of(SHOP_INFO_COLLECTED, OFFERS_ON_MARKET_COINCIDE,
            CLONE_CHECKER, OTHER, CLONE, OTHER_UNCRITICAL, CUT_PRICE_CHECK, AUTOORDER, MONITORINGS, ANTI_FRAUD,
            CATEGORY_CHANGE, INFO_IN_PARTNER_COINCIDE, PHONE_ORDER, NO_PROBLEMS_WITH_ORDER);

    @Autowired
    private PremodProcessor premodProcessor;
    private MbiApiService mbiApiService;
    @Autowired
    private PremodItemService premodItemService;
    @Autowired
    private PremodTicketService premodTicketService;
    private PremodTicketService premodTicketServiceSpy;
    @Autowired
    private PremodManager premodManager;
    @Autowired
    private ShopDataItemHelper shopInfoItemHelper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DbMailAccountService dbMailAccountService;

    @BeforeEach
    public void setUp() throws Exception {
        premodTicketServiceSpy = spy(premodTicketService);
        doNothing().when(premodTicketServiceSpy).ensureTicketNotFinished(anyLong());

        mbiApiService = mock(MbiApiService.class);
        PremodManager premodManagerSpy = spy(premodManager);
        premodManagerSpy.setPremodTicketService(premodTicketServiceSpy);

        premodProcessor.setMbiApiService(mbiApiService);
        premodProcessor.setPremodManager(premodManagerSpy);
        premodProcessor.setPremodTicketService(premodTicketServiceSpy);

        ShopInfoService shopInfoService = mock(ShopInfoService.class);
        when(shopInfoService.getShopInfo(anyLong())).thenReturn(initShopInfo());
        shopInfoItemHelper.setShopInfoService(shopInfoService);

        var accountId = dbMailAccountService.storeAccount("", -1L, ACCOUNT_TYPE);
        jdbcTemplate.update("INSERT INTO core_counter (name, value) VALUES (?, ?)",
                CoreCounter.AUTO_ORDER_ACCOUNT_CURRENT_ACTIVE_ID.asPrefixTo(ACCOUNT_TYPE.name()), accountId);
        jdbcTemplate.update("UPDATE core_config SET value = '1' WHERE id = ?", CoreConfig.PREMOD_AUTO_ORDER.getId());
    }

    @Test
    public void testCreateNewTicketsFromMbiApi() {
        var premodShops = List.of(new PremoderationReadyShopResponse(SHOP_ID, TestingType.CPC_PREMODERATION, false, 1));
        doReturn(premodShops).when(mbiApiService).getShopsForPremoderation();
        doReturn(List.of()).when(premodTicketServiceSpy).loadRunningTicketsByTypes(PremodProcessor.SYNC_CHECK_TYPES);

        premodProcessor.syncTicketsWithMbiApi();

        var ticket = premodTicketService.loadLastByTypeAndStatuses(
                SHOP_ID, PremodCheckType.CPC_PREMODERATION, EnumSet.allOf(PremodTicketStatus.class)
        ).orElseThrow();
        assertEquals(PremodTicketStatus.NEW, ticket.getStatus());
        var itemTypes = StreamEx.of(premodItemService.loadPremodItemsByTicket(ticket.getId()))
                .map(PremodItem::getType)
                .toSet();
        assertEquals(EXPECTED_ITEM_TYPES, itemTypes);
    }

    @Test
    void initProcessTicketsTests() {
        var premodRequest = new PremoderationTicketRequest(SHOP_ID, PremodCheckType.CPC_PREMODERATION);
        var ticket = premodTicketService.createPremodTicket(premodRequest);
        flushAndClear();

        premodProcessor.processTickets();
        flushAndClear();

        ticket = premodTicketService.loadPremodTicket(ticket.getId());
        assertEquals(PremodTicketStatus.IN_PROGRESS, ticket.getStatus());
    }

    private ShopInfo initShopInfo() {
        ShopInfo shopInfo = new ShopInfo();
        shopInfo.setCpa(ProgramState.OFF);
        shopInfo.setCpc(ProgramState.OFF);
        shopInfo.setGlobal(false);
        return shopInfo;
    }
}
