package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThatCode;


public class ReferenceItemRepositoryImplTest extends MdmBaseDbTestClass {

    private static final String TEST_SHOP_SSKU = "DEADLOCK_TEST";
    private static final int NUMBER_OF_ITEMS = 100;
    private static final int NUMBER_OF_CONCURRENT_PROCESSES = 10;

    @Autowired
    ReferenceItemRepository repository;

    @After
    public void setUp() throws ExecutionException, InterruptedException {
        // удаляем через дочерний поток, чтобы не держать лок
        runAsync(() -> repository.deleteBatch(repository.findAll()
            .stream()
            .filter(it -> it.getKey().getShopSku().equals(TEST_SHOP_SSKU))
            .collect(Collectors.toList())
        )).get();
    }

    @Test
    public void shouldNotHaveDeadlocks() throws InterruptedException, ExecutionException {
        // given
        List<ReferenceItemWrapper> items = new ArrayList<>();
        for (int i = 1; i < NUMBER_OF_ITEMS; i++) {
            ReferenceItemWrapper item = new ReferenceItemWrapper(new ShopSkuKey(i, TEST_SHOP_SSKU));
            items.add(item);
        }
        // вставляем через дочерний поток, чтобы дочерние транзакции увидели реузльтат
        runAsync(() -> repository.insertBatch(items)).get();

        // when
        List<CompletableFuture<Void>> parallelUpdates = new ArrayList<>(NUMBER_OF_CONCURRENT_PROCESSES);
        for (int i = 0; i < NUMBER_OF_CONCURRENT_PROCESSES; i++) {
            parallelUpdates.add(runAsync(() -> makeTxn(items)));
        }
        CompletableFuture<Void> parallelUpdate = allOf(parallelUpdates.toArray(new CompletableFuture[0]));
        assertThatCode(parallelUpdate::get)
            .doesNotThrowAnyException();
    }

    private void makeTxn(List<ReferenceItemWrapper> items) {
        List<ShopSkuKey> ids = items.stream()
            .map(ReferenceItemWrapper::getKey)
            .collect(Collectors.toList());

        repository.processWithLock(ids, it ->
            it.stream()
                // какая-то бесполезная работа с записями
                .map(el -> el.setWeightDimensionsUpdatedTs(el.getWeightDimensionsUpdatedTs().minusMillis(100L)))
                .collect(Collectors.toList()));
    }
}
