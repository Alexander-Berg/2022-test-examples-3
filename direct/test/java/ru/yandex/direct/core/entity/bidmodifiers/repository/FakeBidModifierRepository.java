package ru.yandex.direct.core.entity.bidmodifiers.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.container.BidModifierKey;

public class FakeBidModifierRepository implements BidModifierRepositoryInterface {

    private final Map<BidModifierKey, BidModifier> bidModifiersMap;

    public FakeBidModifierRepository(List<BidModifier> modifiers) {
        bidModifiersMap = fromListToMap(modifiers);
    }

    public Map<BidModifierKey, BidModifier> getBidModifiersByKeys(
        int shard, Collection<BidModifierKey> modifierKeys
    ) {
        var modifiers = modifierKeys.stream()
                .filter(bidModifiersMap::containsKey)
                .map(bidModifiersMap::get)
                .collect(Collectors.toList());

        return fromListToMap(modifiers);
    }

    private Map<BidModifierKey, BidModifier> fromListToMap(List<BidModifier> modifiers) {
        Map<BidModifierKey, BidModifier> map = new HashMap<>();
        modifiers.forEach(modifier -> map.put(new BidModifierKey(modifier), modifier));
        return map;
    }

}
