package ru.yandex.market.crm.platform.reducers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.EmailOwnership;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.crm.platform.reducers.EmailOwnershipReducer.FACT_ID;

/**
 * @author apershukov
 */
public class EmailOwnershipReducerTest {

    private EmailOwnershipReducer reducer = new EmailOwnershipReducer();

    private static EmailOwnership.Builder ownershipBuilder() {
        return EmailOwnership.newBuilder()
                .setUid(
                        Uids.create(UidType.PUID, 33478676)
                )
                .setEmail("apershukov@yandex.ru")
                .setStatus("CONFIRMED")
                .setSource("PASSPORT")
                .setActive(true)
                .setModificationTime(System.currentTimeMillis() / 1000);
    }

    @Test
    public void testAddNewOwnership() {
        EmailOwnership ownership = ownershipBuilder().build();

        YieldMock collector = reduce(Collections.singleton(ownership));

        assertEquals(0, collector.getRemoved(FACT_ID).size());

        Collection<EmailOwnership> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());
        assertEquals(ownership, added.iterator().next());
    }

    @Test
    public void testSelectNewestOwnershipVersion() {
        EmailOwnership ownership1 = ownershipBuilder()
                .setModificationTime(222)
                .build();

        EmailOwnership ownership2 = ownershipBuilder()
                .setModificationTime(333)
                .build();

        EmailOwnership ownership3 = ownershipBuilder()
                .setModificationTime(111)
                .build();

        YieldMock collector = reduce(Arrays.asList(ownership1, ownership2, ownership3));

        assertEquals(0, collector.getRemoved(FACT_ID).size());

        Collection<EmailOwnership> added = collector.getAdded(FACT_ID);
        assertEquals(1, added.size());
        assertEquals(ownership2, added.iterator().next());
    }

    @Test
    public void testIgnoreNewFactIfSavedFactIsNewer() {
        EmailOwnership oldFact = ownershipBuilder()
                .setModificationTime(333)
                .build();

        EmailOwnership newFact = ownershipBuilder()
                .setModificationTime(111)
                .build();

        YieldMock collector = reduce(Collections.singletonList(oldFact), Collections.singletonList(newFact));

        assertEquals(0, collector.getRemoved(FACT_ID).size());
        assertEquals(0, collector.getAdded(FACT_ID).size());
    }

    private YieldMock reduce(Collection<EmailOwnership> ownerships) {
        return reduce(Collections.emptyList(), ownerships);
    }

    private YieldMock reduce(List<EmailOwnership> oldOwnerships, Collection<EmailOwnership> ownerships) {
        YieldMock collector = new YieldMock();
        reducer.reduce(oldOwnerships, ownerships, collector);
        return collector;
    }
}
