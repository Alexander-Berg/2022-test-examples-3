package ru.yandex.market.ir.tms.mbo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.ir.tms.mbo.mbologs.formalization.FormalizationYtHypothesesMapper;
import ru.yandex.market.ir.tms.mbo.mbologs.formalization.FormalizationYtHypothesesReducer;
import ru.yandex.market.ir.tms.mbo.mbologs.vendorcodes.VendorCodesProcessingUtils;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author astafurovme
 */
public class FormalizationYtHypothesisTest {

    private static final int HID_100 = 100;
    private static final int HID_200 = 200;

    private TovarTree tree;
    private final Map<String, List<Long>> paramNames = new HashMap<>();
    private final Map<Long, Long> paramCategories = new HashMap<>();

    private final List<YTreeMapNode> mapReduceInput = new ArrayList<>();

    private static final Statistics STATISTICS_STUB = new Statistics() {
        @Override
        public void write(YTreeMapNode metricsDict) {
        }

        @Override
        public void close() throws IOException {
        }
    };

    /* Make test tovar tree with parameters
                                                      Param_1(1), Param_2(2), Param_3(3)
                                                     /
                       /-- Тестовая категория 1 (100)
     Все товары (90401)
                       \-- Тестовая категория 2 (200)
                                                     \
                                                      Param_4(4), Param_5(5), Param_6(6)
    */
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        // Make test tovar tree
        List<TovarCategory> categories = Arrays.asList(
            makeTovarCategory("Все товары", TovarCategory.ROOT_HID, 0),
            makeTovarCategory("Тестовая категория 1", HID_100, TovarCategory.ROOT_HID),
            makeTovarCategory("Тестовая категория 2", HID_200, TovarCategory.ROOT_HID)
        );

        final Map<Long, TovarCategoryNode> nodes = new HashMap<>(categories.size());
        for (TovarCategory tc : categories) {
            nodes.put(tc.getHid(), new TovarCategoryNode(tc));
        }

        tree = new TovarTree(nodes.get(TovarCategory.ROOT_HID));

        for (TovarCategoryNode node : nodes.values()) {
            TovarCategoryNode parent = nodes.get(node.getParentHid());
            if (parent != null) {
                parent.addChild(node);
            }
        }
        tree.levelTree();

        // Link test parameters with test tovar tree categories (hid, param_id, param_name)
        List<String[]> parameters = Arrays.asList(
            new String[]{"100", "1", "Param_1"},
            new String[]{"100", "2", "Param_2"},
            new String[]{"100", "3", "Param_3"},
            new String[]{"200", "4", "Param_4"},
            new String[]{"200", "5", "Param_5"},
            new String[]{"200", "6", "Param_6"}
        );
        for (String[] param : parameters) {
            long hid = Long.parseLong(param[0]);
            long paramId = Long.parseLong(param[1]);
            String paramName = VendorCodesProcessingUtils.normalize(param[2]);

            paramCategories.put(paramId, hid);

            List<Long> paramIds = paramNames.get(paramName);
            if (paramIds == null) {
                paramIds = new ArrayList<>();
                paramNames.put(paramName, paramIds);
            }
            paramIds.add(paramId);
        }

