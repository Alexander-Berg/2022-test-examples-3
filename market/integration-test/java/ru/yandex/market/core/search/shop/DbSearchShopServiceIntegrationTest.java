package ru.yandex.market.core.search.shop;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.config.DevJdbcConfig;
import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.core.contact.db.BusinessOwnerService;
import ru.yandex.market.core.search.shop.model.SearchShopFilter;
import ru.yandex.market.core.search.shop.model.SearchShopInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест для {@link DbSearchShopService} проверят кейсы поиска магазина по критериям.
 * <p>Db unit тестом покрыть не получается, так как он h2 генерит огромное кол-во мусора при
 * построении плана, что приводит к OEM.
 *
 * @author stani on 13.04.18.
 */
@SpringJUnitConfig(DbSearchShopServiceIntegrationTest.Config.class)
class DbSearchShopServiceIntegrationTest {

    private static final long NO_MANAGER = 0L;
    private static final int ORDER = 1;
    private static final int PAGE_FROM = 0;
    private static final int PAGE_TO = 10;
    private static final long DATASOURCE_ID = 100000774L;
    private static final String DATASOURCE_NAME = "DbSearchShopServiceIntegrationTestShop";
    private static final long CAMPAIGN_ID = 200000774L;
    private static final long SUPPLIER_ID = 1000000001L;
    private static final String SUPPLIER_NAME = "DbSearchShopServiceIntegrationTestSupplier";
    private static final long SUPPLIER_CAMPAIGN_ID = 2000000001L;
    private static final long FMCG_DATASOURCE_ID = 100000884L;
    private static final String FMCG_DATASOURCE_NAME = "DbSearchShopServiceIntegrationTestFmcg";
    private static final long FMCG_CAMPAIGN_ID = 200000884L;
    private static final Collection<Long> ONE_CAMPAIGN_ONLY = Collections.singletonList(CAMPAIGN_ID);
    private static final long BUSINESS_ID = 1600691846;
    private static final String BUSINESS_NAME = "test_bussines";
    private static final long BUSINESS_OWNER_UID = 10000000042L;
    private static final long CONTACT_ID = 12300000042L;
    private static final long CONTACT_LINK_ID = 22300000042L;
    private static final long CONTACT_ROLE_ID = 32300000042L;

    @Autowired
    SearchShopService searchShopService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        String datasources = Stream.of(DATASOURCE_ID, FMCG_DATASOURCE_ID)
                .map(String::valueOf)
                .collect(Collectors.joining(",", "(", ")"));
        String campaigns = Stream.of(CAMPAIGN_ID, FMCG_CAMPAIGN_ID, SUPPLIER_CAMPAIGN_ID)
                .map(String::valueOf)
                .collect(Collectors.joining(",", "(", ")"));
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                jdbcTemplate.update("delete from shops_web.CONTACT_ROLE where id = " + CONTACT_ROLE_ID);
                jdbcTemplate.update("delete from shops_web.CONTACT_LINK where id = " + CONTACT_LINK_ID);
                jdbcTemplate.update("delete from shops_web.CONTACT where id = " + CONTACT_ID);
                jdbcTemplate.update("delete from shops_web.BUSINESS_SERVICE where business_id = " + BUSINESS_ID);
                jdbcTemplate.update("delete from shops_web.BUSINESS where id = " + BUSINESS_ID);
                jdbcTemplate.update(
                        "delete from shops_web.partner_placement_program where partner_id = " + BUSINESS_ID);
                jdbcTemplate.update("delete from shops_web.PARTNER where id = " + BUSINESS_ID);

