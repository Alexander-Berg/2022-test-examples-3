package ru.yandex.direct.core.testing.repository;

import java.util.List;

import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.tag.model.Tag;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.dbschema.ppc.Tables.TAG_CAMPAIGN_LIST;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Работа с метками в тестах
 */
public class TestTagRepository {
    private final DslContextProvider dslContextProvider;
    private final ShardHelper shardHelper;
    private final JooqMapperWithSupplier<Tag> jooqMapper;

    @Autowired
    public TestTagRepository(DslContextProvider dslContextProvider,
                             ShardHelper shardHelper, TagRepository tagRepository) {
        this.dslContextProvider = dslContextProvider;
        this.shardHelper = shardHelper;

        jooqMapper = tagRepository.getTagMapper();
    }

    /**
     * Добавляет метки кампаний в базу
     *
     * @param shard    Шард
     * @param clientId ID кампании
     * @param tags     Список тегов
     * @return Список ID добавленных тегов
     */
    public List<Long> addCampaignTags(int shard, ClientId clientId, List<Tag> tags) {
        generateKeys(clientId, tags);
        new InsertHelper<>(dslContextProvider.ppc(shard), TAG_CAMPAIGN_LIST)
                .addAll(jooqMapper, tags)
                .executeIfRecordsAdded();
        return mapList(tags, Tag::getId);
    }

    private void generateKeys(ClientId clientId, List<Tag> tags) {
        List<Long> ids = shardHelper.generateTagIds(clientId.asLong(), tags.size());
        StreamEx.of(tags).zipWith(ids.stream())
                .forKeyValue(Tag::setId);
    }

    /**
     * Удаляет теги из таблицы tag_campaign_list
     *
     * @param shard Шард
     * @param tags  Список тегов
     */
    public void deleteTags(int shard, List<Long> tags) {
        dslContextProvider.ppc(shard)
                .deleteFrom(TAG_CAMPAIGN_LIST)
                .where(TAG_CAMPAIGN_LIST.TAG_ID.in(tags))
                .execute();
    }
}
