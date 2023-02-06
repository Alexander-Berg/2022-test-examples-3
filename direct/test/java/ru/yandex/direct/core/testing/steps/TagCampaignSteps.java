package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.lang.RandomStringUtils;
import org.jooq.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.repository.internal.AdGroupTagsRepository;
import ru.yandex.direct.core.entity.tag.model.Tag;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestTagRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

public class TagCampaignSteps {

    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private TestTagRepository testTagRepository;
    @Autowired
    private AdGroupTagsRepository adGroupTagsRepository;

    public Tag addDefaultTag(AdGroupInfo adGroupInfo) {
        Tag tag = createDefaultTag(adGroupInfo.getCampaignInfo());
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        adGroupTagsRepository.addAdGroupTags(conf, singletonMap(adGroupInfo.getAdGroupId(), singleton(tag.getId())));
        return tag;
    }

    public void addAdGroupTag(AdGroupInfo adGroupInfo, Tag tag) {
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        adGroupTagsRepository.addAdGroupTags(conf, singletonMap(adGroupInfo.getAdGroupId(), singleton(tag.getId())));
    }

    public Tag createDefaultTag(CampaignInfo campaignInfo) {
        Tag tag = new Tag()
                .withName(String.format("tag_%s", RandomStringUtils.randomNumeric(20)))
                .withCampaignId(campaignInfo.getCampaignId())
                .withCreateTime(LocalDateTime.now());
        testTagRepository.addCampaignTags(campaignInfo.getShard(), campaignInfo.getClientId(), singletonList(tag));
        return tag;
    }

    public List<Long> createDefaultTags(int shard, ClientId clientId, long campaignId, int count) {
        checkArgument(count > 0);
        LocalDateTime now = LocalDateTime.now();
        List<Tag> tags = IntStream.range(0, count)
                .mapToObj(i -> new Tag().withCampaignId(campaignId).withCreateTime(now))
                .collect(toList());
        return testTagRepository.addCampaignTags(shard, clientId, tags);
    }
}
