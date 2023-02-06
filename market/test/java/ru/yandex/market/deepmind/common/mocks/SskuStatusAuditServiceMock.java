package ru.yandex.market.deepmind.common.mocks;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import lombok.Getter;

import ru.yandex.market.mboc.common.audit.OfferStatusAuditInfoRead;
import ru.yandex.market.mboc.common.audit.SskuStatusAuditInfoWrite;
import ru.yandex.market.mboc.common.audit.SskuStatusAuditService;
import ru.yandex.market.mboc.common.audit.SskuStatusAuditWrapper;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class SskuStatusAuditServiceMock implements SskuStatusAuditService {

    @Getter
    private List<OfferStatusAuditInfoRead> info;

    @Override
    public SskuStatusAuditWrapper getSskuStatusAuditInfo(ShopSkuKey shopSkuKey, @Nullable Instant lastTs) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void writeSskuStatusAuditInfo(List<SskuStatusAuditInfoWrite> info) {
        this.info = info.stream()
            .map(SskuStatusAuditInfoWrite::toReadInfo)
            .collect(Collectors.toList());
    }
}
