package ru.yandex.market.ir.matcher2.shard_worker;

import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.ir.matcher2.shard_worker.utils.FileUtil;

import static org.junit.Assert.assertArrayEquals;

public class CategoryIdsRepoImplTest {


    private CategoryIdsRepoImpl categoryIdsRepo;

    @Before
    public void setUp() {
        categoryIdsRepo = new CategoryIdsRepoImpl(FileUtil.getAbsolutePath("/sharding"), 4);
    }

    @Test
    @Ignore
    public void loadCategoryIds() {
        final Set<Integer> categoryIds = categoryIdsRepo.loadCategoryIds(1);

        assertArrayEquals(new Integer[]{90404, 90555, 90796, 91491, 15685787, 18540110},
                categoryIds.stream().sorted().toArray(Integer[]::new));
    }
}