        // Make Map operation input
        // || offer_id | hid | offer_params_xml    ||
        // || 0000     | 100 | <offer_params .../> ||
        // || 0001     | 100 | <offer_params .../> ||
        mapReduceInput.addAll(
            Arrays.asList(
                // Correct entries. Parameters with given names exists in appropriate categories
                buildOfferIdNode("0000", HID_100,
                    buildOfferParamXmlEntry("Param_1", "Value_1"),
                    buildOfferParamXmlEntry("Param_2", "Value_2")
                ),
                buildOfferIdNode("0001", HID_100,
                    buildOfferParamXmlEntry("Param_2", "Value_2"),
                    buildOfferParamXmlEntry("Param_3", "Value_3")
                ),
                buildOfferIdNode("0002", HID_100,
                    buildOfferParamXmlEntry("Param_3", "Value_3")
                ),
                buildOfferIdNode("0003", HID_100,
                    buildOfferParamXmlEntry("Param_3", "Value_3")
                ),
                buildOfferIdNode("0004", HID_200,
                    buildOfferParamXmlEntry("Param_4", "Value_2")
                ),
                // Ignored entries. Param_2 and Param_1 does't exists in category 200
                buildOfferIdNode("0005", HID_200,
                    buildOfferParamXmlEntry("Param_2", "Value_2")
                ),
                buildOfferIdNode("0006", HID_200,
                    buildOfferParamXmlEntry("Param_1", "Value_1")
                )
            )
        );
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testMapReduce() throws Exception {
        MapOperationOut mapOut = new MapOperationOut();
        FormalizationYtHypothesesMapper mapper = new FormalizationYtHypothesesMapper(paramNames, paramCategories, tree);
        for (YTreeMapNode mapEntry : mapReduceInput) {
            mapper.map(mapEntry, mapOut, STATISTICS_STUB);
        }

        ReduceOperationOut reduceOut = new ReduceOperationOut();
        FormalizationYtHypothesesReducer reducer = new FormalizationYtHypothesesReducer(tree);
        for (Map.Entry<YTreeMapNode, ListF<YTreeMapNode>> combinedOffers : mapOut.allGroupedOffers.entrySet()) {
            YTreeMapNode key = combinedOffers.getKey();
            ListF<YTreeMapNode> offers = combinedOffers.getValue();
            reducer.reduce(key, offers.iterator(), reduceOut, STATISTICS_STUB);
        }

        check(reduceOut, 1, HID_100, "Value_1", "0000", HID_100, HID_100, 1);
        check(reduceOut, 2, HID_100, "Value_2", "0000", HID_100, HID_100, 2);
        check(reduceOut, 2, HID_100, "Value_2", "0001", HID_100, HID_100, 2);
        check(reduceOut, 3, HID_100, "Value_3", "0001", HID_100, HID_100, 3);
        check(reduceOut, 3, HID_100, "Value_3", "0002", HID_100, HID_100, 3);
        check(reduceOut, 3, HID_100, "Value_3", "0003", HID_100, HID_100, 3);
        check(reduceOut, 4, HID_200, "Value_2", "0004", HID_200, HID_200, 1);

        Assert.assertTrue(reduceOut.reducedData.size() == 7);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static void check(ReduceOperationOut reduceOperationStub,
                              long paramId, long paramHid, String value,
                              String offerId, long offerHid, long resultHid, int freq) {
        YTreeMapNode entries = new YTreeBuilder().beginMap()
            .key("param_id").value(paramId)
            .key("param_hid").value(paramHid)
            .key("value").value(value)
            .key("offer_id").value(offerId)
            .key("offer_hid").value(offerHid)
            .key("result_hid").value(resultHid)
            .key("frequency").value(freq)
            .buildMap();

        Assert.assertTrue(reduceOperationStub.reducedData.contains(entries));
    }

    /**
     * Group data by key (param_id, param_hid, value) after map operation.
     */
    private static class MapOperationOut implements Yield<YTreeMapNode> {

        private Map<YTreeMapNode, ListF<YTreeMapNode>> allGroupedOffers = new HashMap<>();

        @Override
        public void yield(YTreeMapNode mappedRow) {
            long paramId = mappedRow.getLong("param_id");
            long paramHid = mappedRow.getLong("param_hid");
            String value = mappedRow.getString("value");

            YTreeMapNode groupByKey = new YTreeBuilder().beginMap()
                .key("param_id").value(paramId)
                .key("param_hid").value(paramHid)
                .key("value").value(value)
                .buildMap();

            ListF<YTreeMapNode> groupedOffers = allGroupedOffers.get(groupByKey);
            if (groupedOffers == null) {
                groupedOffers = new ArrayListF<>();
                allGroupedOffers.put(groupByKey, groupedOffers);
            }
            groupedOffers.add(mappedRow);
        }

        @Override
        public void yield(int index, YTreeMapNode value) {
        }

        @Override
        public void close() throws IOException {
        }

    }

    /**
     * Just store reduced data as is.
     */
    private static class ReduceOperationOut implements Yield<YTreeMapNode> {

        private List<YTreeMapNode> reducedData = new ArrayList<>();

        @Override
        public void yield(YTreeMapNode value) {
            reducedData.add(value);
        }

        @Override
        public void yield(int index, YTreeMapNode value) {
        }

        @Override
        public void close() throws IOException {
        }
    }

    private static TovarCategory makeTovarCategory(String name, long hid, long parentHid) {
        return new TovarCategory(name, hid, parentHid);
    }

    private static YTreeMapNode buildOfferIdNode(String offerId, long hid, String... offerParamsEntiries) {
        return new YTreeBuilder().beginMap()
            .key("classifier_magic_id").value(offerId)
            .key("category_id").value(hid)
            .key("offer_params").value(buildOfferParamsXml(offerParamsEntiries))
            .buildMap();
    }

    private static String buildOfferParamXmlEntry(String paramName, String value) {
        return String.format("<param name=\"%s\" unit=\"\">%s</param>", paramName, value);
    }

    private static String buildOfferParamsXml(String... paramEntries) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><offer_params>");
        for (String paramEntry : paramEntries) {
            sb.append(paramEntry);
        }
        sb.append("</offer_params>");
        return sb.toString();
    }

}
