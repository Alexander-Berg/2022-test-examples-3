package ru.yandex.direct.core.testing.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.CLIENTS_OPTIONS;

public class TestClientOptionsRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    /**
     * Добавляет дефолтную запись в таблицу ppc.clients_options.
     *
     * @param shard     шард
     * @param clientId  id клиента
     */
    public void addEmptyClientsOptions(int shard, ClientId clientId) {
        dslContextProvider.ppc(shard)
                .insertInto(CLIENTS_OPTIONS)
                .columns(CLIENTS_OPTIONS.CLIENT_ID, CLIENTS_OPTIONS.BALANCE_TID, CLIENTS_OPTIONS.OVERDRAFT_LIM,
                        CLIENTS_OPTIONS.DEBT, CLIENTS_OPTIONS.NEXT_PAY_DATE)
                .values(clientId.asLong(), 0L, BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now())
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void setClientFlags(int shard, ClientId clientId, String clientFlags) {
        dslContextProvider.ppc(shard)
                .update(CLIENTS_OPTIONS)
                .set(CLIENTS_OPTIONS.CLIENT_FLAGS, clientFlags)
                .where(CLIENTS_OPTIONS.CLIENT_ID.eq(clientId.asLong()))
                .execute();
    }
}
