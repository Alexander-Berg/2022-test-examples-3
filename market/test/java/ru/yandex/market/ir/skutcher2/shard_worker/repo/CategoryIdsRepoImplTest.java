package ru.yandex.market.ir.skutcher2.shard_worker.repo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.ir.sharding.Sharder;
import ru.yandex.ir.sharding.StatShardJsonEntry;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class CategoryIdsRepoImplTest {

    @InjectMocks
    private CategoryIdsRepoImpl categoryIdsRepo;

    @Mock
    private Sharder skutcherSharder;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        when(skutcherSharder.makeShardJsonEntryList()).thenReturn(Arrays.asList(
                new StatShardJsonEntry(0, 10),
                new StatShardJsonEntry(0, 15),
                new StatShardJsonEntry(0, 20),
                new StatShardJsonEntry(1, 11),
                new StatShardJsonEntry(1, 16),
                new StatShardJsonEntry(1, 21),
                new StatShardJsonEntry(2, 12),
                new StatShardJsonEntry(2, 17),
                new StatShardJsonEntry(2, 22),
                new StatShardJsonEntry(3, 13),
                new StatShardJsonEntry(3, 18),
                new StatShardJsonEntry(3, 23)
        ));
    }

    @Test
    public void loadCategoryIds() {
        Set<Integer> categoryIds = categoryIdsRepo.loadCategoryIds(0);
        assertArrayEquals(new Integer[]{10, 15, 20}, categoryIds.stream().sorted().toArray(Integer[]::new));

        categoryIds = categoryIdsRepo.loadCategoryIds(1);
        assertArrayEquals(new Integer[]{11, 16, 21}, categoryIds.stream().sorted().toArray(Integer[]::new));

        categoryIds = categoryIdsRepo.loadCategoryIds(2);
        assertArrayEquals(new Integer[]{12, 17, 22}, categoryIds.stream().sorted().toArray(Integer[]::new));

        categoryIds = categoryIdsRepo.loadCategoryIds(3);
        assertArrayEquals(new Integer[]{13, 18, 23}, categoryIds.stream().sorted().toArray(Integer[]::new));
    }
}
