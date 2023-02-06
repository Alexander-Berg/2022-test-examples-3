package ru.yandex.travel.hotels.searcher.services.cache.travelline.availability;

import ru.yandex.travel.hotels.searcher.services.cache.travelline.availability.inmemory.InmemoryAvailabilityRepository;
import ru.yandex.travel.hotels.searcher.services.cache.travelline.availability.inmemory.InmemoryInventoryRepository;
import ru.yandex.travel.hotels.searcher.services.cache.travelline.availability.inmemory.InmemoryTransactionSupplier;

public class InmemoryL2CacheTests extends BaseL2CacheTests {

    @Override
    public void prepare() {
        cache = new L2CacheImplementation(new InmemoryInventoryRepository(), new InmemoryAvailabilityRepository(),
                new InmemoryTransactionSupplier());
    }
}
