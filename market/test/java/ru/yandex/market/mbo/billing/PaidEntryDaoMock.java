package ru.yandex.market.mbo.billing;

import ru.yandex.market.mbo.db.billing.dao.FullPaidEntry;
import ru.yandex.market.mbo.db.billing.dao.PaidEntry;
import ru.yandex.market.mbo.db.billing.dao.PaidEntryDao;
import ru.yandex.market.mbo.billing.counter.base.PaidEntryQueryParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author dmserebr
 * @date 02/08/2019
 */
public class PaidEntryDaoMock implements PaidEntryDao {

    private Map<PaidEntryQueryParams, PaidEntry> paidEntries = new HashMap<>();

    public void addPaidEntry(PaidEntryQueryParams params, PaidEntry entry) {
        this.paidEntries.put(params, entry);
    }

    @Override
    public Optional<PaidEntry> getPaidEntry(PaidEntryQueryParams params) {
        return Optional.ofNullable(paidEntries.get(params));
    }

    @Override
    public List<FullPaidEntry> getYangPaidEntries(long categoryId, long taskId, long contractorUid) {
        return Collections.emptyList();
    }
}
