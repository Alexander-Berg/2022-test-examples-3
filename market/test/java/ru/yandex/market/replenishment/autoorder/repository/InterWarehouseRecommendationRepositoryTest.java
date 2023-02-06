package ru.yandex.market.replenishment.autoorder.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.InterWarehouseRecommendationsFilter;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjInterWarehouseRecommendationQtyDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.InterWarehouseExportedRecommendationDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.InterWarehouseRecommendation;
import ru.yandex.market.replenishment.autoorder.model.dto.InterWarehouseRecommendationsFilterDTO;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Assortment;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Category;
import ru.yandex.market.replenishment.autoorder.repository.postgres.AssortmentRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.CategoryRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.InterWarehouseRecommendationRepository;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class InterWarehouseRecommendationRepositoryTest extends FunctionalTest {

    @Autowired
    private InterWarehouseRecommendationRepository repository;

    @Autowired
    private AssortmentRepository assortmentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationRepository.before.csv")
    public void adjustRecommendationWithId() {
        var recommendationDTO = new AdjInterWarehouseRecommendationQtyDTO();
        recommendationDTO.setAdjustedPurchQty(1L);
        recommendationDTO.setCorrectionReason(1L);

        repository.adjustRecommendationById(2L, "boris", LocalDateTime.of(2020, 9, 1, 0, 0), recommendationDTO);
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationRepository.before.csv")
    public void getRecommendationsWithoutFilter() {
        InterWarehouseRecommendationsFilterDTO filter = new InterWarehouseRecommendationsFilterDTO(
            "2019-03-25",
            "2019-03-28",
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        List<InterWarehouseRecommendation> recommendations = repository.getInterWarehouseRecommendations(filter);

        assertThat(recommendations, hasSize(3));
        recommendations.sort(Comparator.comparing(InterWarehouseRecommendation::getMsku));
        assertEquals(100L, recommendations.get(0).getMsku());
        assertEquals(200L, recommendations.get(1).getMsku());
        assertEquals(300L, recommendations.get(2).getMsku());
        assertTrue(recommendations.get(1).isLoaded());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationRepository.before.csv")
    public void getRecommendationsWithFilter() {
        InterWarehouseRecommendationsFilterDTO filter = new InterWarehouseRecommendationsFilterDTO(
            "2019-03-25",
            "2019-03-26",
            null,
            171L,
            null,
            null,
            null,
            null,
            null
        );
        List<InterWarehouseRecommendation> recommendations = repository.getInterWarehouseRecommendations(filter);

        assertThat(recommendations, hasSize(1));
        assertEquals(200L, recommendations.get(0).getMsku());
        assertTrue(recommendations.get(0).isLoaded());
        assertNull(recommendations.get(0).getTruckQuantity());
        assertNull(recommendations.get(0).getExportedQty());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseReplenishmentRepository_LoadedFilter.before.csv")
    public void getRecommendationsWithFilterLoadedIsFalse() {
        InterWarehouseRecommendationsFilterDTO filter = new InterWarehouseRecommendationsFilterDTO(
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null
        );
        List<InterWarehouseRecommendation> recommendations = repository.getInterWarehouseRecommendations(filter);

        assertThat(recommendations, hasSize(1));
        assertEquals(1L, recommendations.get(0).getId());
        assertFalse(recommendations.get(0).isLoaded());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseReplenishmentRepository_LoadedFilter.before.csv")
    public void getRecommendationsWithFilterLoadedIsTrue() {
        InterWarehouseRecommendationsFilterDTO filter = new InterWarehouseRecommendationsFilterDTO(
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            null,
            null
        );
        List<InterWarehouseRecommendation> recommendations = repository.getInterWarehouseRecommendations(filter);

        assertThat(recommendations, hasSize(1));
        assertEquals(2L, recommendations.get(0).getId());
        assertTrue(recommendations.get(0).isLoaded());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationRepository.before.csv")
    public void getRecommendationsWithDeepMindErrorCode() {
        InterWarehouseRecommendationsFilterDTO filter = new InterWarehouseRecommendationsFilterDTO(
            "2019-03-25",
            "2019-03-28",
            null,
            null,
            null,
            null,
            null,
            "Broken mind",
            null
        );
        List<InterWarehouseRecommendation> recommendations = repository.getInterWarehouseRecommendations(filter);

        assertThat(recommendations, hasSize(1));
        assertEquals(3L, recommendations.get(0).getId());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationRepository.before.csv")
    public void getRecommendationsByTruckId() {
        InterWarehouseRecommendationsFilter filter = new InterWarehouseRecommendationsFilter(
            "2019-03-25",
            "2019-03-26",
            6L,
            null,
            null,
            null,
            null
        );
        List<InterWarehouseRecommendation> recommendations = repository.getInterWarehouseRecommendationsByTruck(filter);

        assertThat(recommendations, hasSize(1));
        assertEquals(100L, recommendations.get(0).getMsku());
        assertTrue(recommendations.get(0).isLoaded());
        assertEquals(Long.valueOf(3), recommendations.get(0).getTruckQuantity());
        assertEquals(Long.valueOf(7), recommendations.get(0).getExportedQty());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationRepository.before.csv")
    public void getRecommendationsByExportedTruckId() {
        InterWarehouseRecommendationsFilter filter = new InterWarehouseRecommendationsFilter(
            null,
            null,
            2L,
            null,
            null,
            null,
            null
        );
        List<InterWarehouseRecommendation> recommendations =
            repository.getExportedInterWarehouseRecommendationsByTruck(filter);

        assertThat(recommendations, hasSize(2));
        recommendations.sort(Comparator.comparing(InterWarehouseRecommendation::getMsku));
        assertEquals(400L, recommendations.get(0).getMsku());
        assertTrue(recommendations.get(0).isLoaded());
        assertEquals(Long.valueOf(3), recommendations.get(0).getTruckQuantity());
        assertNull(recommendations.get(0).getExportedQty());
        assertEquals(500L, recommendations.get(1).getMsku());
        assertEquals(Long.valueOf(5), recommendations.get(1).getTruckQuantity());
        assertNull(recommendations.get(1).getExportedQty());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationRepository.before.csv")
    public void getInterWarehouseRecommendationDTO() {
        Assortment assortment = new Assortment();
        assortment.setMsku(200);
        assortment.setTitle("test-msku");
        assortment.setCategoryId(100500);
        assortmentRepository.save(assortment);

        Category category = new Category();
        category.setId(100500);
        category.setName("test-category");
        categoryRepository.save(category);

        InterWarehouseRecommendationsFilterDTO filter = new InterWarehouseRecommendationsFilterDTO(
            "2019-03-25",
            "2019-03-28",
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        List<InterWarehouseRecommendation> recommendations =
            repository.getInterWarehouseRecommendationsForPage(null, null, filter);

        assertThat(recommendations, hasSize(3));
        recommendations.sort(Comparator.comparing(InterWarehouseRecommendation::getMsku));

        assertEquals(100L, recommendations.get(0).getMsku());
        assertNull(recommendations.get(0).getLeafCategoryId());
        assertNull(recommendations.get(0).getLeafCategoryName());

        assertEquals(200L, recommendations.get(1).getMsku());
        assertTrue(recommendations.get(1).isLoaded());
        assertEquals(Long.valueOf(100500L), recommendations.get(1).getLeafCategoryId());
        assertEquals("test-msku", recommendations.get(1).getTitle());
        assertEquals("test-category", recommendations.get(1).getLeafCategoryName());

        assertEquals(300L, recommendations.get(2).getMsku());
        assertNull(recommendations.get(2).getLeafCategoryId());
        assertNull(recommendations.get(2).getLeafCategoryName());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationRepository.before.csv")
    public void getInterWarehouseRecommendationDTOForPage() {
        InterWarehouseRecommendationsFilterDTO filter = new InterWarehouseRecommendationsFilterDTO(
            "2019-03-25",
            "2019-03-28",
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        List<InterWarehouseRecommendation> recommendations =
            repository.getInterWarehouseRecommendationsForPage(1L, 2L, filter);

        assertThat(recommendations, hasSize(2));
        recommendations.sort(Comparator.comparing(InterWarehouseRecommendation::getMsku));
        assertEquals(100L, recommendations.get(0).getMsku());
        assertEquals(200L, recommendations.get(1).getMsku());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationRepository.before.csv")
    public void getExportedRecommendationsWithoutFilter() {
        InterWarehouseRecommendationsFilterDTO filter = new InterWarehouseRecommendationsFilterDTO(
            "2021-03-24",
            "2021-03-26",
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            null,
            null
        );
        List<InterWarehouseExportedRecommendationDTO> recommendations = getExportedRecommendations(filter);

        assertThat(recommendations, hasSize(2));
        recommendations.sort(Comparator.comparing(InterWarehouseExportedRecommendationDTO::getMsku));
        assertEquals(100L, recommendations.get(0).getMsku());
        assertEquals(10L, recommendations.get(0).getPurchaseQuantity());
        assertEquals(8L, recommendations.get(0).getAdjustedPurchaseQuantity());
        assertEquals(1L, recommendations.get(0).getMovementId().longValue());
        assertTrue(recommendations.get(0).isManuallyCreated());

        assertEquals(100L, recommendations.get(1).getMsku());
        assertEquals(20L, recommendations.get(1).getPurchaseQuantity());
        assertEquals(30L, recommendations.get(1).getAdjustedPurchaseQuantity());
        assertEquals(2L, recommendations.get(1).getMovementId().longValue());
        assertFalse(recommendations.get(1).isManuallyCreated());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationRepository.before.csv")
    public void getExportedRecommendationsWithMovementIdFilter() {
        InterWarehouseRecommendationsFilterDTO filter = new InterWarehouseRecommendationsFilterDTO(
            "2021-03-24",
            "2021-03-26",
            null,
            null,
            null,
            null,
            List.of(1L),
            null,
            null
        );
        List<InterWarehouseExportedRecommendationDTO> recommendations = getExportedRecommendations(filter);

        assertThat(recommendations, hasSize(1));
        assertEquals(100L, recommendations.get(0).getMsku());
        assertEquals(10L, recommendations.get(0).getPurchaseQuantity());
        assertEquals(8L, recommendations.get(0).getAdjustedPurchaseQuantity());
        assertEquals(1L, recommendations.get(0).getMovementId().longValue());
        assertTrue(recommendations.get(0).isManuallyCreated());
    }

    private List<InterWarehouseExportedRecommendationDTO> getExportedRecommendations(InterWarehouseRecommendationsFilterDTO filter) {
        return repository.getInterWarehouseExportedRecommendations(null, null, filter);
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationRepository.before.csv")
    public void getCatteams() {
        var result = new HashSet<>(repository.getCatteams());
        assertThat(result, hasSize(3));
        assertTrue(result.contains("Посуда"));
        assertTrue(result.contains("Техника"));
        assertTrue(result.contains("Парфюмерия"));
    }
}
