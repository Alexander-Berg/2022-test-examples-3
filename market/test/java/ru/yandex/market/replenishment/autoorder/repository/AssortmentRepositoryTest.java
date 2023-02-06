package ru.yandex.market.replenishment.autoorder.repository;

import java.util.Optional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Assortment;
import ru.yandex.market.replenishment.autoorder.repository.postgres.AssortmentRepository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
public class AssortmentRepositoryTest extends FunctionalTest {
    @Autowired
    private AssortmentRepository repository;

    private static final Assortment ASSORTMENT_ONE = new Assortment();

    static {
        ASSORTMENT_ONE.setMsku(1);
        ASSORTMENT_ONE.setTitle("Телек \"Северное сияние\"");
        ASSORTMENT_ONE.setCategoryId(2);
        ASSORTMENT_ONE.setPersisted();
    }

    @Test
    @DbUnitDataSet(before = "AssortmentRepository.before.csv")
    public void test() {
        Optional<Assortment> assortment = repository.findById(1L);
        assertTrue(assortment.isPresent());
        assertThat(assortment.get(), is(samePropertyValuesAs(ASSORTMENT_ONE)));
    }
}
