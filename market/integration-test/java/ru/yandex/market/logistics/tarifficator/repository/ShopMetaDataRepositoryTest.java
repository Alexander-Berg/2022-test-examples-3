package ru.yandex.market.logistics.tarifficator.repository;

import javax.transaction.Transactional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.entity.shop.ShopMetaDataEntity;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.PartnerPlacementProgramType;
import ru.yandex.market.logistics.tarifficator.repository.shop.ShopMetaDataRepository;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
public class ShopMetaDataRepositoryTest extends AbstractContextualTest {

    @Autowired
    private ShopMetaDataRepository tested;

    @Test
    @DatabaseSetup("/repository/shop-meta-data/shopMetaData.before.xml")
    @ExpectedDatabase(
        value = "/repository/shop-meta-data/createShopMetaData.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateShopMetaData() {
        ShopMetaDataEntity entity = new ShopMetaDataEntity();
        entity.setShopId(100L);
        entity.setCurrency(Currency.EUR);
        entity.setPlacementPrograms(String.join(",", PartnerPlacementProgramType.DROPSHIP_BY_SELLER.name()));

        tested.save(entity);
    }

    @Test
    @DatabaseSetup("/repository/shop-meta-data/shopMetaData.before.xml")
    @Transactional
    void testGet() {
        softly.assertThat(tested.getOne(774L))
            .isNotNull()
            .extracting(ShopMetaDataEntity::getCurrency)
            .isEqualTo(Currency.RUR);
    }
}
