package ru.yandex.market.loyalty.core.dao.coin;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.TableFormat;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static ru.yandex.market.loyalty.core.dao.coin.GeneratorType.YANDEX_WALLET;

public class BunchGenerationRequestDaoTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private BunchGenerationRequestDao bunchGenerationRequestDao;
    @Autowired
    private PromoManager promoManager;

    @Test
    public void shouldQueryAllRequests() {
        final Promo accrualPromo = promoManager.createAccrualPromo(
                PromoUtils.WalletAccrual.defaultModelAccrual()
        );
        final BunchGenerationRequest scheduled = BunchGenerationRequest.scheduled(
                accrualPromo.getPromoId().getId(), "test", 1, null, TableFormat.YT,
                "source", YANDEX_WALLET
        );
        bunchGenerationRequestDao.save(scheduled);
        final List<BunchGenerationRequest> all = bunchGenerationRequestDao.findAll();
        assertThat(all, contains(samePropertyValuesAs(scheduled,
                "id", "inputFile", "inputFileCluster", "strictSchema", "paramsContainer")));
    }

    @Test
    public void shouldQueryScheduledRequestsOrderedByModificationTime() {
        var promo = promoManager.createAccrualPromo(PromoUtils.WalletAccrual.defaultModelAccrual());

        var firstScheduled = BunchGenerationRequest.scheduled(
                promo.getPromoId().getId(), "test1", 1, null, TableFormat.YT,
                "source1", YANDEX_WALLET
        );
        var secondScheduled = BunchGenerationRequest.scheduled(
                promo.getPromoId().getId(), "test2", 1, null, TableFormat.YT,
                "source2", YANDEX_WALLET
        );

        bunchGenerationRequestDao.save(firstScheduled);
        bunchGenerationRequestDao.save(secondScheduled);

        var firstId = bunchGenerationRequestDao.getRequest(firstScheduled.getKey()).get().getId();
        var secondId = bunchGenerationRequestDao.getRequest(secondScheduled.getKey()).get().getId();

        bunchGenerationRequestDao.incrementRequestTryCount(firstId, "Some message");

        var allScheduledOrdered = bunchGenerationRequestDao.getScheduledRequests(YANDEX_WALLET, 10);

        assertThat(allScheduledOrdered, hasSize(2));
        assertThat(allScheduledOrdered, containsInAnyOrder(
                hasProperty("id", equalTo(secondId)),
                hasProperty("id", equalTo(firstId))
        ));
    }
}
