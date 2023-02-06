package ru.yandex.market.promoboss.dao;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.model.GenerateableUrl;
import ru.yandex.market.promoboss.model.MechanicsType;
import ru.yandex.market.promoboss.model.Promo;
import ru.yandex.market.promoboss.model.PromoMainParams;
import ru.yandex.market.promoboss.model.SourceType;
import ru.yandex.market.promoboss.model.Status;
import ru.yandex.market.promoboss.service.TimeService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = PromoDao.class)
@MockBean(classes = TimeService.class)
public class PromoDaoTest extends AbstractDaoTest {
    protected Promo promo;
    private Promo updatedPromo;
    protected static final Long CREATE_AT = 123L;
    protected static final String PROMO_ID = "cf_123";

    @Autowired
    protected TimeService timeService;

    @Autowired
    protected PromoDao promoDao;

    @BeforeEach
    void buildUpdatePromo() {
        promo = Promo.builder()
                .promoId(PROMO_ID)
                .mainParams(
                        PromoMainParams.builder()
                                .promoKey("promo_key")
                                .parentPromoId("parent_promo_id")
                                .source(SourceType.CATEGORYIFACE)
                                .name("name")
                                .status(Status.NEW)
                                .active(true)
                                .hidden(false)
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url("https://landing.url")
                                                .auto(false)
                                                .build()
                                )
                                .rulesUrl(GenerateableUrl.builder()
                                        .url("https://rules.url")
                                        .auto(false)
                                        .build()
                                )
                                .startAt(1657630081L)
                                .endAt(1657633681L)
                                .createdAt(CREATE_AT)
                                .build()
                )
                .build();

        updatedPromo = Promo.builder()
                .promoId(PROMO_ID)
                .mainParams(
                        PromoMainParams.builder()
                                .promoKey(promo.getMainParams().getPromoKey())
                                .parentPromoId("new_parent_promo_id")
                                .source(SourceType.ANAPLAN)
                                .name("new_name")
                                .status(Status.CANCELED)
                                .active(false)
                                .hidden(true)
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url("https://new-landing.url")
                                                .auto(true)
                                                .build()
                                )
                                .rulesUrl(
                                        GenerateableUrl.builder()
                                                .url("https://new-rules.url")
                                                .auto(true)
                                                .build()
                                )
                                .startAt(1657635891L)
                                .endAt(1657639491L)
                                .createdAt(promo.getMainParams().getCreatedAt())
                                .build()
                )
                .build();
    }

    @Test
    @DbUnitDataSet(before = "PromoDaoTest.shouldNotReturnRecordIfNotExists.before.csv")
    public void shouldNotReturnRecordIfNotExists() {

        // act
        Optional<Promo> promoByPromoId = promoDao.getFullPromoByPromoId(PROMO_ID);

        // verify
        assertFalse(promoByPromoId.isPresent());
    }

    @Test
    @DbUnitDataSet(before = "PromoDaoTest.shouldReturnRecordByPromoId.before.csv")
    public void shouldReturnRecordByPromoId() {
        // act
        Optional<Promo> promoByPromoId = promoDao.getFullPromoByPromoId(PROMO_ID);

        // verify
        assertTrue(promoByPromoId.isPresent());
        assertEquals(promo, promoByPromoId.get());
    }

    @Test
    @DbUnitDataSet(
            after = "PromoDaoTest.shouldCreateNewPromo.after.csv"
    )
    public void shouldCreateNewPromo() {
        // setup
        when(timeService.getEpochSecond()).thenReturn(CREATE_AT);

        // act
        promoDao.insertPromo(promo.getPromoId(), promo.getMainParams());
    }

    @Test
    @DbUnitDataSet(
            after = "PromoDaoTest.shouldCreateNewPromoWithoutUrls.after.csv"
    )
    public void shouldCreateNewPromoWithoutUrls() {
        // setup
        when(timeService.getEpochSecond()).thenReturn(CREATE_AT);
        promo.getMainParams().setLandingUrl(null);
        promo.getMainParams().setRulesUrl(null);

        // act
        promoDao.insertPromo(promo.getPromoId(), promo.getMainParams());
    }

    @Test
    @DbUnitDataSet(
            before = "PromoDaoTest.shouldThrowExceptionDuringCreationIfRecordAlreadyExists.before.csv"
    )
    public void shouldThrowExceptionDuringCreationIfRecordAlreadyExists() {
        // act
        assertThrows(DuplicateKeyException.class, () -> promoDao.insertPromo(promo.getPromoId(), promo.getMainParams()));
    }

    @Test
    @DbUnitDataSet(
            before = "PromoDaoTest.shouldUpdateExistedPromo.before.csv",
            after = "PromoDaoTest.shouldUpdateExistedPromo.after.csv"
    )
    public void shouldUpdateExistedPromo() {
        // act
        promoDao.updatePromo(updatedPromo.getPromoId(), updatedPromo.getMainParams());
    }

    @Test
    @DbUnitDataSet(
            before = "PromoDaoTest.shouldUpdateExistedPromoWithoutUrls.before.csv",
            after = "PromoDaoTest.shouldUpdateExistedPromoWithoutUrls.after.csv"
    )
    public void shouldUpdateExistedPromoWithoutUrls() {

        // setup
        updatedPromo.getMainParams().setLandingUrl(null);
        updatedPromo.getMainParams().setRulesUrl(null);

        // act
        promoDao.updatePromo(updatedPromo.getPromoId(), updatedPromo.getMainParams());
    }

    @Test
    public void shouldThrowExceptionIfRecordDoesNotExist() {

        // act and verify
        assertThrows(IncorrectResultSizeDataAccessException.class, () -> promoDao.updatePromo(promo.getPromoId(), promo.getMainParams()));
    }
}
