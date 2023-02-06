package ru.yandex.market.mboc.common.services.jooq_test;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import ru.yandex.market.mbo.jooq.repo.notification.EntityChangeEvent.ChangeType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.JooqTestEntity;
import ru.yandex.market.mboc.common.services.jooq_test.JooqTestRepository.Filter;
import ru.yandex.market.mboc.common.services.jooq_test.JooqTestRepository.SortBy;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * @author pochemuto@yandex-team.ru
 */
@SuppressWarnings("checkstyle:magicNumber")
public class JooqTestRepositoryTest extends BaseDbTestClass {
    public static final int TEST_SIZE = 10;

    private static final String ALPHA = "alpha";
    private static final String BETA = "beta";
    private static final String GAMMA = "gamma";

    private static final int HUGE_TEST_DATA_ITEMS_COUNT = 1_000_000;

    private final List<EntityChangeEvent<JooqTestEntity>> changes = new ArrayList<>();

    private EnhancedRandom random;

    @Resource(name = "dsl")
    private DSLContext dslContext;
    private JooqTestRepository repository;

    @Before
    public void setUp() {
        random = new EnhancedRandomBuilder()
            .seed(123)
            .exclude(OffsetDateTime.class)
            .build();

        repository = new JooqTestRepository(dslContext);
        repository.addObserver(changes::addAll);
    }

    @Test
    public void saveSuccessful() {
        JooqTestEntity created = new JooqTestEntity();
        created.setDescription(ALPHA);

        JooqTestEntity saved = repository.save(created);
        assertThat(created.getId()).isNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDescription()).isEqualTo(ALPHA);

        JooqTestEntity loaded = repository.getById(saved.getId());

