package ru.yandex.market.promoboss.service;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.promoboss.model.PromoMechanicsParams;
import ru.yandex.market.promoboss.model.mechanics.CheapestAsGift;
import ru.yandex.market.promoboss.model.mechanics.Promocode;
import ru.yandex.market.promoboss.model.mechanics.PromocodeType;
import ru.yandex.market.promoboss.service.mechanics.CheapestAsGiftService;
import ru.yandex.market.promoboss.service.mechanics.PromocodeService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest
@ContextConfiguration(classes = {PromoMechanicsService.class})
class PromoMechanicsServiceTest {
    Long id = 1L;

    @Autowired
    private PromoMechanicsService promoMechanicsService;

    @MockBean
    private CheapestAsGiftService cheapestAsGiftService;

    @MockBean
    private PromocodeService promocodeService;

    private PromoMechanicsParams promoMechanicsParams;

    @BeforeEach
    void build() {
        promoMechanicsParams = PromoMechanicsParams.builder()
                .cheapestAsGift(
                        CheapestAsGift.builder()
                                .count(1)
                                .build()
                )
                .promocode(
                        Promocode.builder()
                                .code("code")
                                .codeType(PromocodeType.FIXED_DISCOUNT)
                                .value(10)
                                .build()
                )
                .build();
    }


    private Answer<Void> createAnswer(Consumer<PromoMechanicsParams.PromoMechanicsParamsBuilder> consumer) {
        return invocation -> {
            PromoMechanicsParams.PromoMechanicsParamsBuilder builder = (PromoMechanicsParams.PromoMechanicsParamsBuilder) invocation.getArguments()[1];
            consumer.accept(builder);
            return null;
        };
    }

    @Test
    void populatePromo_ok() {
        // setup
        doAnswer(createAnswer(builder -> builder.cheapestAsGift(promoMechanicsParams.getCheapestAsGift()))).when(cheapestAsGiftService).populatePromo(any(), any());
        doAnswer(createAnswer(builder -> builder.promocode(promoMechanicsParams.getPromocode()))).when(promocodeService).populatePromo(any(), any());

        // act
        var actual = promoMechanicsService.getPromoMechanicsParams(id);

        // verify
        verify(cheapestAsGiftService).populatePromo(any(), any());
        verifyNoMoreInteractions(cheapestAsGiftService);
        verify(promocodeService).populatePromo(any(), any());
        verifyNoMoreInteractions(promocodeService);
        assertEquals(promoMechanicsParams, actual);
    }

    @Test
    void populatePromo_throw() {
        // setup
        doThrow(new RuntimeException("Some error")).when(cheapestAsGiftService).populatePromo(any(), any());
        doThrow(new RuntimeException("Some error")).when(promocodeService).populatePromo(any(), any());


        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoMechanicsService.getPromoMechanicsParams(id));

        // verify
        assertEquals("Some error", e.getMessage());
        verify(cheapestAsGiftService, atMostOnce()).populatePromo(any(), any());
        verifyNoMoreInteractions(cheapestAsGiftService);
        verify(promocodeService, atMostOnce()).populatePromo(any(), any());
        verifyNoMoreInteractions(promocodeService);
    }

    @Test
    void insertPromo_ok() {
        // setup

        // act
        assertDoesNotThrow(() -> promoMechanicsService.insertPromo(id, promoMechanicsParams));

        // verify
        verify(cheapestAsGiftService).insertPromo(id, promoMechanicsParams);
        verifyNoMoreInteractions(cheapestAsGiftService);
        verify(promocodeService).insertPromo(id, promoMechanicsParams);
        verifyNoMoreInteractions(promocodeService);
    }

    @Test
    void insertPromo_throw() {
        // setup
        doThrow(new RuntimeException("Some error")).when(cheapestAsGiftService).insertPromo(any(), any());

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoMechanicsService.insertPromo(id, promoMechanicsParams));

        // verify
        assertEquals("Some error", e.getMessage());
        verify(cheapestAsGiftService, atMostOnce()).insertPromo(id, promoMechanicsParams);
        verifyNoMoreInteractions(cheapestAsGiftService);
        verify(promocodeService, atMostOnce()).insertPromo(id, promoMechanicsParams);
        verifyNoMoreInteractions(promocodeService);
    }

    @Test
    void updatePromo_ok() {
        // setup

        // act
        promoMechanicsService.updatePromo(id, promoMechanicsParams);

        // verify
        verify(cheapestAsGiftService).updatePromo(id, promoMechanicsParams);
        verifyNoMoreInteractions(cheapestAsGiftService);
        verify(promocodeService).updatePromo(id, promoMechanicsParams);
        verifyNoMoreInteractions(promocodeService);
    }

    @Test
    void updatePromo_throw() {
        // setup
        doThrow(new RuntimeException("Some error")).when(cheapestAsGiftService).updatePromo(any(), any());

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoMechanicsService.updatePromo(id, promoMechanicsParams));

        // verify
        assertEquals("Some error", e.getMessage());
        verify(cheapestAsGiftService, atMostOnce()).updatePromo(id, promoMechanicsParams);
        verifyNoMoreInteractions(cheapestAsGiftService);
        verify(promocodeService, atMostOnce()).updatePromo(id, promoMechanicsParams);
        verifyNoMoreInteractions(promocodeService);
    }
}
