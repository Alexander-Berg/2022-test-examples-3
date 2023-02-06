package ru.yandex.market.checkout.pushapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.market.checkout.pushapi.client.entity.Region;
import ru.yandex.market.common.region.RegionHelper;

import java.util.ArrayList;
import java.util.List;

public class GeoRegionService implements GeoService {
    
    private RegionService regionService;

    @Autowired
    public void setRegionService(RegionService regionService) {
        this.regionService = regionService;
    }

    @Override
    public Region getRegion(long id) {
        final ru.yandex.common.util.region.Region region = regionService.getRegionTree().getRegion((int) id);

        final ArrayList<ru.yandex.common.util.region.Region> regionsList = new ArrayList<>();
        toList(region, regionsList);

        return convert(RegionHelper.fromParentList(regionsList));
    }

    private void toList(ru.yandex.common.util.region.Region region, List<ru.yandex.common.util.region.Region> list) {
        if(region != null) {
            list.add(region);
            toList(region.getParent(), list);
        }
    }
    
    private Region convert(ru.yandex.common.util.region.Region region) {
        if(region == null) {
            return null;
        } else {
            return new Region(
                region.getId(),
                region.getName(),
                region.getType(),
                convert(region.getParent())
            );
        }
    }
}
