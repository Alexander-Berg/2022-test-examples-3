package ru.yandex.market.mbo.export.helper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.CachedTreeService;
import ru.yandex.market.mbo.db.TovarTreeDaoMock;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceMock;
import ru.yandex.market.mbo.gwt.models.linkedvalues.LinkDirection;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkType;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ayratgdl
 * @date 03.12.17
 */
public class LinkedValuesExportHelperTest {

    private static final Integer CACHE_TIMEOUT_MINUTES = 5;

    private static final TovarCategory CATEGORY_1 = new TovarCategory("Категория 1", 101L, -1L);
    private static final TovarCategory CATEGORY_2 = new TovarCategory("Категория 2", 102L, 101L);
    private static final Long OPTION_P1_1 = 311L;
    private static final Long OPTION_P1_2 = 312L;
    private static final Long OPTION_P1_3 = 313L;
    private static final Long OPTION_P2_1 = 321L;
    private static final Long OPTION_P2_2 = 322L;
    private static final Long OPTION_P2_3 = 323L;
    private static final CategoryParam PARAMETER_1 =
        buildParameter(201L, OPTION_P1_1, OPTION_P1_2, OPTION_P1_3);
    private static final CategoryParam PARAMETER_2 =
        buildParameter(202L, OPTION_P2_1, OPTION_P2_2, OPTION_P2_3);

    private static final ValueLinkBuilder LINK_BUILDER_P1_P2 =
        new ValueLinkBuilder(PARAMETER_1.getId(), PARAMETER_2.getId());

    private ValueLinkServiceMock valueLinkService;
    private CachedTreeService treeService;

    @Before
    public void setUp() throws Exception {
        valueLinkService = new ValueLinkServiceMock();

        TovarTreeDaoMock tovarTreeDao = new TovarTreeDaoMock(CATEGORY_1, CATEGORY_2);
        treeService = new CachedTreeService(tovarTreeDao, CACHE_TIMEOUT_MINUTES);
    }

    @Test
    public void getValueLinksEmpty() {
        List<ValueLink> actualValueLinks =
            LinkedValuesExportHelper.getValueLinks(valueLinkService, treeService,
                                                   CATEGORY_1.getHid(), Collections.emptyList());

        Assert.assertEquals(Collections.emptyList(), actualValueLinks);
    }

    @Test
    public void getValueLinksFromCategory() {
        ValueLink link1 = LINK_BUILDER_P1_P2.build(OPTION_P1_1, OPTION_P2_1, CATEGORY_1.getHid());
        valueLinkService.saveValueLink(link1);

        List<ValueLink> actualValueLinks =
            LinkedValuesExportHelper.getValueLinks(valueLinkService, treeService,
                                                   CATEGORY_1.getHid(), Arrays.asList(PARAMETER_1, PARAMETER_2));

        List<ValueLink> expectedValueLinks = Arrays.asList(link1);

        Assert.assertEquals(expectedValueLinks, actualValueLinks);
    }

    @Test
    public void getValueLinksFromGlobal() {
        ValueLink link1 = LINK_BUILDER_P1_P2.build(OPTION_P1_1, OPTION_P2_1, KnownIds.GLOBAL_CATEGORY_ID);
        valueLinkService.saveValueLink(link1);

        List<ValueLink> actualValueLinks =
            LinkedValuesExportHelper.getValueLinks(valueLinkService, treeService,
                                                   KnownIds.GLOBAL_CATEGORY_ID,
                                                   Arrays.asList(PARAMETER_1, PARAMETER_2));

        List<ValueLink> expectedValueLinks = Arrays.asList(link1);

        Assert.assertEquals(expectedValueLinks, actualValueLinks);
    }

    @Test
    public void getValueLinksFromParentCategory() {
        ValueLink link1 = LINK_BUILDER_P1_P2.build(OPTION_P1_1, OPTION_P2_1, CATEGORY_1.getHid());
        valueLinkService.saveValueLink(link1);

        List<ValueLink> actualValueLinks =
            LinkedValuesExportHelper.getValueLinks(valueLinkService, treeService,
                                                   CATEGORY_2.getHid(), Arrays.asList(PARAMETER_1, PARAMETER_2));

        List<ValueLink> expectedValueLinks = Arrays.asList(link1);

        Assert.assertEquals(expectedValueLinks, actualValueLinks);
    }

