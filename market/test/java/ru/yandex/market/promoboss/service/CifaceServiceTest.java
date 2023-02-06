package ru.yandex.market.promoboss.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.promoboss.dao.CifaceDao;
import ru.yandex.market.promoboss.dao.CifaceMultiplePropertiesDao;
import ru.yandex.market.promoboss.dao.CifacePromotionDao;
import ru.yandex.market.promoboss.model.CifaceMultipleProperty;
import ru.yandex.market.promoboss.model.CifaceMultipleValue;
import ru.yandex.market.promoboss.model.CifacePromo;
import ru.yandex.market.promoboss.model.CifacePromotion;
import ru.yandex.market.promoboss.model.SrcCiface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(classes = {CifaceService.class})
public class CifaceServiceTest {

    private static final Long ID = 1000L;

    private static final CifacePromo CIFACE_PROMO = CifacePromo.builder()
            .promoPurpose("promoPurpose")
            .compensationSource("compensationSource")
            .tradeManager("tradeManager")
            .markom("catManager")
            .promoKind("promoKind")
            .supplierType("supplierType")
            .mediaPlanS3Key("mediaPlanS3Key")
            .mediaPlanS3FileName("mediaPlanS3FileName")
            .compensationTicket("compensationTicket")
            .assortmentLoadMethod("assortmentLoadMethod")
            .build();

    private static final List<CifaceMultipleValue> CIFACE_MULTIPLE_PROPERTIES = List.of(
            CifaceMultipleValue.builder()
                    .property(CifaceMultipleProperty.COMPENSATION_RECEIVE_METHOD)
                    .stringValue("compensationReceiveMethod1")
                    .build(),
            CifaceMultipleValue.builder()
                    .property(CifaceMultipleProperty.COMPENSATION_RECEIVE_METHOD)
                    .stringValue("compensationReceiveMethod2")
                    .build(),
            CifaceMultipleValue.builder()
                    .property(CifaceMultipleProperty.CATEGORY_STREAM)
                    .stringValue("categoryStream1")
                    .build(),
            CifaceMultipleValue.builder()
                    .property(CifaceMultipleProperty.CATEGORY_STREAM)
                    .stringValue("categoryStream2")
                    .build(),
            CifaceMultipleValue.builder()
                    .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                    .stringValue("categoryDepartment1")
                    .build(),
            CifaceMultipleValue.builder()
                    .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                    .stringValue("categoryDepartment2")
                    .build()
    );

    private static final List<CifacePromotion> CIFACE_PROMOTIONS = List.of(
            CifacePromotion.builder()
                    .catteam("catteam")
                    .category("category")
                    .channel("channel")
                    .count(123L)
                    .countUnit(null)
                    .budgetPlan(124L)
                    .budgetFact(125L)
                    .isCustomBudgetPlan(false)
                    .comment("comment")
                    .build()
    );

    private static final List<CifacePromotion> OLD_CIFACE_PROMOTIONS = List.of(
            CifacePromotion.builder()
                    .id(123L)
                    .catteam("catteam")
                    .category("category")
                    .channel("channel")
                    .count(123L)
                    .countUnit(null)
                    .budgetPlan(124L)
                    .budgetFact(125L)
                    .isCustomBudgetPlan(false)
                    .comment("comment")
                    .build()
    );

    private static final List<CifaceMultipleValue> OLD_MULTIPLE_PROPERTIES = List.of(
            CifaceMultipleValue.builder()
                    .id(121L)
                    .property(CifaceMultipleProperty.COMPENSATION_RECEIVE_METHOD)
                    .stringValue("compensationReceiveMethod1-old")
                    .build(),
            CifaceMultipleValue.builder()
                    .id(122L)
                    .property(CifaceMultipleProperty.COMPENSATION_RECEIVE_METHOD)
                    .stringValue("compensationReceiveMethod2")
                    .build(),
            CifaceMultipleValue.builder()
                    .id(123L)
                    .property(CifaceMultipleProperty.CATEGORY_STREAM)
                    .stringValue("categoryStream1-old")
                    .build(),
            CifaceMultipleValue.builder()
                    .id(124L)
                    .property(CifaceMultipleProperty.CATEGORY_STREAM)
                    .stringValue("categoryStream2")
                    .build(),
            CifaceMultipleValue.builder()
                    .id(125L)
                    .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                    .stringValue("categoryDepartment1-old")
                    .build(),
            CifaceMultipleValue.builder()
                    .id(126L)
                    .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                    .stringValue("categoryDepartment2")
                    .build()
    );

