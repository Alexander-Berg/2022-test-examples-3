package ru.yandex.market.pvz.core.test.factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.pvz.core.domain.consumable.ConsumableMapper;
import ru.yandex.market.pvz.core.domain.consumable.type.ConsumablePickupPointType;
import ru.yandex.market.pvz.core.domain.consumable.type.ConsumableType;
import ru.yandex.market.pvz.core.domain.consumable.type.ConsumableTypeParams;
import ru.yandex.market.pvz.core.domain.consumable.type.ConsumableTypeRepository;
import ru.yandex.market.tpl.common.util.RandomUtil;

@Slf4j
@Transactional
public class TestConsumableTypeFactory extends TestObjectFactory {

    @Autowired
    private ConsumableTypeRepository consumableTypeRepository;

    @Autowired
    private ConsumableMapper mapper;

    public ConsumableTypeParams create() {
        return create(ConsumableTypeTestParams.builder().build());
    }

    public ConsumableTypeParams create(ConsumableTypeTestParams params) {
        return mapper.map(consumableTypeRepository.save(new ConsumableType(
                params.getName(),
                params.getTicketTag(),
                params.getOrderingPeriodDays(),
                params.getCountPerPeriod(),
                params.isShipWithFirstOrder(),
                new ArrayList<>(params.getAvailableToPickupPointTypes()),
                new ArrayList<>(params.getAvailableToPickupPointIds())
        )));
    }

    @Data
    @Builder
    public static class ConsumableTypeTestParams {

        private static final List<String> CONSUMABLE_NAMES = List.of(
                "Скотч на рот",
                "Пакет на голову",
                "Сейф-пакет для контрабанды",
                "Нижнее бельё для курьера",
                "Наклейка на лоб оператору ПВЗ"
        );

        private static final int DEFAULT_ORDERING_PERIOD_DAYS = 7;
        private static final int DEFAULT_COUNT_PER_PERIOD = 3;
        private static final boolean DEFAULT_SHIP_WITH_FIRST_ORDER = false;

        @Builder.Default
        private String name = RandomUtil.getRandomFromList(CONSUMABLE_NAMES);

        @Builder.Default
        private String ticketTag = randomString(10);

        @Builder.Default
        private int orderingPeriodDays = DEFAULT_ORDERING_PERIOD_DAYS;

        @Builder.Default
        private int countPerPeriod = DEFAULT_COUNT_PER_PERIOD;

        @Builder.Default
        private boolean shipWithFirstOrder = DEFAULT_SHIP_WITH_FIRST_ORDER;

        @Builder.Default
        private List<ConsumablePickupPointType> availableToPickupPointTypes =
                Arrays.asList(ConsumablePickupPointType.values());

        @Builder.Default
        private List<Long> availableToPickupPointIds = List.of();

    }

}
