package ru.yandex.market.mbo.mdm.common.masterdata.services.iris;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MskuToSskuSilverItem;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.Multiwatch;

/**
 * Mock service for tests that does not enrich golden item with surplus and cis info.
 *
 * @author dmserebr
 * @date 08/05/2020
 */
public class SurplusAndCisGoldenItemServiceMock implements SurplusAndCisGoldenItemService {
    @Override
    public Optional<ReferenceItemWrapper> calculateGoldenItem(ShopSkuKey key,
                                                              GoldComputationContext context,
                                                              List<? extends MskuToSskuSilverItem> silverItems,
                                                              @Nullable ReferenceItemWrapper existingGoldenItem,
                                                              @Nullable Multiwatch watch) {
        return calculateGoldenItem(key, context, silverItems, existingGoldenItem);
    }

    @Override
    public Optional<ReferenceItemWrapper> calculateGoldenItem(ShopSkuKey key,
                                                              GoldComputationContext context,
                                                              List<? extends MskuToSskuSilverItem> silverItems,
                                                              @Nullable ReferenceItemWrapper existingGoldenItem) {
        return Optional.empty();
    }
}
