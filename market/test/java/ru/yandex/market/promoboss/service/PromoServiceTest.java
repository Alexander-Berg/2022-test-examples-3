package ru.yandex.market.promoboss.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.market.promoboss.dao.PromoDao;
import ru.yandex.market.promoboss.exception.PromoNotFoundException;
import ru.yandex.market.promoboss.exception.PromocodeReservationException;
import ru.yandex.market.promoboss.model.CifacePromo;
import ru.yandex.market.promoboss.model.CifacePromotion;
import ru.yandex.market.promoboss.model.Constraints;
import ru.yandex.market.promoboss.model.GenerateableUrl;
import ru.yandex.market.promoboss.model.MechanicsData;
import ru.yandex.market.promoboss.model.MechanicsType;
import ru.yandex.market.promoboss.model.Promo;
import ru.yandex.market.promoboss.model.PromoEvent;
import ru.yandex.market.promoboss.model.PromoField;
import ru.yandex.market.promoboss.model.PromoMainParams;
import ru.yandex.market.promoboss.model.PromoMechanicsParams;
import ru.yandex.market.promoboss.model.PromoSrcParams;
import ru.yandex.market.promoboss.model.SrcCiface;
import ru.yandex.market.promoboss.model.SskuData;
import ru.yandex.market.promoboss.model.mechanics.CheapestAsGift;
import ru.yandex.market.promoboss.model.mechanics.Promocode;
import ru.yandex.market.promoboss.model.mechanics.PromocodeType;
import ru.yandex.market.promoboss.model.postgres.SupplierConstraintDto;
import ru.yandex.market.promoboss.model.postgres.WarehouseConstraintDto;
import ru.yandex.market.promoboss.service.mechanics.PromocodeReservationService;
import ru.yandex.market.promoboss.service.search.PromoSearchService;
import ru.yandex.market.promoboss.utils.PromoFieldUtilsTest;
import ru.yandex.market.promoboss.validator.exception.MechanicsTypeValidationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.promoboss.utils.PromoEnricherUtils.generateLandingUrl;
import static ru.yandex.market.promoboss.utils.PromoEnricherUtils.generateRulesUrl;

@SpringBootTest
@ContextConfiguration(classes = {PromoService.class, PromoSearchService.class})
public class PromoServiceTest {

    private static final Long ID = 1000L;
    private static final String PROMO_ID = "cf_123";
    private static final Set<String> SSKU = Set.of("ssku1", "ssku2");
    private static final CheapestAsGift CHEAPEST_AS_GIFT = new CheapestAsGift(3);
    private static final Promocode PROMOCODE = Promocode.builder()
            .code("CODE")
            .codeType(PromocodeType.PERCENTAGE)
            .value(10)
            .minCartPrice(0L)
            .maxCartPrice(100L)
            .build();
    private static final SskuData SSKU_DATA = new SskuData(new MechanicsData(MechanicsType.CHEAPEST_AS_GIFT,
            CHEAPEST_AS_GIFT));

    private static final SskuData SSKU_DATA_EMPTY = new SskuData(new MechanicsData(MechanicsType.CHEAPEST_AS_GIFT,
            null));

    private static final CifacePromo CIFACE_PROMO = CifacePromo.builder()
            .author("sergey")
            .promoPurpose("promoPurpose")
            .compensationSource("compensationSource")
            .tradeManager("tradeManager")
            .markom("catManager")
            .promoKind("promoKind")
            .supplierType("supplierType")
            .finalBudget(true)
            .autoCompensation(false)
            .build();
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
    private static final SrcCiface CIFACE_DTO = SrcCiface.builder()
            .cifacePromo(CIFACE_PROMO)
            .cifacePromotions(CIFACE_PROMOTIONS)
            .build();

    private static final Constraints CONSTRAINTS =
            Constraints.builder().suppliers(List.of(
                    SupplierConstraintDto.builder().supplierId(123L).exclude(false).build(),
                    SupplierConstraintDto.builder().supplierId(124L).exclude(false).build(),
                    SupplierConstraintDto.builder().supplierId(125L).exclude(false).build()
            )).warehouses(List.of(
                    WarehouseConstraintDto.builder().warehouseId(523L).exclude(true).build(),
                    WarehouseConstraintDto.builder().warehouseId(525L).exclude(true).build(),
                    WarehouseConstraintDto.builder().warehouseId(525L).exclude(true).build()
            )).build();
    private static final PromoEvent CREATE_PROMO_EVENT = PromoEvent.create(ID, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
    private static final PromoEvent UPDATE_PROMO_EVENT = PromoEvent.create(ID, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));

    @MockBean
    private PromoDao promoDao;

    @MockBean
    private PromoMechanicsService promoMechanicsService;

    @MockBean
    private CifaceService cifaceService;

    @MockBean
    private SskuService sskuService;

    @MockBean
    private ConstraintsService constraintsService;

    @MockBean
    private PromoSearchService promoSearchService;

    @MockBean
    private PromoEventService promoEventService;

    @MockBean
    private PromocodeReservationService promocodeReservationService;

    @MockBean
    private PromoChangeLogService promoChangeLogService;

    @Autowired
    private PromoService promoService;

    @Captor
    private ArgumentCaptor<PromoMainParams> promoMainParamsArgumentCaptor;

    private Promo promo;

    private String requestId;

    @BeforeEach
    public void beforeEach() {
        promo = Promo.builder()
                .promoId(PROMO_ID)
                .mainParams(
                        PromoMainParams.builder()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url("https://landing.url")
                                                .auto(true)
                                                .build()
                                )
                                .rulesUrl(GenerateableUrl.builder()
                                        .url("https://rules.url")
                                        .auto(true)
                                        .build()
                                )
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder().build()
                )
                .srcParams(
                        PromoSrcParams.builder().build()
                )
                .build();

