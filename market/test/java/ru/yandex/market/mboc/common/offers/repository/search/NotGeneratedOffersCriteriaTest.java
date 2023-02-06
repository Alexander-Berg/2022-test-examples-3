package ru.yandex.market.mboc.common.offers.repository.search;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class NotGeneratedOffersCriteriaTest extends BaseDbTestClass {

    private static final String ROBOT = NotGeneratedOffersCriteria.AUTOGEN_USERS.get(0);
    private static final String HUMAN = "Скайримов Довакин Вадимович";

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    private OffersFilter filter = new OffersFilter().addCriteria(OfferCriterias.changedByHumanCriteria());

    @Before
    public void setup() {
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
    }

    /**
     * Проверим, что критерий поможет найти все оффера, тронутые человеком. Построим табличку со всеми возможными
     * вариантами "авторства" оффера. R - робот, H - человек.
     * <p>
     * created_by_login | modified_by_login | Берём?
     * -----------------+-------------------+-------
     * null       |       null        |  Да
     * R         |       null        |  Нет
     * R         |        R          |  Нет
     * R         |        H          |  Да
     * H         |       null        |  Да
     * H         |        H          |  Да
     * H         |        R          |  Нет
     * -----------------+-------------------+-------
     * <p>
     * Сгенерим восемь офферов по такой схеме. Найтись, соответственно,
     * должны будут оффера 1, 4, 5, 6 (считая с единицы).
     */
    @Test
    public void testCriterionWorksForHumanProcessedOffers() {
        Offer nn = offer(null, null);
        Offer rn = offer(ROBOT, null);
        Offer rr = offer(ROBOT, ROBOT);
        Offer rh = offer(ROBOT, HUMAN);
        Offer hn = offer(HUMAN, null);
        Offer hh = offer(HUMAN, HUMAN);
        Offer hr = offer(HUMAN, ROBOT);
        offerRepository.insertOffers(nn, rn, rr, rh, hn, hh, hr);
        List<Offer> found = offerRepository.findOffers(filter);
        assertThat(found).containsExactlyInAnyOrder(nn, rh, hn, hh);
    }

    private Offer offer(String created, String modified) {
        return OfferTestUtils.nextOffer().setCreatedByLogin(created).setModifiedByLogin(modified);
    }
}
