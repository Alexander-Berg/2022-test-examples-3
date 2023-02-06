package ru.yandex.market.fulfillment.stockstorage;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.JobWhPair;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.SyncJobName;
import ru.yandex.market.fulfillment.stockstorage.service.sync.KorobyteSync;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemReference;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KorobyteSyncTest extends AbstractContextualTest {

    private static final UnitId FF_UNIT_0 = new UnitId(null, 12L, "sku0");
    private static final UnitId FF_UNIT_1 = new UnitId(null, 12L, "sku1");
    private static final Korobyte KOROBYTE_0 = new Korobyte(3, 1, 2, BigDecimal.valueOf(4), null, null);
    private static final Korobyte KOROBYTE_1 = new Korobyte(7, 5, 6, BigDecimal.valueOf(8), null, null);
    private static final ItemReference ITEM_REFERENCE_0 = new ItemReference(
            FF_UNIT_0,
            KOROBYTE_0,
            123,
            Collections.emptySet(),
            null
    );
    private static final ItemReference ITEM_REFERENCE_1 = new ItemReference(
            FF_UNIT_1,
            KOROBYTE_1,
            123,
            Collections.emptySet(),
            null
    );
    private static final Partner PARTNER = new Partner(1L);

    @Autowired
    private FulfillmentClient lgwClient;
    @Autowired
    private KorobyteSync korobyteSync;

    @BeforeEach
    public void before() {
        setActiveWarehouses(1);
    }

    @AfterEach
    @Override
    public void resetMocks() {
        super.resetMocks();
        Mockito.reset(lgwClient);
    }

    /**
     * Сценарий #1:
     * <p>
     * Проверяем, что при trigger'е на пустой БД - не будет произведено ни 1 батча в execution_queue.
     */
    @Test
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/1.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void triggerOnEmptyDatabase() {
        trigger();
    }

    /**
     * Сценарий #2:
     * <p>
     * Проверяем, что при consume'е на пустой БД - не будет вызван LGW.
     */
    @Test
    public void consumeOnEmptyDatabase() {
        korobyteSync.consume();

        verify(lgwClient, Mockito.never()).getReferenceItems(anyList(), any(Partner.class));
    }

    /**
     * Сценарий #3:
     * <p>
     * Проверяем, что при trigger'е на БД, в которой у всех SKU уже существуют заполнены korobyte'ы -
     * не будет произведена ни одна запись в execution_queue.
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/3.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void triggerOnAllSkusWithKorobytes() {
        trigger();
    }

    /**
     * Сценарий #4:
     * <p>
     * Проверяем, что при trigger'е на БД, в которой у всех SKU отсутствуют korobyte'ы -
     * будет создана соответствующая запись в execution_queue.
     */
    @Test
    @DatabaseSetup({
            "classpath:database/states/stocks_pushed.xml",
            "classpath:database/states/korobytes/sync/4.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/4.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void triggerOnAllSkusWithoutKorobytes() {
        trigger();
    }

    /**
     * Сценарий #5:
     * <p>
     * Проверяем, что при consume'е на БД, в которой присутствует 1 корректная запись в execution_queue
     * с типом KOROBYTE_SYNC будет успешно произведен синк необходимых SKU в БД.
     */
    @Test
    @DatabaseSetup({
            "classpath:database/states/stocks_pushed.xml",
            "classpath:database/states/korobytes/sync/5.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/stocks_korobytes_pushed.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/5.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void consumeOnAllSkusWithoutKorobytes() {
        List<UnitId> units = asList(FF_UNIT_0, FF_UNIT_1);
        when(lgwClient.getReferenceItems(units, PARTNER))
                .thenReturn(asList(ITEM_REFERENCE_0, ITEM_REFERENCE_1));
        korobyteSync.consume();
        verify(lgwClient).getReferenceItems(eq(units), eq(PARTNER));
    }

    /**
     * Сценарий #6:
     * <p>
     * Проверяем, что при consume на БД, в которой присутствует батч от 0 до 20, по факту будут обработаны только те
     * SKU,
     * у которых действительно нету korobyte'ов или же они заполнены частично.
     */
    @Test
    @DatabaseSetup({
            "classpath:database/states/stocks_korobytes_of_one_stock_pushed.xml",
            "classpath:database/states/korobytes/sync/6.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/stocks_korobytes_of_one_stock_synced.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/6.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void consumeOnOneSkuWithoutKorobytes() {
        when(lgwClient.getReferenceItems(anyList(), any(Partner.class)))
                .thenReturn(singletonList(ITEM_REFERENCE_1));
        korobyteSync.consume();
        verify(lgwClient).getReferenceItems(eq(singletonList(FF_UNIT_1)), eq(PARTNER));
    }

    /**
     * Сценарий #7:
     * <p>
     * Проверяем, что при trigger'е на БД, в которой присутствует только 1 СКУ, у которого необходимо обновить
     * коробайты -
     * будет создан ровно 1 батч с соответствующими параметрами пагинации и total.
     */
    @Test
    @DatabaseSetup({
            "classpath:database/states/stocks_korobytes_of_one_stock_pushed.xml",
            "classpath:database/states/korobytes/sync/7.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/7.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void triggerOnOneSkuWithoutKorobytes() {
        trigger();
    }

    /**
     * Сценарий #8:
     * Проверяем, что при trigger'е на БД, в которой присутствуют СКУ с разных warehouse'ов -
     * в БД будут созданы батчи для каждого из них.
     */
    @Test
    @DatabaseSetup("classpath:database/states/korobytes/sync/8.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/8.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void triggerOnSkusFromDifferentWarehouses() {
        setActiveWarehouses(666, 667);

        trigger();
    }

    /**
     * Сценарий #9:
     * <p>
     * Проверяем, что при consume'е на БД, в которой присутствует батч на warehouseId, отличный от 1/145 -
     * он будет использован в запросах к LGW.
     */
    @Test
    @DatabaseSetup("classpath:database/states/korobytes/sync/9.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/9.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void consumeOnWarehouseIdDifferentFromDefault() {
        when(lgwClient.getReferenceItems(anyList(), any(Partner.class)))
                .thenReturn(singletonList(ITEM_REFERENCE_0));
        setActiveWarehouses(666);

        korobyteSync.consume();
        verify(lgwClient).getReferenceItems(eq(singletonList(FF_UNIT_0)), eq(new Partner(666L)));
    }

    /**
     * Сценарий #10:
     * <p>
     * Проверяем, что в случае если коробайты товара не отличаются от того, что сохранено в БД - апдейта не произойдет.
     * У полученного и хранимого коробайта - все обязательные поля (w,h,l,weight_gross) = 0.
     * Не должно произойти обновления sku, появления информации в event_audit и записи в mds_updater_queue.
     */
    @Test
    @DatabaseSetup("classpath:database/states/korobytes/sync/10.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/10.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void equalKorobytesAreNotUpdated() {
        Korobyte zeroKorobyte = new Korobyte(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        ItemReference itemReferenceWithZeroKorobyte = new ItemReference(
                FF_UNIT_0,
                zeroKorobyte,
                0,
                Collections.emptySet(),
                null
        );

        when(lgwClient.getReferenceItems(anyList(), any(Partner.class)))
                .thenReturn(singletonList(itemReferenceWithZeroKorobyte));
        korobyteSync.consume();
        verify(lgwClient).getReferenceItems(eq(singletonList(FF_UNIT_0)), eq(new Partner(1L)));
    }

    /**
     * Сценарий #11:
     * Проверяем, что при consume'е на warehouse'е, который не считается активным - батч будет обработан.
     */
    @Test
    @DatabaseSetup("classpath:database/states/korobytes/sync/11.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/11.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void consumeOnDisabledWarehouse() {
        when(lgwClient.getReferenceItems(anyList(), any(Partner.class)))
                .thenReturn(singletonList(ITEM_REFERENCE_0));

        setActiveWarehouses();

        korobyteSync.consume();

        verify(lgwClient).getReferenceItems(eq(singletonList(FF_UNIT_0)), eq(new Partner(666L)));
    }

    /**
     * Сценарий #12:
     * Проверяем, что при trigger'е батчи будут созданы только для тех warehouse'ов, которые считаются активными.
     */
    @Test
    @DatabaseSetup("classpath:database/states/korobytes/sync/12.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/12.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void triggerOnDisabledWarehouse() {
        setActiveWarehouses(666);

        trigger();
    }

    /**
     * Сценарий #13:
     * В БД нет подходящих СКУ и присутствует активный warehouse_id = 1.
     * <p>
     * В очереди НЕ должна появиться запись
     * from=0 to=0 (Батч пустой БД)
     */
    @Test
    @DatabaseSetup("classpath:database/states/korobytes/sync/13.xml")
    @ExpectedDatabase(value = "classpath:database/states/korobytes/sync/13.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void triggerOnEmptyDatabaseWithActiveWarehouses() {
        setActiveWarehouses(1);

        trigger();
    }

    /**
     * Сценарий #14:
     * Проверка установки pageSize.
     * Запускаем генерацию таски через JobWhPair, в котором задаем batchSize = 250.
     * Проверяем, что размер pageSize в записи execution_queue будут равны 250.
     * <p>
     * В очереди execution_queue должны появиться одна запись с pageSize = 250.
     */
    @Test
    @DatabaseSetup({
            "classpath:database/states/korobytes/sync/sku_for_korobyte_sync.xml",
            "classpath:database/states/korobytes/sync/14.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/14.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void triggerByJobWhPairWithPageSize() {
        setActiveWarehouses(1);

        trigger();
    }

    /**
     * Сценарий #15:
     * Проверка установки невалидного pageSize.
     * Запускаем генерацию таски через JobWhPair, в котором задаем невалидный размер batchSize = 0.
     * Проверяем, что новых записей в execution_queue не появилось.
     * <p>
     * В очереди execution_queue не должно появиться ни одной запись.
     */
    @Test
    @DatabaseSetup("classpath:database/states/korobytes/sync/sku_for_korobyte_sync.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/15.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void triggerByJobWhPairWithInvalidPageSize() {
        final JobWhPair pair = new JobWhPair(1, "KorobyteSync").setBatchSize(0);

        korobyteSync.trigger(pair);
    }

    /**
     * Сценарий #16:
     * Проверка фильтра по имени джобы.
     * В БД есть две sku, с разным warehouse_id = (1, 2), при этом отсутствуют записи в FFInterval для KorobyteSync.
     * Проверяем, что записи в execution_queue не будут созданы,
     * тк имя полученной по-умолчанию джобы не соответствует запускаемой джобы.
     * <p>
     * В очереди execution_queue не должно появиться ни одной записи.
     */
    @Test
    @DatabaseSetup("classpath:database/states/korobytes/sync/sku_for_korobyte_sync.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/16.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldFilterJobByName() {
        setActiveWarehouses(1, 2);

        trigger();
    }

    /**
     * Сценарий #17:
     * - продьюсер добавляет в очередь таску для последнего батча (from: 1000, to: MAX_LONG, pageSize: 5, last: true)
     * - к моменту тоого, как консьюмер начинает читать таску из очереди, под последний батч уже попадает не 5 sku, а 11
     * - консьюмер должен дернуть getReferenceItems по складу трижды с батчем по 5
     * <p>
     * В очереди execution_queue не должно появиться ни одной записи.
     */
    @Test
    @DatabaseSetup("classpath:database/states/korobytes/sync/17.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/17.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldProcessLastPageByCorrectPageSize() {
        setActiveWarehouses(PARTNER.getId().intValue());

        // first batch
        UnitId unitId0 = new UnitId(null, 12L, "sku0");
        ItemReference item0 = createItem(unitId0, PARTNER.getId(), KOROBYTE_0);

        UnitId unitId1 = new UnitId(null, 12L, "sku1");
        ItemReference item1 = createItem(unitId1, PARTNER.getId(), KOROBYTE_0);

        // second batch
        UnitId unitId2 = new UnitId(null, 12L, "sku2");
        ItemReference item2 = createItem(unitId2, PARTNER.getId(), KOROBYTE_0);

        UnitId unitId3 = new UnitId(null, 12L, "sku3");
        ItemReference item3 = createItem(unitId3, PARTNER.getId(), KOROBYTE_0);

        // third batch
        UnitId unitId4 = new UnitId(null, 12L, "sku4");
        ItemReference item4 = createItem(unitId4, PARTNER.getId(), KOROBYTE_0);

        when(lgwClient.getReferenceItems(anyList(), any())).thenAnswer(invocation -> {
            if (!invocation.getArguments()[1].equals(PARTNER)) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            List<UnitId> firstArg = (List) invocation.getArguments()[0];
            if (ImmutableSet.of(unitId0, unitId1).containsAll(firstArg)) {
                return ImmutableList.of(item0, item1);
            } else if (ImmutableSet.of(unitId2, unitId3).containsAll(firstArg)) {
                return ImmutableList.of(item2, item3);
            } else if (Collections.singletonList(unitId4).containsAll(firstArg)) {
                return Collections.singletonList(item4);
            }

            return Collections.emptyList();
        });

        korobyteSync.consume();

        verify(lgwClient, times(3)).getReferenceItems(any(), any());
    }

    private ItemReference createItem(UnitId unitId, Long warehouseId, Korobyte korobyte) {
        return new ItemReference(
                unitId,
                korobyte,
                warehouseId.intValue(),
                Collections.emptySet(),
                null
        );
    }

    /**
     * Сценарий #18:
     * <p>
     * Проверяем, что при consume'е на БД, в которой присутствует 1 корректная запись в execution_queue
     * с типом KOROBYTE_SYNC, в случае фейла от LGW, останется запись в execution_queue, а attempt_number у нее будет
     * увеличен на единицу.
     */
    @Test
    @DatabaseSetup({
            "classpath:database/states/stocks_pushed.xml",
            "classpath:database/states/korobytes/sync/18.xml"
    })
    @ExpectedDatabase(
            value = "classpath:database/states/stocks_pushed.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
            value = "classpath:database/expected/korobytes/sync/18.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void consumeWithLgwError() {
        List<UnitId> units = asList(FF_UNIT_0, FF_UNIT_1);
        when(lgwClient.getReferenceItems(units, PARTNER)).thenThrow(new RuntimeException());
        korobyteSync.consume();
    }

    private void trigger() {
        Set<JobWhPair> jobWhPairs = warehouseSyncService.getSyncJobWHPairs(1).stream()
                .filter(jobWhPair -> StringUtils.equals(jobWhPair.getSyncJobName(),
                        SyncJobName.KOROBYTE_SYNC.getValue()))
                .collect(Collectors.toSet());

        korobyteSync.trigger(jobWhPairs);
    }
}