        assertThat(loaded.getDescription()).isEqualTo(ALPHA);
    }

    @Test
    public void saveNewWithId() {
        JooqTestEntity created = random.nextObject(JooqTestEntity.class);

        JooqTestEntity saved = repository.save(created);
        Assertions.assertThat(saved)
            .isEqualToIgnoringGivenFields(created, "modifiedDate");

        JooqTestEntity loaded = repository.getById(saved.getId());
        Assertions.assertThat(loaded)
            .isEqualToIgnoringGivenFields(created, "modifiedDate");
    }


    @Test
    public void saveBatch() {
        List<JooqTestEntity> created = repository.save(Arrays.asList(
            entityWithDescription(ALPHA), entityWithDescription(BETA), entityWithDescription(GAMMA)
        ));

        assertThat(created).hasSize(3)
            .extracting(JooqTestEntity::getId)
            .doesNotContainNull();

        List<Long> ids = created.stream().map(JooqTestEntity::getId).collect(Collectors.toList());

        List<JooqTestEntity> loaded = repository.getByIds(ids);
        assertThat(loaded).extracting(JooqTestEntity::getDescription)
            .containsExactlyInAnyOrder(ALPHA, BETA, GAMMA);
    }

    @Test
    @Ignore("Медленный тест. Запустите его вручную, чтобы убедиться, что все работает")
    public void fetchMoreThanMaxShort() throws IOException {
        List<JooqTestEntity> entities = random.ints()
            .limit(HUGE_TEST_DATA_ITEMS_COUNT)
            .mapToObj(i -> new JooqTestEntity())
            .collect(Collectors.toList());

        repository.save(entities);

        List<Long> created = repository.findIds(Filter.all());
        assertThat(created).hasSize(HUGE_TEST_DATA_ITEMS_COUNT);

        int count = repository.count(Filter.withIds(created));
        assertThat(created).hasSize(count);
    }

    @Test
    public void saveBatchComplex() {
        List<JooqTestEntity> toExists = random.objects(JooqTestEntity.class, TEST_SIZE).collect(Collectors.toList());
        repository.save(toExists);

        List<JooqTestEntity> existing = toExists.stream()
            .map(e -> repository.getById(e.getId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        Assertions.assertThat(existing).hasSameSizeAs(toExists);

        List<JooqTestEntity> newWithId = random.objects(JooqTestEntity.class, TEST_SIZE).collect(Collectors.toList());
        List<JooqTestEntity> newWithoutId = random.objects(JooqTestEntity.class, TEST_SIZE)
            .peek(e -> e.setId(null))
            .collect(Collectors.toList());

        List<JooqTestEntity> testData = Stream.of(existing, newWithId, newWithoutId)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        List<JooqTestEntity> testDataSaved = repository.save(testData);

        List<JooqTestEntity> testDataLoaded = testDataSaved.stream()
            .map(e -> repository.getById(e.getId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        Assertions.assertThat(testDataLoaded).usingElementComparatorIgnoringFields("modifiedDate")
            .containsAll(existing);
        Assertions.assertThat(testDataLoaded).usingElementComparatorIgnoringFields("modifiedDate")
            .containsAll(newWithId);
        Assertions.assertThat(testDataLoaded).usingElementComparatorIgnoringFields("id", "modifiedDate")
            .containsAll(newWithoutId);
    }

    @Test
    public void textGetExistingIds() {
        List<JooqTestEntity> toExists = random.objects(JooqTestEntity.class, TEST_SIZE).collect(Collectors.toList());
        List<JooqTestEntity> newWithId = random.objects(JooqTestEntity.class, TEST_SIZE).collect(Collectors.toList());

        repository.save(toExists);

        List<Long> existingIds = toExists.stream().map(JooqTestEntity::getId).collect(Collectors.toList());
        List<Long> notExistingIds = newWithId.stream().map(JooqTestEntity::getId).collect(Collectors.toList());

        List<Long> idsToTest = Stream.of(existingIds, notExistingIds)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        Set<Long> found = repository.getExistingIds(idsToTest);

        Assertions.assertThat(found)
            .containsOnlyElementsOf(existingIds);
    }

    @Test
    public void findAllAsc() {
        createTestData();

        List<JooqTestEntity> entities = repository.find(Filter.all(), SortBy.DESCRIPTION.asc(), OffsetFilter.all());

        assertThat(entities).extracting(JooqTestEntity::getDescription)
            .containsExactly(ALPHA, BETA, GAMMA);
    }

    @Test
    public void getById() {
        repository.save(entityWithDescription(ALPHA));
        JooqTestEntity created = repository.save(entityWithDescription(BETA));
        repository.save(entityWithDescription(GAMMA));

        JooqTestEntity loaded = repository.getById(created.getId());

        assertThat(loaded).isNotNull();
        assertThat(loaded.getId()).isEqualTo(created.getId());
    }

    @Test
    public void getByIds() {
        repository.save(entityWithDescription(ALPHA));
        JooqTestEntity beta = repository.save(entityWithDescription(BETA));
        JooqTestEntity gamma = repository.save(entityWithDescription(GAMMA));

        List<JooqTestEntity> loaded = repository.getByIds(Arrays.asList(beta.getId(), gamma.getId()));

        assertThat(loaded).isNotNull();
        assertThat(loaded).extracting(JooqTestEntity::getDescription)
            .containsExactlyInAnyOrder(BETA, GAMMA);
    }

    @Test
    public void changesSingle() {
        JooqTestEntity first = repository.save(entityWithDescription(ALPHA));

        assertThat(changes).hasSize(1);
        EntityChangeEvent<JooqTestEntity> change = changes.get(0);
        assertThat(change.getChangeType()).isEqualTo(ChangeType.ADD);
        assertThat(change.getBefore()).isNull();
        assertThat(change.getAfter()).isNotNull();
        assertThat(change.getAfter().getId()).isNotNull();
        assertThat(change.getAfter().getDescription()).isEqualTo(ALPHA);

        changes.clear();
        repository.save(first.setDescription(BETA));

        assertThat(changes).hasSize(1);
        change = changes.get(0);
        assertThat(change.getChangeType()).isEqualTo(ChangeType.CHANGE);
        assertThat(change.getBefore()).isNotNull();
        assertThat(change.getAfter()).isNotNull();
        assertThat(change.getBefore().getDescription()).isEqualTo(ALPHA);
        assertThat(change.getAfter().getDescription()).isEqualTo(BETA);

        changes.clear();
        repository.delete(first.getId());

        assertThat(changes).hasSize(1);
        change = changes.get(0);
        assertThat(change.getChangeType()).isEqualTo(ChangeType.DELETE);
        assertThat(change.getBefore()).isNotNull();
        assertThat(change.getAfter()).isNull();
        assertThat(change.getBefore().getDescription()).isEqualTo(BETA);
    }

    @Test
    public void changesBatch() {
        repository.save(Arrays.asList(entityWithDescription(ALPHA), entityWithDescription(BETA)));
        assertThat(changes).hasSize(2);
        assertThat(changes)
            .extracting(EntityChangeEvent::getBefore)
            .containsOnlyNulls();
        assertThat(changes)
            .extracting(EntityChangeEvent::getAfter)
            .extracting(JooqTestEntity::getDescription)
            .containsExactly(ALPHA, BETA);
    }

    private JooqTestEntity entityWithDescription(String alpha) {
        return new JooqTestEntity().setDescription(alpha);
    }


    @Test
    public void findLimit() {
        createTestData();

        List<JooqTestEntity> entities = repository.find(Filter.all(), SortBy.DESCRIPTION.asc(),
            OffsetFilter.offset(1, 1));

        assertThat(entities).extracting(JooqTestEntity::getDescription)
            .containsExactly(BETA);
    }

    @Test
    public void filter() {
        repository.save(new JooqTestEntity());
        repository.save(new JooqTestEntity());
        List<JooqTestEntity> testData = createTestData();
        repository.save(new JooqTestEntity());

        int count = repository.count(Filter.all());
        assertThat(count).isEqualTo(testData.size() + 3);

        assertThat(repository.count(Filter.hasDescription())).isEqualTo(testData.size());

        List<JooqTestEntity> entities = repository.find(Filter.hasDescription(), SortBy.DESCRIPTION.desc(),
            OffsetFilter.all());

        assertThat(entities).extracting(JooqTestEntity::getDescription)
            .doesNotContainNull();
    }

    private List<JooqTestEntity> createTestData() {
        return repository.save(Arrays.asList(
            entityWithDescription(GAMMA),
            entityWithDescription(ALPHA),
            entityWithDescription(BETA)
        ));
    }

    @Test
    public void optimisticLocking() {
        JooqTestEntity created = new JooqTestEntity();
        created.setDescription(ALPHA);

        JooqTestEntity savedFirst = repository.save(created);

        repository.save(savedFirst); // concurrent modification

        assertThatThrownBy(() -> {
            created.setDescription(BETA);
            repository.save(savedFirst);
        }).isInstanceOf(ConcurrentModificationException.class);

        JooqTestEntity loaded = repository.getById(savedFirst.getId());

        assertThat(loaded.getDescription()).isEqualTo(ALPHA);
    }

    @Test
    public void generateIds() {
        List<Long> longs = repository.generateIds(10);
        assertThat(longs)
            .hasSize(10)
            .isSorted();

        assertThat(new HashSet<>(longs)).hasSameSizeAs(longs);
    }

    @Test
    public void testDoubleSave() {
        JooqTestEntity save1 = repository.save(new JooqTestEntity().setDescription(ALPHA));

        save1.setDescription(BETA);
        JooqTestEntity save2 = repository.save(save1);
        Assertions.assertThat(save1.getId()).isEqualTo(save2.getId());
        Assertions.assertThat(save1.getDescription()).isEqualTo(save2.getDescription());
        Assertions.assertThat(save1.getModifiedDate()).isBefore(save2.getModifiedDate());
    }

    @Test
    public void testOptimisticLockingFieldAreGeneratedInLocalTimeZone() {
        JooqTestEntity save = repository.save(new JooqTestEntity());

        Assertions.assertThat(save.getModifiedDate())
            .isCloseTo(DateTimeUtils.dateTimeNow(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    public void testTimestampSave() {
        Instant now = DateTimeUtils.instantNow();
        JooqTestEntity entry1 = repository.save(new JooqTestEntity().setCreatedAt(now));
        JooqTestEntity saved1 = repository.getById(entry1.getId());
        Assertions.assertThat(saved1.getCreatedAt()).isEqualTo(now);

        Instant future = now.plus(10L, ChronoUnit.MINUTES);
        JooqTestEntity entry2 = repository.save(new JooqTestEntity().setCreatedAt(future));
        JooqTestEntity saved2 = repository.getById(entry2.getId());
        Assertions.assertThat(saved2.getCreatedAt()).isEqualTo(future);
    }

    @Test
    public void testTimestampTzSave() {
        Instant now = DateTimeUtils.instantNow();

        OffsetDateTime nowAtMoscow = OffsetDateTime.ofInstant(now, ZoneId.of("Europe/Moscow"));
        JooqTestEntity entry1 = repository.save(new JooqTestEntity().setCreatedAtTz(nowAtMoscow));
        JooqTestEntity saved1 = repository.getById(entry1.getId());
        // сравниваем по instant, так как сравнение по equals бессмысленно потому что created_at_tz
        // будет представлено в таймзоне java приложения
        Assertions.assertThat(saved1.getCreatedAtTz().toInstant()).isEqualTo(now);

        OffsetDateTime nowInYekaterinburg = OffsetDateTime.ofInstant(now, ZoneId.of("Asia/Yekaterinburg"));
        JooqTestEntity entry2 = repository.save(new JooqTestEntity().setCreatedAtTz(nowInYekaterinburg));
        JooqTestEntity saved2 = repository.getById(entry2.getId());
        Assertions.assertThat(saved2.getCreatedAtTz().toInstant()).isEqualTo(now);
    }

    @Test
    public void testExists() {
        boolean exists = repository.exists(Filter.all());
        assertThat(exists).isFalse();

        JooqTestEntity value = random.nextObject(JooqTestEntity.class);
        repository.save(value);

        exists = repository.exists(Filter.all());
        assertThat(exists).isTrue();
    }

    @Test
    public void testModifiedDateChanged() {
        JooqTestEntity entry1 = repository.save(new JooqTestEntity().setDescription("A"));
        changes.clear();
        JooqTestEntity entry2 = repository.save(new JooqTestEntity(entry1).setDescription("B"));

        // change entry has different data
        Assertions.assertThat(entry1.getDescription()).isEqualTo("A");
        Assertions.assertThat(entry2.getDescription()).isEqualTo("B");
        Assertions.assertThat(entry1.getModifiedDate()).isBefore(entry2.getModifiedDate());

        // change different data in events
        assertThat(changes).hasSize(1);
        var change = changes.get(0);
        assertThat(change.getChangeType()).isEqualTo(ChangeType.CHANGE);
        assertThat(change.getBefore()).isEqualTo(entry1);
        assertThat(change.getBefore().getDescription()).isEqualTo("A");

        assertThat(change.getAfter()).isEqualTo(entry2);
        assertThat(change.getAfter().getDescription()).isEqualTo("B");
        Assertions.assertThat(change.getBefore().getModifiedDate()).isBefore(change.getAfter().getModifiedDate());
    }

    @Test
    public void testModifiedDateChangedInBatch() {
        List<JooqTestEntity> entries1 = repository.save(
            new JooqTestEntity().setDescription("A"),
            new JooqTestEntity().setDescription("A")
        );
        changes.clear();

        List<JooqTestEntity> entries2 = repository.save(
            new JooqTestEntity(entries1.get(0)).setDescription("B"),
            new JooqTestEntity(entries1.get(1)).setDescription("B")
        );
        var entries11 = entries1.get(0);
        var entries12 = entries1.get(1);
        var entries21 = entries2.get(0);
        var entries22 = entries2.get(1);

        // change entry has different data
        Assertions.assertThat(entries11.getDescription()).isEqualTo("A");
        Assertions.assertThat(entries12.getDescription()).isEqualTo("A");
        Assertions.assertThat(entries21.getDescription()).isEqualTo("B");
        Assertions.assertThat(entries22.getDescription()).isEqualTo("B");
        Assertions.assertThat(entries11.getModifiedDate()).isBefore(entries21.getModifiedDate());
        Assertions.assertThat(entries12.getModifiedDate()).isBefore(entries22.getModifiedDate());

        // change different data in events
        assertThat(changes).hasSize(2);
        for (var change : changes) {
            assertThat(change.getChangeType()).isEqualTo(ChangeType.CHANGE);
            assertThat(change.getBefore().getDescription()).isEqualTo("A");
            assertThat(change.getAfter().getDescription()).isEqualTo("B");
            Assertions.assertThat(change.getBefore().getModifiedDate()).isBefore(change.getAfter().getModifiedDate());
        }
    }
}
