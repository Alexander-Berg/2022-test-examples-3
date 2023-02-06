package ru.yandex.market.promoboss.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.promoboss.dao.SskuDao;
import ru.yandex.market.promoboss.model.MechanicsData;
import ru.yandex.market.promoboss.model.MechanicsType;
import ru.yandex.market.promoboss.model.SskuData;
import ru.yandex.market.promoboss.model.mechanics.CheapestAsGift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SskuServiceTest {

    private static final Long PROMO_ID_1 = 1000L;

    private static final String SSKU_1 = "ssku1";
    private static final String SSKU_2 = "ssku2";
    private static final String SSKU_3 = "ssku3";

    private static final SskuDao sskuDao = Mockito.mock(SskuDao.class);

    private static final SskuService sskuService = new SskuService(sskuDao);

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(sskuDao);
    }

    @Test
    public void shouldCallGetSskuByPromoId() {

        // setup
        Set<String> sskus = new HashSet<>() {{
            add(SSKU_1);
            add(SSKU_2);
        }};

        when(sskuDao.getSskuByPromoId(PROMO_ID_1)).thenReturn(sskus);

        // act
        Set<String> sskuByPromoId = sskuService.getSskuByPromoId(PROMO_ID_1);

        assertEquals(2, sskuByPromoId.size());
        assertEquals(sskus, sskuByPromoId);
    }

    @Test
    public void shouldCallGetSskuByPromoId_thrownException() {

        // setup
        when(sskuDao.getSskuByPromoId(PROMO_ID_1)).thenThrow(new RuntimeException("Some error"));

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> sskuService.getSskuByPromoId(PROMO_ID_1));

        assertEquals("Some error", e.getMessage());
    }

    @Test
    public void shouldCallGetPromoIdBySsku() {

        // setup
        Set<Long> promoIds = Collections.singleton(PROMO_ID_1);

        when(sskuDao.getPromoIdBySsku(SSKU_1)).thenReturn(promoIds);

        // act
        Set<Long> promoIdBySsku = sskuService.getPromoIdBySsku(SSKU_1);

        assertEquals(1, promoIdBySsku.size());
        assertEquals(promoIds, promoIdBySsku);
    }

    @Test
    public void shouldCallGetPromoIdBySsku_thrownException() {

        // setup
        when(sskuDao.getPromoIdBySsku(SSKU_1)).thenThrow(new RuntimeException("Some error"));

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> sskuService.getPromoIdBySsku(SSKU_1));

        assertEquals("Some error", e.getMessage());
    }

    @Test
    public void shouldDeleteOldAndInsertNewSsku() {

        // setup
        Set<String> oldSsku = new HashSet<>() {{
            add(SSKU_1);
            add(SSKU_2);
        }};

        Set<String> newSsku = new HashSet<>() {{
            add(SSKU_2);
            add(SSKU_3);
        }};

        SskuData sskuData = createSskuData();

        when(sskuDao.getSskuByPromoId(PROMO_ID_1)).thenReturn(oldSsku);

        // act
        sskuService.saveSsku(PROMO_ID_1, newSsku, sskuData);

        // verify
        verify(sskuDao).getSskuByPromoId(PROMO_ID_1);

        verify(sskuDao).deleteSskuWithPromoId(Set.of(SSKU_1), PROMO_ID_1);
        verify(sskuDao).insertSskuWithPromoId(Set.of(SSKU_3), PROMO_ID_1, sskuData);
    }

    private SskuData createSskuData() {
        MechanicsData mechanicsData = new MechanicsData(MechanicsType.CHEAPEST_AS_GIFT, new CheapestAsGift(3));
        return new SskuData(mechanicsData);
    }

    @Test
    public void shouldDeleteOldAndInsertNewSsku_oldIsEmpty() {

        // setup
        Set<String> oldSsku = Collections.emptySet();

        Set<String> newSsku = new HashSet<>() {{
            add(SSKU_2);
            add(SSKU_3);
        }};

        SskuData sskuData = createSskuData();

        when(sskuDao.getSskuByPromoId(PROMO_ID_1)).thenReturn(oldSsku);

        // act
        sskuService.saveSsku(PROMO_ID_1, newSsku, sskuData);

        // verify
        verify(sskuDao).getSskuByPromoId(PROMO_ID_1);

        verify(sskuDao).insertSskuWithPromoId(newSsku, PROMO_ID_1, sskuData);
    }

    @Test
    public void shouldDeleteOldAndInsertNewSsku_newIsEmpty() {

        // setup
        Set<String> oldSsku = new HashSet<>() {{
            add(SSKU_1);
            add(SSKU_2);
        }};
        Set<String> newSsku = Collections.emptySet();

        SskuData sskuData = createSskuData();

        when(sskuDao.getSskuByPromoId(PROMO_ID_1)).thenReturn(oldSsku);

        // act
        sskuService.saveSsku(PROMO_ID_1, newSsku, sskuData);

        // verify
        verify(sskuDao).getSskuByPromoId(PROMO_ID_1);

        verify(sskuDao).deleteSskuWithPromoId(oldSsku, PROMO_ID_1);
        verify(sskuDao).updateSskuDataByPromoId(PROMO_ID_1, sskuData);
    }

    @Test
    public void shouldDeleteOldAndInsertNewSsku_newIsNull() {

        // setup
        Set<String> oldSsku = new HashSet<>() {{
            add(SSKU_1);
            add(SSKU_2);
        }};
        Set<String> newSsku = null;

        SskuData sskuData = createSskuData();

        when(sskuDao.getSskuByPromoId(PROMO_ID_1)).thenReturn(oldSsku);

        // act
        sskuService.saveSsku(PROMO_ID_1, newSsku, sskuData);

        // verify
        verify(sskuDao).getSskuByPromoId(PROMO_ID_1);

        verify(sskuDao).deleteSskuWithPromoId(oldSsku, PROMO_ID_1);
        verify(sskuDao).updateSskuDataByPromoId(PROMO_ID_1, sskuData);
    }

    @Test
    public void shouldDeleteOldAndInsertNewSsku_thrownException() {

        // setup
        when(sskuDao.getSskuByPromoId(PROMO_ID_1)).thenThrow(new RuntimeException("Some error"));

        // act
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> sskuService.saveSsku(PROMO_ID_1, Set.of(SSKU_1), createSskuData()));

        assertEquals("Some error", e.getMessage());
    }
}
