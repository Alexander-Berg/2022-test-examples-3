package ru.yandex.market.api.domain;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.PagedResult;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by fettsery on 10.07.18.
 */
public class PageUtilsTest extends UnitTestBase {
    @Test
    public void shouldNotCutResultsInBounds() {
        PageInfo pageInfo = new PageInfo();
        pageInfo.setTotalPages(10);
        pageInfo.setPage(2);
        pageInfo.setCount(10);

        PagedResult<Integer> result = PageUtils.cutTotalCount(new PagedResult<>(Collections.nCopies(10, 1), pageInfo),
            10, 1000);

        Assert.assertEquals(10, (int) result.getPageInfo().getTotalPages());
        Assert.assertEquals(2, result.getPageInfo().getPage());
        Assert.assertEquals(10, result.getPageInfo().getCount());
        Assert.assertEquals(10, result.getElements().size());
    }

    @Test
    public void shouldCutWholePage() {
        PageInfo pageInfo = new PageInfo();
        pageInfo.setTotalPages(200);
        pageInfo.setPage(101);
        pageInfo.setCount(10);

        PagedResult<Integer> result = PageUtils.cutTotalCount(new PagedResult<>(new ArrayList<>(Collections.nCopies(10, 1)), pageInfo),
            10, 1000);

        Assert.assertEquals(100, (int) result.getPageInfo().getTotalPages());
        Assert.assertEquals(100, result.getPageInfo().getPage());
        Assert.assertEquals(0, result.getPageInfo().getCount());
        Assert.assertEquals(0, result.getElements().size());
    }

    @Test
    public void shouldCutPartOfPage() {
        PageInfo pageInfo = new PageInfo();
        pageInfo.setTotalPages(200);
        pageInfo.setPage(34);
        pageInfo.setCount(30);

        PagedResult<Integer> result = PageUtils.cutTotalCount(new PagedResult<>(new ArrayList<>(Collections.nCopies(30, 1)), pageInfo),
            30, 1000);

        Assert.assertEquals(34, (int) result.getPageInfo().getTotalPages());
        Assert.assertEquals(34, result.getPageInfo().getPage());
        Assert.assertEquals(10, result.getPageInfo().getCount());
        Assert.assertEquals(10, result.getElements().size());
    }
}
