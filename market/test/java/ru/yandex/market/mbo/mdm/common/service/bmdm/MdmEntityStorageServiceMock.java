package ru.yandex.market.mbo.mdm.common.service.bmdm;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.I18nStringUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.proto.MdmEntityStorageService;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.MdmEntity;
import ru.yandex.market.mdm.http.MdmExternalKey;
import ru.yandex.market.mdm.http.MdmOrderByClause;
import ru.yandex.market.mdm.http.MdmOrderByClauses;
import ru.yandex.market.mdm.http.MdmOrderByDirection;
import ru.yandex.market.mdm.http.entity.GetMdmEntityByExternalKeysRequest;
import ru.yandex.market.mdm.http.entity.GetMdmEntityByMdmIdsRequest;
import ru.yandex.market.mdm.http.entity.GetMdmEntityResponse;
import ru.yandex.market.mdm.http.entity.SaveMdmEntityRequest;
import ru.yandex.market.mdm.http.entity.SaveMdmEntityResponse;
import ru.yandex.market.mdm.http.entity.SaveMdmEntityResult;
import ru.yandex.market.mdm.lib.util.AlgorithmKt;

public class MdmEntityStorageServiceMock implements MdmEntityStorageService {
    private static final Comparator<MdmAttributeValue> ATTRIBUTE_VALUE_COMPARATOR = attributeValueComparator();
    private static final Comparator<MdmEntity> MDM_ID_COMPARATOR = Comparator.comparingLong(MdmEntity::getMdmId);
    private final IdProvider idProvider = new IdProvider();
    private final ConcurrentMap<Long, Storage> storages = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Long> defaultStorageIds = new ConcurrentHashMap<>();

    @Override
    public GetMdmEntityResponse getByExternalKeys(GetMdmEntityByExternalKeysRequest request) {
        long requestedStorageId = request.getMdmStorageId();
        long entityTypeId = request.getMdmExternalKeys().getMdmEntityTypeId();
        if (entityTypeId <= 0) {
            throw new IllegalArgumentException("Got illegal entity type id " + entityTypeId);
        }

        List<MdmEntity> entities = findStorageAndProcessAction(
            requestedStorageId,
            entityTypeId,
            storage -> {
                if (request.getMdmExternalKeys().getMdmExternalKeysCount() == 0
                    && request.getPageSize() > 0) {
                    return storage.findAll();
                }
                return request.getMdmExternalKeys().getMdmExternalKeysList().stream()
                    .map(storage::findEntityByExternalKey)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            }
        );

        Comparator<MdmEntity> comparator = request.hasOrderBy()
            ? createComparator(request.getOrderBy()).thenComparing(MDM_ID_COMPARATOR)
            : MDM_ID_COMPARATOR;
        long limit = request.getPageSize() > 0 ? request.getPageSize() : Long.MAX_VALUE;
        long offset = request.getPageToken();

        List<MdmEntity> orderedEntities = entities.stream()
            .sorted(comparator)
            .distinct()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        return GetMdmEntityResponse.newBuilder()
            .addAllMdmEntities(orderedEntities)
            .build();
    }

    @Override
    public GetMdmEntityResponse getByMdmIds(GetMdmEntityByMdmIdsRequest request) {
        if (!request.hasMdmIds()) {
            return GetMdmEntityResponse.getDefaultInstance();
        }
        long requestedStorageId = request.getMdmStorageId();
        long entityTypeId = request.getMdmIds().getMdmEntityTypeId();
        if (entityTypeId <= 0) {
            throw new IllegalArgumentException("Got illegal entity type id " + entityTypeId);
        }
        List<Long> idsList = request.getMdmIds().getMdmIdsList();
        List<MdmEntity> entities = findStorageAndProcessAction(
            requestedStorageId,
            entityTypeId,
            storage -> storage.findEntitiesByIds(idsList)
        );
        return GetMdmEntityResponse.newBuilder()
            .addAllMdmEntities(entities)
            .build();
    }

