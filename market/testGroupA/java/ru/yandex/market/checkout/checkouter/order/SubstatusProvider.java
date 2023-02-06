package ru.yandex.market.checkout.checkouter.order;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PACKAGING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.READY_TO_SHIP;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.STARTED;

/**
 * Удобный способ подстановки списка возможных статусов для параметризованных тестов
 */
public class SubstatusProvider {

    private static final Set<OrderSubstatus> BEFORE_READY_TO_SHIP_STATUSES =
            Set.of(STARTED, PACKAGING);
    private final SubstatusProvider.ProvideType type;
    private final List<OrderSubstatus> substatuses;

    private SubstatusProvider(SubstatusProvider.ProvideType type,
                              List<OrderSubstatus> substatuses) {
        this.substatuses = substatuses;
        this.type = type;
    }

    public static SubstatusProvider anySubstatuses() {
        return new SubstatusProvider(SubstatusProvider.ProvideType.ANY, List.of());
    }

    public static SubstatusProvider onlySubstatuses(OrderSubstatus... substatuses) {
        return new SubstatusProvider(SubstatusProvider.ProvideType.ONLY, Arrays.asList(substatuses));
    }

    public static SubstatusProvider withoutSubstatuses(OrderSubstatus... substatuses) {
        return new SubstatusProvider(SubstatusProvider.ProvideType.WITHOUT, Arrays.asList(substatuses));
    }

    public static SubstatusProvider afterReadyToShip() {
        List<OrderSubstatus> substatuses = Arrays.stream(OrderSubstatus.values())
                .filter(substatus -> substatus.getStatus() == OrderStatus.PROCESSING)
                .filter(substatus -> !BEFORE_READY_TO_SHIP_STATUSES.contains(substatus))
                .filter(substatus -> substatus != READY_TO_SHIP)
                .collect(Collectors.toList());
        return new SubstatusProvider(ProvideType.ONLY_PROCESSING,
                substatuses);
    }

    public static SubstatusProvider afterOrAtReadyToShip() {
        List<OrderSubstatus> substatuses = Arrays.stream(OrderSubstatus.values())
                .filter(substatus -> substatus.getStatus() == OrderStatus.PROCESSING)
                .filter(substatus -> !BEFORE_READY_TO_SHIP_STATUSES.contains(substatus))
                .collect(Collectors.toList());
        return new SubstatusProvider(ProvideType.ONLY_PROCESSING,
                substatuses);
    }

    public static SubstatusProvider beforeReadyToShip() {
        List<OrderSubstatus> substatuses = Arrays.stream(OrderSubstatus.values())
                .filter(substatus -> substatus.getStatus() == OrderStatus.PROCESSING)
                .filter(BEFORE_READY_TO_SHIP_STATUSES::contains)
                .collect(Collectors.toList());
        return new SubstatusProvider(ProvideType.ONLY_PROCESSING,
                substatuses);
    }

    public static SubstatusProvider beforeOrAtReadyToShip() {
        List<OrderSubstatus> substatuses = Arrays.stream(OrderSubstatus.values())
                .filter(substatus -> substatus.getStatus() == OrderStatus.PROCESSING)
                .filter(BEFORE_READY_TO_SHIP_STATUSES::contains)
                .collect(Collectors.toList());
        substatuses.add(READY_TO_SHIP);
        return new SubstatusProvider(ProvideType.ONLY_PROCESSING,
                substatuses);
    }

    /**
     * Отдает список подстатусов подходящих под тип генерации {@link ProvideType} и конкретного статуса
     */
    public List<OrderSubstatus> getSubstatusesFor(OrderStatus status) {
        if (type == SubstatusProvider.ProvideType.ANY) {
            List<OrderSubstatus> result = Arrays.stream(OrderSubstatus.values())
                    .filter(substatus -> substatus.getStatus() == status)
                    .collect(Collectors.toList());
            result.add(null);
            return result;
        }
        if (type == SubstatusProvider.ProvideType.WITHOUT) {
            List<OrderSubstatus> result = Arrays.stream(OrderSubstatus.values())
                    .filter(substatus -> substatus.getStatus() == status)
                    .filter(substatus -> !substatuses.contains(substatus))
                    .collect(Collectors.toList());
            if (!substatuses.contains(null)) {
                result.add(null);
            }
            return result;
        }
        if (type == ProvideType.ONLY_PROCESSING) {
            if (status != OrderStatus.PROCESSING) {
                throw new IllegalArgumentException("Order status != PROCESSING");
            }
        }
        return substatuses;
    }

    @Override
    public String toString() {
        if (type == SubstatusProvider.ProvideType.ANY) {
            return "any substatuses";
        }
        if (type == SubstatusProvider.ProvideType.WITHOUT) {
            return "without substatuses " + substatuses;
        }
        return "only substatuses " + substatuses;
    }

    private enum ProvideType {
        ANY, ONLY, WITHOUT, ONLY_PROCESSING
    }
}
