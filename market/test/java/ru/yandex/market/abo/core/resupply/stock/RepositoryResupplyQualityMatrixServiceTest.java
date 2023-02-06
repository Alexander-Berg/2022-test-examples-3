package ru.yandex.market.abo.core.resupply.stock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.hamcrest.MockitoHamcrest;

import ru.yandex.market.abo.core.category.CanonicalCategoryEntity;
import ru.yandex.market.abo.core.category.CanonicalCategoryRepository;
import ru.yandex.market.abo.core.category.CategoryService;
import ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr;

import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.DEFORMED;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.PACKAGE_CONTAMINATION;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.PACKAGE_HOLES;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.PACKAGE_JAMS;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.PACKAGE_SCRATCHES;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.WRONG_OR_DAMAGED_PAPERS;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
@ParametersAreNonnullByDefault
class RepositoryResupplyQualityMatrixServiceTest {

    @Mock
    private CanonicalCategoryRepository tovarCategoryRepository;

    @Mock
    private ResupplyQualityMatrixRepository qualityMatrixRepository;

    @Mock
    private QualityMatrixAttrInclusionRepository qualityMatrixAttrInclusionRepository;

    @Mock
    private CategoryService categoryService;

    private RepositoryResupplyQualityMatrixService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new RepositoryResupplyQualityMatrixService(
                categoryService,
                tovarCategoryRepository, qualityMatrixRepository, qualityMatrixAttrInclusionRepository
        );
    }

    @Test
    void testExisting() {
        CanonicalCategoryEntity category = new CanonicalCategoryEntity();
        category.setId(27);
        category.setName("Шляпы");
        category.setParentId(6);

        LocalDateTime updateTime = LocalDateTime.now();
        ResupplyQualityMatrixEntity matrix = new ResupplyQualityMatrixEntity().setId(13L)
                .setCategory(category)
                .setUpdatedAt(updateTime.minusDays(1));
        List<QualityMatrixAttrInclusionEntity> beforeInclusions =
                Arrays.asList(
                        newQualityMatrixAttrInclusionEntity(4L, matrix, PACKAGE_SCRATCHES),
                        newQualityMatrixAttrInclusionEntity(5L, matrix, WRONG_OR_DAMAGED_PAPERS),
                        newQualityMatrixAttrInclusionEntity(7L, matrix, DEFORMED),
                        newQualityMatrixAttrInclusionEntity(8L, matrix, PACKAGE_JAMS)
                );
        EnumSet<ResupplyItemAttr> after = EnumSet.of(
                DEFORMED,
                PACKAGE_JAMS,
                PACKAGE_CONTAMINATION,
                PACKAGE_HOLES
        );

        Mockito.when(qualityMatrixRepository.findByCategoryId(Mockito.eq(27)))
                .thenAnswer(invocation -> Optional.of(matrix));
        Mockito.when(qualityMatrixAttrInclusionRepository.findByMatrixId(Mockito.eq(13L)))
                .thenAnswer(invocation -> new ArrayList<>(beforeInclusions));

        service.setCategoryQualityMatrix(27, after, updateTime);

        Mockito.verify(qualityMatrixAttrInclusionRepository).findByMatrixId(Mockito.eq(13L));
        Mockito.verify(qualityMatrixAttrInclusionRepository)
                .deleteAll(MockitoHamcrest.argThat(Matchers.containsInAnyOrder(Arrays.asList(
                        Matchers.hasProperty("id", Matchers.is(Long.valueOf(4))),
                        Matchers.hasProperty("id", Matchers.is(Long.valueOf(5)))
                ))));
        Mockito.verify(qualityMatrixAttrInclusionRepository)
                .saveAll(MockitoHamcrest.argThat(Matchers.<QualityMatrixAttrInclusionEntity>containsInAnyOrder(Arrays.asList(
                        Matchers.allOf(
                                Matchers.hasProperty("id", Matchers.nullValue()),
                                Matchers.hasProperty("attribute", Matchers.is(PACKAGE_CONTAMINATION)),
                                Matchers.hasProperty("matrix", Matchers.is(matrix))
                        ),
                        Matchers.allOf(
                                Matchers.hasProperty("id", Matchers.nullValue()),
                                Matchers.hasProperty("attribute", Matchers.is(PACKAGE_HOLES)),
                                Matchers.hasProperty("matrix", Matchers.is(matrix))
                        )
                ))));
        Mockito.verifyNoMoreInteractions(qualityMatrixAttrInclusionRepository);
    }

    @Test
    void testNewMatrix() {
        CanonicalCategoryEntity category = new CanonicalCategoryEntity();
        category.setId(27);
        category.setName("Шляпы");
        category.setParentId(6);

        LocalDateTime updateTime = LocalDateTime.now();
        ResupplyQualityMatrixEntity matrix = new ResupplyQualityMatrixEntity().setId(13L)
                .setCategory(category)
                .setUpdatedAt(updateTime.minusDays(1));
        EnumSet<ResupplyItemAttr> attributes = EnumSet.of(
                PACKAGE_JAMS,
                PACKAGE_CONTAMINATION,
                PACKAGE_HOLES
        );

        Mockito.when(qualityMatrixRepository.findByCategoryId(Mockito.eq(27)))
                .thenReturn(Optional.empty());
        Mockito.when(tovarCategoryRepository.findById(Mockito.eq(27)))
                .thenReturn(Optional.of(category));
        Mockito.when(qualityMatrixRepository.save(Mockito.any(ResupplyQualityMatrixEntity.class)))
                .thenReturn(matrix);
        Mockito.when(qualityMatrixAttrInclusionRepository.findByMatrixId(Mockito.eq(13L)))
                .thenAnswer(invocation -> new ArrayList<>());

        service.setCategoryQualityMatrix(27, attributes, updateTime);

        Mockito.verify(qualityMatrixAttrInclusionRepository).findByMatrixId(Mockito.eq(13L));
        Mockito.verify(qualityMatrixAttrInclusionRepository)
                .deleteAll(MockitoHamcrest.argThat(Matchers.empty()));
        Mockito.verify(qualityMatrixAttrInclusionRepository)
                .saveAll(MockitoHamcrest.argThat(Matchers.<QualityMatrixAttrInclusionEntity>containsInAnyOrder(Arrays.asList(
                        Matchers.allOf(
                                Matchers.hasProperty("id", Matchers.nullValue()),
                                Matchers.hasProperty("attribute", Matchers.is(PACKAGE_JAMS)),
                                Matchers.hasProperty("matrix", Matchers.is(matrix))
                        ),
                        Matchers.allOf(
                                Matchers.hasProperty("id", Matchers.nullValue()),
                                Matchers.hasProperty("attribute", Matchers.is(PACKAGE_CONTAMINATION)),
                                Matchers.hasProperty("matrix", Matchers.is(matrix))
                        ),
                        Matchers.allOf(
                                Matchers.hasProperty("id", Matchers.nullValue()),
                                Matchers.hasProperty("attribute", Matchers.is(PACKAGE_HOLES)),
                                Matchers.hasProperty("matrix", Matchers.is(matrix))
                        )
                ))));
        Mockito.verifyNoMoreInteractions(qualityMatrixAttrInclusionRepository);
    }

    @Test
    void testDeleteAll() {
        CanonicalCategoryEntity category = new CanonicalCategoryEntity();
        category.setId(27);
        category.setName("Шляпы");
        category.setParentId(6);

        LocalDateTime updateTime = LocalDateTime.now();
        ResupplyQualityMatrixEntity matrix = new ResupplyQualityMatrixEntity().setId(13L)
                .setCategory(category)
                .setUpdatedAt(updateTime.minusDays(1));
        List<QualityMatrixAttrInclusionEntity> beforeInclusions =
                Arrays.asList(
                        newQualityMatrixAttrInclusionEntity(4L, matrix, PACKAGE_SCRATCHES),
                        newQualityMatrixAttrInclusionEntity(5L, matrix, WRONG_OR_DAMAGED_PAPERS)
                );

        Mockito.when(qualityMatrixRepository.findByCategoryId(Mockito.eq(27)))
                .thenAnswer(invocation -> Optional.of(matrix));
        Mockito.when(qualityMatrixAttrInclusionRepository.findByMatrixId(Mockito.eq(13L)))
                .thenAnswer(invocation -> new ArrayList<>(beforeInclusions));

        service.setCategoryQualityMatrix(27, Collections.emptySet(), updateTime);

        Mockito.verify(qualityMatrixAttrInclusionRepository).findByMatrixId(Mockito.eq(13L));
        Mockito.verify(qualityMatrixAttrInclusionRepository)
                .deleteAll(MockitoHamcrest.argThat(Matchers.containsInAnyOrder(Arrays.asList(
                        Matchers.hasProperty("id", Matchers.is(Long.valueOf(4))),
                        Matchers.hasProperty("id", Matchers.is(Long.valueOf(5)))
                ))));
        Mockito.verify(qualityMatrixAttrInclusionRepository)
                .saveAll(MockitoHamcrest.argThat(Matchers.<QualityMatrixAttrInclusionEntity>empty()));
        Mockito.verifyNoMoreInteractions(qualityMatrixAttrInclusionRepository);
    }

    private static QualityMatrixAttrInclusionEntity newQualityMatrixAttrInclusionEntity(
            long id,
            ResupplyQualityMatrixEntity matrix,
            ResupplyItemAttr attribute
    ) {
        return new QualityMatrixAttrInclusionEntity().setId(id).setMatrix(matrix).setAttribute(attribute);
    }
}
