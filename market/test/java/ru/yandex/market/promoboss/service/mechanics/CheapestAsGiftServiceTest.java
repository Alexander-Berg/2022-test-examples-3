package ru.yandex.market.promoboss.service.mechanics;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.promoboss.dao.mechanics.CheapestAsGiftDao;
import ru.yandex.market.promoboss.model.PromoMechanicsParams;
import ru.yandex.market.promoboss.model.mechanics.CheapestAsGift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(classes = {CheapestAsGiftService.class})
class CheapestAsGiftServiceTest {
    private static final Long PROMO_ID = 1000L;

    private static final CheapestAsGift CHEAPEST_AS_GIFT = CheapestAsGift.builder()
            .count(1)
            .build();

    @Autowired
    private CheapestAsGiftService cheapestAsGiftService;

    @MockBean
    private CheapestAsGiftDao cheapestAsGiftDao;

    @Test
    void getCheapestAsGiftByPromoId_exists() {
        // setup
        when(cheapestAsGiftDao.getCheapestAsGiftByPromoId(PROMO_ID)).thenReturn(Optional.of(CHEAPEST_AS_GIFT));

        // act
        PromoMechanicsParams.PromoMechanicsParamsBuilder builder = PromoMechanicsParams.builder();
        cheapestAsGiftService.populatePromo(PROMO_ID, builder);
        PromoMechanicsParams promoMechanicsParams = builder.build();

        // verify
        assertEquals(CHEAPEST_AS_GIFT, promoMechanicsParams.getCheapestAsGift());
    }

    @Test
    void getCheapestAsGiftByPromoId_notExists() {
        // setup
        when(cheapestAsGiftDao.getCheapestAsGiftByPromoId(PROMO_ID)).thenReturn(Optional.empty());

        // act
        PromoMechanicsParams.PromoMechanicsParamsBuilder builder = PromoMechanicsParams.builder();
        cheapestAsGiftService.populatePromo(PROMO_ID, builder);
        PromoMechanicsParams promoMechanicsParams = builder.build();

        // verify
        assertNull(promoMechanicsParams.getCheapestAsGift());
    }

    @Test
    void insertCheapestByPromoId_insert() {
        // setup
        PromoMechanicsParams mechanicsParams = PromoMechanicsParams.builder().cheapestAsGift(CHEAPEST_AS_GIFT).build();

        // act
        cheapestAsGiftService.insertPromo(PROMO_ID, mechanicsParams);

        // verify
        verify(cheapestAsGiftDao).insertCheapestAsGift(PROMO_ID, CHEAPEST_AS_GIFT.getCount());
        verifyNoMoreInteractions(cheapestAsGiftDao);
    }

    @Test
    void updateCheapestAsGiftByPromoId_insert() {
        // setup
        PromoMechanicsParams mechanicsParams = PromoMechanicsParams.builder().cheapestAsGift(CHEAPEST_AS_GIFT).build();

        when(cheapestAsGiftDao.getCheapestAsGiftByPromoId(PROMO_ID)).thenReturn(Optional.empty());

        // act
        cheapestAsGiftService.updatePromo(PROMO_ID, mechanicsParams);

        // verify
        verify(cheapestAsGiftDao).getCheapestAsGiftByPromoId(PROMO_ID);
        verify(cheapestAsGiftDao).insertCheapestAsGift(PROMO_ID, CHEAPEST_AS_GIFT.getCount());
        verifyNoMoreInteractions(cheapestAsGiftDao);
    }

    @Test
    void updateCheapestAsGiftByPromoId_update() {
        // setup
        PromoMechanicsParams mechanicsParams = PromoMechanicsParams.builder().cheapestAsGift(CHEAPEST_AS_GIFT).build();

        when(cheapestAsGiftDao.getCheapestAsGiftByPromoId(PROMO_ID)).thenReturn(Optional.of(CHEAPEST_AS_GIFT));

        // act
        cheapestAsGiftService.updatePromo(PROMO_ID, mechanicsParams);

        // verify
        verify(cheapestAsGiftDao).getCheapestAsGiftByPromoId(PROMO_ID);
        verify(cheapestAsGiftDao).updateCheapestAsGift(PROMO_ID, CHEAPEST_AS_GIFT.getCount());
        verifyNoMoreInteractions(cheapestAsGiftDao);
    }

    @Test
    void updateCheapestAsGiftByPromoId_delete() {
        // setup
        PromoMechanicsParams mechanicsParams = PromoMechanicsParams.builder().cheapestAsGift(null).build();

        when(cheapestAsGiftDao.getCheapestAsGiftByPromoId(PROMO_ID)).thenReturn(Optional.of(CHEAPEST_AS_GIFT));

        // act
        cheapestAsGiftService.updatePromo(PROMO_ID, mechanicsParams);

        // verify
        verify(cheapestAsGiftDao).deleteCheapestAsGift(PROMO_ID);
        verifyNoMoreInteractions(cheapestAsGiftDao);
    }
}
