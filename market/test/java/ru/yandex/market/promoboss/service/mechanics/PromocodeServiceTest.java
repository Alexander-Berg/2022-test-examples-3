package ru.yandex.market.promoboss.service.mechanics;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.promoboss.dao.mechanics.PromocodeDao;
import ru.yandex.market.promoboss.model.PromoMechanicsParams;
import ru.yandex.market.promoboss.model.mechanics.Promocode;
import ru.yandex.market.promoboss.model.mechanics.PromocodeType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(classes = {PromocodeService.class})
public class PromocodeServiceTest {
    private static final Long PROMO_ID = 1000L;

    private static final Promocode PROMOCODE = Promocode.builder()
            .codeType(PromocodeType.FIXED_DISCOUNT)
            .value(1)
            .code("code")
            .minCartPrice(1L)
            .maxCartPrice(2L)
            .applyMultipleTimes(true)
            .budget(123L)
            .additionalConditions("additionalConditions")
            .build();

    @Autowired
    private PromocodeService promocodeService;

    @MockBean
    private PromocodeDao promocodeDao;

    @Test
    void getPromocodeByPromoId_exists() {
        // setup
        when(promocodeDao.getPromocodeByPromoId(PROMO_ID)).thenReturn(Optional.of(PROMOCODE));

        // act
        PromoMechanicsParams.PromoMechanicsParamsBuilder builder = PromoMechanicsParams.builder();
        promocodeService.populatePromo(PROMO_ID, builder);
        PromoMechanicsParams mechanicsParams = builder.build();

        // verify
        assertEquals(PROMOCODE, mechanicsParams.getPromocode());
    }

    @Test
    void getPromocodeByPromoId_notExists() {
        // setup
        when(promocodeDao.getPromocodeByPromoId(PROMO_ID)).thenReturn(Optional.empty());

        // act
        PromoMechanicsParams.PromoMechanicsParamsBuilder builder = PromoMechanicsParams.builder();
        promocodeService.populatePromo(PROMO_ID, builder);
        PromoMechanicsParams mechanicsParams = builder.build();

        // verify
        assertNull(mechanicsParams.getCheapestAsGift());
    }

    @Test
    void updatePromocodeByPromoId_insert() {
        // setup
        PromoMechanicsParams mechanicsParams = PromoMechanicsParams.builder().promocode(PROMOCODE).build();

        when(promocodeDao.getPromocodeByPromoId(PROMO_ID)).thenReturn(Optional.empty());

        // act
        promocodeService.updatePromo(PROMO_ID, mechanicsParams);

        // verify
        verify(promocodeDao).getPromocodeByPromoId(PROMO_ID);
        verify(promocodeDao).insertPromocode(PROMO_ID, PROMOCODE);
        verifyNoMoreInteractions(promocodeDao);
    }

    @Test
    void updatePromocodeByPromoId_update() {
        // setup
        PromoMechanicsParams mechanicsParams = PromoMechanicsParams.builder().promocode(PROMOCODE).build();

        when(promocodeDao.getPromocodeByPromoId(PROMO_ID)).thenReturn(Optional.of(PROMOCODE));

        // act
        promocodeService.updatePromo(PROMO_ID, mechanicsParams);

        // verify
        verify(promocodeDao).getPromocodeByPromoId(PROMO_ID);
        verify(promocodeDao).updatePromocode(PROMO_ID, PROMOCODE);
        verifyNoMoreInteractions(promocodeDao);
    }

    @Test
    void updatePromocodeByPromoId_delete() {
        // setup
        PromoMechanicsParams mechanicsParams = PromoMechanicsParams.builder().promocode(null).build();

        when(promocodeDao.getPromocodeByPromoId(PROMO_ID)).thenReturn(Optional.of(PROMOCODE));

        // act
        promocodeService.updatePromo(PROMO_ID, mechanicsParams);

        // verify
        verify(promocodeDao).deletePromocode(PROMO_ID);
        verifyNoMoreInteractions(promocodeDao);
    }
}
