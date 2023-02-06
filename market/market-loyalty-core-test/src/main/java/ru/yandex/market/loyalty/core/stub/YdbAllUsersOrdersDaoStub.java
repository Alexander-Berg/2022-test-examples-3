package ru.yandex.market.loyalty.core.stub;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.loyalty.core.dao.ydb.AllUserOrdersDao;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserOrder;

public class YdbAllUsersOrdersDaoStub implements AllUserOrdersDao, StubDao {
    public static final long USER_WITH_ORDER = 234L;
    private static final Set<String> MOBILE_APP_PLATFORMS = Set.of(Platform.IOS.name(), Platform.ANDROID.name());

    private final List<UserOrder> savedOrders = new LinkedList<>();

    private Map<Long, Set<UserOrder>> userOrdersMap;

    @Override
    public void upsert(UserOrder newRow) {
        savedOrders.add(newRow);
        addToOrdersMap(newRow);
    }

    @Override
    public void updateStatus(UserOrder row) {
        for (int i = 0; i < savedOrders.size(); i++) {
            if (equalsOrder(savedOrders.get(i), row)) {
                savedOrders.remove(i);
                savedOrders.add(row);
                return;
            }
        }
    }

    private static boolean equalsOrder(UserOrder current, UserOrder target) {
        return current.getUid()
                .equals(target.getUid()) &&
                current.getBindingKey()
                        .equals(target.getBindingKey());
    }

    @Override
    public List<UserOrder> selectByUidWithNotCancelledStatus(Long uid) {
        return userOrdersMap.getOrDefault(uid, Collections.emptySet()).stream()
                .filter(order -> !order.getStatus().equals("CANCELLED"))
                .collect(Collectors.toList());
    }

    @Override
    public Set<Long> findOrdersForWelcomePromo(long uid, Instant promoStartDate, Instant startDateOfOrdersChecking) {
        return Stream.concat(
                savedOrders.stream()
                        .filter(userOrder -> userOrder.getUid()
                                .equals(uid))
                        .filter(userOrder -> !userOrder.getCreationTime()
                                .isBefore(promoStartDate))
                        .filter(userOrder -> userOrder.getCreationTime()
                                .isAfter(startDateOfOrdersChecking))
                        .filter(userOrder -> MOBILE_APP_PLATFORMS.contains(userOrder.getPlatform()))
                        .collect(Collectors.groupingBy(
                                userOrder -> userOrder.hasMultiOrderId()
                                        ? userOrder.getMultiOrderId()
                                        : userOrder.getOrderId()
                        ))
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue()
                                .stream()
                                .noneMatch(userOrder -> userOrder.getStatus()
                                        .equals("CANCELLED")))
                        .flatMap(e -> e.getValue()
                                .stream())
                        .map(UserOrder::getOrderId)
                        .map(Long::parseLong)
                        .distinct(),
                savedOrders.stream()
                        .filter(userOrder -> userOrder.getUid()
                                .equals(uid))
                        .filter(userOrder -> userOrder.getCreationTime()
                                .isBefore(promoStartDate))
                        .filter(userOrder -> userOrder.getCreationTime()
                                .isAfter(startDateOfOrdersChecking))
                        .filter(userOrder -> MOBILE_APP_PLATFORMS.contains(userOrder.getPlatform()))
                        .map(UserOrder::getOrderId)
                        .map(Long::parseLong)
                        .distinct()
        )
                .collect(Collectors.toSet());
    }

    @Override
    public List<UserOrder> getPreviousYearDeliveredOrders(long uid) {
        return
                savedOrders.stream()
                        .filter(userOrder -> userOrder.getUid()
                                .equals(uid))
                        .filter(userOrder -> "DELIVERED".equals(userOrder.getStatus()))
                        .filter(userOrder -> userOrder.getCreationTime()
                                .isAfter(Instant.now()
                                        .minus(Duration.ofDays(365))))
                        .distinct()
                        .collect(Collectors.toList());
    }

    @Override
    public List<UserOrder> selectByPK(Long uid, String bindingKey) {
        return Lists.newArrayList(userOrdersMap.get(uid));
    }

    @Override
    public void clear() {
        savedOrders.clear();
        reloadOrdersMap();
    }

    public void addToOrdersMap(UserOrder order) {
        userOrdersMap.compute(order.getUid(), (uid, orders) -> {
            if (orders == null) {
                return Sets.newHashSet(order);
            } else {
                orders.add(order);
                return orders;
            }
        });
    }

    @PostConstruct
    public void reloadOrdersMap() {
        userOrdersMap = Maps.newHashMap(Map.of(
                123L, new HashSet<>(),
                USER_WITH_ORDER, Sets.newHashSet(new UserOrder(
                        USER_WITH_ORDER, "PROCESSING", Instant.now(), "binding-key", Platform.DESKTOP
                ))
        ));
    }
}
