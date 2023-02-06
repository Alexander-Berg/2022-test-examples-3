package ru.yandex.market.loyalty.admin.service.bunch.generator;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestService;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.TableFormat;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestShardDao;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.dao.custom.coin.BunchGenerationRequestDaoCustomImpl;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.api.model.CoinGeneratorType.AUTH;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.BUNCH_REQUEST_LAST_PROCESSED_UID;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.COIN_GENERATOR_TYPE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.ERROR_OUTPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.INPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.INPUT_TABLE_CLUSTER;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.OUTPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.OUTPUT_TABLE_CLUSTER;
import static ru.yandex.market.loyalty.core.dao.coin.CoinDao.DISCOUNT_TABLE;

/**
 * @author artemmz
 */
public abstract class BunchGeneratorTest extends MarketLoyaltyAdminMockedDbTest {
    static final String YT_INPUT_TABLE = "//input/input_table";

    @Autowired
    PromoManager promoManager;
    @Autowired
    BunchGenerationRequestDaoCustomImpl requestDaoCustom;
    @Autowired
    BunchRequestService bunchRequestService;
    @Autowired
    BunchGenerationRequestShardDao requestShardDao;
    @Autowired
    CoinDao coinDao;

    Pair<BunchGenerationRequest, Promo> createBunchRequest(String requestKey) {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(100500));
        BunchGenerationRequest request = createBunchRequest(promo, requestKey);

        return Pair.of(request, promo);
    }

    BunchGenerationRequest createBunchRequest(Promo promo, String requestKey) {
        long requestId = bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        requestKey, 100500, null, TableFormat.YT, "src", GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, AUTH.getCode())
                                .put(INPUT_TABLE, YT_INPUT_TABLE)
                                .put(INPUT_TABLE_CLUSTER, "hahn")
                                .put(OUTPUT_TABLE, "//output/output_table")
                                .put(OUTPUT_TABLE_CLUSTER, "hahn")
                                .put(ERROR_OUTPUT_TABLE, "//output/error_output_table")
                                .build()
                )
        );

        return bunchRequestService.getRequest(requestId);
    }

    void checkGeneration(BunchGenerationRequest request, List<Long> expectInsertedUids, String requestKey) {
        BunchGenerationRequest requestFromDb = bunchRequestService.getRequest(request.getId());
        assertEquals(expectInsertedUids.size(), (int) requestFromDb.getProcessedCount());
        int coinsCount = coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%" + requestKey + "%"));
        assertEquals(coinsCount, expectInsertedUids.size());
        assertEquals(expectInsertedUids.stream().max(Long::compareTo).orElseThrow(),
                requestFromDb.getParam(BUNCH_REQUEST_LAST_PROCESSED_UID).orElseThrow());
    }

}
