package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.Arrays;
import java.util.List;

import com.yandex.direct.api.v5.ads.ResumeRequest;
import com.yandex.direct.api.v5.ads.SuspendRequest;
import com.yandex.direct.api.v5.general.IdsCriteria;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class AdsSuspendResumeRequestConverterTest {
    private List<Long> bannerIds = Arrays.asList(1L, 2L);
    private AdsSuspendResumeRequestConverter converter = new AdsSuspendResumeRequestConverter();

    @Test
    public void convertSuspendTest() throws Exception {
        SuspendRequest request = new SuspendRequest().withSelectionCriteria(new IdsCriteria().withIds(bannerIds));
        List<ModelChanges<BannerWithSystemFields>> result = converter.convertSuspend(request);
        checkAllPositive(bannerIds, result, false);
    }

    @Test
    public void convertResumeTest() throws Exception {
        ResumeRequest request = new ResumeRequest().withSelectionCriteria(new IdsCriteria().withIds(bannerIds));
        List<ModelChanges<BannerWithSystemFields>> result = converter.convertResume(request);
        checkAllPositive(bannerIds, result, true);
    }

    private void checkAllPositive(List<Long> bannerIds, List<ModelChanges<BannerWithSystemFields>> result,
                                  boolean expected) {
        assertSoftly(softly -> softly.assertThat(result.size()).isEqualTo(bannerIds.size()));
        result.stream()
                .peek(change -> assertSoftly(softly -> softly
                        .assertThat(change.getChangedProp(BannerWithSystemFields.STATUS_SHOW)).isEqualTo(expected)))
                .forEach(change -> assertSoftly(softly -> softly.assertThat(change.getId()).isIn(bannerIds)));
    }


}
