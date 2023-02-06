package ru.yandex.market.abo.core.hiding.rules.common;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

public class OfferHidingRulesAbstractTest extends EmptyTest {
    protected static final long MODEL_ID = 35;
    protected static final long SHOP_ID = 1L;
    protected static final long ANOTHER_SHOP_ID = 2L;
    protected static final long MODEL_ID_1 = 1L;
    protected static final long MODEL_ID_2 = 2L;
    protected static final long MODEL_ID_3 = 3L;
    protected static final long MODEL_ID_4 = 4L;
    protected static final String OFFER_1 = "/offerX.html";
    protected static final String OFFER_2 = "/offerY.html";
    protected static final OfferHidingRuleAction ACTION_1 = new OfferHidingRuleAction(99L, "Some comment", Instant.now());
    protected static final OfferHidingRuleAction ACTION_2 = new OfferHidingRuleAction(88L, "Another comment", Instant.now());
    protected static final long VENDOR_ID_1 = 111L;
    protected static final long VENDOR_ID_2 = 222L;
    protected static final String WORD_1 = "badword";
    protected static final String WORD_2 = "uglyword";


    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected OfferHidingRulesService service;

    @BeforeEach
    public void clearDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE offer_hiding_rule_action CASCADE");
    }

    @Test
    public void dummyTest() {

    }

}
