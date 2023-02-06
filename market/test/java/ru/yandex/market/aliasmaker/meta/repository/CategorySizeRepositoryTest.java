package ru.yandex.market.aliasmaker.meta.repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.aliasmaker.meta.BaseTest;
import ru.yandex.market.aliasmaker.meta.repository.dto.CategorySizeInfoDTO;

/**
 * @author apluhin
 * @created 4/22/22
 */

public class CategorySizeRepositoryTest extends BaseTest {

    @Autowired
    private CategorySizeRepository categorySizeRepository;

    @Test
    public void testUnboundCategory() {
        var now = Instant.now();
        categorySizeRepository.insertBatch(
                CategorySizeInfoDTO.builder().categoryId(1L).allocatedTo("1").allocatedUntil(now.minusSeconds(60)).build(),
                CategorySizeInfoDTO.builder().categoryId(2L).allocatedTo("2").allocatedUntil(now.plusSeconds(60)).build(),
                CategorySizeInfoDTO.builder().categoryId(3L).reserveAllocatedTo("3").reserveAllocatedUntil(now.minusSeconds(60)).build(),
                CategorySizeInfoDTO.builder().categoryId(4L).reserveAllocatedTo("4").reserveAllocatedUntil(now.plusSeconds(60)).build()
        );
        categorySizeRepository.unboundCategory();

        List<CategorySizeInfoDTO> allocatedCategory = categorySizeRepository.findAllocatedCategory(false);
        Assertions.assertThat(allocatedCategory.stream().map(CategorySizeInfoDTO::getCategoryId))
                .containsExactlyInAnyOrder(2L, 4L);
    }

    @Test
    public void testAllocateOffset() {
        var allocate = Instant.ofEpochMilli(new Date().getTime()).plus(5, ChronoUnit.MINUTES);
        categorySizeRepository.deleteAll();
        categorySizeRepository.insertBatch(
                CategorySizeInfoDTO.builder().categoryId(1L).allocatedTo("1").allocatedUntil(allocate).build(),
                CategorySizeInfoDTO.builder().categoryId(2L).allocatedTo("2").allocatedUntil(allocate.plus(6,
                        ChronoUnit.MINUTES)).build()
        );
        long timeOffset = 600_000L;
        categorySizeRepository.updateAllocatedTo(Set.of(1L, 2L), timeOffset);
        Assertions.assertThat(categorySizeRepository.findById(1L).getAllocatedUntil())
                .isAfterOrEqualTo(allocate.plusMillis(timeOffset));
        Assertions.assertThat(categorySizeRepository.findById(2L).getAllocatedUntil())
                .isEqualTo(allocate.plus(6, ChronoUnit.MINUTES));
    }
}
