package ru.yandex.direct.core.testing.repository;

import java.time.LocalDateTime;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.currency.repository.CurrencyConvertQueueRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.CURRENCY_CONVERT_QUEUE;

/**
 * Репозиторий для тестов на работу с заявками на переход клиентов в реальную валюту.
 */
@Repository
@ParametersAreNonnullByDefault
public class TestCurrencyConvertQueueRepository extends CurrencyConvertQueueRepository {
    public TestCurrencyConvertQueueRepository(
            DslContextProvider dslContextProvider) {
        super(dslContextProvider);
    }

    /**
     * Добавляет новую запись в таблицу заявок на переход с у.е. на реальную валюту.
     *
     * @param clientInfo Информация о клиенте включая ID
     * @param regionId   ID региона
     */
    public void createCurrencyConversionQueueEntry(ClientInfo clientInfo, long regionId) {
        dslContextProvider.ppc(clientInfo.getShard()).insertInto(CURRENCY_CONVERT_QUEUE,
                CURRENCY_CONVERT_QUEUE.CLIENT_ID, CURRENCY_CONVERT_QUEUE.CREATE_TIME,
                CURRENCY_CONVERT_QUEUE.COUNTRY_REGION_ID)
                .values(clientInfo.getClientId().asLong(), LocalDateTime.now(), regionId)
                .execute();
    }
}
