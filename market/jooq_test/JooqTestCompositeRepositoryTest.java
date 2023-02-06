package ru.yandex.market.mboc.common.services.jooq_test;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mbo.jooq.repo.notification.EntityChangeEvent;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.JooqTestComposite;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

public class JooqTestCompositeRepositoryTest extends BaseDbTestClass {

    public static final int TEST_SIZE = 10;

    private static final String ALPHA = "alpha";
    private static final String BETA = "beta";
    private static final String GAMMA = "gamma";

    private static final int HUGE_TEST_DATA_ITEMS_COUNT = 1_000_000;

    private final List<EntityChangeEvent<JooqTestComposite>> changes = new ArrayList<>();

    private EnhancedRandom random;

    @Resource(name = "dsl")
    private DSLContext dslContext;
    private JooqTestCompositeRepository repository;

    @Before
    public void setUp() {
        random = new EnhancedRandomBuilder()
            .seed(123)
            .exclude(OffsetDateTime.class)
            .build();

        repository = new JooqTestCompositeRepository(dslContext);
        repository.addObserver(changes::addAll);
    }

    @Test
    public void saveSuccessful() {
        var created = new JooqTestComposite();
        created.setSupplierId(1);
        created.setShopSku("a");
        created.setDescription(ALPHA);

        var saved = repository.save(created);
        assertThat(saved.getDescription()).isEqualTo(ALPHA);

        var loaded = repository.getById(new ShopSkuKey(1, "a"));
        assertThat(loaded.getDescription()).isEqualTo(ALPHA);
    }

    @Test(expected = RuntimeException.class)
    public void saveFailedWithoutKeySuccessful() {
        var created = new JooqTestComposite();
        created.setDescription(ALPHA);

        repository.save(created);
    }

    @Test
    public void saveNewWithId() {
        var created = random.nextObject(JooqTestComposite.class);

        var saved = repository.save(created);
        Assertions.assertThat(saved)
            .isEqualToIgnoringGivenFields(created, "modifiedDate");

        var loaded = repository.getById(new ShopSkuKey(created.getSupplierId(), created.getShopSku()));
        Assertions.assertThat(loaded)
            .isEqualToIgnoringGivenFields(created, "modifiedDate");
    }

    @Test
    public void saveBatch() {
        List<JooqTestComposite> created = repository.save(
            entityWithDescription(1, "a", ALPHA),
            entityWithDescription(1, "b", BETA),
            entityWithDescription(1, "c", GAMMA)
        );

        assertThat(created).hasSize(3)
            .extracting(JooqTestComposite::getSupplierId)
            .doesNotContainNull();

        List<ShopSkuKey> ids = created.stream().map(v -> new ShopSkuKey(v.getSupplierId(), v.getShopSku()))
            .collect(Collectors.toList());

        List<JooqTestComposite> loaded = repository.getByIds(ids);
        assertThat(loaded)
            .extracting(JooqTestComposite::getDescription)
            .containsExactlyInAnyOrder(ALPHA, BETA, GAMMA);
    }

    @Test
    @Ignore("Медленный тест. Запустите его вручную, чтобы убедиться, что все работает")
    public void fetchMoreThanMaxShort() {
        List<JooqTestComposite> entities = random.ints()
            .limit(HUGE_TEST_DATA_ITEMS_COUNT)
            .mapToObj(i -> new JooqTestComposite().setSupplierId(i).setShopSku("a"))
            .collect(Collectors.toList());

        repository.save(entities);

        List<ShopSkuKey> created = repository.findIds(new JooqTestCompositeRepository.Filter());
        assertThat(created).hasSize(HUGE_TEST_DATA_ITEMS_COUNT);

        int count = repository.count(new JooqTestCompositeRepository.Filter()
            .setIds(created));
        assertThat(created).hasSize(count);
    }

