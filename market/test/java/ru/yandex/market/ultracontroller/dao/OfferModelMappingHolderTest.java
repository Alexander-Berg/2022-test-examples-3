package ru.yandex.market.ultracontroller.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * @author inenakhov
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class OfferModelMappingHolderTest {
    @Mock
    private OfferModelMappingDao offerModelMappingDao;

    private final int addActionsInDb = 10;
    private final int removeActionsInDb = 3;
    private final ArrayList<OfferModelMappingDao.OfferModelMappingAction> normalActions = new ArrayList<>();
    private final ArrayList<OfferModelMappingDao.OfferModelMappingAction> extraRemoveAction = new ArrayList<>();
    private final ArrayList<OfferModelMappingDao.OfferModelMappingAction> extraAddAction = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < addActionsInDb; i++) {
            normalActions.add(createMappingAction(i, OfferModelMappingDao.Action.ADD));
        }

        for (int i = 0; i < removeActionsInDb; i++) {
            normalActions.add(createMappingAction(i, OfferModelMappingDao.Action.REMOVE));
        }

        extraAddAction.addAll(normalActions);
        extraAddAction.add(createMappingAction(removeActionsInDb + 1, OfferModelMappingDao.Action.ADD));
        extraRemoveAction.addAll(normalActions);
        extraRemoveAction.add(createMappingAction(removeActionsInDb - 1, OfferModelMappingDao.Action.REMOVE));
    }

    @Test
    public void get() throws Exception {
        when(offerModelMappingDao.loadAllMappingActions(anyInt())).thenReturn(normalActions);

        OfferModelMappingHolder holder = new OfferModelMappingHolder();
        holder.setOfferModelMappingDao(offerModelMappingDao);

        holder.doReload();

        assertEquals(createMapping(addActionsInDb - 1), holder.get(String.valueOf(addActionsInDb - 1)).get());
    }

    @Test
    public void getActualGoodIds() throws Exception {
        when(offerModelMappingDao.loadAllMappingActions(anyInt())).thenReturn(normalActions);

        OfferModelMappingHolder holder = new OfferModelMappingHolder();
        holder.setOfferModelMappingDao(offerModelMappingDao);

        holder.doReload();

        assertEquals(addActionsInDb - removeActionsInDb, holder.getActualGoodIds().size());
        for (int i = removeActionsInDb; i < addActionsInDb; i++) {
            holder.getActualGoodIds().contains(String.valueOf(i));
        }
    }

    @Test
    public void doReload1() throws Exception {
        when(offerModelMappingDao.loadAllMappingActions(anyInt())).thenReturn(normalActions);

        OfferModelMappingHolder holder = new OfferModelMappingHolder();
        holder.setOfferModelMappingDao(offerModelMappingDao);

        holder.doReload();

        assertEquals(addActionsInDb - removeActionsInDb, holder.getActualGoodIds().size());
    }

    @Test
    public void doReload2() throws Exception {
        when(offerModelMappingDao.loadAllMappingActions(anyInt())).thenReturn(extraAddAction);

        OfferModelMappingHolder holder = new OfferModelMappingHolder();
        holder.setOfferModelMappingDao(offerModelMappingDao);

        holder.doReload();

        assertEquals(addActionsInDb - removeActionsInDb, holder.getActualGoodIds().size());
    }

    @Test
    public void doReload3() throws Exception {
        when(offerModelMappingDao.loadAllMappingActions(anyInt())).thenReturn(extraRemoveAction);

        OfferModelMappingHolder holder = new OfferModelMappingHolder();
        holder.setOfferModelMappingDao(offerModelMappingDao);

        holder.doReload();

        assertEquals(addActionsInDb - removeActionsInDb, holder.getActualGoodIds().size());
    }

    private OfferModelMapping createMapping(int id) {
        return new OfferModelMapping(String.valueOf(id), id, id);
    }

    private OfferModelMappingDao.OfferModelMappingAction createMappingAction(int id,
                                                                             OfferModelMappingDao.Action action) {
        return new OfferModelMappingDao.OfferModelMappingAction(id, createMapping(id), action);
    }
}
