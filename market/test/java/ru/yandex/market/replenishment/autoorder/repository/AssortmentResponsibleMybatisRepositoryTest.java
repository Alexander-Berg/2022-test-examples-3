package ru.yandex.market.replenishment.autoorder.repository;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.AssortmentResponsible;
import ru.yandex.market.replenishment.autoorder.repository.postgres.AssortmentResponsibleMybatisRepository;
import ru.yandex.market.replenishment.autoorder.utils.data_expansion.ExpandingUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
public class AssortmentResponsibleMybatisRepositoryTest extends FunctionalTest {

    @Autowired
    private AssortmentResponsibleMybatisRepository assortmentResponsibleMybatisRepository;

    @Test
    @DbUnitDataSet(before = "AssortmentResponsibleMybatisRepositoryTest.deleteAllExpanded.before.csv",
            after = "AssortmentResponsibleMybatisRepositoryTest.deleteAllExpanded.after.csv")
    public void testDeleteAllExpanded() {
        assortmentResponsibleMybatisRepository.deleteAllExpanded(ExpandingUtils.MAGIC_CONSTANT);
    }

    @Test
    @DbUnitDataSet(before = "AssortmentResponsibleMybatisRepositoryTest.deleteMskuResponsiblesWithCategory.before.csv",
            after = "AssortmentResponsibleMybatisRepositoryTest.deleteMskuResponsiblesWithCategory1p.after.csv")
    public void testDeleteMskuResponsiblesWithCategory1p() {
        List<AssortmentResponsible> deletedItems =
                assortmentResponsibleMybatisRepository.deleteMskuResponsiblesWithCategory(DemandType.TYPE_1P);
        assertThat(deletedItems, hasSize(1));
        AssortmentResponsible deleted = deletedItems.get(0);
        assertThat(deleted.getMsku(), equalTo(10001L));
        assertThat(deleted.getCategory(), nullValue());
        assertThat(deleted.getDemandType(), equalTo(DemandType.TYPE_1P));
    }

    @Test
    @DbUnitDataSet(before = "AssortmentResponsibleMybatisRepositoryTest.deleteMskuResponsiblesWithCategory.before.csv",
            after = "AssortmentResponsibleMybatisRepositoryTest.deleteMskuResponsiblesWithCategory3p.after.csv")
    public void testDeleteMskuResponsiblesWithCategory3p() {
        assortmentResponsibleMybatisRepository.deleteMskuResponsiblesWithCategory(DemandType.TYPE_3P);
    }

    @Test
    @DbUnitDataSet(before = "AssortmentResponsibleMybatisRepositoryTest.deleteMskuResponsiblesWithCategory.before.csv",
            after = "AssortmentResponsibleMybatisRepositoryTest.deleteMskuResponsiblesWithCategoryTender.after.csv")
    public void testDeleteMskuResponsiblesWithCategoryTender() {
        assortmentResponsibleMybatisRepository.deleteMskuResponsiblesWithCategory(DemandType.TENDER);
    }
}