    @Override
    public SaveMdmEntityResponse save(SaveMdmEntityRequest request) {
        if (request == null) {
            return SaveMdmEntityResponse.getDefaultInstance();
        }
        long requestedStorageId = request.getMdmStorageId();
        List<MdmEntity> mdmEntities = request.getMdmEntitiesList();
        Set<Long> entityTypeIds = mdmEntities.stream()
            .map(MdmEntity::getMdmEntityTypeId)
            .collect(Collectors.toSet());
        if (entityTypeIds.size() != 1) {
            throw new IllegalArgumentException();
        }
        long entityTypeId = entityTypeIds.iterator().next();
        if (entityTypeId <= 0) {
            throw new IllegalArgumentException("Got illegal entity type id " + entityTypeId);
        }
        List<SaveMdmEntityResult> saveMdmEntityResults = findStorageAndProcessAction(
            requestedStorageId,
            entityTypeId,
            storage -> mdmEntities.stream()
                .map(storage::save)
                .collect(Collectors.toList())
        );
        return SaveMdmEntityResponse.newBuilder()
            .addAllResults(saveMdmEntityResults)
            .build();
    }

    /**
     * Для этого метода не гарантируется потокобезопасность
     */
    public long registerStorage(long mdmEntityType,
                                List<IndexDescription> indexDescriptions,
                                boolean markDefaultForEntityType) {
        Storage storage = new Storage(idProvider, mdmEntityType, indexDescriptions);
        long storageId = idProvider.nextId();
        storages.put(storageId, storage);
        if (markDefaultForEntityType) {
            defaultStorageIds.put(mdmEntityType, storageId);
        }
        return storageId;
    }

    /**
     * Для этого метода не гарантируется потокобезопасность
     */
    public void clearAllStorages() {
        defaultStorageIds.clear();
        storages.clear();
    }

    private <T> T findStorageAndProcessAction(long requestedStorageId,
                                              long entityTypeId,
                                              Function<Storage, T> action) {
        long storageId;
        if (requestedStorageId < 0) {
            throw new IllegalArgumentException("Storage id can't be negative. Given " + requestedStorageId);
        }
        if (requestedStorageId == 0) {
            Long defaultStorageId = defaultStorageIds.get(entityTypeId);
            if (defaultStorageId == null) {
                throw new IllegalArgumentException("Can't find default storage for entity type " + entityTypeId);
            }
            storageId = defaultStorageId;
        } else {
            storageId = requestedStorageId;
        }
        Keeper<T> keeper = new Keeper<>();
        storages.compute(storageId, (id, storage) -> {
            if (storage == null) {
                throw new IllegalArgumentException("Can't find storage with id " + id);
            }
            if (storage.entityType != entityTypeId) {
                throw new IllegalStateException(String.format(
                    "Storage %d is for entity type %d, not %d.",
                    id, storage.entityType, entityTypeId
                ));
            }
            keeper.keepValue(action.apply(storage));
            return storage;
        });
        return keeper.getValue();
    }

    private static final class IdProvider {
        private final AtomicLong id;

        IdProvider() {
            id = new AtomicLong(1);
        }

        long nextId() {
            return id.getAndIncrement();
        }
    }

    private static final class Storage {
        private final IdProvider idProvider;
        private final Map<Long, MdmEntity> entities;
        private final List<StorageIndex> storageIndices;
        private final StorageIndex primaryIndex;
        private final long entityType;

        Storage(IdProvider idProvider, long entityType, Collection<IndexDescription> indices) {
            this.idProvider = idProvider;
            this.entities = new HashMap<>();
            this.entityType = entityType;
            this.storageIndices = indices.stream()
                .map(StorageIndex::new)
                .collect(Collectors.toList());
            List<IndexDescription> primary = indices.stream()
                .filter(indexDescription -> indexDescription.isPrimary)
                .collect(Collectors.toList());
            if (primary.size() != 1) {
                throw new IllegalArgumentException();
            }
            this.primaryIndex = new StorageIndex(primary.get(0));
        }

        List<MdmEntity> findEntitiesByIds(List<Long> ids) {
            return ids.stream()
                .map(entities::get)
                .filter(Objects::nonNull)
                .filter(it -> !it.getMdmUpdateMeta().getDeleted())
                .collect(Collectors.toList());
        }

