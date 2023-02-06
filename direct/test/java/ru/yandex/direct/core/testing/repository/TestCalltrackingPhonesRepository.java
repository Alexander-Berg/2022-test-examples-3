package ru.yandex.direct.core.testing.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.calltracking.model.CalltrackingPhone;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppcdict.tables.CalltrackingPhones.CALLTRACKING_PHONES;

public class TestCalltrackingPhonesRepository {
    @Autowired
    private DslContextProvider dslContextProvider;

    public void add(String phone, LocalDateTime lastUpdate) {
        dslContextProvider.ppcdict()
                .insertInto(CALLTRACKING_PHONES)
                .set(CALLTRACKING_PHONES.PHONE, phone)
                .set(CALLTRACKING_PHONES.LAST_UPDATE, lastUpdate)
                .execute();
    }

    public List<CalltrackingPhone> getAllPhones() {
        return dslContextProvider.ppcdict()
                .select(CALLTRACKING_PHONES.PHONE, CALLTRACKING_PHONES.LAST_UPDATE)
                .from(CALLTRACKING_PHONES)
                .orderBy(CALLTRACKING_PHONES.PHONE)
                .fetchInto(CalltrackingPhone.class);
    }

    public void deleteAll() {
        dslContextProvider.ppcdict()
                .deleteFrom(CALLTRACKING_PHONES)
                .execute();
    }
}
