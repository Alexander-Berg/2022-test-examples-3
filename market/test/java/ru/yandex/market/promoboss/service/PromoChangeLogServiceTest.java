package ru.yandex.market.promoboss.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.promoboss.dao.PromoChangeLogDao;
import ru.yandex.market.promoboss.model.Promo;
import ru.yandex.market.promoboss.model.PromoChangeLogItem;
import ru.yandex.market.promoboss.model.PromoMainParams;
import ru.yandex.market.promoboss.model.SourceType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ContextConfiguration(classes = {PromoChangeLogService.class})
class PromoChangeLogServiceTest {
    @Autowired
    private PromoChangeLogService promoChangeLogService;
    @MockBean
    private PromoChangeLogDao promoChangeLogDao;

    @Test
    void start_ok() {
        doNothing().when(promoChangeLogDao).insert(any());

        Promo promo = Promo.builder()
                .promoId("promoId")
                .modifiedBy("modifiedBy")
                .mainParams(
                        PromoMainParams.builder()
                                .source(SourceType.CATEGORYIFACE)
                                .build()
                )
                .build();

        promoChangeLogService.start(promo, "requestId");

        verify(promoChangeLogDao).insert(PromoChangeLogItem.builder()
                .sourcePromoId("promoId")
                .requestId("requestId")
                .updatedBy("modifiedBy")
                .source(SourceType.CATEGORYIFACE)
                .build());
    }

    @Test
    void start_unknownSource_throw() {
        Promo promo = Promo.builder()
                .promoId("promoId")
                .modifiedBy("modifiedBy")
                .mainParams(null)
                .build();

        RuntimeException e = assertThrows(RuntimeException.class, () -> promoChangeLogService.start(promo, "requestId"));
        assertEquals("Could not determine the source of the change", e.getMessage());
    }
}
