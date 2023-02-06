package ru.yandex.market.mboc.app.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.web.DataPage;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;

public class CategoryInfoControllerTest extends BaseMbocAppTest {
    @Autowired
    private CategoryInfoRepository categoryInfoRepository;

    private CategoryInfoController categoryInfoController;

    @Before
    public void setUp() {
        categoryInfoController = new CategoryInfoController(categoryInfoRepository);
    }

    @Test
    public void all() {
        List<CategoryInfo> categoryInfoList = new ArrayList<>();
        int size = 100;
        range(0, size).forEach(i -> categoryInfoList.add(i, new CategoryInfo(i)));
        categoryInfoRepository.insertBatch(categoryInfoList);
        int limit = size / 5;
        int offset = size / 10;
        OffsetFilter offsetFilter = new OffsetFilter(offset, limit);
        DataPage<CategoryInfo> dataPage = categoryInfoController.all(offsetFilter);
        List<CategoryInfo> items = dataPage.getItems();
        long totalCount = dataPage.getTotalCount();
        validateCategoryInfoList(items, offset, limit, size, totalCount);

        limit = size;
        offset = size - 10;
        offsetFilter = new OffsetFilter(offset, limit);
        dataPage = categoryInfoController.all(offsetFilter);
        items = dataPage.getItems();
        totalCount = dataPage.getTotalCount();
        validateCategoryInfoList(items, offset, limit, size, totalCount);

        limit = size / 2;
        offset = size + 10;
        offsetFilter = new OffsetFilter(offset, limit);
        dataPage = categoryInfoController.all(offsetFilter);
        items = dataPage.getItems();
        totalCount = dataPage.getTotalCount();
        validateCategoryInfoList(items, offset, limit, size, totalCount);
    }

    @Test
    public void findByIds() {
        List<CategoryInfo> categoryInfoList = new ArrayList<>();
        int size = 100;
        range(0, size).forEach(i -> categoryInfoList.add(i, new CategoryInfo(i)));
        categoryInfoRepository.insertBatch(categoryInfoList);
        Collection<Long> ids = new ArrayList<>();
        range(0, size / 10).forEach(i -> ids.add(i * 9L));
        List<CategoryInfo> categoryInfos = categoryInfoController.findByIds(ids);
        assertThat(categoryInfos).hasSize(size / 10);
        range(0, size / 10).forEach(i -> assertThat(categoryInfos.get(i).getCategoryId()).isEqualTo(i * 9L));
    }

    private void validateCategoryInfoList(List<CategoryInfo> categoryInfoList, int offset, int limit, int size,
                                          long totalCount) {
        assertThat(totalCount).isEqualTo(categoryInfoRepository.totalCount());
        List<CategoryInfo> categoryInfos = getCategoryInfoByOffsetAndLimit(offset, limit, size);
        assertThat(categoryInfos).hasSize(categoryInfoList.size());
        assertThat(categoryInfoList).isEqualTo(categoryInfos);
    }

    private List<CategoryInfo> getCategoryInfoByOffsetAndLimit(int offset, int limit, int size) {
        List<CategoryInfo> categoryInfoList = new ArrayList<>();
        for (int i = offset; i < Math.min(offset + limit, size); i++) {
            categoryInfoList.add(i - offset, new CategoryInfo(i));
        }
        return categoryInfoList;
    }
}
