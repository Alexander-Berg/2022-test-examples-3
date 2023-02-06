package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.vcard.repository.internal.AddressesRepository;
import ru.yandex.direct.core.entity.vcard.repository.internal.DbAddress;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.ADDRESSES;

/**
 * Работа с адресами в визитках в тестах
 */
public class TestAddressesRepository {

    private final AddressesRepository addressesRepository;
    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestAddressesRepository(
            AddressesRepository addressesRepository,
            DslContextProvider dslContextProvider) {
        this.addressesRepository = addressesRepository;
        this.dslContextProvider = dslContextProvider;
    }

    /**
     * Получить список адресов для данных визиток
     *
     * @param shard    Шард
     * @param clientId ID клиента
     * @param ids      Список ID адресов из визиток
     * @return Маппинг ID адреса - адрес
     */
    public Map<Long, DbAddress> getAddresses(int shard, ClientId clientId, Collection<Long> ids) {
        return dslContextProvider.ppc(shard)
                .select(addressesRepository.addressMapper.getFieldsToRead())
                .from(ADDRESSES)
                .where(ADDRESSES.CLIENT_ID.eq(clientId.asLong()))
                .and(ADDRESSES.AID.in(ids))
                .orderBy(ADDRESSES.AID)
                .fetchMap(ADDRESSES.AID, addressesRepository.addressMapper::fromDb);
    }
}
