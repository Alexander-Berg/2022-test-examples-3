package ru.yandex.market.abo.shoppinger.generator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.pinger.model.MpGeneratorType;
import ru.yandex.market.abo.shoppinger.generator.util.ShopForCheck;
import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;


/**
 * @author agavrikov
 * @date 23.06.2019
 */
public class ProblemPriceRepingGeneratorTest extends EmptyTest {

    private static final int OFFER_LIMIT = 4;
    private static final long SHOP_ID = 774L;

    @Autowired
    @InjectMocks
    private ProblemPriceRepingGenerator problemPriceRepingGenerator;
    @Mock
    private OfferService offerService;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        doReturn(LongStream.range(1, OFFER_LIMIT + 1).mapToObj(ProblemPriceRepingGeneratorTest::offer)
                .collect(Collectors.toList()))
                .when(offerService).findWithParams(any());
    }

    @Test
    public void testAddNewTasks() {
        //проверяем, что если нет core_problem, то нет загруженных магазинов и нет tasks в net_check
        List<ShopForCheck> shops = problemPriceRepingGenerator.loadShops();
        assertEquals(0, shops.size());
        problemPriceRepingGenerator.createTasks(shops, OFFER_LIMIT);
        assertNetCheck(0);
    }

    private void assertNetCheck(int count) {
        assertEquals(count,
                pgJdbcTemplate.queryForList(
                        "SELECT count(*) FROM pinger_content_task " +
                                " WHERE gen_id = " + MpGeneratorType.PROBLEM_PRICE_REPING.getId(), Integer.class)
                        .stream().findFirst().orElse(0).intValue()
        );
    }

    private void addProblem(long sourceId) {
//        createProblem(SHOP_ID, GenId.PRICE_PINGER_GEN, ProblemTypeId.PINGER_PRICE, ProblemStatus.APPROVED,
//                tagService.createTag(FakeUsers.PRICE_CONTENT_PINGER.getId()), sourceId);
    }

    private static Offer offer(long id) {
        Offer res = new Offer();
        res.setId(id);
        res.setDirectUrl("direct-url");
        res.setPrice(BigDecimal.valueOf(1001));
        res.setShopPrice(BigDecimal.valueOf(1001));
        res.setClassifierMagicId("cmId");
        res.setWareMd5("wareMd5");
        return res;
    }
}