                jdbcTemplate.update("delete from shops_web.contact_link where campaign_id in " + campaigns);
                jdbcTemplate.update("delete from market_billing.campaign_info where campaign_id in " + campaigns);
                jdbcTemplate.update("delete from shops_web.open_cutoff where datasource_Id in " + datasources);
                jdbcTemplate.update("delete from shops_web.disabled_periods where datasource_Id in " + datasources);
                jdbcTemplate.update("delete from shops_web.manager_history where datasource_Id in " + datasources);
                jdbcTemplate.update("delete from shops_web.cpa_open_cutoff where datasource_Id in " + datasources);
                jdbcTemplate.update("delete from shops_web.datasources_in_testing where datasource_Id in "
                        + datasources);
                jdbcTemplate.update("delete from shops_web.DATASOURCE where id in " + datasources);
                jdbcTemplate.update(
                        "delete from shops_web.partner_placement_program where partner_id in " + datasources);
                jdbcTemplate.update("delete from shops_web.PARTNER where id in " + datasources);
                jdbcTemplate.update("delete from MARKET_BILLING.CAMPAIGN_BALANCE_ACTUAL where campaign_id in "
                        + campaigns);
                jdbcTemplate.update("delete from MARKET_BILLING.CAMPAIGN_BALANCE_PAID where campaign_id in "
                        + campaigns);
                jdbcTemplate.update("delete from MARKET_BILLING.CAMPAIGN_BALANCE_SHIPPED where campaign_id in "
                        + campaigns);
                jdbcTemplate.update("delete from MARKET_BILLING.CAMPAIGN_BALANCE_SPENT where campaign_id in "
                        + campaigns);
                jdbcTemplate.update("delete from shops_web.SUPPLIER where id = " + SUPPLIER_ID);
                jdbcTemplate.update(
                        "delete from shops_web.partner_placement_program where partner_id = " + SUPPLIER_ID);
                jdbcTemplate.update("delete from shops_web.PARTNER where id = " + SUPPLIER_ID);

