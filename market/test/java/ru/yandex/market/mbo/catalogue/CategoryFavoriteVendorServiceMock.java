package ru.yandex.market.mbo.catalogue;

import java.util.List;

public class CategoryFavoriteVendorServiceMock implements CategoryFavoriteVendorService {

    @Override
    public void addFavoriteVendor(long hid, long globalVendorId) {

    }

    @Override
    public void removeFavoriteVendor(long hid, long globalVendorId) {

    }

    @Override
    public List<Long> getFavoriteVendors(long hid) {
        return null;
    }
}