        List<MdmEntity> findEntityByExternalKey(MdmExternalKey mdmExternalKey) {
            List<List<Long>> paths = mdmExternalKey.getMdmAttributeValuesList().stream()
                .map(mdmAttributeValues -> mdmAttributeValues.getMdmAttributePathCount() > 0
                    ? mdmAttributeValues.getMdmAttributePathList()
                    : List.of(mdmAttributeValues.getMdmAttributeId()))
                .collect(Collectors.toList());
            IndexDescription indexDescription = new IndexDescription(paths);
            List<StorageIndex> suitableIndexes = storageIndices.stream()
                .filter(index -> indexDescription.isPrefixOf(index.indexDescription))
                .collect(Collectors.toList());
            if (suitableIndexes.isEmpty()) {
                throw new UnsupportedOperationException("Can't find index for key " + mdmExternalKey);
            }
            List<MdmAttributeValue> indexParts = mdmExternalKey.getMdmAttributeValuesList().stream()
                .map(mdmAttributeValues -> {
                    if (mdmAttributeValues.getValuesCount() != 1) {
                        throw new IllegalArgumentException();
                    }
                    MdmAttributeValue candidate = mdmAttributeValues.getValues(0);
                    if (candidate.hasStruct()) {
                        throw new IllegalArgumentException();
                    }
                    return candidate;
                })
                .collect(Collectors.toList());
            IndexKey indexKey = new IndexKey(indexParts);
            return suitableIndexes.stream()
                .map(index -> index.find(indexKey))
                .flatMap(Set::stream)
                .distinct()
                .map(entities::get)
                .filter(it -> !it.getMdmUpdateMeta().getDeleted())
                .collect(Collectors.toList());
        }

        SaveMdmEntityResult save(MdmEntity mdmEntity) {
            long mdmId = Optional.of(mdmEntity.getMdmId())
                .filter(id -> id > 0)
                .or(() -> primaryIndex.find(mdmEntity))
                .orElseGet(idProvider::nextId);
            MdmEntity oldEntity = entities.get(mdmId);

            if (oldEntity != null && oldEntity.getMdmUpdateMeta().getFrom() != mdmEntity.getMdmUpdateMeta().getFrom()) {
                return SaveMdmEntityResult.newBuilder()
                    .setMdmId(mdmId)
                    .setCode(SaveMdmEntityResult.Code.CONCURRENT_UPDATE)
                    .build();
            }

            MdmEntity entityToSave = mdmEntity.toBuilder()
                .setMdmId(mdmId)
                .setMdmUpdateMeta(mdmEntity.getMdmUpdateMeta().toBuilder()
                    .setFrom(Instant.now().toEpochMilli()))
                .build();
            entities.put(mdmId, entityToSave);
            storageIndices.forEach(storageIndex -> storageIndex.save(entityToSave, oldEntity));
            return SaveMdmEntityResult.newBuilder()
                .setMdmId(mdmId)
                .setFrom(entityToSave.getMdmUpdateMeta().getFrom())
                .setCode(SaveMdmEntityResult.Code.OK)
                .build();
        }

        List<MdmEntity> findAll() {
            return List.copyOf(entities.values());
        }
    }

    private static final class Keeper<T> {
        private T value;

        void keepValue(T value) {
            if (value != null) {
                this.value = value;
            } else {
                throw new IllegalStateException("Already keep value");
            }
        }

        public T getValue() {
            return value;
        }
    }

    private static final class StorageIndex {
        private final StorageIndexNode rootNode = new StorageIndexNode();
        private final IndexDescription indexDescription;

        StorageIndex(IndexDescription indexDescription) {
            this.indexDescription = indexDescription;
        }

        void save(MdmEntity mdmEntity, MdmEntity oldEntity) {
            Set<IndexKey> indexKeys = extractIndexKeys(mdmEntity);
            Set<IndexKey> oldIndexKeys = extractIndexKeys(oldEntity);
            oldIndexKeys.forEach(this::remove);
            indexKeys.forEach(indexKey -> this.add(indexKey, mdmEntity.getMdmId()));
        }

