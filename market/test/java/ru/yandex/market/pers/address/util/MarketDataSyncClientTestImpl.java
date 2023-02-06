package ru.yandex.market.pers.address.util;

import ru.yandex.market.pers.address.model.identity.Identity;
import ru.yandex.market.pers.address.services.MarketDataSyncClient;
import ru.yandex.market.pers.address.services.exception.DataSyncAddressNotFoundException;
import ru.yandex.market.pers.address.services.model.MarketDataSyncAddress;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MarketDataSyncClientTestImpl implements MarketDataSyncClient {
    private final Map<Identity<?>, Map<String, MarketDataSyncAddress>> addresses = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    @Override
    public List<MarketDataSyncAddress> getAddresses(Identity<?> identity) {
        return addresses.computeIfAbsent(identity, (key) -> new ConcurrentHashMap<>()).values().stream()
            .map(MarketDataSyncAddress::copyBuilder)
            .map(MarketDataSyncAddress.Builder::build)
            .collect(Collectors.toList());
    }

    @Override
    public MarketDataSyncAddress getAddress(Identity<?> identity, String addressId) {
        MarketDataSyncAddress address = addresses.computeIfAbsent(identity, (key) -> new ConcurrentHashMap<>()).get(addressId);
        if (address == null) {
            throw new DataSyncAddressNotFoundException(identity, addressId);
        }
        return MarketDataSyncAddress.copyBuilder(address).build();
    }

    @Override
    public String saveNewAddress(Identity<?> identity, MarketDataSyncAddress address) {
        String generatedId = Optional.ofNullable(address.getId()).orElse(String.valueOf(idGenerator.incrementAndGet()));
        addresses.computeIfAbsent(identity, (key) -> new ConcurrentHashMap<>())
            .put(generatedId, MarketDataSyncAddress.copyBuilder(address).setId(generatedId).build());
        return generatedId;
    }

    @Override
    public void updateAddress(Identity<?> identity, MarketDataSyncAddress address) {
        addresses.computeIfAbsent(identity, (key) -> new ConcurrentHashMap<>())
            .put(address.getId(), MarketDataSyncAddress.copyBuilder(address).build());
    }

    @Override
    public void deleteAddress(Identity<?> identity, String addressId) {
        addresses.computeIfAbsent(identity, (key) -> new ConcurrentHashMap<>()).remove(addressId);
    }

    public void clear() {
        addresses.clear();
    }
}
