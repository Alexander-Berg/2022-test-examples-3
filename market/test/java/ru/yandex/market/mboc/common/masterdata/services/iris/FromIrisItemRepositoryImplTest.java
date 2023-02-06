package ru.yandex.market.mboc.common.masterdata.services.iris;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

@SuppressWarnings("checkstyle:MagicNumber")
public class FromIrisItemRepositoryImplTest extends MdmBaseDbTestClass {

    private static final long SEED = 19517;
    private static final int VALID_SUPPLIER_ID = 12345;

    @Autowired
    private FromIrisItemRepositoryImpl repository;

    private EnhancedRandom random;

    @Before
    public void before() {
        random = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenInsertItemShouldFindNewEqualItem() {
        FromIrisItemWrapper item = random.nextObject(FromIrisItemWrapper.class);

        // leave single reference information + fix supplier id
        item.setSingleInformationItem(
            item.getItem().toBuilder()
                .setItemId(item.getItem().getItemId().toBuilder().setSupplierId(VALID_SUPPLIER_ID))
                .build());
        Assertions.assertThat(item.getItem().getInformationCount()).isEqualTo(1);

        repository.insert(item);

        FromIrisItemWrapper foundInRepositoryItem = repository.findAll().get(0);
        Assertions.assertThat(foundInRepositoryItem).isNotSameAs(item);
        Assertions.assertThat(foundInRepositoryItem).isEqualToIgnoringGivenFields(item, "receivedTs");
    }

    @Test
    public void whenUpdateItemShouldFindUpdatedItem() {
        FromIrisItemWrapper item1 = random.nextObject(FromIrisItemWrapper.class);
        item1.setSingleInformationItem(
            item1.getItem().toBuilder()
                .setItemId(item1.getItem().getItemId().toBuilder().setSupplierId(VALID_SUPPLIER_ID))
                .build());
        repository.insert(item1);

        MdmIrisPayload.Item.Builder itemBuilder = random.nextObject(FromIrisItemWrapper.class).getItem().toBuilder();
        itemBuilder.setItemId(item1.getMdmIdentifier().toBuilder().setSupplierId(VALID_SUPPLIER_ID));
        itemBuilder.getInformationBuilder(0)
            .setSource(item1.getReferenceInformation().getSource());

        FromIrisItemWrapper item2 = new FromIrisItemWrapper(itemBuilder.build());
        repository.update(item2);

        FromIrisItemWrapper foundInRepositoryItem = repository.findAll().get(0);
        Assertions.assertThat(item1).isNotEqualTo(foundInRepositoryItem);
    }

    @Test
    public void testAllItemsIterator() {
        FromIrisItemWrapper item1 = fromIrisItem(1, "1", MdmIrisPayload.MasterDataSource.SUPPLIER, "1");
        FromIrisItemWrapper item2 = fromIrisItem(1, "1", MdmIrisPayload.MasterDataSource.SUPPLIER, "2");
        FromIrisItemWrapper item3 = fromIrisItem(1, "1", MdmIrisPayload.MasterDataSource.WAREHOUSE, "1");
        FromIrisItemWrapper item4 = fromIrisItem(2, "1", MdmIrisPayload.MasterDataSource.SUPPLIER, "1");
        FromIrisItemWrapper item5 = fromIrisItem(2, "2", MdmIrisPayload.MasterDataSource.MEASUREMENT, "0");
        FromIrisItemWrapper item6 = fromIrisItem(2, "3", MdmIrisPayload.MasterDataSource.MDM, "0");
        FromIrisItemWrapper item7 = fromIrisItem(3, "0", MdmIrisPayload.MasterDataSource.MDM, "0");
        FromIrisItemWrapper item8 = fromIrisItem(3, "8", MdmIrisPayload.MasterDataSource.MDM, "0");
        FromIrisItemWrapper item9 = fromIrisItem(3, "8", MdmIrisPayload.MasterDataSource.MDM, "1");
        FromIrisItemWrapper item10 = fromIrisItem(4, "1", MdmIrisPayload.MasterDataSource.SUPPLIER, "1");
        FromIrisItemWrapper item11 = fromIrisItem(4, "1", MdmIrisPayload.MasterDataSource.SUPPLIER, "2");
        FromIrisItemWrapper item12 = fromIrisItem(4, "1", MdmIrisPayload.MasterDataSource.SUPPLIER, "3");
        FromIrisItemWrapper item13 = fromIrisItem(5, "1", MdmIrisPayload.MasterDataSource.SUPPLIER, "1");

        List<FromIrisItemWrapper> items = new ArrayList<>(List.of(
            item1, item2, item3, item4, item5, item6, item7, item8, item9, item10, item11, item12, item13
        ));
        Collections.shuffle(items);
        items.forEach(repository::insertOrUpdate);

        Iterator<List<FromIrisItemWrapper>> allItemsIterator = repository.allItemsIterator(4, null);

        Assertions.assertThat(allItemsIterator).hasNext();
        Assertions.assertThat(allItemsIterator).hasNext();
        Assertions.assertThat(allItemsIterator.next()).containsExactly(item1, item2, item3, item4);
        Assertions.assertThat(allItemsIterator).hasNext();
        Assertions.assertThat(allItemsIterator.next()).containsExactly(item5, item6, item7, item8);
        Assertions.assertThat(allItemsIterator.next()).containsExactly(item9, item10, item11, item12);
        Assertions.assertThat(allItemsIterator).hasNext();
        Assertions.assertThat(allItemsIterator.next()).containsExactly(item13);
        Assertions.assertThat(allItemsIterator.hasNext()).isFalse();
        //noinspection ConstantConditions
        Assertions.assertThat(allItemsIterator.hasNext()).isFalse();
        Assertions.assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(allItemsIterator::next);
        //noinspection ConstantConditions
        Assertions.assertThat(allItemsIterator.hasNext()).isFalse();
    }

    private FromIrisItemWrapper fromIrisItem(int supplierId,
                                             String shopSku,
                                             MdmIrisPayload.MasterDataSource sourceType,
                                             String sourceId) {
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(
            new ShopSkuKey(supplierId, shopSku),
            sourceType,
            sourceId,
            ItemWrapperTestUtil.generateShippingUnit(
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble()
            )
        );
        return new FromIrisItemWrapper(item);
    }
}
