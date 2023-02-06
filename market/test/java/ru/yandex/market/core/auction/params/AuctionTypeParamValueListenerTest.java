package ru.yandex.market.core.auction.params;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.listener.AuctionTypeParamValueListener;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.StringParamValue;
import ru.yandex.market.core.partner.PartnerService;

/**
 * Тесты для {@link AuctionTypeParamValueListener}.
 */
@DbUnitDataSet(before = "db/AuctionTypeParamValueListenerTest.before.csv")
class AuctionTypeParamValueListenerTest extends FunctionalTest {

    private static final long SHOP_ID_774 = 774L;
    private static final StringParamValue IGNORED = null;
    private static final StringParamValue AUCTION_TYPE_YML
            = new StringParamValue(ParamType.AUCTION_TYPE, SHOP_ID_774, "YML");
    private static final StringParamValue AUCTION_TYPE_WEB
            = new StringParamValue(ParamType.AUCTION_TYPE, SHOP_ID_774, "WEB");
    private static final StringParamValue AUCTION_TYPE_DISABLED
            = new StringParamValue(ParamType.AUCTION_TYPE, SHOP_ID_774, "DISABLED");
    private static final long SOME_ACTION_ID = 890L;
    private AuctionTypeParamValueListener listener;
    @Autowired
    private ParamService paramService;
    @Autowired
    private PartnerService partnerService;

    @BeforeEach
    void beforeEach() {
        listener = new AuctionTypeParamValueListener(paramService, partnerService);
    }

    @Test
    @DbUnitDataSet(after = "db/AuctionTypeParamValueListenerTest.empty.csv")
    void test_create_web() {
        listener.onCreate(AUCTION_TYPE_WEB, SOME_ACTION_ID);
    }

    @Test
    @DbUnitDataSet(after = "db/AuctionTypeParamValueListenerTest.empty.csv")
    void test_create_disabled() {
        listener.onCreate(AUCTION_TYPE_DISABLED, SOME_ACTION_ID);
    }

    @Test
    @DbUnitDataSet(after = "db/AuctionTypeParamValueListenerTest.id_type.csv")
    void test_create_yml() {
        listener.onCreate(AUCTION_TYPE_YML, SOME_ACTION_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "db/AuctionTypeParamValueListenerTest.id_type.csv",
            after = "db/AuctionTypeParamValueListenerTest.id_type.csv"
    )
    void test_create_yml_when_existed() {
        listener.onCreate(AUCTION_TYPE_YML, SOME_ACTION_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "db/AuctionTypeParamValueListenerTest.null_auction_type.csv",
            after = "db/AuctionTypeParamValueListenerTest.id_type.csv"
    )
    void test_create_yml_when_null() {
        listener.onCreate(AUCTION_TYPE_YML, SOME_ACTION_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "db/AuctionTypeParamValueListenerTest.id_type.csv",
            after = "db/AuctionTypeParamValueListenerTest.id_type.csv"
    )
    void test_change_yml_when_existed() {
        listener.onChange(AUCTION_TYPE_YML, IGNORED, SOME_ACTION_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "db/AuctionTypeParamValueListenerTest.id_type.csv",
            after = "db/AuctionTypeParamValueListenerTest.id_type.csv"
    )
    void test_change_web_when_existed() {
        listener.onChange(AUCTION_TYPE_WEB, IGNORED, SOME_ACTION_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "db/AuctionTypeParamValueListenerTest.id_type.csv",
            after = "db/AuctionTypeParamValueListenerTest.id_type.csv"
    )
    void test_change_disabled_when_existed() {
        listener.onChange(AUCTION_TYPE_DISABLED, IGNORED, SOME_ACTION_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "db/AuctionTypeParamValueListenerTest.null_auction_type.csv",
            after = "db/AuctionTypeParamValueListenerTest.id_type.csv"
    )
    void test_change_yml_when_null() {
        listener.onChange(AUCTION_TYPE_YML, IGNORED, SOME_ACTION_ID);
    }

}
