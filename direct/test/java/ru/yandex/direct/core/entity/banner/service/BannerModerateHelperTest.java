package ru.yandex.direct.core.entity.banner.service;

import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.service.moderation.BannerModerationHelper;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.testing.data.TestNewImageBanners;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BannerModerateHelperTest {

    @Mock
    CreativeRepository creativeRepository;

    @InjectMocks
    BannerModerationHelper helper;

    List<AppliedChanges<BannerWithSystemFields>> changes;

    @Before
    public void setUp() throws Exception {
        Creative creative = new Creative().withId(1L);
        BannerWithSystemFields firstBanner = TestNewImageBanners.clientImageBannerWithCreative(creative.getId())
                .withId(1L);
        BannerWithSystemFields secondBanner = TestNewImageBanners.clientImageBannerWithCreative(creative.getId())
                .withId(2L);
        changes = StreamEx.of(firstBanner, secondBanner)
                .map(b -> new ModelChanges<>(b.getId(), BannerWithSystemFields.class).applyTo(b))
                .toList();
        when(creativeRepository.getCreatives(anyInt(), eq(singleton(creative.getId()))))
                .thenReturn(singletonList(creative));
    }

    @Test
    public void prepareCreativeChangesByBannerId() {
        Map<Long, AppliedChanges<Creative>> result = helper.prepareCreativeChangesByBannerId(1, changes);
        changes.forEach(ac -> assertThat(result, hasKey(ac.getModel().getId())));
    }
}
