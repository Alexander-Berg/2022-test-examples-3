package ru.yandex.market.deepmind.common.repository.category;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseJooqRepositoryTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryAvailabilityMatrix;
import ru.yandex.market.mbo.jooq.repo.JooqRepository;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicnumber")
public class CategoryAvailabilityMatrixRepositoryTest
    extends DeepmindBaseJooqRepositoryTestClass<CategoryAvailabilityMatrix, Long> {
    @Autowired
    private CategoryAvailabilityMatrixRepository categoryAvailabilityMatrixRepository;

    public CategoryAvailabilityMatrixRepositoryTest() {
        super(CategoryAvailabilityMatrix.class, CategoryAvailabilityMatrix::getId);
        generatedFields = new String[]{"modifiedAt", "createdAt", "blockReasonKey"};
    }

    @Override
    protected JooqRepository<CategoryAvailabilityMatrix, ?, Long, ?, ?> repository() {
        return categoryAvailabilityMatrixRepository;
    }
}