    private static final SrcCiface CI_FACE_DTO = SrcCiface.builder()
            .cifacePromo(CIFACE_PROMO)
            .multipleProperties(CIFACE_MULTIPLE_PROPERTIES)
            .cifacePromotions(CIFACE_PROMOTIONS)
            .build();

    @Autowired
    private CifaceService cifaceService;

    @MockBean
    private CifaceDao cifaceDao;

    @MockBean
    private CifacePromotionDao cifacePromotionDao;

    @MockBean
    private CifaceMultiplePropertiesDao cifaceMultiplePropertiesDao;

    @Test
    public void getCifaceNotEmpty() {
        // setup
        when(cifaceDao.findByPromoId(ID)).thenReturn(Optional.of(CIFACE_PROMO));
        when(cifacePromotionDao.findByPromoId(ID)).thenReturn(CIFACE_PROMOTIONS);
        when(cifaceMultiplePropertiesDao.findByPromoId(ID)).thenReturn(CIFACE_MULTIPLE_PROPERTIES);

        // act
        var ciface = cifaceService.getSrcCifaceByPromoId(ID);

        // verify
        assertNotNull(ciface);
        assertEquals(CI_FACE_DTO, ciface);

        var inOrder = Mockito.inOrder(cifaceDao, cifacePromotionDao, cifaceMultiplePropertiesDao);
        inOrder.verify(cifaceDao).findByPromoId(ID);
        inOrder.verify(cifaceMultiplePropertiesDao).findByPromoId(ID);
        inOrder.verify(cifacePromotionDao).findByPromoId(ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void getCifaceNoPromotions() {
        // setup
        when(cifaceDao.findByPromoId(ID)).thenReturn(Optional.of(CIFACE_PROMO));
        when(cifacePromotionDao.findByPromoId(ID)).thenReturn(Collections.emptyList());
        when(cifaceMultiplePropertiesDao.findByPromoId(ID)).thenReturn(Collections.emptyList());

        // act
        var ciface = cifaceService.getSrcCifaceByPromoId(ID);

        // verify
        assertNotNull(ciface);
        assertEquals(SrcCiface.builder()
                .cifacePromo(CIFACE_PROMO)
                .cifacePromotions(Collections.emptyList())
                .multipleProperties(Collections.emptyList())
                .build(), ciface);

        var inOrder = Mockito.inOrder(cifaceDao, cifacePromotionDao, cifaceMultiplePropertiesDao);
        inOrder.verify(cifaceDao).findByPromoId(ID);
        inOrder.verify(cifaceMultiplePropertiesDao).findByPromoId(ID);
        inOrder.verify(cifacePromotionDao).findByPromoId(ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void getCifaceEmpty() {
        // setup
        when(cifaceDao.findByPromoId(ID)).thenReturn(Optional.empty());
        when(cifacePromotionDao.findByPromoId(ID)).thenReturn(Collections.emptyList());
        when(cifaceMultiplePropertiesDao.findByPromoId(ID)).thenReturn(Collections.emptyList());

        // act
        var ciface = cifaceService.getSrcCifaceByPromoId(ID);

        // verify
        assertNull(ciface);

        var inOrder = Mockito.inOrder(cifaceDao, cifacePromotionDao, cifaceMultiplePropertiesDao);
        inOrder.verify(cifaceDao).findByPromoId(ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void getCiface_throwExceptionInCifaceDao() {
        // setup
        when(cifaceDao.findByPromoId(ID)).thenThrow(new RuntimeException("Some error"));
        when(cifacePromotionDao.findByPromoId(ID)).thenReturn(CIFACE_PROMOTIONS);

        // act
        var e = assertThrows(RuntimeException.class,
                () -> cifaceService.getSrcCifaceByPromoId(ID));

        // verify
        assertEquals("Some error", e.getMessage());

        var inOrder = Mockito.inOrder(cifaceDao, cifacePromotionDao, cifaceMultiplePropertiesDao);
        inOrder.verify(cifaceDao).findByPromoId(ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void getCiface_throwExceptionInCifacePromotionDao() {
        // setup
        when(cifaceDao.findByPromoId(ID)).thenReturn(Optional.of(CIFACE_PROMO));
        when(cifacePromotionDao.findByPromoId(ID)).thenThrow(new RuntimeException("Some error"));

        // act
        var e = assertThrows(RuntimeException.class,
                () -> cifaceService.getSrcCifaceByPromoId(ID));

        // verify
        assertEquals("Some error", e.getMessage());

        var inOrder = Mockito.inOrder(cifaceDao, cifacePromotionDao, cifaceMultiplePropertiesDao);
        inOrder.verify(cifaceDao).findByPromoId(ID);
        inOrder.verify(cifacePromotionDao).findByPromoId(ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void getCiface_throwExceptionInCifaceMultiplePropertiesDao() {
        // setup
        when(cifaceDao.findByPromoId(ID)).thenReturn(Optional.of(CIFACE_PROMO));
        when(cifacePromotionDao.findByPromoId(ID)).thenReturn(Collections.emptyList());
        when(cifaceMultiplePropertiesDao.findByPromoId(ID)).thenThrow(new RuntimeException("Some error"));

        // act
        var e = assertThrows(RuntimeException.class,
                () -> cifaceService.getSrcCifaceByPromoId(ID));

        // verify
        assertEquals("Some error", e.getMessage());

        var inOrder = Mockito.inOrder(cifaceDao, cifacePromotionDao, cifaceMultiplePropertiesDao);
        inOrder.verify(cifaceDao).findByPromoId(ID);
        inOrder.verify(cifaceMultiplePropertiesDao).findByPromoId(ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void insertCiface() {
        // setup

        // act
        cifaceService.insertSrcCiface(ID, CI_FACE_DTO);

        // verify
        var inOrder = Mockito.inOrder(cifaceDao, cifacePromotionDao, cifaceMultiplePropertiesDao);
        inOrder.verify(cifaceDao).insertCiface(ID, CIFACE_PROMO);
        inOrder.verify(cifaceMultiplePropertiesDao, times(6)).insert(eq(ID), any());
        inOrder.verify(cifacePromotionDao).saveAll(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void updateCiface_noPromotions() {
        // setup
        when(cifaceDao.findByPromoId(ID)).thenReturn(Optional.of(CIFACE_PROMO));

        // act
        cifaceService.updateSrcCiface(ID, CI_FACE_DTO);

        // verify
        var inOrder = Mockito.inOrder(cifaceDao, cifacePromotionDao, cifaceMultiplePropertiesDao);
        inOrder.verify(cifaceDao).updateCiface(ID, CIFACE_PROMO);
        inOrder.verify(cifaceMultiplePropertiesDao).findByPromoId(ID);
        inOrder.verify(cifaceMultiplePropertiesDao, times(6)).insert(eq(ID), any());
        inOrder.verify(cifacePromotionDao).findByPromoId(ID);
        inOrder.verify(cifacePromotionDao).saveAll(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void updateCiface_promotionsExists() {
        // setup
        when(cifaceDao.findByPromoId(ID)).thenReturn(Optional.of(CIFACE_PROMO));
        when(cifacePromotionDao.findByPromoId(ID)).thenReturn(OLD_CIFACE_PROMOTIONS);
        when(cifaceMultiplePropertiesDao.findByPromoId(ID)).thenReturn(OLD_MULTIPLE_PROPERTIES);

        // act
        cifaceService.updateSrcCiface(ID, CI_FACE_DTO);

        // verify
        var inOrder = Mockito.inOrder(cifaceDao, cifacePromotionDao, cifaceMultiplePropertiesDao);
        inOrder.verify(cifaceDao).updateCiface(ID, CIFACE_PROMO);
        inOrder.verify(cifaceMultiplePropertiesDao).findByPromoId(ID);
        inOrder.verify(cifaceMultiplePropertiesDao).deleteAll(any());
        inOrder.verify(cifaceMultiplePropertiesDao, times(3)).insert(eq(ID), any());
        inOrder.verify(cifacePromotionDao).findByPromoId(ID);
        inOrder.verify(cifacePromotionDao).deleteAll(OLD_CIFACE_PROMOTIONS);
        inOrder.verify(cifacePromotionDao).saveAll(any());
        inOrder.verifyNoMoreInteractions();
    }
}
