package ru.yandex.ir.modelsclusterizer.slave.offer;

import net.sf.saxon.functions.Collection;
import org.junit.Test;
import ru.yandex.utils.Pair;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class RawOffersWithVendorsTest {

    @Test
    public void doBuildVendorBatches() {
        List<List<Long>> result = RawOffersWithVendors.doBuildVendorBatches(Arrays.asList(
            new Pair<>(1L, 4),
            new Pair<>(2L, 1),
            new Pair<>(3L, 2),
            new Pair<>(4L, 1),
            new Pair<>(5L, 8)
        ), 3);
        assertEquals(result, Arrays.asList(
            Arrays.asList(1L),
            Arrays.asList(2L, 3L),
            Arrays.asList(4L),
            Arrays.asList(5L)
        ));
    }
}