    @Test
    public void saveBatchComplex() {
        List<JooqTestComposite> toExists = random.objects(JooqTestComposite.class, TEST_SIZE)
            .collect(Collectors.toList());
        repository.save(toExists);

        List<JooqTestComposite> existing = toExists.stream()
            .map(e -> repository.getById(new ShopSkuKey(e.getSupplierId(), e.getShopSku())))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        Assertions.assertThat(existing).hasSameSizeAs(toExists);

        List<JooqTestComposite> newWithId = random.objects(JooqTestComposite.class, TEST_SIZE)
            .collect(Collectors.toList());

        List<JooqTestComposite> testData = Stream.of(existing, newWithId)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        List<JooqTestComposite> testDataSaved = repository.save(testData);

        List<JooqTestComposite> testDataLoaded = testDataSaved.stream()
            .map(e -> repository.getById(new ShopSkuKey(e.getSupplierId(), e.getShopSku())))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        Assertions.assertThat(testDataLoaded)
            .usingElementComparatorIgnoringFields("modifiedDate")
            .containsAll(existing);
        Assertions.assertThat(testDataLoaded)
            .usingElementComparatorIgnoringFields("modifiedDate")
            .containsAll(newWithId);
    }

    @Test
    public void textGetExistingIds() {
        List<JooqTestComposite> toExists =
            random.objects(JooqTestComposite.class, TEST_SIZE).collect(Collectors.toList());
        List<JooqTestComposite> newWithId =
            random.objects(JooqTestComposite.class, TEST_SIZE).collect(Collectors.toList());

        repository.save(toExists);

        List<ShopSkuKey> existingIds = toExists.stream().map(e -> new ShopSkuKey(e.getSupplierId(), e.getShopSku()))
            .collect(Collectors.toList());
        List<ShopSkuKey> notExistingIds = newWithId.stream().map(e -> new ShopSkuKey(e.getSupplierId(), e.getShopSku()))
            .collect(Collectors.toList());

        List<ShopSkuKey> idsToTest = Stream.of(existingIds, notExistingIds)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        Set<ShopSkuKey> found = repository.getExistingIds(idsToTest);

        Assertions.assertThat(found)
            .containsOnlyElementsOf(existingIds);
    }

    @Test
    public void findAllAsc() {
        createTestData();

        List<JooqTestComposite> entities = repository
            .find(new JooqTestCompositeRepository.Filter(), JooqTestCompositeRepository.SortBy.DESCRIPTION.asc(),
                OffsetFilter.all());

        assertThat(entities).extracting(JooqTestComposite::getDescription)
            .containsExactly(ALPHA, BETA, GAMMA);
    }

    @Test
    public void getById() {
        repository.save(entityWithDescription(1, "a", ALPHA));
        var created = repository.save(entityWithDescription(2, "b", BETA));
        repository.save(entityWithDescription(1, "g", GAMMA));

        var loaded = repository.getById(new ShopSkuKey(2, "b"));

        assertThat(loaded).isNotNull();
        assertThat(loaded.getSupplierId()).isEqualTo(2);
        assertThat(loaded.getShopSku()).isEqualTo("b");
    }

    @Test
    public void getByIds() {
        repository.save(entityWithDescription(1, "a", ALPHA));
        var beta = repository.save(entityWithDescription(1, "b", BETA));
        var gamma = repository.save(entityWithDescription(1, "g", GAMMA));

        List<JooqTestComposite> loaded = repository.getByIds(
            new ShopSkuKey(1, "b"), new ShopSkuKey(1, "g")
        );

        assertThat(loaded).isNotNull();
        assertThat(loaded).extracting(JooqTestComposite::getDescription)
            .containsExactlyInAnyOrder(BETA, GAMMA);
    }

    @Test
    public void changesSingle() {
        var first = repository.save(entityWithDescription(1, "a", ALPHA));

        assertThat(changes).hasSize(1);
        EntityChangeEvent<JooqTestComposite> change = changes.get(0);
        assertThat(change.getChangeType()).isEqualTo(EntityChangeEvent.ChangeType.ADD);
        assertThat(change.getBefore()).isNull();
        assertThat(change.getAfter()).isNotNull();
        assertThat(change.getAfter().getSupplierId()).isEqualTo(1);
        assertThat(change.getAfter().getDescription()).isEqualTo(ALPHA);

        changes.clear();
        repository.save(first.setDescription(BETA));

        assertThat(changes).hasSize(1);
        change = changes.get(0);
        assertThat(change.getChangeType()).isEqualTo(EntityChangeEvent.ChangeType.CHANGE);
        assertThat(change.getBefore()).isNotNull();
        assertThat(change.getAfter()).isNotNull();
        assertThat(change.getBefore().getDescription()).isEqualTo(ALPHA);
        assertThat(change.getAfter().getDescription()).isEqualTo(BETA);

        changes.clear();
        repository.delete(new ShopSkuKey(1, "a"));

        assertThat(changes).hasSize(1);
        change = changes.get(0);
        assertThat(change.getChangeType()).isEqualTo(EntityChangeEvent.ChangeType.DELETE);
        assertThat(change.getBefore()).isNotNull();
        assertThat(change.getAfter()).isNull();
        assertThat(change.getBefore().getDescription()).isEqualTo(BETA);
    }

