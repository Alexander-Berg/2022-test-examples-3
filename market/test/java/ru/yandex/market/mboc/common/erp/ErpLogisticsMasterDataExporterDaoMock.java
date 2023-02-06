package ru.yandex.market.mboc.common.erp;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.mboc.common.erp.model.ErpLogisticsMasterData;

public class ErpLogisticsMasterDataExporterDaoMock implements ErpLogisticsMasterDataExporterDao {
    private final List<ErpLogisticsMasterData> data = new ArrayList<>();

    @Override
    public Integer insertSupply(List<ErpLogisticsMasterData> supplies) {
        data.addAll(supplies);
        return supplies.size();
    }

    @Override
    public List<ErpLogisticsMasterData> findAll() {
        return List.copyOf(data);
    }
}
