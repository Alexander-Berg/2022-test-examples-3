package ru.yandex.direct.grid.processing.service.campaign;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.grid.model.campaign.GdMetaCampaignFilterType;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignMetatype;
import ru.yandex.direct.grid.model.campaign.GdiCampaignSource;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@RunWith(Parameterized.class)
public class CampaignServiceUtilsMetaTypePredicateTest {
    @Parameterized.Parameter()
    public List<GdMetaCampaignFilterType> filterTypes;

    @Parameterized.Parameter(1)
    public CampaignType campaignType;

    @Parameterized.Parameter(2)
    public GdiCampaignSource campaignSource;

    @Parameterized.Parameter(3)
    public GdiCampaignMetatype campaignMetatype;

    @Parameterized.Parameter(4)
    public boolean expected;

    @Parameterized.Parameters(name = "filter = {0}, type = {1}, source = {2}, meta type={3}, expected= {4}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {List.of(GdMetaCampaignFilterType.TEXT), CampaignType.TEXT, GdiCampaignSource.DIRECT, null, true},
                {List.of(GdMetaCampaignFilterType.TEXT), CampaignType.MOBILE_CONTENT, GdiCampaignSource.DIRECT, null,
                        false},
                {List.of(GdMetaCampaignFilterType.UC), CampaignType.TEXT, GdiCampaignSource.DIRECT, null, false},
                {List.of(GdMetaCampaignFilterType.UC), CampaignType.MOBILE_CONTENT, GdiCampaignSource.UAC, null, true},
                {List.of(GdMetaCampaignFilterType.UC), CampaignType.TEXT, GdiCampaignSource.UAC, null, true},
                {List.of(GdMetaCampaignFilterType.UC), CampaignType.CPM_BANNER, GdiCampaignSource.UAC, null, true},
                {List.of(GdMetaCampaignFilterType.UC), CampaignType.TEXT, GdiCampaignSource.UAC,
                        GdiCampaignMetatype.ECOM, true},
                {List.of(GdMetaCampaignFilterType.UC_TEXT), CampaignType.TEXT, GdiCampaignSource.UAC,
                        GdiCampaignMetatype.ECOM, false},
                {List.of(GdMetaCampaignFilterType.UC_TEXT), CampaignType.TEXT, GdiCampaignSource.UAC, null, true},
                {List.of(GdMetaCampaignFilterType.UC_ECOM), CampaignType.TEXT, GdiCampaignSource.UAC, null, false},
                {List.of(GdMetaCampaignFilterType.UC_ECOM), CampaignType.TEXT, GdiCampaignSource.UAC,
                        GdiCampaignMetatype.ECOM, true},
                {List.of(GdMetaCampaignFilterType.UC_MOBILE_CONTENT), CampaignType.MOBILE_CONTENT,
                        GdiCampaignSource.UAC, null, true},
                {List.of(GdMetaCampaignFilterType.UC_MOBILE_CONTENT), CampaignType.MOBILE_CONTENT,
                        GdiCampaignSource.DIRECT, null, false},
                {List.of(GdMetaCampaignFilterType.UC_CPM_BANNER), CampaignType.CPM_BANNER, GdiCampaignSource.UAC,
                        null, true},
                {List.of(GdMetaCampaignFilterType.UC_CPM_BANNER), CampaignType.CPM_BANNER, GdiCampaignSource.DIRECT,
                        null, false},
                {List.of(GdMetaCampaignFilterType.TEXT, GdMetaCampaignFilterType.UC_ECOM), CampaignType.TEXT,
                        GdiCampaignSource.DIRECT, null, true},
                {List.of(GdMetaCampaignFilterType.TEXT, GdMetaCampaignFilterType.UC_ECOM), CampaignType.TEXT,
                        GdiCampaignSource.UAC, GdiCampaignMetatype.ECOM, true},
        });
    }

    @Test
    public void testMetaTypePredicate() {
        GdiCampaign campaignToTest = new GdiCampaign()
                .withType(campaignType)
                .withSource(campaignSource)
                .withMetatype(campaignMetatype);
        assertThat(CampaignServiceUtils.metaTypePredicate(listToSet(filterTypes), campaignToTest)).isEqualTo(expected);
    }
}