        requestId = GUID.create().toString();
    }

    @Test
    public void getEmptyPromo() {

        // setup
        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.empty());

        // act
        assertThrows(PromoNotFoundException.class, () -> promoService.getPromo(PROMO_ID, PromoFieldUtilsTest.createAll()));

        // verify
        verify(promoDao).getFullPromoByPromoId(PROMO_ID);
        verifyNoMoreInteractions(promoDao);

        verifyNoInteractions(promoMechanicsService);
        verifyNoInteractions(cifaceService);
        verifyNoInteractions(sskuService);
        verifyNoInteractions(constraintsService);
    }

    @Test
    public void getPromoWithoutExtraFields() {

        // setup
        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo));
        when(cifaceService.getSrcCifaceByPromoId(any())).thenReturn(null);
        when(sskuService.getSskuByPromoId(any())).thenReturn(Collections.emptySet());
        when(constraintsService.getByPromoId(any(), any())).thenReturn(
                Constraints.builder().suppliers(Collections.emptyList()).warehouses(Collections.emptyList()).build());
        when(promoMechanicsService.getPromoMechanicsParams(any())).thenReturn(promo.getMechanicsParams());

        // act
        AtomicReference<Promo> promoAtomicReference = new AtomicReference<>();
        assertDoesNotThrow(() -> promoAtomicReference.set(promoService.getPromo(PROMO_ID, PromoFieldUtilsTest.createAll())));
        Promo result = promoAtomicReference.get();

        // verify
        InOrder inOrder = Mockito.inOrder(promoDao, promoMechanicsService, cifaceService, sskuService);

        inOrder.verify(promoDao).getFullPromoByPromoId(PROMO_ID);
        inOrder.verify(promoMechanicsService).getPromoMechanicsParams(any());
        inOrder.verify(cifaceService).getSrcCifaceByPromoId(any());
        inOrder.verify(sskuService).getSskuByPromoId(any());

        verifyNoMoreInteractions(promoDao);
        verifyNoMoreInteractions(promoMechanicsService);
        verifyNoMoreInteractions(cifaceService);
        verifyNoMoreInteractions(sskuService);

        assertNotNull(result);
        assertEquals(promo.getPromoId(), result.getPromoId());

        assertNull(result.getMechanicsParams().getCheapestAsGift());
        assertTrue(result.getConstraints().getSuppliers().isEmpty());
        assertTrue(result.getConstraints().getWarehouses().isEmpty());

        assertTrue(result.getSsku().isEmpty());
    }

    @Test
    public void getPromoWithExtraFields() {

        // setup
        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo));
        when(promoMechanicsService.getPromoMechanicsParams(any())).thenReturn(PromoMechanicsParams.builder().cheapestAsGift(CHEAPEST_AS_GIFT).build());
        when(cifaceService.getSrcCifaceByPromoId(any())).thenReturn(CIFACE_DTO);
        when(sskuService.getSskuByPromoId(any())).thenReturn(SSKU);
        when(constraintsService.getByPromoId(any(), any())).thenReturn(CONSTRAINTS);

        // act
        AtomicReference<Promo> promoAtomicReference = new AtomicReference<>();
        assertDoesNotThrow(() -> promoAtomicReference.set(promoService.getPromo(PROMO_ID, PromoFieldUtilsTest.createAll())));
        Promo result = promoAtomicReference.get();

        // verify
        InOrder inOrder =
                Mockito.inOrder(promoDao, promoMechanicsService, cifaceService, sskuService, constraintsService);

        inOrder.verify(promoDao).getFullPromoByPromoId(PROMO_ID);
        inOrder.verify(promoMechanicsService).getPromoMechanicsParams(any());
        inOrder.verify(cifaceService).getSrcCifaceByPromoId(any());
        inOrder.verify(sskuService).getSskuByPromoId(any());
        inOrder.verify(constraintsService).getByPromoId(any(), any());

        verifyNoMoreInteractions(promoDao);
        verifyNoMoreInteractions(promoMechanicsService);
        verifyNoMoreInteractions(cifaceService);
        verifyNoMoreInteractions(sskuService);

        assertEquals(promo, result);

        assertEquals(CHEAPEST_AS_GIFT, result.getMechanicsParams().getCheapestAsGift());

        assertEquals(CIFACE_DTO, result.getSrcParams().getCiface());

        assertEquals(CONSTRAINTS.getSuppliers(), result.getConstraints().getSuppliers());
        assertEquals(CONSTRAINTS.getWarehouses(), result.getConstraints().getWarehouses());

        assertEquals(SSKU, result.getSsku());
    }

    @Test
    public void getPromo_throwExceptionInCheapestAsGiftDao() {

        // setup
        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo));
        doThrow(new RuntimeException("Some error")).when(promoMechanicsService).getPromoMechanicsParams(any());

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.getPromo(PROMO_ID, PromoFieldUtilsTest.createAll()));

        // verify
        assertEquals("Some error", e.getMessage());

        InOrder inOrder = Mockito.inOrder(promoDao, promoMechanicsService, cifaceService, sskuService);

        inOrder.verify(promoDao).getFullPromoByPromoId(PROMO_ID);
        inOrder.verify(promoMechanicsService).getPromoMechanicsParams(any());

        verifyNoMoreInteractions(promoDao);
        verifyNoMoreInteractions(promoMechanicsService);

        verifyNoInteractions(cifaceService);
        verifyNoInteractions(sskuService);
    }

    @Test
    public void getPromo_throwExceptionInCifaceService() {

        // setup
        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo));
        doThrow(new RuntimeException("Some error")).when(cifaceService).getSrcCifaceByPromoId(any());

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.getPromo(PROMO_ID, PromoFieldUtilsTest.createAll()));

        // verify
        assertEquals("Some error", e.getMessage());

        InOrder inOrder = Mockito.inOrder(promoDao, promoMechanicsService, cifaceService, sskuService);

        inOrder.verify(promoDao).getFullPromoByPromoId(PROMO_ID);
        inOrder.verify(promoMechanicsService).getPromoMechanicsParams(any());
        inOrder.verify(cifaceService).getSrcCifaceByPromoId(any());

        verifyNoMoreInteractions(promoDao);
        verifyNoMoreInteractions(promoMechanicsService);
        verifyNoMoreInteractions(cifaceService);

        verifyNoInteractions(sskuService);
    }

    @Test
    public void getPromo_throwExceptionInSskuService() {

        // setup
        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo));
        when(cifaceService.getSrcCifaceByPromoId(any())).thenReturn(null);
        when(sskuService.getSskuByPromoId(any())).thenThrow(new RuntimeException("Some error"));

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.getPromo(PROMO_ID, PromoFieldUtilsTest.createAll()));

        // verify
        assertEquals("Some error", e.getMessage());

        InOrder inOrder = Mockito.inOrder(promoDao, promoMechanicsService, cifaceService, sskuService);

        inOrder.verify(promoDao).getFullPromoByPromoId(PROMO_ID);
        inOrder.verify(promoMechanicsService).getPromoMechanicsParams(any());
        inOrder.verify(cifaceService).getSrcCifaceByPromoId(any());
        inOrder.verify(sskuService).getSskuByPromoId(any());

        verifyNoMoreInteractions(promoDao);
        verifyNoMoreInteractions(promoMechanicsService);
        verifyNoMoreInteractions(cifaceService);
        verifyNoMoreInteractions(sskuService);
    }

    @Test
    public void getPromo_throwExceptionInConstraintService() {

        // setup
        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo));
        when(constraintsService.getByPromoId(any(), any())).thenThrow(new RuntimeException("Some error"));

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.getPromo(PROMO_ID, PromoFieldUtilsTest.createAll()));

        // verify
        assertEquals("Some error", e.getMessage());
    }

    @Test
    public void createPromoWithoutExtraFields() {

        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        when(promoDao.insertPromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        when(promoEventService.saveCreateEvent(ID)).thenReturn(CREATE_PROMO_EVENT);

        // act
        promoService.createPromo(modifiedFields, promo, requestId);

        // verify
        verify(promoChangeLogService).start(promo, requestId);
        verify(promoMechanicsService).insertPromo(ID, promo.getMechanicsParams());
        verify(sskuService).saveSsku(ID, null, SSKU_DATA_EMPTY);
        verify(promoEventService).saveCreateEvent(ID);
        verify(promoSearchService).addPromoToSearch(ID, PromoFieldUtilsTest.createAll(), promo, CREATE_PROMO_EVENT);
    }

    @Test
    public void createPromoWithExtraFields() {

        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setSsku(SSKU);
        promo.setConstraints(CONSTRAINTS);

        when(promoDao.insertPromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        when(promoEventService.saveCreateEvent(ID)).thenReturn(CREATE_PROMO_EVENT);

        // act
        promoService.createPromo(modifiedFields, promo, requestId);

        // verify
        InOrder inOrder = Mockito.inOrder(promoChangeLogService, promoDao, promoMechanicsService, cifaceService, sskuService, constraintsService);

        inOrder.verify(promoChangeLogService).start(promo, requestId);
        inOrder.verify(promoMechanicsService).insertPromo(ID, promo.getMechanicsParams());
        when(cifaceService.getSrcCifaceByPromoId(any())).thenReturn(null);
        inOrder.verify(sskuService).saveSsku(eq(ID), eq(SSKU), eq(SSKU_DATA));
        inOrder.verify(constraintsService).saveForPromoId(eq(ID), eq(PromoFieldUtilsTest.createAll()), eq(CONSTRAINTS));
        verify(promoEventService).saveCreateEvent(ID);
        verify(promoSearchService).addPromoToSearch(ID, PromoFieldUtilsTest.createAll(), promo, CREATE_PROMO_EVENT);

        verifyNoMoreInteractions(promoMechanicsService);
        verifyNoMoreInteractions(sskuService);
    }

    @Test
    public void createPromoWithoutUrls() {

        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setSsku(SSKU);
        promo.setConstraints(CONSTRAINTS);

        promo.getMainParams().setLandingUrl(null);
        promo.getMainParams().setRulesUrl(null);

        when(promoDao.insertPromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        when(promoEventService.saveCreateEvent(ID)).thenReturn(CREATE_PROMO_EVENT);

        // act
        promoService.createPromo(modifiedFields, promo, requestId);

        // verify
        InOrder inOrder = Mockito.inOrder(promoChangeLogService, promoDao, promoMechanicsService, cifaceService, sskuService, constraintsService);

        inOrder.verify(promoChangeLogService).start(promo, requestId);
        inOrder.verify(promoMechanicsService).insertPromo(ID, promo.getMechanicsParams());
        when(cifaceService.getSrcCifaceByPromoId(any())).thenReturn(null);
        inOrder.verify(sskuService).saveSsku(eq(ID), eq(SSKU), eq(SSKU_DATA));
        inOrder.verify(constraintsService).saveForPromoId(eq(ID), eq(PromoFieldUtilsTest.createAll()), eq(CONSTRAINTS));
        verify(promoEventService).saveCreateEvent(ID);
        verify(promoSearchService).addPromoToSearch(ID, PromoFieldUtilsTest.createAll(), promo, CREATE_PROMO_EVENT);

        verifyNoMoreInteractions(promoMechanicsService);
        verifyNoMoreInteractions(sskuService);
    }


    @Test
    public void createPromo_throwExceptionInCheapestAsGiftDao() {

        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setSsku(SSKU);

        when(promoDao.insertPromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        doThrow(new RuntimeException("Some error")).when(promoMechanicsService).insertPromo(eq(ID), any());

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.createPromo(modifiedFields, promo, requestId));

        // verify
        assertEquals("Some error", e.getMessage());

        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao).insertPromo(promo.getPromoId(), promo.getMainParams());
        verifyNoMoreInteractions(promoDao);

        verify(promoMechanicsService).insertPromo(ID, promo.getMechanicsParams());
        verifyNoMoreInteractions(promoMechanicsService);

        verifyNoInteractions(cifaceService);
        verifyNoInteractions(sskuService);
    }

    @Test
    public void createPromo_throwExceptionInPromoDao() {

        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setSsku(SSKU);

        when(promoDao.insertPromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        doThrow(new RuntimeException("Some error")).when(cifaceService).insertSrcCiface(ID, CIFACE_DTO);

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.createPromo(modifiedFields, promo, requestId));

        // verify
        assertEquals("Some error", e.getMessage());

        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao).insertPromo(promo.getPromoId(), promo.getMainParams());
        verifyNoMoreInteractions(promoDao);

        verify(promoMechanicsService).insertPromo(ID, promo.getMechanicsParams());
        verifyNoMoreInteractions(promoMechanicsService);

        verify(cifaceService).insertSrcCiface(ID, CIFACE_DTO);
        verifyNoMoreInteractions(cifaceService);

        verifyNoInteractions(sskuService);
    }

    @Test
    public void createPromo_throwExceptionInSskuService() {

        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setSsku(SSKU);

        when(promoDao.insertPromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        doThrow(new RuntimeException("Some error")).when(sskuService).saveSsku(ID, SSKU, SSKU_DATA);

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.createPromo(modifiedFields, promo, requestId));

        // verify
        assertEquals("Some error", e.getMessage());

        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao).insertPromo(promo.getPromoId(), promo.getMainParams());
        verifyNoMoreInteractions(promoDao);

        verify(promoMechanicsService).insertPromo(ID, promo.getMechanicsParams());
        verifyNoMoreInteractions(promoMechanicsService);

        verify(cifaceService).insertSrcCiface(ID, CIFACE_DTO);
        verifyNoMoreInteractions(cifaceService);

        verify(sskuService).saveSsku(ID, SSKU, SSKU_DATA);
        verifyNoMoreInteractions(sskuService);
    }

    @Test
    public void createPromo_throwExceptionInConstraintService() {

        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setSsku(SSKU);
        promo.setConstraints(CONSTRAINTS);

        when(promoDao.insertPromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        doThrow(new RuntimeException("Some error")).when(constraintsService).saveForPromoId(ID, PromoFieldUtilsTest.createAll(), CONSTRAINTS);

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.createPromo(modifiedFields, promo, requestId));

        // verify
        assertEquals("Some error", e.getMessage());

        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao).insertPromo(promo.getPromoId(), promo.getMainParams());
        verifyNoMoreInteractions(promoDao);

        verify(promoMechanicsService).insertPromo(ID, promo.getMechanicsParams());
        verifyNoMoreInteractions(promoMechanicsService);

        verify(cifaceService).insertSrcCiface(ID, CIFACE_DTO);
        verifyNoMoreInteractions(cifaceService);

        verify(sskuService).saveSsku(ID, SSKU, SSKU_DATA);
        verifyNoMoreInteractions(sskuService);

        verify(constraintsService).saveForPromoId(ID, PromoFieldUtilsTest.createAll(), CONSTRAINTS);
        verifyNoMoreInteractions(constraintsService);
    }

    @Test
    public void createPromo_throwExceptionInSearchService() {

        when(promoDao.insertPromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        when(promoEventService.saveCreateEvent(ID)).thenReturn(CREATE_PROMO_EVENT);
        doThrow(new RuntimeException("Some error in search service.")).when(promoSearchService)
                .addPromoToSearch(ID, PromoFieldUtilsTest.createAll(), promo, CREATE_PROMO_EVENT);

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.createPromo(PromoFieldUtilsTest.createAll(), promo, requestId));

        // verify
        assertEquals("Some error in search service.", e.getMessage());
    }

    @Test
    public void createPromo_shouldGenerateUrlsAndMore() {

        // setup
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        GenerateableUrl landingUrl = GenerateableUrl.builder()
                .url("")
                .auto(true)
                .build();
        GenerateableUrl rulesUrl = GenerateableUrl.builder()
                .url("")
                .auto(true)
                .build();
        promo.getMainParams().setLandingUrl(landingUrl);
        promo.getMainParams().setRulesUrl(rulesUrl);

        when(promoDao.insertPromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);

        var promoFields = PromoFieldUtilsTest.createAll();

        // act
        promoService.createPromo(promoFields, promo, requestId);

        // verify
        InOrder inOrder = Mockito.inOrder(promoChangeLogService, promoDao);
        inOrder.verify(promoChangeLogService).start(promo, requestId);
        inOrder.verify(promoDao).insertPromo(eq(PROMO_ID), promoMainParamsArgumentCaptor.capture());
        inOrder.verifyNoMoreInteractions();

        assertFalse(StringUtils.isBlank(promoMainParamsArgumentCaptor.getValue().getPromoKey()));
        assertFalse(StringUtils.isBlank(promoMainParamsArgumentCaptor.getValue().getLandingUrl().getUrl()));
        assertFalse(StringUtils.isBlank(promoMainParamsArgumentCaptor.getValue().getRulesUrl().getUrl()));
    }

    @Test
    public void createPromo_shouldNotGenerateUrlsAndMore() {

        // setup
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        GenerateableUrl landingUrl = GenerateableUrl.builder()
                .url("landingUrl")
                .auto(false)
                .build();
        GenerateableUrl rulesUrl = GenerateableUrl.builder()
                .url("rulesUrl")
                .auto(false)
                .build();
        promo.getMainParams().setLandingUrl(landingUrl);
        promo.getMainParams().setRulesUrl(rulesUrl);

        when(promoDao.insertPromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);

        var promoFields = PromoFieldUtilsTest.createAll();

        // act
        promoService.createPromo(promoFields, promo, requestId);

        // verify
        InOrder inOrder = Mockito.inOrder(promoChangeLogService, promoDao);
        inOrder.verify(promoChangeLogService).start(promo, requestId);
        inOrder.verify(promoDao).insertPromo(eq(PROMO_ID), promoMainParamsArgumentCaptor.capture());
        inOrder.verifyNoMoreInteractions();

        assertFalse(StringUtils.isBlank(promoMainParamsArgumentCaptor.getValue().getPromoKey()));
        assertEquals(landingUrl, promoMainParamsArgumentCaptor.getValue().getLandingUrl());
        assertEquals(rulesUrl, promoMainParamsArgumentCaptor.getValue().getRulesUrl());
    }

    @Disabled("Временно отключено резервирование промокодов")
    @Test
    void createPromo_promocodeReservation_ok() {
        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        promo.getMechanicsParams().setPromocode(PROMOCODE);
        promo.getMainParams().setMechanicsType(MechanicsType.PROMO_CODE);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setConstraints(CONSTRAINTS);

        when(promoDao.insertPromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        when(promoEventService.saveCreateEvent(ID)).thenReturn(CREATE_PROMO_EVENT);
        doNothing().when(promocodeReservationService).reservePromocode(PROMOCODE.getCode());

        // act
        promoService.createPromo(modifiedFields, promo, requestId);

        // verify
        InOrder inOrder =
                Mockito.inOrder(promoChangeLogService, promoDao, promoMechanicsService, cifaceService, constraintsService,
                        promocodeReservationService);

        inOrder.verify(promoChangeLogService).start(promo, requestId);
        inOrder.verify(promoMechanicsService).insertPromo(ID, promo.getMechanicsParams());
        when(cifaceService.getSrcCifaceByPromoId(any())).thenReturn(null);
        inOrder.verify(constraintsService).saveForPromoId(eq(ID), eq(PromoFieldUtilsTest.createAll()), eq(CONSTRAINTS));
        verify(promoEventService).saveCreateEvent(ID);
        verify(promoSearchService).addPromoToSearch(ID, PromoFieldUtilsTest.createAll(), promo, CREATE_PROMO_EVENT);
        inOrder.verify(promocodeReservationService).reservePromocode(PROMOCODE.getCode());
        verifyNoMoreInteractions(promoMechanicsService);
    }


    @Disabled("Временно отключено резервирование промокодов")
    @Test
    void createPromo_promocodeReservationFailed_throws() {
        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        promo.getMechanicsParams().setPromocode(PROMOCODE);
        promo.getMainParams().setMechanicsType(MechanicsType.PROMO_CODE);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setConstraints(CONSTRAINTS);

        when(promoDao.insertPromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        when(promoEventService.saveCreateEvent(ID)).thenReturn(CREATE_PROMO_EVENT);
        doThrow(new PromocodeReservationException("Promocode occupied")).when(promocodeReservationService)
                .reservePromocode(PROMOCODE.getCode());

        // act
        PromocodeReservationException e = assertThrows(PromocodeReservationException.class,
                () -> promoService.createPromo(modifiedFields, promo, requestId));

        Assertions.assertEquals("Promocode occupied", e.getMessage());


        // verify
        InOrder inOrder =
                Mockito.inOrder(promoChangeLogService, promoDao, promoMechanicsService, cifaceService, constraintsService,
                        promocodeReservationService);

        inOrder.verify(promoChangeLogService).start(promo, requestId);
        inOrder.verify(promoMechanicsService).insertPromo(ID, promo.getMechanicsParams());
        when(cifaceService.getSrcCifaceByPromoId(any())).thenReturn(null);
        inOrder.verify(constraintsService).saveForPromoId(eq(ID), eq(PromoFieldUtilsTest.createAll()), eq(CONSTRAINTS));
        verify(promoEventService).saveCreateEvent(ID);
        verify(promoSearchService).addPromoToSearch(ID, PromoFieldUtilsTest.createAll(), promo, CREATE_PROMO_EVENT);
        inOrder.verify(promocodeReservationService).reservePromocode(PROMOCODE.getCode());
        verifyNoMoreInteractions(promoMechanicsService);
    }

    @Test
    public void updatePromoWithoutExtraFields() {

        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        when(promoDao.updatePromo(PROMO_ID, promo.getMainParams())).thenReturn(ID);
        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo.withId(ID)));
        when(promoMechanicsService.getPromoMechanicsParams(any())).thenReturn(PromoMechanicsParams.builder().build());
        when(cifaceService.getSrcCifaceByPromoId(any())).thenReturn(null);
        when(sskuService.getSskuByPromoId(ID)).thenReturn(Collections.emptySet());
        when(promoEventService.saveUpdateEvent(ID)).thenReturn(CREATE_PROMO_EVENT);

        // act and verify
        assertDoesNotThrow(() -> promoService.updatePromo(modifiedFields, promo, requestId));

        InOrder inOrder =
                Mockito.inOrder(promoChangeLogService, promoDao, promoMechanicsService, cifaceService, sskuService, constraintsService,
                        promoEventService, promoSearchService);

        inOrder.verify(promoChangeLogService).start(promo, requestId);
        inOrder.verify(promoDao).updatePromo(any(), any());
        inOrder.verify(promoMechanicsService).updatePromo(eq(ID), any());
        inOrder.verify(cifaceService).updateSrcCiface(ID, null);
        inOrder.verify(sskuService).saveSsku(ID, null, SSKU_DATA_EMPTY);
        inOrder.verify(promoEventService).saveUpdateEvent(ID);
        inOrder.verify(promoSearchService).updatePromoInSearch(ID, modifiedFields, promo, CREATE_PROMO_EVENT);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void updatePromoWithoutUrls() {

        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();

        promo.getMainParams().setLandingUrl(null);
        promo.getMainParams().setRulesUrl(null);

        when(promoDao.updatePromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo.withId(ID)));
        when(promoMechanicsService.getPromoMechanicsParams(any())).thenReturn(PromoMechanicsParams.builder().build());
        when(cifaceService.getSrcCifaceByPromoId(any())).thenReturn(null);
        when(sskuService.getSskuByPromoId(ID)).thenReturn(Collections.emptySet());
        when(promoEventService.saveUpdateEvent(ID)).thenReturn(CREATE_PROMO_EVENT);

        // act
        assertDoesNotThrow(() -> promoService.updatePromo(modifiedFields, promo, requestId));

        // verify
        InOrder inOrder =
                Mockito.inOrder(promoChangeLogService, promoDao, promoMechanicsService, cifaceService, sskuService, constraintsService,
                        promoEventService, promoSearchService);

        inOrder.verify(promoChangeLogService).start(promo, requestId);
        inOrder.verify(promoDao).updatePromo(promo.getPromoId(), promo.getMainParams());
        inOrder.verify(promoMechanicsService).updatePromo(ID, promo.getMechanicsParams());
        inOrder.verify(cifaceService).updateSrcCiface(ID, promo.getSrcParams().getCiface());
        inOrder.verify(sskuService).saveSsku(ID, null, SSKU_DATA_EMPTY);
        inOrder.verify(promoEventService).saveUpdateEvent(ID);
        inOrder.verify(promoSearchService).updatePromoInSearch(ID, modifiedFields, promo, CREATE_PROMO_EVENT);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void updateWithExtraFields() {

        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setSsku(SSKU);
        promo.setConstraints(CONSTRAINTS);

        when(promoDao.updatePromo(eq(PROMO_ID), any())).thenReturn(ID);
        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo.withId(ID)));
        when(promoMechanicsService.getPromoMechanicsParams(any())).thenReturn(PromoMechanicsParams.builder().cheapestAsGift(CHEAPEST_AS_GIFT).build());
        when(cifaceService.getSrcCifaceByPromoId(ID)).thenReturn(CIFACE_DTO);
        when(sskuService.getSskuByPromoId(ID)).thenReturn(SSKU);
        when(constraintsService.getByPromoId(eq(ID), any())).thenReturn(CONSTRAINTS);
        when(promoEventService.saveUpdateEvent(ID)).thenReturn(UPDATE_PROMO_EVENT);

        // act and verify
        assertDoesNotThrow(() -> promoService.updatePromo(modifiedFields, promo, requestId));

        InOrder inOrder =
                Mockito.inOrder(promoChangeLogService, promoDao, promoMechanicsService, cifaceService, sskuService, constraintsService,
                        promoSearchService, promoEventService);

        // update
        inOrder.verify(promoChangeLogService).start(promo, requestId);
        inOrder.verify(promoDao).getFullPromoByPromoId(PROMO_ID);
        inOrder.verify(promoDao).updatePromo(PROMO_ID, promo.getMainParams());
        inOrder.verify(promoMechanicsService).updatePromo(ID, promo.getMechanicsParams());
        inOrder.verify(cifaceService).updateSrcCiface(ID, CIFACE_DTO);
        inOrder.verify(sskuService).saveSsku(ID, SSKU, SSKU_DATA);
        inOrder.verify(constraintsService).updateForPromoId(eq(ID), eq(modifiedFields), any(Constraints.class));
        inOrder.verify(promoEventService).saveUpdateEvent(ID);
        inOrder.verify(promoSearchService).updatePromoInSearch(ID, modifiedFields, promo, UPDATE_PROMO_EVENT);

        verifyNoMoreInteractions(promoDao);
        verifyNoMoreInteractions(promoMechanicsService);
        verifyNoMoreInteractions(cifaceService);
        verifyNoMoreInteractions(sskuService);
        verifyNoMoreInteractions(constraintsService);
        verifyNoMoreInteractions(promoSearchService);
    }

    @Test
    public void updatePromo_throwExceptionInCheapestAsGiftDao() {

        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setSsku(SSKU);

        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo.withId(ID)));
        when(promoDao.updatePromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        doThrow(new RuntimeException("Some error")).when(promoMechanicsService).updatePromo(eq(ID), any());

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.updatePromo(modifiedFields, promo, requestId));

        // verify
        assertEquals("Some error", e.getMessage());

        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao).getFullPromoByPromoId(eq(PROMO_ID));
        verify(promoDao).updatePromo(eq(PROMO_ID), any());
        verifyNoMoreInteractions(promoDao);

        verify(promoMechanicsService).updatePromo(ID, promo.getMechanicsParams());
        verifyNoMoreInteractions(promoMechanicsService);

        verifyNoInteractions(cifaceService);
        verifyNoInteractions(sskuService);
    }

    @Test
    public void updatePromo_throwExceptionInCifaceDao() {

        // setup
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setSsku(SSKU);

        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo.withId(ID)));
        when(promoDao.updatePromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        doThrow(new RuntimeException("Some error")).when(cifaceService).updateSrcCiface(ID, CIFACE_DTO);

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.updatePromo(PromoFieldUtilsTest.createAll(), promo, requestId));

        // verify
        assertEquals("Some error", e.getMessage());

        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao).getFullPromoByPromoId(promo.getPromoId());
        verify(promoDao).updatePromo(promo.getPromoId(), promo.getMainParams());
        verifyNoMoreInteractions(promoDao);

        verify(promoMechanicsService).updatePromo(ID, promo.getMechanicsParams());
        verifyNoMoreInteractions(promoMechanicsService);

        verify(cifaceService).updateSrcCiface(ID, CIFACE_DTO);
        verifyNoMoreInteractions(cifaceService);

        verifyNoInteractions(sskuService);
    }

    @Test
    public void updatePromo_throwExceptionInSskuService() {

        // setup
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setSsku(SSKU);

        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo.withId(ID)));
        when(promoDao.updatePromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        doThrow(new RuntimeException("Some error")).when(sskuService).saveSsku(ID, SSKU, SSKU_DATA);

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.updatePromo(PromoFieldUtilsTest.createAll(), promo, requestId));

        // verify
        assertEquals("Some error", e.getMessage());

        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao).getFullPromoByPromoId(PROMO_ID);
        verify(promoDao).updatePromo(PROMO_ID, promo.getMainParams());
        verifyNoMoreInteractions(promoDao);

        verify(promoMechanicsService).updatePromo(ID, promo.getMechanicsParams());
        verifyNoMoreInteractions(promoSearchService);

        verify(cifaceService).updateSrcCiface(ID, CIFACE_DTO);
        verifyNoMoreInteractions(cifaceService);

        verify(sskuService).saveSsku(ID, SSKU, SSKU_DATA);
        verifyNoMoreInteractions(sskuService);
    }

    @Test
    public void updatePromo_throwExceptionInConstraintsService() {

        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);
        promo.getSrcParams().setCiface(CIFACE_DTO);
        promo.setSsku(SSKU);
        promo.setConstraints(CONSTRAINTS);

        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo.withId(ID)));
        when(promoDao.updatePromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        doThrow(new RuntimeException("Some error")).when(constraintsService).updateForPromoId(ID, PromoFieldUtilsTest.createAll(), CONSTRAINTS);

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.updatePromo(modifiedFields, promo, requestId));

        // verify
        assertEquals("Some error", e.getMessage());

        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao).getFullPromoByPromoId(promo.getPromoId());
        verify(promoDao).updatePromo(promo.getPromoId(), promo.getMainParams());
        verifyNoMoreInteractions(promoDao);

        verify(promoMechanicsService).updatePromo(ID, promo.getMechanicsParams());
        verifyNoMoreInteractions(promoMechanicsService);

        verify(cifaceService).updateSrcCiface(ID, CIFACE_DTO);
        verifyNoMoreInteractions(cifaceService);

        verify(sskuService).saveSsku(ID, SSKU, SSKU_DATA);
        verifyNoMoreInteractions(sskuService);

        verify(constraintsService).updateForPromoId(ID, PromoFieldUtilsTest.createAll(), CONSTRAINTS);
        verifyNoMoreInteractions(constraintsService);
    }

    @Test
    public void updatePromo_throwExceptionInSearchService() {

        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo.withId(ID)));
        when(promoDao.updatePromo(promo.getPromoId(), promo.getMainParams())).thenReturn(ID);
        when(promoEventService.saveUpdateEvent(ID)).thenReturn(UPDATE_PROMO_EVENT);
        doThrow(new RuntimeException("Some error in search service.")).when(promoSearchService)
                .updatePromoInSearch(ID, PromoFieldUtilsTest.createAll(), promo, UPDATE_PROMO_EVENT);

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> promoService.updatePromo(PromoFieldUtilsTest.createAll(), promo, requestId));

        // verify
        assertEquals("Some error in search service.", e.getMessage());
    }

    @Test
    public void updatePromo_throwExceptionOnModifyMechanicsType() {
        // setup
        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo
                        .withId(ID)
                        .withMainParams(promo.getMainParams().withMechanicsType(MechanicsType.BLUE_FLASH))
                )
        );

        // act and verify
        MechanicsTypeValidationException exception = assertThrows(MechanicsTypeValidationException.class,
                () -> promoService.updatePromo(PromoFieldUtilsTest.createAll(), promo, requestId));
        assertEquals("Change of the mechanicsType is not allowed", exception.getMessage());
    }

    @Test
    public void updatePromo_shouldNotRegenerateUrlsIfIncorrect() {
        // setup
        String landingUrlUrl = "landingUrl";
        boolean landingUrlAuto = false;
        String rulesUrlUrl = "landingUrl";
        boolean rulesUrlAuto = false;

        when(promoDao.getFullPromoByPromoId(any())).thenReturn(Optional.of(promo));

        promo = promo
                .withMainParams(promo.getMainParams()
                        .withLandingUrl(GenerateableUrl.builder()
                                .url(landingUrlUrl)
                                .auto(landingUrlAuto)
                                .build())
                        .withRulesUrl(GenerateableUrl.builder()
                                .url(rulesUrlUrl)
                                .auto(rulesUrlAuto)
                                .build())
                );

        // act
        promoService.updatePromo(PromoFieldUtilsTest.createAll(), promo, requestId);

        // verify
        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao).getFullPromoByPromoId(eq(PROMO_ID));
        verify(promoDao).updatePromo(eq(PROMO_ID), promoMainParamsArgumentCaptor.capture());
        assertEquals(landingUrlUrl, promoMainParamsArgumentCaptor.getValue().getLandingUrl().getUrl());
        assertEquals(rulesUrlUrl, promoMainParamsArgumentCaptor.getValue().getRulesUrl().getUrl());
    }

    @Test
    public void updatePromo_shouldRegenerateUrlsIfIncorrect() {
        // setup
        GenerateableUrl landingUrl = GenerateableUrl.builder()
                .url("landingUrl")
                .auto(true)
                .build();
        GenerateableUrl rulesUrl = GenerateableUrl.builder()
                .url("rulesUrl")
                .auto(true)
                .build();

        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo
                .withMainParams(promo.getMainParams()
                        .withLandingUrl(landingUrl)
                        .withRulesUrl(rulesUrl)
                )
        ));

        // act
        promoService.updatePromo(PromoFieldUtilsTest.createAll(), promo, requestId);

        // verify
        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao).updatePromo(eq(PROMO_ID), promoMainParamsArgumentCaptor.capture());
        assertFalse(StringUtils.isBlank(promoMainParamsArgumentCaptor.getValue().getLandingUrl().getUrl()));
        assertFalse(StringUtils.isBlank(promoMainParamsArgumentCaptor.getValue().getRulesUrl().getUrl()));
    }

    @Test
    public void updatePromo_shouldNotRegenerateUrlsIfCountChanged() {
        // setup
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);

        GenerateableUrl landingUrl = GenerateableUrl.builder()
                .url(generateLandingUrl(promo))
                .auto(false)
                .build();
        GenerateableUrl rulesUrl = GenerateableUrl.builder()
                .url(generateRulesUrl(promo))
                .auto(false)
                .build();

        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo
                .withMainParams(promo.getMainParams()
                        .withLandingUrl(landingUrl)
                        .withRulesUrl(rulesUrl)
                )
        ));

        promo.getMechanicsParams().getCheapestAsGift().setCount(promo.getMechanicsParams().getCheapestAsGift().getCount() + 1);
        promo.getMainParams().getLandingUrl().setAuto(false);
        promo.getMainParams().getRulesUrl().setAuto(false);

        String expectedLandingUrl = promo.getMainParams().getLandingUrl().getUrl();
        String expectedRulesUrl = promo.getMainParams().getRulesUrl().getUrl();

        // act
        promoService.updatePromo(PromoFieldUtilsTest.createAll(), promo, requestId);

        // verify
        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao).updatePromo(eq(PROMO_ID), promoMainParamsArgumentCaptor.capture());
        assertEquals(expectedLandingUrl, promoMainParamsArgumentCaptor.getAllValues().get(0).getLandingUrl().getUrl());
        assertEquals(expectedRulesUrl, promoMainParamsArgumentCaptor.getAllValues().get(0).getRulesUrl().getUrl());
    }

    @Test
    public void updatePromo_shouldRegenerateUrlsIfCountChanged() {
        // setup
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);

        GenerateableUrl landingUrl = GenerateableUrl.builder()
                .url(generateLandingUrl(promo))
                .auto(true)
                .build();
        GenerateableUrl rulesUrl = GenerateableUrl.builder()
                .url(generateRulesUrl(promo))
                .auto(true)
                .build();

        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo));

        promo.getMainParams().setLandingUrl(landingUrl);
        promo.getMainParams().setLandingUrl(rulesUrl);
        promo.getMechanicsParams().getCheapestAsGift().setCount(promo.getMechanicsParams().getCheapestAsGift().getCount() + 1);

        String expectedLandingUrl = generateLandingUrl(promo);
        String expectedRulesUrl = generateRulesUrl(promo);

        // act
        promoService.updatePromo(PromoFieldUtilsTest.createAll(), promo, requestId);

        // verify
        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao).getFullPromoByPromoId(eq(PROMO_ID));
        verify(promoDao).updatePromo(eq(PROMO_ID), promoMainParamsArgumentCaptor.capture());
        assertEquals(expectedLandingUrl, promoMainParamsArgumentCaptor.getValue().getLandingUrl().getUrl());
        assertEquals(expectedRulesUrl, promoMainParamsArgumentCaptor.getValue().getRulesUrl().getUrl());
    }

    @Test
    public void updatePromo_shouldNotRegenerateUrls() {
        // setup
        promo.getMechanicsParams().setCheapestAsGift(CHEAPEST_AS_GIFT);

        GenerateableUrl landingUrl = GenerateableUrl.builder()
                .url(generateLandingUrl(promo))
                .auto(true)
                .build();
        GenerateableUrl rulesUrl = GenerateableUrl.builder()
                .url(generateRulesUrl(promo))
                .auto(true)
                .build();

        promo.getMainParams().setLandingUrl(landingUrl);
        promo.getMainParams().setRulesUrl(rulesUrl);

        when(promoDao.getFullPromoByPromoId(PROMO_ID)).thenReturn(Optional.of(promo));

        // act
        promoService.updatePromo(PromoFieldUtilsTest.createAll(), promo, requestId);

        // verify
        verify(promoChangeLogService).start(promo, requestId);
        verify(promoDao, times(1)).updatePromo(eq(PROMO_ID), promoMainParamsArgumentCaptor.capture());
        assertEquals(landingUrl, promoMainParamsArgumentCaptor.getAllValues().get(0).getLandingUrl());
        assertEquals(rulesUrl, promoMainParamsArgumentCaptor.getAllValues().get(0).getRulesUrl());
    }
}
