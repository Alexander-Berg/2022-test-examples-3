package ru.yandex.market.mbo.tms.contentquality;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.OperationContext;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.db.params.ParameterProtoConverter;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.yt.utils.StatisticsStub;
import ru.yandex.market.mbo.yt.utils.YieldStub;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class YtContentQualityParamsMapperTest {

    private static final String TITLE_TEMPLATE = "{\"delimiter\":\" \"," +
        "\"values\":[[(v12513498 ),(v12513498 )],[(1 ),(v12513498 ),(v7893318 ),(true)]," +
        "{\"values\":[[(t0 ),(t0 ),null,(true)],[(v12513498 ),(\", \"+ v12513498 )]]}]}";
    private static final String TITLE_TEMPLATE_WITHOUT_PARAMS = "{\"delimiter\":\" \"," +
        "\"values\":[[(v12513498 ),(\"не параметр \" ),null,(true)],[(1 ),(t0 ),null,(true)]]}";

    private static final MboParameters.Category CATEGORY = MboParameters.Category.newBuilder()
        .setHid(1)
        .addName(ParameterProtoConverter.convert(WordUtil.defaultWord("test")))
        .setGuruTitleTemplate(TITLE_TEMPLATE)
        .setSkuTitleTemplate(TITLE_TEMPLATE_WITHOUT_PARAMS)
        .build();

    private YtContentQualityParamsMapper mapper;
    private YieldStub<YTreeMapNode> yield;
    private StatisticsStub statistics;
    private OperationContext context;

    @Before
    public void init() {
        mapper = new YtContentQualityParamsMapper();
        yield = new YieldStub<>();
        statistics = new StatisticsStub();
        context = new OperationContext();
    }

    @Test
    public void mapContentQualityParamsTest() {
        mapper.map(categoryMapNode(), yield, statistics, context);

        YTreeMapNode node = yield.getOut().get(0);

        Assert.assertEquals(CATEGORY.getHid(),
            node.getLong(YtContentQualityParamsMapper.HID));
        Assert.assertEquals(getCategoryName(),
            node.getString(YtContentQualityParamsMapper.NAME));

        YTreeListNode guruTitleParamsNode = node.getOrThrow(YtContentQualityParamsMapper.GURU_TITLE_PARAMS).listNode();
        Assert.assertEquals(2, guruTitleParamsNode.size());

        YTreeListNode skuTitleParamsNode = node.getOrThrow(YtContentQualityParamsMapper.SKU_TITLE_PARAMS).listNode();
        Assert.assertEquals(0, skuTitleParamsNode.size());

        Assert.assertFalse(node.get(YtContentQualityParamsMapper.BLUE_GROUPING_TITLE_PARAMS).isPresent());
    }

    private YTreeMapNode categoryMapNode() {
        Map<String, Object> map = new HashMap<>();
        map.put("hid", CATEGORY.getHid());
        map.put("name", getCategoryName());
        map.put("data", CATEGORY.toByteArray());
        return YTree.builder().value(map).build().mapNode();
    }

    private String getCategoryName() {
        return WordUtil.getFirstDefaultWord(CATEGORY.getNameList()
            .stream().map(ParameterProtoConverter::convert)
            .collect(Collectors.toList()));
    }
}
