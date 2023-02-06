package ru.yandex.market.mbo.cms.core.utils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.entitydata.HidData;
import ru.yandex.market.mbo.cms.core.navigation.data.Navigation;
import ru.yandex.market.mbo.cms.core.navigation.data.NavigationTree;
import ru.yandex.market.mbo.cms.core.navigation.data.Node;

public class MboDataHelperTest {

    public static final long HID_1 = 9991;
    public static final long HID_2 = 99911;
    public static final long HID_3 = 9992;
    public static final long HID_4 = 9993;
    public static final long NID_1 = 1;
    public static final long NID_2 = 11;
    public static final long NID_3 = 2;
    public static final long NID_4 = 22;
    public static final long NID_5 = 3;
    public static final String NODE_NAME_1 = "node1";
    public static final String NODE_NAME_2 = "node11";
    public static final String NODE_NAME_3 = "node2";
    public static final String NODE_NAME_4 = "node22";
    public static final String NODE_NAME_5 = "node3";

    @Test
    @SuppressWarnings("magicnumber")
    public void getHidDataTest() {
        Navigation navigation = new Navigation();

        NavigationTree tree1 = new NavigationTree();
        tree1.setCode("green");
        Node node1 = new Node();
        node1.setName(NODE_NAME_1);
        node1.setId(NID_1);
        node1.setHid(HID_1);
        Node node11 = new Node();
        node11.setName(NODE_NAME_2);
        node11.setId(NID_2);
        node11.setHid(HID_2);
        node1.getNode().add(node11);
        tree1.setNode(node1);
        navigation.getNavigationTree().add(tree1);

        NavigationTree tree2 = new NavigationTree();
        tree2.setCode("blue");
        Node node2 = new Node();
        node2.setName(NODE_NAME_3);
        node2.setId(NID_3);
        node2.setHid(HID_3);
        Node node22 = new Node();
        node22.setName(NODE_NAME_4);
        node22.setId(NID_4);
        node22.setHid(HID_3);
        node2.getNode().add(node22);
        tree2.setNode(node2);
        navigation.getNavigationTree().add(tree2);

        NavigationTree tree3 = new NavigationTree();
        tree3.setCode("unknown");
        Node node3 = new Node();
        node3.setName(NODE_NAME_5);
        node3.setId(NID_5);
        node3.setHid(HID_4);

        List<HidData> result = MboDataHelper.getHidData(navigation);

        Map<Long, HidData> hidToData = result.stream().collect(Collectors.toMap(HidData::getHid, Function.identity()));

        Assert.assertEquals(3, result.size());
        Assert.assertEquals(1, hidToData.get(HID_1).getNids().size());
        Assert.assertEquals(NODE_NAME_1, hidToData.get(HID_1).getTitle());
        Assert.assertEquals(NID_1, hidToData.get(HID_1).getNids().get(0).getId());
        Assert.assertEquals(1, hidToData.get(HID_2).getNids().size());
        Assert.assertEquals(NODE_NAME_2, hidToData.get(HID_2).getTitle());
        Assert.assertEquals(NID_2, hidToData.get(HID_2).getNids().get(0).getId());
        Assert.assertEquals(2, hidToData.get(HID_3).getNids().size());
        Assert.assertEquals(NID_3, hidToData.get(HID_3).getNids().get(0).getId());
        Assert.assertEquals(NID_4, hidToData.get(HID_3).getNids().get(1).getId());
        Assert.assertEquals(NODE_NAME_3, hidToData.get(HID_3).getTitle());
    }
}