                jdbcTemplate.update("insert into shops_web.PARTNER (ID, TYPE) values (" + SUPPLIER_ID +
                        ", 'SUPPLIER" +
                        "')");
                jdbcTemplate.update("insert into shops_web.PARTNER (ID, TYPE) values (" + DATASOURCE_ID
                        + ", 'SHOP')");
                jdbcTemplate.update("insert into shops_web.PARTNER (ID, TYPE) values (" + FMCG_DATASOURCE_ID
                        + ", " +
                        "'FMCG')");
                jdbcTemplate.update("insert into shops_web.PARTNER (ID, TYPE) values (" + BUSINESS_ID
                        + ", 'BUSINESS" +
                        "')");
                jdbcTemplate.update("insert into shops_web.DATASOURCE (ID,NAME,MANAGER_ID,COMMENTS) values ("
                        + DATASOURCE_ID + ", '" + DATASOURCE_NAME
                        + "', -2, 'Db Search Shop Service Integration Test Comment')");
                jdbcTemplate.update("insert into shops_web.DATASOURCE (ID,NAME,MANAGER_ID,COMMENTS) values ("
                        + FMCG_DATASOURCE_ID + ", '" + FMCG_DATASOURCE_NAME
                        + "', -2, 'Db Search Fmcg Service Integration Test Comment')");
                jdbcTemplate.update("insert into market_billing.campaign_info (CAMPAIGN_ID,DATASOURCE_ID," +
                        "BILLING_TYPE,START_DATE,CLIENT_ID) values (" + CAMPAIGN_ID + ", " + DATASOURCE_ID + ", 1, " +
                        "sysdate-1, " + SUPPLIER_ID + ")");
                jdbcTemplate.update("insert into market_billing.campaign_info (CAMPAIGN_ID,DATASOURCE_ID," +
                        "BILLING_TYPE,START_DATE,CLIENT_ID) values (" + FMCG_CAMPAIGN_ID + ", " + FMCG_DATASOURCE_ID
                        + ", 1, sysdate-1, " + SUPPLIER_ID + ")");
                jdbcTemplate.update("insert into market_billing.campaign_info (CAMPAIGN_ID,DATASOURCE_ID," +
                        "BILLING_TYPE,START_DATE,CLIENT_ID) values (" + SUPPLIER_CAMPAIGN_ID + ", " + SUPPLIER_ID +
                        ", 1, " + "sysdate-1, " + SUPPLIER_ID + ")");
                jdbcTemplate.update("insert into MARKET_BILLING.CAMPAIGN_BALANCE_ACTUAL (" +
                        "CAMPAIGN_ID,ACTUAL_BALANCE," +
                        "BLOCKED_BALANCE) values (" + CAMPAIGN_ID + ",100,50)");
                jdbcTemplate.update("insert into MARKET_BILLING.CAMPAIGN_BALANCE_ACTUAL (" +
                        "CAMPAIGN_ID,ACTUAL_BALANCE," +
                        "BLOCKED_BALANCE) values (" + FMCG_CAMPAIGN_ID + ",100,50)");
                jdbcTemplate.update("insert into MARKET_BILLING.CAMPAIGN_BALANCE_PAID (" +
                        "CAMPAIGN_ID,SUM_PAID_EXACT) " +
                        "values (" + CAMPAIGN_ID + ",0)");
                jdbcTemplate.update("insert into MARKET_BILLING.CAMPAIGN_BALANCE_PAID (C" +
                        "AMPAIGN_ID,SUM_PAID_EXACT) " +
                        "values (" + FMCG_CAMPAIGN_ID + ",0)");
                jdbcTemplate.update("insert into MARKET_BILLING.CAMPAIGN_BALANCE_SHIPPED (CAMPAIGN_ID," +
                        "SUM_SPENT_SHIPPED) values (" + CAMPAIGN_ID + ",0)");
                jdbcTemplate.update("insert into MARKET_BILLING.CAMPAIGN_BALANCE_SHIPPED (CAMPAIGN_ID," +
                        "SUM_SPENT_SHIPPED) values (" + FMCG_CAMPAIGN_ID + ",0)");
                jdbcTemplate.update("insert into MARKET_BILLING.CAMPAIGN_BALANCE_SPENT (CAMPAIGN_ID,SUM_SPENT)" +
                        " values" +
                        " (" + CAMPAIGN_ID + ",0)");
                jdbcTemplate.update("insert into MARKET_BILLING.CAMPAIGN_BALANCE_SPENT (CAMPAIGN_ID,SUM_SPENT)" +
                        " values" +
                        " (" + FMCG_CAMPAIGN_ID + ",0)");
                jdbcTemplate.update("insert into SHOPS_WEB.SUPPLIER (ID,CAMPAIGN_ID,CREATED_AT,CLIENT_ID,NAME)" +
                        " values" +
                        " (" + SUPPLIER_ID + "," + SUPPLIER_CAMPAIGN_ID + ",sysdate-1,10000001,'" + SUPPLIER_NAME +
                        "')");
                jdbcTemplate.update("insert into SHOPS_WEB.BUSINESS (ID,NAME) values (" + BUSINESS_ID + ",'"
                        + BUSINESS_NAME + "')");
                jdbcTemplate.update("insert into SHOPS_WEB.BUSINESS_SERVICE (" +
                        "BUSINESS_ID,SERVICE_ID, SERVICE_TYPE) " +
                        "values (" + BUSINESS_ID + ",'" + DATASOURCE_ID + "', 'SHOP')");
                jdbcTemplate.update("insert into shops_web.CONTACT (ID, LOGIN) values (" + CONTACT_ID + ", " +
                        "'yaSearchTestUser" +
                        "')");
                jdbcTemplate.update("insert into shops_web.CONTACT_LINK (ID, CAMPAIGN_ID, CONTACT_ID) values " +
                        "(" + CONTACT_LINK_ID + ", " + CAMPAIGN_ID + ", " + CONTACT_ID + ")");
                jdbcTemplate.update("insert into shops_web.CONTACT_ROLE (ID, CONTACT_LINK_ID, ROLE_ID) values " +
                        "(" + CONTACT_ROLE_ID + ", " + CONTACT_LINK_ID + ", " + InnerRole.SHOP_ADMIN.getCode() + ")");
            }
        });
    }

    @Test
    void testNoCriteriaSearch() {
        Pair<Integer, Collection<SearchShopInfo>> searchResults = searchShopService.search(
                new SearchShopFilter(0, null, "", null, null,
                        Collections.emptySet()),
                1, 0, 10);
        assertTrue(searchResults.first >= 2);
        assertTrue(searchResults.second.size() >= 2);
    }

    @Test
    void testSearch() {

        //By query
        assertSearchResults(searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, DATASOURCE_NAME, null, null,
                        Collections.emptySet()), ORDER,
                PAGE_FROM, PAGE_TO), 1,
                ONE_CAMPAIGN_ONLY);
        //By shop_id or supplier_id
        assertSearchResults(searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, "100000774", null, null,
                        Collections.emptySet()), ORDER,
                PAGE_FROM, PAGE_TO), 1,
                ONE_CAMPAIGN_ONLY);
        //By shop_id or supplier_id with market service prefix 11-
        assertSearchResults(searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, "11-200000774", null, null,
                        Collections.emptySet()), ORDER,
                PAGE_FROM, PAGE_TO), 1,
                ONE_CAMPAIGN_ONLY);
        //By shop_id or supplier_id
        assertSearchResults(searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, "1000000001", null, null,
                        Collections.emptySet()), ORDER,
                PAGE_FROM, PAGE_TO), 1,
                Collections.singletonList(SUPPLIER_CAMPAIGN_ID));
        //By campaign_id
        assertSearchResults(searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, "2000000001", null, null,
                        Collections.emptySet()), ORDER,
                PAGE_FROM, PAGE_TO), 1,
                Collections.singletonList(SUPPLIER_CAMPAIGN_ID));
        //By shop query match shops and suppliers
        assertSearchResults(searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, "DbSearchShopServiceIntegrationTest",
                        null, null,
                        Collections.emptySet()), ORDER, PAGE_FROM, PAGE_TO), 3,
                Arrays.asList(CAMPAIGN_ID, SUPPLIER_CAMPAIGN_ID, FMCG_CAMPAIGN_ID));
        //By shop query and manager
        assertSearchResults(searchShopService.search(
                new SearchShopFilter(-2, null, "SearchShopService", null,
                        null, Collections.emptySet()), ORDER,
                PAGE_FROM, PAGE_TO), 2,
                Arrays.asList(CAMPAIGN_ID, FMCG_CAMPAIGN_ID));
        //By comment
        assertSearchResults(searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, "",
                        "Db Search Shop Service Integration Test Comment", null,
                        Collections.emptySet()), ORDER, PAGE_FROM, PAGE_TO), 1,
                ONE_CAMPAIGN_ONLY);
        //By business_id
        assertSearchResults(searchShopService.search(
                new SearchShopFilter(NO_MANAGER, "bus", DATASOURCE_NAME, null, null,
                        Collections.emptySet()), ORDER,
                PAGE_FROM, PAGE_TO), 1,
                ONE_CAMPAIGN_ONLY);
        //By user name
        assertSearchResults(searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, null, null, "yaSearchTestU",
                        Collections.emptySet()), ORDER,
                PAGE_FROM, PAGE_TO), 1,
                ONE_CAMPAIGN_ONLY);
        assertEquals(0, searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, "Мар", null, "yaSearchTestU",
                        Collections.emptySet()), ORDER,
                PAGE_FROM, PAGE_TO).first);
        assertEquals(0, searchShopService.search(
                new SearchShopFilter(NO_MANAGER, "not_bus", DATASOURCE_NAME, null, null,
                        Collections.emptySet()),
                ORDER, PAGE_FROM, PAGE_TO).first);
        assertSearchResults(searchShopService.search(
                new SearchShopFilter(NO_MANAGER, String.valueOf(BUSINESS_ID), DATASOURCE_NAME, null, null,
                        Collections.emptySet()), ORDER, PAGE_FROM, PAGE_TO), 1,
                ONE_CAMPAIGN_ONLY);
        assertEquals(0, searchShopService.search(
                new SearchShopFilter(NO_MANAGER, String.valueOf(BUSINESS_ID + 1), DATASOURCE_NAME, null,
                        null,
                        Collections.emptySet()), ORDER, PAGE_FROM, PAGE_TO).first);
    }

    @Test
    void testShopNameSuffix() {
        Pair<Integer, Collection<SearchShopInfo>> result = searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null,
                        "DbSearchShopServiceIntegrationTest", null, null,
                        Collections.emptySet()), ORDER, PAGE_FROM,
                PAGE_TO);
        assertThat(result.second.stream().map(SearchShopInfo::getId).collect(Collectors.toSet()),
                hasItems(DATASOURCE_ID, FMCG_DATASOURCE_ID, SUPPLIER_ID));

        Map<Long, String> shopNames = new HashMap<>();
        shopNames.put(DATASOURCE_ID, BUSINESS_NAME);
        shopNames.put(FMCG_DATASOURCE_ID, FMCG_DATASOURCE_NAME + " в \"Суперчек\"");
        shopNames.put(SUPPLIER_ID, SUPPLIER_NAME + " на маркетплейсе");

        result.second.forEach(
                searchShopInfo -> assertThat(searchShopInfo.getShopName(), is(shopNames.get(searchShopInfo.getId()))));
    }

    @Test
    void testPager() {
        //page less than all results
        Pair<Integer, Collection<SearchShopInfo>> searchResults = searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, "test", null, null,
                        Collections.emptySet()), ORDER, 0, 1);
        assertTrue(searchResults.first >= 2);
        assertThat(searchResults.second, hasSize(1));

    }

    @Test
    void testCampaignTypeSupplier() {
        Pair<Integer, Collection<SearchShopInfo>> searchResults = searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, "test", null, null,
                        Set.of(CampaignType.SUPPLIER)), ORDER, 0, 1);
        assertTrue(searchResults.first >= 2);
        assertThat(searchResults.second, hasSize(1));
    }

    @Test
    void testCampaignTypeList() {
        Pair<Integer, Collection<SearchShopInfo>> searchResultsWithType = searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, "test", null, null,
                        Set.of(CampaignType.values())), ORDER, 0, 1);
        assertTrue(searchResultsWithType.first >= 2);
        assertThat(searchResultsWithType.second, hasSize(1));
    }

    @Test
    void testBusinessOwner() {
        Pair<Integer, Collection<SearchShopInfo>> searchResults = searchShopService.search(
                new SearchShopFilter(NO_MANAGER, null, "100000774", null, null,
                        Collections.emptySet()),
                ORDER, PAGE_FROM, PAGE_TO
        );
        assertEquals(1, searchResults.first);
        assertTrue(searchResults.second.stream()
                .allMatch(ssi -> ssi.getSuperAdminUid().equals(BUSINESS_OWNER_UID))
        );
    }

    private void assertSearchResults(Pair<Integer, Collection<SearchShopInfo>> searchResults, int expectedAllCount,
                                     Collection<Long> expectedcampaignIds) {
        assertEquals(expectedAllCount, searchResults.first.intValue());
        Collection<SearchShopInfo> searchShops = searchResults.second;
        assertThat(searchShops.stream()
                        .map(SearchShopInfo::getCampaignId)
                        .collect(Collectors.toList()),
                containsInAnyOrder(expectedcampaignIds.toArray()));
    }

    @Configuration
    @Import(DevJdbcConfig.class)
    static class Config {
        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Bean
        public SearchShopService searchShopService() {
            BusinessOwnerService businessOwnerService = Mockito.mock(BusinessOwnerService.class);
            Mockito.when(businessOwnerService.getPartnerIdToOwnerUid(ArgumentMatchers.anyCollection()))
                    .thenReturn(Map.of(DATASOURCE_ID, BUSINESS_OWNER_UID,
                            SUPPLIER_ID, BUSINESS_OWNER_UID,
                            FMCG_DATASOURCE_ID, BUSINESS_OWNER_UID
                            )
                    );

            return new DbSearchShopService(jdbcTemplate, businessOwnerService);
        }
    }
}
