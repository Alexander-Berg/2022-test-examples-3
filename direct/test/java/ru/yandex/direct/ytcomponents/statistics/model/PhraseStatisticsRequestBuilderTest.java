package ru.yandex.direct.ytcomponents.statistics.model;

import org.jooq.types.ULong;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class PhraseStatisticsRequestBuilderTest {

    @Test
    public void build_success_whenBsPhraseIdWithEverythingAreSet() {
        PhraseStatisticsRequest actual = new PhraseStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                .withBannerId(3L)
                /*.withPhraseId(4L)*/
                .withBsPhraseId(ULong.valueOf(5L))
                .build();

        assertSoftly(softly -> {
            softly.assertThat(actual.getCampaignId())
                    .describedAs("CampaignId").isEqualTo(1L);
            softly.assertThat(actual.getAdGroupId())
                    .describedAs("AdGroupId").isEqualTo(2L);
            softly.assertThat(actual.getBannerId())
                    .describedAs("BannerId").isEqualTo(3L);
            softly.assertThat(actual.getBsPhraseId())
                    .describedAs("BsPhraseId").isEqualTo(ULong.valueOf(5L));
        });
    }

    @Test
    public void build_success_whenPhraseIdWithEverythingAreSet() {
        PhraseStatisticsRequest actual = new PhraseStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                .withBannerId(3L)
                .withPhraseId(4L)
                /*.withBsPhraseId(ULong.valueOf(5L))*/
                .build();

        assertSoftly(softly -> {
            softly.assertThat(actual.getCampaignId())
                    .describedAs("CampaignId").isEqualTo(1L);
            softly.assertThat(actual.getAdGroupId())
                    .describedAs("AdGroupId").isEqualTo(2L);
            softly.assertThat(actual.getBannerId())
                    .describedAs("BannerId").isEqualTo(3L);
            softly.assertThat(actual.getPhraseId())
                    .describedAs("PhraseId").isEqualTo(4L);
        });
    }

    @Test
    public void build_error_whenNoCampaignId() {
        assertThatThrownBy(() -> new PhraseStatisticsRequest.Builder()
                /*.withCampaignId(1L)*/
                .withAdGroupId(2L)
                .withBannerId(3L)
                .withPhraseId(4L)
                /*.withBsPhraseId(ULong.valueOf(5L))*/
                .build()
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CampaignId");
    }

    @Test
    public void build_error_whenNoAdGroupId() {
        assertThatThrownBy(() -> new PhraseStatisticsRequest.Builder()
                .withCampaignId(1L)
                /*.withAdGroupId(2L)*/
                .withBannerId(3L)
                .withPhraseId(4L)
                /*.withBsPhraseId(ULong.valueOf(5L))*/
                .build()
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AdGroup");
    }

    @Test
    public void build_success_whenNoBannerId() {
        assertThatCode(() -> new PhraseStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                /*.withBannerId(3L)*/
                .withPhraseId(4L)
                /*.withBsPhraseId(ULong.valueOf(5L))*/
                .build()
        ).doesNotThrowAnyException();
    }

    @Test
    public void build_error_whenNoAnyPhraseId() {
        assertThatThrownBy(() -> new PhraseStatisticsRequest.Builder()
                .withCampaignId(1L)
                .withAdGroupId(2L)
                .withBannerId(3L)
                /*.withPhraseId(4L)*/
                /*.withBsPhraseId(ULong.valueOf(5L))*/
                .build()
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PhraseId");
    }

}
