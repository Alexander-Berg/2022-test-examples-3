package ru.yandex.market.mbo.db.vendor;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;

import ru.yandex.utils.Pair;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Collections;

public class GlobalVendorUtilDBMock extends GlobalVendorUtilDB {
    private GlobalVendorDBMock vendorDB;

    public GlobalVendorUtilDBMock(GlobalVendorDBMock vendorDBMock) {
        vendorDB = vendorDBMock;
    }

    @Override
    public List<Pair<Long, String>> searchVendorsByName(String name, int limit) {
        return vendorDB.loadAllVendors().stream()
            .filter(gv -> gv.getDefaultName().equals(name))
            .limit(limit)
            .map(gv -> new Pair<>(gv.getId(), gv.getDefaultName()))
            .collect(Collectors.toList());
    }

    @Override
    public String loadVendorName(long vendorId) {
        return vendorDB.loadVendor(vendorId).getDefaultName();
    }

    @Override
    public List<Long> getVendorIdsByNormalizedSite(String site) {
        if (StringUtils.isEmpty(site)) {
            return Collections.emptyList();
        }
        return vendorDB.loadAllVendors().stream()
            .filter(gv -> gv.getSite().equals(site))
            .map(gv -> gv.getId())
            .collect(Collectors.toList());
    }

    @Override
    public List<Long> getVendorIdsByNamesAndAliases(List<String> namesOrAliases) {
        if (namesOrAliases.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> namesOrAliasesLowerCase = namesOrAliases.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toList());
        return vendorDB.loadAllVendors().stream()
            .filter(gv -> intersectNameOrAliases(gv, namesOrAliasesLowerCase))
            .map(gv -> gv.getId())
            .collect(Collectors.toList());
    }

    private boolean intersectNameOrAliases(GlobalVendor globalVendor, List<String> namesOrAliases) {
        List<String> vendorNameAndAliases = new ArrayList<>(globalVendor.getDefaultAliases());
        vendorNameAndAliases.addAll(globalVendor.getNames().stream()
            .map(name -> name.getWord())
            .collect(Collectors.toList()));
        vendorNameAndAliases = vendorNameAndAliases.stream().map(String::toLowerCase).collect(Collectors.toList());
        for (String str : namesOrAliases) {
            if (vendorNameAndAliases.contains(str.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

}
