package ru.yandex.market.abo.cpa.order.service;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.rating.operational.OperationalRating;
import ru.yandex.market.abo.core.rating.operational.OperationalRatingRepo;
import ru.yandex.market.abo.cpa.order.model.OrderOperation;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author artemmz
 * @date 20/08/19.
 */
class OperationalRatingQueriesTest extends EmptyTest {
    @Autowired
    private OrderOperationRepo orderOperationRepo;

    @Autowired
    private OperationalRatingRepo operationalRatingRepo;

    @Test
    void testFindByPeriodAndPartnerModel() {
        LocalDate now = LocalDate.now();
        List<OrderOperation> operationStat = orderOperationRepo.findByPeriod(
                now.minusDays(90), now
        );
        assertNotNull(operationStat);
    }

    @Test
    void testFindPreviousOperationRatingAndPartnerModel() {
        List<OperationalRating> operationalRatings = operationalRatingRepo.findPreviousOperationRating(
                List.of(1L)
        );
        assertNotNull(operationalRatings);
    }
}
