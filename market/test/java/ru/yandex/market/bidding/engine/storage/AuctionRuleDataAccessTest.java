package ru.yandex.market.bidding.engine.storage;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.bidding.engine.HeavyDataAccessOptions;
import ru.yandex.market.bidding.model.ResultField;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link AuctionRuleDataAccess}.
 */
@ExtendWith(MockitoExtension.class)
class AuctionRuleDataAccessTest {
    @Mock
    private JdbcTemplate jdbcTemplate;

    private AuctionRuleDataAccess dataAccess;

    @BeforeEach
    void beforeEach() {
        dataAccess = new AuctionRuleDataAccess(
                jdbcTemplate,
                mock(TransactionTemplate.class),
                mock(HeavyDataAccessOptions.class)
        );
    }

    @Test
    void testGetRuleResultInsertFullQueryClause() {
        assertEquals(
                "INSERT INTO shops_web.TMP_AUCTION_RULE " +
                        "(shop_id,domain_type,domain_id,feed_id,bid_type,bid_value,generation_id,result_code,other_id,modified_date) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?)",
                dataAccess.getRuleResultInsertFullQueryClause("TMP_AUCTION_RULE")
        );
    }

    @Test
    void testGetRuleResultInsertFullQueryClauseForModelBidding() {
        dataAccess.setDataAccess("", "", "", "", "", ResultField.MODEL_FILEDS);
        assertEquals(
                "INSERT INTO shops_web.TMP_AUCTION_RULE " +
                        "(domain_type,domain_id,bid_type,bid_value,generation_id,result_code,modified_date,vendor_id) " +
                        "VALUES (?,?,?,?,?,?,?,?)",
                dataAccess.getRuleResultInsertFullQueryClause("TMP_AUCTION_RULE")
        );
    }

    @Test
    void testExchangePartition() {
        dataAccess.exchangePartition(123, "TMP_TABLE");
        Mockito.verify(jdbcTemplate).execute("ALTER TABLE shops_web.AUCTION_RULE EXCHANGE PARTITION FOR(123) WITH TABLE shops_web.TMP_TABLE INCLUDING INDEXES");
        verifyNoMoreInteractions(jdbcTemplate);
    }
}