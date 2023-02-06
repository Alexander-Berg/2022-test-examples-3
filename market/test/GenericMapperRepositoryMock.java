package ru.yandex.market.mbo.lightmapper.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.esotericsoftware.kryo.Kryo;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.UnmodifiableLazyStringList;

import ru.yandex.market.mbo.lightmapper.GenericMapperRepository;

/**
 * @author yuramalinov
 * @created 14.05.18
 */
public abstract class GenericMapperRepositoryMock<Item, ItemKey> implements GenericMapperRepository<Item, ItemKey> {
    private static final ThreadLocal<Kryo> KRYOS = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.addDefaultSerializer(AbstractMessage.class, new ProtobufKryoSerializer());
        kryo.addDefaultSerializer(UnmodifiableLazyStringList.class, new KryoUnmodifiableListSerializer());
        return kryo;
    });
    private final LinkedHashMap<ItemKey, Item> objects = new LinkedHashMap<>();
    // ReentrantReadWriteLock нужно для эмуляции sql комбинации "select ... for update" DEEPMIND-209
    // Идея следующая. Когда дергается метод "findForUpdate" мы берем эксклюзивную write блокировку
    // Когда вызывается insert/update/delete мы берем сначала read блокировку, потом отпускаем ее,
    // и отпускаем write блокировку
    // Таким образом реализовываем эксклюзивную блокировку строки.
    private final ConcurrentHashMap<ItemKey, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    @Nullable
    private final BiConsumer<Item, ItemKey> idSetter;
    private final Function<Item, ItemKey> idGetter;

    public GenericMapperRepositoryMock(@Nullable BiConsumer<Item, ItemKey> idSetter, Function<Item, ItemKey> idGetter) {
        this.idSetter = idSetter;
        this.idGetter = idGetter;
    }

    @Override
    public Item insert(@Nonnull Item item) {
        validate(item);
        if (idSetter != null) {
            idSetter.accept(item, nextId());
        }
        ItemKey id = idGetter.apply(item);
        unlock(id, () -> objects.put(id, copy(item)));
        return item;
    }

    @Override
    public List<Item> insertBatch(@Nonnull Collection<Item> instances) {
        return instances.stream().map(this::insert).collect(Collectors.toList());
    }

    @Override
    public Item update(@Nonnull Item item) {
        validate(item);
        ItemKey id = idGetter.apply(item);
        unlock(id, () -> objects.put(id, copy(item)));
        return item;
    }

    @Override
    public List<Item> updateBatch(@Nonnull Collection<Item> instances, int batchSize) {
        return instances.stream().map(this::update).collect(Collectors.toList());
    }

    @Override
    public void delete(@Nonnull List<ItemKey> ids) {
        ids.forEach(id -> {
            if (!objects.containsKey(id)) {
                return;
            }

            unlock(id, () -> {
                objects.remove(id);
                locks.remove(id);
            });
        });
    }

    @Override
    public void deleteBatch(@Nonnull Collection<Item> items) {
        delete(items.stream().map(idGetter).collect(Collectors.toList()));
    }

    @Override
    public void deleteAll() {
        deleteBatch(new ArrayList<>(this.objects.values()));
    }

    @Override
    public List<Item> insertOrUpdateAll(@Nonnull Collection<Item> items) {
        items.forEach(this::validate);
        items.forEach(item -> {
            ItemKey id = idGetter.apply(item);
            if (!objects.containsKey(id)) {
                if (idSetter != null) {
                    id = nextId();
                    idSetter.accept(item, id);
                }
            }

            ItemKey finalId = id;
            unlock(id, () -> objects.put(finalId, copy(item)));
        });
        return new ArrayList<>(items);
    }

    @Override
    public Integer insertOrUpdateAllIfDifferent(@Nonnull Collection<Item> items) {
        insertOrUpdateAll(items);
        return items.size();
    }

    @Override
    public Item findById(@Nonnull ItemKey id) {
        Item item = objects.get(id);
        if (item == null) {
            throw new NoSuchElementException("Can't find id = " + id);
        }
        return copy(item);
    }

    @Override
    public Item findByIdForUpdate(@Nonnull ItemKey id) {
        return lock(id, () -> findById(id));
    }

    @Override
    public List<Item> findByIds(@Nonnull Collection<ItemKey> ids) {
        Kryo kryo = KRYOS.get();
        return ids.stream().flatMap(id -> {
            Item item = objects.get(id);
            return item == null ? Stream.empty() : Stream.of(kryo.copy(item));
        }).collect(Collectors.toList());
    }

    @Override
    public List<Item> findByIdsForUpdate(@Nonnull Collection<ItemKey> ids) {
        ids.stream().map(locks::get).forEach(t -> t.writeLock().lock());
        return findByIds(ids);
    }

    @Override
    public List<Item> findAll() {
        return findWhere(o -> true);
    }

    @Override
    public int totalCount() {
        return objects.size();
    }

    protected Stream<Item> findWhereStream(Predicate<Item> predicate) {
        Kryo kryo = KRYOS.get();
        return objects.values().stream().filter(predicate).map(kryo::copy);
    }

    protected List<Item> findWhere(Predicate<Item> predicate) {
        return findWhereStream(predicate).collect(Collectors.toList());
    }

    protected long countWhere(Predicate<Item> predicate) {
        return findWhereStream(predicate).count();
    }

    protected Item findOne(Predicate<Item> predicate) {
        Kryo kryo = KRYOS.get();
        List<Item> result = objects.values().stream().filter(predicate).map(kryo::copy).collect(Collectors.toList());

        if (result.isEmpty()) {
            throw new NoSuchElementException("Nothing found for given predicate, check logs for details");
        } else if (result.size() > 1) {
            throw new NoSuchElementException("Found " + result.size() + " records, " +
                "expected 1 for given predicate, check logs for details");
        }
        return result.get(0);
    }

    protected Optional<Item> findFirst(Predicate<Item> predicate) {
        return objects.values().stream()
            .filter(predicate)
            .map(KRYOS.get()::copy)
            .findFirst();
    }

    private Item copy(Item instance) {
        return KRYOS.get().copy(instance);
    }

    protected abstract ItemKey nextId();

    /**
     * Валидация перед сохранением - переопредели в своём коде, если нужно.
     */
    protected void validate(Item instance) {
    }

    private <T> T lock(ItemKey key, Supplier<T> supplier) {
        ReentrantReadWriteLock lock = locks.get(key);
        if (lock == null) {
            throw new NoSuchElementException("Can't find id = " + key);
        }
        lock.writeLock().lock();
        return supplier.get();
    }

    private void unlock(ItemKey key, Runnable runnable) {
        ReentrantReadWriteLock lock = locks.computeIfAbsent(key, __ -> new ReentrantReadWriteLock());
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            readLock.lock();
            runnable.run();
        } finally {
            readLock.unlock();
            try {
                writeLock.unlock();
            } catch (IllegalMonitorStateException skip) {
                // этот Exception викинется, если поток, вызывающий unlock не вызвал до этого лок.
                // В нашем случае это совершенно нормальная ситуация
            }
        }
    }
}
