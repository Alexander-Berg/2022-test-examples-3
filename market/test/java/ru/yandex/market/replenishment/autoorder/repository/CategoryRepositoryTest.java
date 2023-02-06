package ru.yandex.market.replenishment.autoorder.repository;

import java.util.Optional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Category;
import ru.yandex.market.replenishment.autoorder.repository.postgres.CategoryRepository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
public class CategoryRepositoryTest extends FunctionalTest {

    @Autowired
    private CategoryRepository repository;

    private static final Category EXPECTED_RESULT = new Category();

    static {
        EXPECTED_RESULT.setId(2);
        EXPECTED_RESULT.setParentId(1);
        EXPECTED_RESULT.setName("пинетки");
        EXPECTED_RESULT.setPersisted();
    }

    @Test
    @DbUnitDataSet(before = "CategoryRepository.before.csv")
    public void test() {
        Optional<Category> maybeResult = repository.findById(2L);
        assertTrue(maybeResult.isPresent());

        assertThat(maybeResult.get(), is(samePropertyValuesAs(EXPECTED_RESULT)));
    }
}
