package ru.yandex.market.clab.common.service.ssbarcode;

import ru.yandex.market.clab.common.service.barcode.GoodBarcodes;
import ru.yandex.market.clab.common.service.barcode.SsBarcodeRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SsBarcodeRepositoryStub implements SsBarcodeRepository {

    private final Map<Long, GoodBarcodes> byId = new HashMap<>();

    @Override
    public List<Long> getGoodIds(String barcode) {
        return byId.values().stream()
            .filter(goodBarcodes -> goodBarcodes.getBarcodes().contains(barcode))
            .map(GoodBarcodes::getGoodId)
            .collect(Collectors.toList());
    }

    @Override
    public List<GoodBarcodes> getBarcodes(Collection<Long> goodIds) {
        List<GoodBarcodes> res = new ArrayList<>();
        goodIds.forEach(id -> {
            GoodBarcodes goodBarcodes = byId.get(id);
            if (goodBarcodes != null) {
                res.add(goodBarcodes);
            }
        });
        return res;
    }
}
