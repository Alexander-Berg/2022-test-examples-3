package ru.yandex.market.mboc.tms;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mboc.common.categorygroups.CategoryGroup;
import ru.yandex.market.mboc.common.categorygroups.CategoryGroupRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class RefreshYangCategoryGroupExecutorTest extends BaseDbTestClass {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CategoryGroupRepository categoryGroupRepository;
    @Autowired
    private TransactionHelper transactionHelper;

    private RefreshYangCategoryGroupExecutor executor;

    @Before
    public void setUp() throws Exception {
        executor = new RefreshYangCategoryGroupExecutor(categoryGroupRepository, categoryRepository, transactionHelper);
    }

    @Test
    public void testUpdateGroupCategories() {
        var categoryGroupOld = new CategoryGroup()
            .setCategories(List.of(1L, 123L))
            .setName("Other")
            .setComment("")
            .setCreated(Instant.now());
        var categoryGroup = categoryGroupRepository.insertOrUpdate(categoryGroupOld);

        ReflectionTestUtils.setField(executor, "OTHER_GROUP_ID", categoryGroup.getId());

        List<Category> newLeafs = Stream.iterate(1, i -> i + 1).limit(10)
            .map(i -> OfferTestUtils.defaultCategory()
                .setCategoryId(i)
                .setLeaf(true)
            )
            .collect(Collectors.toList());
        categoryRepository.insertBatch(newLeafs);

        executor.execute();

        var newLeafIds = newLeafs.stream()
            .map(Category::getCategoryId)
            .collect(Collectors.toList());

        var expectedOtherGroupCategories = Stream.of(categoryGroupOld.getCategories(), newLeafIds)
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toList());
        var otherCategoryGroupUpdated = categoryGroupRepository.findById(categoryGroup.getId());
        assertThat(otherCategoryGroupUpdated.getCategories())
            .containsExactlyInAnyOrder(expectedOtherGroupCategories.toArray(Long[]::new));
    }
}