    @Test
    public void getValueLinksExpectedReplaceDuplicateFromParentCategory() {
        ValueLink link2 = LINK_BUILDER_P1_P2.build(OPTION_P1_1, OPTION_P2_1, CATEGORY_2.getHid());
        valueLinkService.saveValueLink(link2);

        ValueLink link1 = LINK_BUILDER_P1_P2.build(OPTION_P1_1, OPTION_P2_1, CATEGORY_1.getHid());
        valueLinkService.saveValueLink(link1);

        List<ValueLink> actualValueLinks =
            LinkedValuesExportHelper.getValueLinks(valueLinkService, treeService,
                                                   CATEGORY_2.getHid(), Arrays.asList(PARAMETER_1, PARAMETER_2));

        List<ValueLink> expectedValueLinks = Arrays.asList(link2);

        Assert.assertEquals(expectedValueLinks, actualValueLinks);
    }

    @Test
    public void getValueLinksFromGlobalParentAndCurrentCategory() {
        ValueLink link1 = LINK_BUILDER_P1_P2.build(OPTION_P1_1, OPTION_P2_1, KnownIds.GLOBAL_CATEGORY_ID);
        valueLinkService.saveValueLink(link1);

        ValueLink link2 = LINK_BUILDER_P1_P2.build(OPTION_P1_2, OPTION_P2_2, CATEGORY_1.getHid());
        valueLinkService.saveValueLink(link2);

        ValueLink link3 = LINK_BUILDER_P1_P2.build(OPTION_P1_3, OPTION_P2_3, CATEGORY_2.getHid());
        valueLinkService.saveValueLink(link3);

        List<ValueLink> actualValueLinks =
            LinkedValuesExportHelper.getValueLinks(valueLinkService, treeService,
                                                   CATEGORY_2.getHid(),
                                                   Arrays.asList(PARAMETER_1, PARAMETER_2));

        List<ValueLink> expectedValueLinks = Arrays.asList(link1, link2, link3);

        Assert.assertEquals(expectedValueLinks, actualValueLinks);
    }

    @Test
    public void getValueLinksExpectedSkipLinkWithAbsentParameter() {
        ValueLink link1 = LINK_BUILDER_P1_P2.build(OPTION_P1_1, OPTION_P2_1, CATEGORY_1.getHid());
        valueLinkService.saveValueLink(link1);

        List<ValueLink> actualValueLinks =
            LinkedValuesExportHelper.getValueLinks(valueLinkService, treeService,
                                                   CATEGORY_1.getHid(), Arrays.asList(PARAMETER_1));

        List<ValueLink> expectedValueLinks = Collections.emptyList();

        Assert.assertEquals(expectedValueLinks, actualValueLinks);
    }

    private static CategoryParam buildParameter(Long paramId, Long... optionIds) {
        CategoryParam param = new Parameter();
        param.setId(paramId);

        for (Long optionId : optionIds) {
            Option option = new OptionImpl();
            option.setId(optionId);
            param.addOption(option);
        }

        return param;
    }

    private static class ValueLinkBuilder {
        private final Long sourceParamId;
        private final Long targetParamId;

        ValueLinkBuilder(Long sourceParamId, Long targetParamId) {
            this.sourceParamId = sourceParamId;
            this.targetParamId = targetParamId;
        }

        public ValueLink build(Long sourceOptionId, Long targetOptionId, Long categoryId) {
            ValueLink link = new ValueLink();
            link.setSourceParamId(sourceParamId);
            link.setSourceOptionId(sourceOptionId);
            link.setTargetParamId(targetParamId);
            link.setTargetOptionId(targetOptionId);
            link.setCategoryHid(categoryId);
            link.setLinkDirection(LinkDirection.DIRECT);
            link.setType(ValueLinkType.GENERAL);
            return link;
        }
    }
}
