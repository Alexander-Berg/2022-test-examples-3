package ru.yandex.direct.core.testing.repository;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.CURRENCY_CONVERT_MONEY_CORRESPONDENCE;

@Repository
@ParametersAreNonnullByDefault
public class TestCurrencyConvertMoneyCorrespondenceRepository {
    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestCurrencyConvertMoneyCorrespondenceRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    /**
     * Добавление новый записи о переходе кампании с у.е. на реальную валюту.
     * Добавляется только связка ID кампаний в у.е. и в валюте.
     *
     * @param clientInfo Информация о клиенте включая ID
     * @param oldCid     ID кампании в у.е.
     * @param newCid     ID кампании в реальной валюте
     */
    public void createCurrencyConvertMoneyCorrespondenceEntry(ClientInfo clientInfo, long oldCid, Long newCid) {
        dslContextProvider.ppc(clientInfo.getShard()).insertInto(CURRENCY_CONVERT_MONEY_CORRESPONDENCE,
                CURRENCY_CONVERT_MONEY_CORRESPONDENCE.CLIENT_ID, CURRENCY_CONVERT_MONEY_CORRESPONDENCE.OLD_CID,
                CURRENCY_CONVERT_MONEY_CORRESPONDENCE.NEW_CID)
                .values(clientInfo.getClientId().asLong(), oldCid, newCid)
                .execute();
    }

}
