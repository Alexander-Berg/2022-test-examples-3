package ru.yandex.market.mbo.catalogue.listeners;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.catalogue.category.CategoryManagersManager;
import ru.yandex.market.mbo.catalogue.model.UpdateAttributesEventParams;
import ru.yandex.market.mbo.core.kdepot.api.KnownEntityTypes;
import ru.yandex.market.mbo.tt.events.Event;
import ru.yandex.market.mbo.tt.events.EventManagerImpl;
import ru.yandex.market.mbo.user.UserManager;
import ru.yandex.market.mbo.user.UserManagerMock;
import ru.yandex.market.mbo.user.UserRolesManager;
import ru.yandex.market.mbo.user.UserRolesManagerMock;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author york
 * @since 01.02.2019
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CategoryDeleteListenerTest {
    private static final long UID1 = 1;
    private static final long UID2 = 2;
    private static final long UID3 = 3;
    private static final long CATEGORY_ID1 = 1000L;
    private static final long CATEGORY_ID2 = 1001L;

    private UserManager userManager;
    private UserRolesManager userRolesManager;
    private CategoryManagersManager categoryManagersManager;
    private EventManagerImpl eventManager;

    @Before
    public void init() {
       userManager = new UserManagerMock();
       userManager.setCategorySuperoperator(UID1, 2L, CATEGORY_ID1);
       userManager.setCategorySuperoperator(UID2, 3L, CATEGORY_ID2);
       userRolesManager = new UserRolesManagerMock(userManager);
       categoryManagersManager = Mockito.mock(CategoryManagersManager.class);
       CategoryDeleteListener listener = new CategoryDeleteListener();
       listener.setUserRolesManager(userRolesManager);
       listener.setCategoryManagersManager(categoryManagersManager);
       eventManager = new EventManagerImpl(null);
       eventManager.setEventListeners(Collections.singletonList(listener));
    }

    @Test
    public void testDiffType() {
        int size = userManager.getSuperUserPerCategory().size();
        eventManager.fireEventAndGetAuditActions(Event.ENTITY_DELETED,
            prms(CATEGORY_ID1, KnownEntityTypes.MARKET_MODEL)
        );
        Assert.assertEquals(size, userManager.getSuperUserPerCategory().size());
    }

    @Test
    public void testDiffActionType() {
        int size = userManager.getSuperUserPerCategory().size();
        eventManager.fireEventAndGetAuditActions(Event.ENTITY_CREATED,
            prms(CATEGORY_ID1, KnownEntityTypes.MARKET_CATEGORY)
        );
        Assert.assertEquals(size, userManager.getSuperUserPerCategory().size());
    }

    @Test
    public void testCleared() {
        int size = userManager.getSuperUserPerCategory().size();
        Assert.assertTrue(getCategoriesWithSupers().contains(CATEGORY_ID1));

        eventManager.fireEventAndGetAuditActions(Event.ENTITY_DELETED,
            prms(CATEGORY_ID1, KnownEntityTypes.MARKET_CATEGORY)
        );
        Assert.assertEquals(size - 1, userManager.getSuperUserPerCategory().size());
        Assert.assertFalse(getCategoriesWithSupers().contains(CATEGORY_ID1));
        Mockito.verify(categoryManagersManager).deleteCategory(CATEGORY_ID1);
    }

    private List<Long> getCategoriesWithSupers() {
        return userManager.getSuperUserPerCategory().stream()
            .map(Pair::getSecond)
            .collect(Collectors.toList());
    }

    private UpdateAttributesEventParams prms(long entityId, long entityTypeId) {
        return new UpdateAttributesEventParams(entityId, entityTypeId, 1,
            Collections.emptyMap(), Collections.emptyMap());
    }
}
