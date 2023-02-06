package ru.yandex.market.ocrm.module.order;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.antifraud.orders.client.MstatAntifraudCrmClient;
import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlockingEvent;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlockingEventGroup;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlockingType;
import ru.yandex.market.antifraud.orders.web.dto.crm.BuyerBlockingGroupsDto;
import ru.yandex.market.antifraud.orders.web.dto.crm.RuleTriggerEvent;
import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.ocrm.module.order.domain.AntifraudBlockingEvent;
import ru.yandex.market.ocrm.module.order.domain.AntifraudBlockingEventGroup;

import static ru.yandex.market.ocrm.module.order.impl.antifraud.AntifraudBlockingEventGroupEntityStorageStrategy.BLOCKING_TYPE_FILTER;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleOrderTestConfiguration.class)
public class AntifraudBlockingEventGroupTest {

    @Inject
    private EntityStorageService entityStorageService;

    @Inject
    private MstatAntifraudCrmClient antifraudCrmClient;

    @Test
    @Transactional
    public void testList() {
        Long uid = Randoms.positiveLongValue();
        BuyerBlockingGroupsDto groupsDto = createBuyerBlockingGroupsDto(uid);

        Mockito.when(antifraudCrmClient.getBlockingGroups(
                        Mockito.eq(uid),
                        Mockito.any(BlockingType.class),
                        Mockito.any(Instant.class),
                        Mockito.any(Instant.class),
                        Mockito.anyInt(),
                        Mockito.anyInt()
                ))
                .thenReturn(groupsDto);

        List<AntifraudBlockingEventGroup> groups = getBlockingEventGroups(uid);
        checkGroups(groupsDto.getBlockingGroups(), groups);
    }

    private BuyerBlockingGroupsDto createBuyerBlockingGroupsDto(Long uid) {
        List<BlockingEventGroup> blockingGroups = List.of(
                createBlockingEventGroup(uid),
                createBlockingEventGroup(uid)
        );
        BuyerBlockingGroupsDto groupsDto = new BuyerBlockingGroupsDto(
                uid,
                Randoms.longValue(),
                Randoms.intValue(),
                Randoms.intValue(),
                BlockingType.ALL,
                blockingGroups
        );
        return groupsDto;
    }

    private BlockingEventGroup createBlockingEventGroup(Long uid) {
        BlockingEventGroup group = new BlockingEventGroup(
                uid,
                Randoms.dateTime().toInstant(),
                Randoms.dateTime().toInstant(),
                Randoms.intValue(),
                Set.of(AntifraudAction.CANCEL_PROMO_CODE, AntifraudAction.ORDER_ITEM_CHANGE),
                Set.of(AntifraudAction.ROBOCALL),
                List.of(createBlockingEvent(uid), createBlockingEvent(uid))
        );

        return group;
    }

    private BlockingEvent createBlockingEvent(Long uid) {
        return new BlockingEvent(
                uid,
                Randoms.dateTime().toInstant(),
                BlockingType.ORDER,
                List.of(createRuleTriggerEvent(), createRuleTriggerEvent(), createRuleTriggerEvent())
        );
    }

    private RuleTriggerEvent createRuleTriggerEvent() {
        return new RuleTriggerEvent(Randoms.string(), Randoms.string());
    }

    private List<AntifraudBlockingEventGroup> getBlockingEventGroups(Long uid) {
        Query query = Query.of(AntifraudBlockingEventGroup.FQN)
                .withFilters(
                        Filters.eq(AntifraudBlockingEventGroup.UID, uid),
                        Filters.eq(BLOCKING_TYPE_FILTER, BlockingType.LOYALTY.name()),
                        Filters.eq(AntifraudBlockingEventGroup.FROM_DATE, Randoms.dateTime()),
                        Filters.eq(AntifraudBlockingEventGroup.TO_DATE, Randoms.dateTime())
                );
        return entityStorageService.list(query);
    }

    private void checkGroups(List<BlockingEventGroup> expected, List<AntifraudBlockingEventGroup> actual) {
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(expected.size(), actual.size());
        Assertions.assertTrue(expected.stream().allMatch(expectedGroup ->
                actual.stream().anyMatch(actualGroup -> isSameGroups(expectedGroup, actualGroup))
        ));
    }

    private boolean isSameGroups(BlockingEventGroup expected, AntifraudBlockingEventGroup actual) {
        Assertions.assertNotNull(actual.getEvents());
        Assertions.assertEquals(expected.getBlockings().size(), actual.getEvents().size());
        boolean isSameEvents = expected.getBlockings().stream()
                .allMatch(expectedEvent ->
                        actual.getEvents().stream()
                                .anyMatch(actualEvent -> isSameEvents(expectedEvent, actualEvent))
                );
        return isSameEvents && Objects.equals(expected.getUid(), actual.getUid())
                && Objects.equals(expected.getFrom(), actual.getFromDate().toInstant())
                && Objects.equals(expected.getTo(), actual.getToDate().toInstant())
                && isSameAntifraudActions(expected.getRestrictions(), actual.getRestrictions())
                && isSameAntifraudActions(expected.getPrivileges(), actual.getPrivileges());
    }

    private boolean isSameAntifraudActions(Set<AntifraudAction> expected,
                                           Collection<ru.yandex.market.ocrm.module.order.domain.AntifraudAction> actual) {
        return Objects.equals(
                expected.stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()),
                CrmCollections.nullToEmpty(actual).stream()
                        .map(CatalogItem::getCode)
                        .collect(Collectors.toSet())
        );
    }

    private boolean isSameEvents(BlockingEvent expected, AntifraudBlockingEvent actual) {
        boolean isSameRules = expected.getRuleTriggerEvents().stream()
                .allMatch(expectedRule ->
                        actual.getRuleTriggerEvents().stream()
                                .anyMatch(actualRule ->
                                        Objects.equals(expectedRule.getDescription(), actualRule.getDescription())
                                                && Objects.equals(expectedRule.getRuleName(), actualRule.getName())
                                )
                );
        return isSameRules && Objects.equals(expected.getUid(), actual.getUid())
                && Objects.equals(expected.getTimestamp(), actual.getCreatedAt().toInstant())
                && Objects.equals(expected.getType().name(), actual.getType().getCode());
    }
}
