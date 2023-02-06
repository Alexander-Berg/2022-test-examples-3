package ru.yandex.market.pers.author;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.agitation.AgitationService;
import ru.yandex.market.pers.author.agitation.model.Agitation;
import ru.yandex.market.pers.author.agitation.model.AgitationUser;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.author.client.api.model.AgitationUserType;
import ru.yandex.market.pers.author.mock.PersAuthorSaasMocks;
import ru.yandex.market.pers.author.mock.mvc.AgitationBeruMvcMocks;
import ru.yandex.market.pers.author.mock.mvc.AgitationOrderMvcMocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pers.author.client.api.model.AgitationCancelReason.CANCEL;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.ORDER_CANCELLATION_REJECTED_BY_SHOP;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.ORDER_CONFIRM_DELIVERY_DATES_MOVED_BY_USER;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.ORDER_DELIVERY_DATES_MOVED_BY_SHOP;
import static ru.yandex.market.pers.author.client.api.model.AgitationType.ORDER_ITEM_REMOVED;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 08.07.2020
 */
public class AgitationBeruControllerTest extends AbstractAgitationControllerTest {
    private static final String ORDER_ID = "4524551";

    @Autowired
    private PersAuthorSaasMocks authorSaasMocks;

    @Autowired
    private AgitationBeruMvcMocks agitationMvc;

    @Autowired
    private AgitationOrderMvcMocks agitationOrderMvc;

    @Autowired
    private AgitationService agitationService;

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasNotification(AgitationUser user) {
        // check, that works even with limit
        agitationService.limitUserAgitations(user, Duration.ofDays(2));

        // generate agitations for 2-3 types in saas.
        // required only in this test. Other tests could be little bit simple, containing less types and no paging
        authorSaasMocks.mockAgitation(user, Map.of(ORDER_ITEM_REMOVED, List.of(ORDER_ID, ORDER_ID + 1, ORDER_ID + 2)));

        // check popup works
        List<Agitation> notification = agitationMvc.getNotification(user, ORDER_ITEM_REMOVED);
        assertAgitationIds(notification,
            Agitation.buildAgitationId(ORDER_ITEM_REMOVED, ORDER_ID),
            Agitation.buildAgitationId(ORDER_ITEM_REMOVED, ORDER_ID + 1)
        );

        // check cache - reset mocks - same result
        resetMocks();
        notification = agitationMvc.getNotification(user, ORDER_ITEM_REMOVED);
        assertAgitationIds(notification,
            Agitation.buildAgitationId(ORDER_ITEM_REMOVED, ORDER_ID),
            Agitation.buildAgitationId(ORDER_ITEM_REMOVED, ORDER_ID + 1)
        );

        // resets after cache invalidation
        invalidateCache();
        notification = agitationMvc.getNotification(user, ORDER_ITEM_REMOVED);
        assertEquals(0, notification.size());
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationSaasAndCancel(AgitationUser user) {
        // three agitations: one simple, one canceled, one canceled and expired
        // only two should work

        // generate saas data
        authorSaasMocks.mockAgitation(user,
            Map.of(ORDER_ITEM_REMOVED, List.of(ORDER_ID, ORDER_ID + 1, ORDER_ID + 2)));

        // cancel agitations
        agitationMvc.cancel(user, Agitation.buildAgitationId(ORDER_ITEM_REMOVED, ORDER_ID + 1), CANCEL);
        agitationMvc.cancel(user, Agitation.buildAgitationId(ORDER_ITEM_REMOVED, ORDER_ID + 2), CANCEL);

        disableAgitationCancel(user, Agitation.buildAgitationId(ORDER_ITEM_REMOVED, ORDER_ID + 2));

        // load agitations
        List<Agitation> notification = agitationMvc.getNotification(user, ORDER_ITEM_REMOVED);
        assertAgitationIds(notification,
            Agitation.buildAgitationId(ORDER_ITEM_REMOVED, ORDER_ID),
            Agitation.buildAgitationId(ORDER_ITEM_REMOVED, ORDER_ID + 2)
        );
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testAgitationAddCompleteAndNext(AgitationUser user) {
        // one agitation in saas, one completed
        // expect two agitations

        // generate saas data
        authorSaasMocks.mockAgitation(user, Map.of(ORDER_ITEM_REMOVED, List.of(ORDER_ID)));

        agitationOrderMvc.addOrderAgitation(user, ORDER_ITEM_REMOVED, ORDER_ID + 2, true);
        agitationOrderMvc.addOrderAgitation(user, ORDER_ITEM_REMOVED, ORDER_ID + 3, true);

        // complete some agitations
        agitationMvc.complete(user, Agitation.buildAgitationId(ORDER_ITEM_REMOVED, ORDER_ID + 2));

        // check agitations
        // load with popup to show all in single request
        List<Agitation> popup = agitationMvc.getNotification(user, AgitationType.values());
        assertAgitationIds(popup,
                Agitation.buildAgitationId(ORDER_ITEM_REMOVED, ORDER_ID + 3),
                Agitation.buildAgitationId(ORDER_ITEM_REMOVED, ORDER_ID)
        );
    }

    @Test
    public void testUnsupportedMediaType() {
        agitationOrderMvc.addOrderAgitationIncorrectMediaType(AgitationUser.uid(123), ORDER_ITEM_REMOVED,
                ORDER_ID + 2, true);
    }

    @ParameterizedTest
    @MethodSource("allUserTypes")
    public void testGetAgitationByOrderId(AgitationUser user) {
        // four agitations in saas
        // expect two agitations

        // generate saas data
        authorSaasMocks.mockAgitation(user, Map.of(
                ORDER_CANCELLATION_REJECTED_BY_SHOP, List.of(ORDER_ID, ORDER_ID + 1),
                ORDER_DELIVERY_DATES_MOVED_BY_SHOP, List.of(ORDER_ID),
                ORDER_CONFIRM_DELIVERY_DATES_MOVED_BY_USER, List.of(ORDER_ID + 1)));

        // check agitations
        // load with popup to show all in single request
        List<Agitation> popup = agitationMvc.getAgitationsByOrderId(
                user,
                Long.parseLong(ORDER_ID),
                ORDER_CANCELLATION_REJECTED_BY_SHOP,
                ORDER_DELIVERY_DATES_MOVED_BY_SHOP);
        assertAgitationIdsInAnyOrder(popup,
                Agitation.buildAgitationId(ORDER_CANCELLATION_REJECTED_BY_SHOP, ORDER_ID),
                Agitation.buildAgitationId(ORDER_DELIVERY_DATES_MOVED_BY_SHOP, ORDER_ID)
        );
    }

    public static Stream<Arguments> allUserTypes() {
        return Arrays.stream(AgitationUserType.values())
            .map(userType -> {
                switch (userType) {
                    case UID:
                        return AgitationUser.uid(32431314);
                    case YANDEXUID:
                        return AgitationUser.yandexUid("ABC1334134ZZZ");
                    default:
                        throw new IllegalArgumentException("Invalid user type: " + userType);
                }
            })
            .map(Arguments::of);
    }
}