        Set<Long> find(IndexKey indexKey) {
            return find(rootNode, indexKey.keyParts, 0);
        }

        Optional<Long> find(MdmEntity mdmEntity) {
            Set<IndexKey> indexKeys = extractIndexKeys(mdmEntity);
            if (indexKeys.size() > 1) {
                throw new IllegalStateException();
            }
            Set<Long> candidates = indexKeys.stream()
                .map(this::find)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
            if (candidates.size() > 1) {
                throw new IllegalStateException();
            }
            return candidates.stream().findAny();
        }

        private Set<IndexKey> extractIndexKeys(MdmEntity mdmEntity) {
            List<List<MdmAttributeValue>> values = indexDescription.attributePaths.stream()
                .map(path -> findValues(mdmEntity, path))
                .collect(Collectors.toList());
            return AlgorithmKt.cartesian(values).stream()
                .map(IndexKey::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        private Set<Long> find(StorageIndexNode indexNode, List<MdmAttributeValue> keyParts, int partId) {
            if (indexNode == null) {
                return Set.of();
            }
            if (keyParts.size() == partId) {
                return indexNode.allLeavesIds();
            }
            return find(indexNode.next.get(keyParts.get(partId)), keyParts, partId + 1);
        }

        private void add(IndexKey indexKey, long mdmId) {
            add(rootNode, indexKey.keyParts, 0, mdmId);
        }

        private StorageIndexNode add(StorageIndexNode storageIndexNode,
                                     List<MdmAttributeValue> keyParts,
                                     int partId,
                                     long mdmId) {
            if (partId == keyParts.size()) {
                return new StorageIndexNode(mdmId);
            }
            if (storageIndexNode == null) {
                storageIndexNode = new StorageIndexNode();
            }
            storageIndexNode.next.compute(keyParts.get(partId), (k, v) -> add(v, keyParts, partId + 1, mdmId));
            return storageIndexNode;
        }

        private void remove(IndexKey indexKey) {
            remove(rootNode, indexKey.keyParts, 0);
        }

        private StorageIndexNode remove(StorageIndexNode storageIndexNode,
                                        List<MdmAttributeValue> keyParts,
                                        int partId) {
            if (storageIndexNode == null || partId == keyParts.size()) {
                return null;
            }
            storageIndexNode.next.compute(keyParts.get(partId), (k, v) -> remove(v, keyParts, partId + 1));
            if (storageIndexNode.next.isEmpty()) {
                return null;
            }
            return storageIndexNode;
        }
    }

    private static final class StorageIndexNode {
        final Map<MdmAttributeValue, StorageIndexNode> next = new HashMap<>();
        final long mdmId;

        StorageIndexNode() {
            this(0L);
        }

        StorageIndexNode(Long mdmId) {
            this.mdmId = mdmId;
        }

        boolean isLeaf() {
            return mdmId > 0 && next.isEmpty();
        }

        Set<Long> allLeavesIds() {
            if (isLeaf()) {
                return Set.of(mdmId);
            }
            return next.values().stream()
                .map(StorageIndexNode::allLeavesIds)
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public static final class IndexDescription {
        private final List<List<Long>> attributePaths;
        private final boolean isPrimary;

        public IndexDescription(List<List<Long>> attributePaths) {
            this(attributePaths, false);
        }

        public IndexDescription(List<List<Long>> attributePaths, boolean isPrimary) {
            this.attributePaths = List.copyOf(attributePaths);
            this.isPrimary = isPrimary;
        }

        public boolean isPrefixOf(IndexDescription otherIndex) {
            if (this.attributePaths.size() > otherIndex.attributePaths.size()) {
                return false;
            }
            for (int i = 0; i < this.attributePaths.size(); i++) {
                if (!this.attributePaths.get(i).equals(otherIndex.attributePaths.get(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IndexDescription that = (IndexDescription) o;
            return Objects.equals(attributePaths, that.attributePaths);
        }

        @Override
        public int hashCode() {
            return Objects.hash(attributePaths);
        }
    }

    private static final class IndexKey {
        private final List<MdmAttributeValue> keyParts;

        IndexKey(List<MdmAttributeValue> keyParts) {
            this.keyParts = List.copyOf(keyParts);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IndexKey indexKey = (IndexKey) o;
            return Objects.equals(keyParts, indexKey.keyParts);
        }

        @Override
        public int hashCode() {
            return Objects.hash(keyParts);
        }

        @Override
        public String toString() {
            return "IndexKey{" +
                "keyParts=" + keyParts +
                '}';
        }
    }

    private static Optional<MdmAttributeValue> findSingleValue(MdmEntity mdmEntity, List<Long> attributePath) {
        List<MdmAttributeValue> values = findValues(mdmEntity, attributePath);
        if (values.size() > 1) {
            throw new IllegalStateException();
        }
        return values.stream().findAny();
    }

    private static List<MdmAttributeValue> findValues(MdmEntity mdmEntity, List<Long> attributePath) {
        return findValues(mdmEntity, attributePath, 0);
    }

    private static List<MdmAttributeValue> findValues(MdmEntity mdmEntity, List<Long> attributePath, int pointer) {
        if (mdmEntity == null || attributePath.size() == 0) {
            return List.of();
        }
        if (pointer == attributePath.size() - 1) {
            return mdmEntity
                .getMdmAttributeValuesOrDefault(attributePath.get(pointer), MdmAttributeValues.getDefaultInstance())
                .getValuesList();
        }
        return mdmEntity
            .getMdmAttributeValuesOrDefault(attributePath.get(pointer), MdmAttributeValues.getDefaultInstance())
            .getValuesList()
            .stream()
            .peek(value -> {
                if (!value.hasStruct()) {
                    throw new IllegalArgumentException();
                }
            })
            .map(MdmAttributeValue::getStruct)
            .map(entity -> findValues(entity, attributePath, pointer + 1))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private static Comparator<MdmEntity> createComparator(MdmOrderByClauses mdmOrderByClauses) {
        return mdmOrderByClauses.getClausesList().stream()
            .map(MdmEntityStorageServiceMock::createComparator)
            .reduce(Comparator::thenComparing)
            .orElse((e1, e2) -> 0);
    }

    private static Comparator<MdmEntity> createComparator(MdmOrderByClause mdmOrderByClause) {
        Comparator<MdmEntity> comparator = (e1, e2) -> {
            List<Long> attributePath = mdmOrderByClause.getMdmAttributePathList();
            Optional<MdmAttributeValue> value1 = findSingleValue(e1, attributePath);
            Optional<MdmAttributeValue> value2 = findSingleValue(e2, attributePath);
            if (value1.isPresent() != value2.isPresent()) {
                return Boolean.compare(value1.isPresent(), value2.isPresent());
            }
            if (value1.isPresent()) {
                return ATTRIBUTE_VALUE_COMPARATOR.compare(value1.get(), value2.get());
            }
            return 0;
        };

        if (mdmOrderByClause.getDirection() == MdmOrderByDirection.DESC) {
            return comparator.reversed();
        }
        return comparator;
    }

    /**
     * Сравнивает attributeValues, если они разного типа - кидает Exception
     */
    private static Comparator<MdmAttributeValue> attributeValueComparator() {
        return (v1, v2) -> {
            if (v1.getValueCase() != v2.getValueCase()) {
                throw new IllegalArgumentException();
            }
            if (v1.hasInt64()) {
                return Long.compare(v1.getInt64(), v2.getInt64());
            }
            if (v1.hasBool()) {
                return Boolean.compare(v1.getBool(), v2.getBool());
            }
            if (v1.hasNumeric()) {
                return new BigDecimal(v1.getNumeric()).compareTo(new BigDecimal(v2.getNumeric()));
            }
            if (v1.hasOption()) {
                return Long.compare(v1.getOption(), v2.getOption());
            }
            if (v1.hasString()) {
                String first = I18nStringUtils.extractRuStrings(v1.getString()).stream()
                    .findAny()
                    .orElse("");
                String second = I18nStringUtils.extractRuStrings(v2.getString()).stream()
                    .findAny()
                    .orElse("");
                return first.compareTo(second);
            }
            throw new UnsupportedOperationException("Can't compare values of type " + v1.getValueCase());
        };
    }
}
