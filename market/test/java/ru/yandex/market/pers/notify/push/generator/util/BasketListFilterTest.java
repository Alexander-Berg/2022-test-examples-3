package ru.yandex.market.pers.notify.push.generator.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yandex.market.pers.list.PersBasketAPI;
import ru.yandex.market.pers.list.model.BasketItem;
import ru.yandex.market.pers.notify.external.history.LightHistoryElement;
import ru.yandex.market.pers.notify.mail.generator.UserWithPayload;
import ru.yandex.market.pers.notify.mock.MockFactory;
import ru.yandex.market.pers.notify.model.UserModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         19.07.16
 */
public class BasketListFilterTest {
    private static final Random RND = new Random();
    private static final Long UID = 1L;
    private static final String UUID = "1";
    private BasketListFilter basketListFilter;
    private List<BasketItem> items = MockFactory.generateItems(100);
    
    @BeforeEach
    public void setUp() throws Exception {
        basketListFilter = new BasketListFilter();
        basketListFilter.persBasketClient = mock(PersBasketAPI.class);
        when(basketListFilter.persBasketClient.getItems(anyString(), any()))
                .thenAnswer(new Answer<List<BasketItem>>() {
                    @Override
                    public List<BasketItem> answer(InvocationOnMock invocation) throws Throwable {
                        return items;
                    }
                });
    }

    @Test
    public void testSimpleCase() throws Exception {
        assertNull(basketListFilter.apply(getUserWithPayload(UID, UUID, 0, 0)));
    }

    @Test
    public void testNotOneInBasket() throws Exception {
        assertNull(basketListFilter.apply(getUserWithPayload(UID, UUID, 100, 0)));
    }
    
    @Test
    public void testLeaveItemsFromBasket() throws Exception {
        UserWithPayload user = basketListFilter.apply(getUserWithPayload(UID, UUID, 100, 10));
        assertEquals(((List<LightHistoryElement>)user.getPayload()).size(), ((List<LightHistoryElement>)user.getPayload()).stream()
                .map(LightHistoryElement::getResourceId)
                .filter(this::modelIdInItems)
                .count());
        assertEquals(10, ((List<LightHistoryElement>)user.getPayload()).size());
    }

    @Test
    public void testRemoveItemsNotFromBasket() throws Exception {
        UserWithPayload user = basketListFilter.apply(getUserWithPayload(UID, UUID, 100, 100));
        assertTrue(((List<LightHistoryElement>)user.getPayload()).stream()
                .map(LightHistoryElement::getResourceId)
                .noneMatch(((Predicate<? super Long>) this::modelIdInItems).negate()));
    }

    private boolean modelIdInItems(long modelId) {
        boolean result = false;
        for (BasketItem item : items) {
            if (item.getType() == null) {
                int a = 1;
            }
            switch (item.getType()) {
                case OFFER:
                case MODEL:
                    result = result || Objects.equals(modelId, item.getModelId());
                    break;
                case GROUP:
                    result = result ||  Objects.equals(modelId, item.getGroupId());
                    break;
                case CLUSTER:
                    result = result ||  Objects.equals(modelId, item.getClusterId());
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    private long getModelId(BasketItem item) {
        switch (item.getType()) {
            case OFFER:
            case MODEL:
                return item.getModelId();
            case GROUP:
                return item.getGroupId();
            case CLUSTER:
                return item.getClusterId();
            default:
                return -1L;
        }
    }

    private UserWithPayload getUserWithPayload(Long uid, String uuid, int countNotInItems, int countInItems) {
        List<LightHistoryElement> payload = new ArrayList<>();
        
        for (int i = 0; i < countNotInItems; i++) {
            long modelId = RND.nextInt(100_000);
            while (modelIdInItems(modelId)) {
                modelId = RND.nextInt(100_000);
            }
            payload.add(new LightHistoryElement(uid, modelId, RND.nextLong(), RND.nextLong(), uuid, null, null));
        }

        for (int i = 0; i < countInItems; i++) {
            long modelId = getModelId(items.get(RND.nextInt(items.size())));
            payload.add(new LightHistoryElement(uid, modelId, RND.nextLong(), RND.nextLong(), uuid, null, null));
        }

        return new UserWithPayload(new UserModel(uid, uuid, null), payload);
    }
}
