package ru.yandex.market.mboc.common.parameters;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.jooq.repo.JooqRepository;
import ru.yandex.market.mboc.common.BaseJooqRepositoryTestClass;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.CategoryParameter;

/**
 * @author eremeevvo
 * @since 17.10.2019
 */
public class CategoryParameterRepositoryTest
    extends BaseJooqRepositoryTestClass<CategoryParameter, Long> {

    @Autowired
    private CategoryParameterRepository repository;

    public CategoryParameterRepositoryTest() {
        super(CategoryParameter.class, CategoryParameter::getId);
        generatedFields = new String[]{"modifiedAt", "createdAt"};
    }

    @Override
    protected JooqRepository<CategoryParameter, ?, Long, ?, ?> repository() {
        return repository;
    }
}
