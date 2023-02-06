package ru.yandex.market.abo.core.problem;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.hiding.util.model.CheckStatus;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.problem.model.CloneProblemCheck;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author artemmz
 * @date 13/06/19.
 */
class CloneProblemServiceTest extends EmptyTest {
    @Autowired
    private CloneProblemService cloneProblemService;

    @Test
    void testDAO() {
        assertTrue(cloneProblemService.getCloneProblemChecks(CheckStatus.NEW, false).isEmpty());
        List<CloneProblemCheck> checks = List.of(someCheck());
        cloneProblemService.storeCloneProblemChecks(checks);
        assertEquals(checks, cloneProblemService.getCloneProblemChecks(CheckStatus.NEW, false));
    }

    private static CloneProblemCheck someCheck() {
        Offer offer = new Offer();
        offer.setWareMd5(String.valueOf(RND.nextLong()));
        offer.setShopId(RND.nextLong());
        offer.setName("offerName");
        offer.setFeedId(RND.nextLong());
        offer.setShopOfferId(String.valueOf(RND.nextLong()));
        CloneProblemCheck check = new CloneProblemCheck(offer);
        check.setProblemId(RND.nextLong());
        return check;
    }
}