package ru.yandex.market.aliasmaker.meta.CategorySizeRepositoryTest;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.aliasmaker.meta.IntegrationTests.IntegrationTestConfig;
import ru.yandex.market.aliasmaker.meta.repository.CategorySizeRepository;
import ru.yandex.market.aliasmaker.meta.repository.dto.CategorySizeInfoDTO;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class,
        initializers = PGaaSZonkyInitializer.class)
@Transactional
public class CategorySizeRepositoryTest {
    @Autowired
    private CategorySizeRepository categorySizeRepository;
    private final Set<CategorySizeInfoDTO> realCategoryCache = new HashSet<>();
    private long idCounter = 0L;

    @Before
    public void startup() {
        for (int i = 0; i < 10; i++) {
            generateRandomCategoryCache();
        }
        categorySizeRepository.insertBatch(realCategoryCache);
        categorySizeRepository.updateCache();
    }

    @Test
    public void canGetAll() {
        var allCached = categorySizeRepository.getAllCached();
        Assertions.assertThat(allCached).isNotEmpty();
        Assertions.assertThat(allCached).hasSize(10);
    }

    @Test
    public void canGetSingle() {
        var toFind = checkForEmpty(realCategoryCache.stream().findFirst());
        var singleCached = categorySizeRepository.getCachedOrCreateNew(toFind.getCategoryId());
        Assertions.assertThat(singleCached).isNotNull();
    }

    @Test
    public void cacheIsWorking() {
        //find cached valud
        var toFind = checkForEmpty(realCategoryCache.stream().findFirst());
        var singleCached = categorySizeRepository.getCachedOrCreateNew(toFind.getCategoryId());
        assert singleCached != null;
        Assertions.assertThat(singleCached.getCategorySize()).isEqualTo(toFind.getCategorySize());

        //change cached value
        toFind.setCategorySize(toFind.getCategorySize() + 500L);
        categorySizeRepository.insertOrUpdate(toFind);

        //check for cache is not changed
        singleCached = categorySizeRepository.getCachedOrCreateNew(toFind.getCategoryId());
        assert singleCached != null;
        Assertions.assertThat(singleCached.getCategorySize()).isNotEqualTo(toFind.getCategorySize());

        //invalidate cache and check for changed value equals
        categorySizeRepository.updateCache();
        singleCached = categorySizeRepository.getCachedOrCreateNew(toFind.getCategoryId());
        assert singleCached != null;
        Assertions.assertThat(singleCached.getCategorySize()).isEqualTo(toFind.getCategorySize());
    }

    @Test
    public void checkCleanSkutherFactor() {
        var id = 400013L;
        categorySizeRepository.insert(CategorySizeInfoDTO.builder().categoryId(id).skutcherFactor(0.54321f).build());
        CategorySizeInfoDTO byId = categorySizeRepository.findById(id);
        Assertions.assertThat(byId.getSkutcherFactor()).isCloseTo(0.543f,
                Percentage.withPercentage(2d));
        byId.clearAllocateCategory();
        categorySizeRepository.update(byId);
        Assertions.assertThat(categorySizeRepository.findById(id).getSkutcherFactor()).isNull();
    }

    private <T> T checkForEmpty(Optional<T> toCheck) {
        if (toCheck.isPresent()) {
            return toCheck.get();
        } else {
            Assertions.fail("realCategoryCache doesnt contains elements to check");
            return null;
        }
    }

    private void generateRandomCategoryCache() {
        Long categoryId = idCounter++;
        Long categorySize = new Random().nextLong();
        CategorySizeInfoDTO result = new CategorySizeInfoDTO();
        result.setCategoryId(categoryId);
        result.setCategorySize(categorySize);
        result.setUnknown(false);
        realCategoryCache.add(result);
    }
}