    @Test
    public void changesBatch() {
        repository.save(
            entityWithDescription(1, "a", ALPHA),
            entityWithDescription(1, "b", BETA)
        );
        assertThat(changes).hasSize(2);
        assertThat(changes)
            .extracting(EntityChangeEvent::getBefore)
            .containsOnlyNulls();
        assertThat(changes)
            .extracting(EntityChangeEvent::getAfter)
            .extracting(JooqTestComposite::getDescription)
            .containsExactly(ALPHA, BETA);
    }

    @Test
    public void findLimit() {
        createTestData();

        List<JooqTestComposite> entities = repository
            .find(new JooqTestCompositeRepository.Filter(), JooqTestCompositeRepository.SortBy.DESCRIPTION.asc(),
                OffsetFilter.offset(1, 1));

        assertThat(entities).extracting(JooqTestComposite::getDescription)
            .containsExactly(BETA);
    }

    @Test
    public void filter() {
        repository.save(entityWithDescription(1, "aa", null));
        repository.save(entityWithDescription(1, "bb", null));
        List<JooqTestComposite> testData = createTestData();
        repository.save(testData);
        repository.save(entityWithDescription(1, "cc", null));

        int count = repository.count(new JooqTestCompositeRepository.Filter());
        assertThat(count).isEqualTo(testData.size() + 3);

        assertThat(repository.count(new JooqTestCompositeRepository.Filter().setHasDescription(true)))
            .isEqualTo(testData.size());

        List<JooqTestComposite> entities = repository
            .find(new JooqTestCompositeRepository.Filter().setHasDescription(true),
                JooqTestCompositeRepository.SortBy.DESCRIPTION.desc(),
                OffsetFilter.all());

        assertThat(entities).extracting(JooqTestComposite::getDescription)
            .doesNotContainNull();
    }

    @Test
    public void optimisticLocking() {
        var savedFirst = repository.save(entityWithDescription(1, "aa", ALPHA));

        repository.save(savedFirst); // concurrent modification

        assertThatThrownBy(() -> {
            savedFirst.setDescription(BETA);
            repository.save(savedFirst);
        }).isInstanceOf(ConcurrentModificationException.class);

        var loaded = repository.getById(new ShopSkuKey(1, "aa"));

        assertThat(loaded.getDescription()).isEqualTo(ALPHA);
    }

    @Test
    public void testDoubleSave() {
        var save1 = repository.save(entityWithDescription(1, "a", ALPHA));

        save1.setDescription(BETA);
        var save2 = repository.save(save1);
        Assertions.assertThat(save1.getSupplierId()).isEqualTo(save2.getSupplierId());
        Assertions.assertThat(save1.getShopSku()).isEqualTo(save2.getShopSku());
        Assertions.assertThat(save1.getDescription()).isEqualTo(save2.getDescription());
        Assertions.assertThat(save1.getModifiedDate()).isBefore(save2.getModifiedDate());
    }

    @Test
    public void testOptimisticLockingFieldAreGeneratedInLocalTimeZone() {
        var save = repository.save(entityWithDescription(1, "a", null));

        Assertions.assertThat(save.getModifiedDate())
            .isCloseTo(DateTimeUtils.dateTimeNow(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    public void testExists() {
        boolean exists = repository.exists(new JooqTestCompositeRepository.Filter());
        assertThat(exists).isFalse();

        var value = random.nextObject(JooqTestComposite.class);
        repository.save(value);

        exists = repository.exists(new JooqTestCompositeRepository.Filter());
        assertThat(exists).isTrue();
    }

    private JooqTestComposite entityWithDescription(int supplierId, String shopSku, @Nullable String desc) {
        return new JooqTestComposite().setSupplierId(supplierId).setShopSku(shopSku).setDescription(desc);
    }

    private List<JooqTestComposite> createTestData() {
        return repository.save(Arrays.asList(
            entityWithDescription(1, "g", GAMMA),
            entityWithDescription(1, "a", ALPHA),
            entityWithDescription(2, "b", BETA)
        ));
    }
}
