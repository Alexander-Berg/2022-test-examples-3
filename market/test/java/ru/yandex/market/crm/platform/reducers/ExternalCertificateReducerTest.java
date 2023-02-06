package ru.yandex.market.crm.platform.reducers;

import java.util.Collections;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.mappers.ExternalCertificateMapper;
import ru.yandex.market.crm.platform.models.ExternalCertificate;
import ru.yandex.market.crm.util.ResourceHelpers;

public class ExternalCertificateReducerTest {

    private ExternalCertificate newState;
    private ExternalCertificate paidState;

    @Test
    public void receiptId_asc() {
        ExternalCertificate reduced = doReduce(newState, paidState);

        Assert.assertNotNull(reduced);
        Assert.assertEquals(310536, reduced.getReceiptId());

    }

    private ExternalCertificate doReduce(ExternalCertificate stored, ExternalCertificate newFacts) {
        YieldMock collector = new YieldMock();
        new ExternalCertificateReducer().reduce(Collections.singletonList(stored), Collections.singleton(newFacts),
                collector);

        return Iterables.get(collector.getAdded("ExternalCertificate"), 0);
    }

    @Test
    public void receiptId_desc() {
        ExternalCertificate reduced = doReduce(paidState, newState);

        Assert.assertNotNull(reduced);
        Assert.assertEquals(310536, reduced.getReceiptId());

    }

    @Before
    public void setUp() {
        newState = read("ExternalCertificate_new.json");
        paidState = read("ExternalCertificate_paid.json");
    }

    public ExternalCertificate read(String resourceName) {
        byte[] resource = ResourceHelpers.getResource(resourceName);
        return Iterables.getFirst(new ExternalCertificateMapper().apply(resource), null);
    }

    @Test
    public void updatedAt_asc() {
        ExternalCertificate reduced = doReduce(newState, paidState);

        Assert.assertNotNull(reduced);
        Assert.assertEquals(paidState.getUpdatedAt(), reduced.getUpdatedAt());

    }

    @Test
    public void updatedAt_desc() {
        ExternalCertificate reduced = doReduce(paidState, newState);

        Assert.assertNotNull(reduced);
        Assert.assertEquals(paidState.getUpdatedAt(), reduced.getUpdatedAt());
    }
}
