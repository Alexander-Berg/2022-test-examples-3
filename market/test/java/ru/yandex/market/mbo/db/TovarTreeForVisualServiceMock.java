package ru.yandex.market.mbo.db;

public class TovarTreeForVisualServiceMock extends TovarTreeForVisualService {

    public TovarTreeForVisualServiceMock(TovarTreeDao tovarTreeDao) {
        this(new CachedTreeService(tovarTreeDao, 1));
    }

    public TovarTreeForVisualServiceMock(CachedTreeService cachedTreeService) {
        super(null, cachedTreeService);
    }
}
