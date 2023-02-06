package ru.yandex.direct.core.testing.repository;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.assertj.core.api.SoftAssertions;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupBsTags;
import ru.yandex.direct.core.entity.adgroup.model.PageGroupTagEnum;
import ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupMappings;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.read.JooqReaderWithSupplier;
import ru.yandex.direct.jooqmapper.read.JooqReaderWithSupplierBuilder;
import ru.yandex.direct.utils.JsonUtils;

import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUP_BS_TAGS;
import static ru.yandex.direct.jooqmapper.read.ReaderBuilders.fromField;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

public class TestAdGroupBsTagsRepository {

    private static final JooqReaderWithSupplier<AdGroupBsTags> BS_TAGS_READER =
            JooqReaderWithSupplierBuilder.builder(AdGroupBsTags::new)
                    .readProperty(AdGroupBsTags.PAGE_GROUP_TAGS, fromField(ADGROUP_BS_TAGS.PAGE_GROUP_TAGS_JSON)
                            .by(t -> t == null ? null : Arrays.asList(JsonUtils.fromJson(t, PageGroupTagEnum[].class))))
                    .readProperty(AdGroupBsTags.TARGET_TAGS, fromField(ADGROUP_BS_TAGS.TARGET_TAGS_JSON)
                            .by(t -> t == null ? null : Arrays.asList(JsonUtils.fromJson(t, TargetTagEnum[].class))))
                    .build();

    @Autowired
    private DslContextProvider dslContextProvider;

    /**
     * Проверяет, что значения adgroup_bs_tags, соответствующие группе с даннным adgroup_id, соответствуют требованиям,
     * сравнением модели по записи в базе с ожидаемой
     */
    public void softAssertionCheckAdGroupTagsInDbConsumer(SoftAssertions soft,
                                                          int shard,
                                                          Long adGroupId,
                                                          AdGroupBsTags expected) {
        AdGroupBsTags actual = fetchTagsByMapper(shard, adGroupId);
        soft.assertThat(actual)
                .describedAs("Теги на группе должны совпадать с ожидаемыми")
                .isEqualTo(expected);
    }

    private AdGroupBsTags fetchTagsByMapper(int shard, Long adGroupId) {
        return dslContextProvider.ppc(shard)
                .select(ADGROUP_BS_TAGS.PID, ADGROUP_BS_TAGS.PAGE_GROUP_TAGS_JSON, ADGROUP_BS_TAGS.TARGET_TAGS_JSON)
                .from(ADGROUP_BS_TAGS)
                .where(ADGROUP_BS_TAGS.PID.eq(adGroupId))
                .fetchOne(BS_TAGS_READER::fromDb);
    }

    /**
     * Проверяет, что значения adgroup_bs_tags, соответствующие группе с даннным adgroup_id, соответствуют требованиям,
     * сравнением строк в базе с ожидаемыми
     */
    public void softAssertionCheckAdGroupTagsInDbRawConsumer(SoftAssertions soft,
                                                             int shard,
                                                             Long adGroupId,
                                                             String expectedPageGroupTagsValue,
                                                             String expectedTargetTagsValue) {
        soft.assertThat(getAdGroupPageTagsRaw(shard, adGroupId))
                .describedAs("Page теги на группе должны совпадать с ожидаемыми")
                .isEqualTo(expectedPageGroupTagsValue);
        soft.assertThat(getAdGroupTargetTagsRaw(shard, adGroupId))
                .describedAs("Target теги на группе должны совпадать с ожидаемыми")
                .isEqualTo(expectedTargetTagsValue);
    }

    private String getAdGroupPageTagsRaw(int shard, Long adGroupId) {
        return dslContextProvider.ppc(shard)
                .select(ADGROUP_BS_TAGS.PID, ADGROUP_BS_TAGS.PAGE_GROUP_TAGS_JSON)
                .from(ADGROUP_BS_TAGS)
                .where(ADGROUP_BS_TAGS.PID.eq(adGroupId))
                .fetchOne(ADGROUP_BS_TAGS.PAGE_GROUP_TAGS_JSON);
    }

    private String getAdGroupTargetTagsRaw(int shard, Long adGroupId) {
        return dslContextProvider.ppc(shard)
                .select(ADGROUP_BS_TAGS.PID, ADGROUP_BS_TAGS.TARGET_TAGS_JSON)
                .from(ADGROUP_BS_TAGS)
                .where(ADGROUP_BS_TAGS.PID.eq(adGroupId))
                .fetchOne(ADGROUP_BS_TAGS.TARGET_TAGS_JSON);
    }

    /**
     * Проверяет, что значения adgroup_bs_tags, соответствующие группе с даннным adgroup_id, соответствуют требованиям,
     * сравнением списки строк по записи в базе с ожидаемыми
     */
    public void softAssertionCheckAdGroupTagsInDbConsumer(SoftAssertions soft,
                                                          int shard,
                                                          Long adGroupId,
                                                          List<String> expectedPageGroupTagsValue,
                                                          List<String> expectedTargetTagsValue) {
        soft.assertThat(getAdGroupPageTags(shard, adGroupId))
                .describedAs("Page теги на группе должны совпадать с ожидаемыми")
                .isEqualTo(expectedPageGroupTagsValue);
        soft.assertThat(getAdGroupTargetTags(shard, adGroupId))
                .describedAs("Target теги на группе должны совпадать с ожидаемыми")
                .isEqualTo(expectedTargetTagsValue);
    }

    @Nullable
    private List<String> getAdGroupPageTags(int shard, Long adGroupId) {
        return ifNotNull(getAdGroupPageTagsRaw(shard, adGroupId), AdGroupMappings::pageGroupTagsFromDb);
    }

    @Nullable
    private List<String> getAdGroupTargetTags(int shard, Long adGroupId) {
        return ifNotNull(getAdGroupTargetTagsRaw(shard, adGroupId), AdGroupMappings::targetTagsFromDb);
    }
}
