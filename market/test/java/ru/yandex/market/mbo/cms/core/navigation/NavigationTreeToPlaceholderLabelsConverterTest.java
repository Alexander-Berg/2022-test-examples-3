package ru.yandex.market.mbo.cms.core.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.navigation.data.Navigation;
import ru.yandex.market.mbo.cms.core.navigation.data.NavigationTree;
import ru.yandex.market.mbo.cms.core.navigation.data.Node;

public class NavigationTreeToPlaceholderLabelsConverterTest {

    @Test
    @SuppressWarnings("magicnumber")
    public void testConvert() {
        List<NavigationTree> navigationTrees = new ArrayList<>();
        NavigationTree whiteTree = new NavigationTree();
        whiteTree.setCode("green");
        whiteTree.setId(1);
        Node whiteNode = new Node();
        whiteNode.setId(111);
        whiteNode.setName("111");
        whiteTree.setNode(whiteNode);
        navigationTrees.add(whiteTree);

        NavigationTree blueTree = new NavigationTree();
        blueTree.setCode("blue");
        blueTree.setId(2);
        Node blueNode = new Node();
        blueNode.setId(222);
        blueNode.setName("222");
        blueTree.setNode(blueNode);
        navigationTrees.add(blueTree);

        Navigation navigation = Mockito.mock(Navigation.class);
        Mockito.doReturn(navigationTrees).when(navigation).getNavigationTree();

        Map<String, Map<String, String>> result = NavigationTreeToPlaceholderLabelsConverter.convert(navigation);
        Assert.assertEquals(2,
                result.get(NavigationTreeHelper.ALL_NIDS_KEY.toUpperCase()).size());
        Assert.assertEquals(2,
                result.get(NavigationTreeHelper.ALL_NIDS_KEY.toLowerCase()).size());
        Assert.assertEquals(1, result.get(NavigationTreeHelper.NID_BLUE.toUpperCase()).size());
        Assert.assertEquals(1, result.get(NavigationTreeHelper.NID_BLUE.toLowerCase()).size());
        Assert.assertEquals(1, result.get(NavigationTreeHelper.NID_WHITE.toUpperCase()).size());
        Assert.assertEquals(1, result.get(NavigationTreeHelper.NID_WHITE.toLowerCase()).size());
        return;
    }
}
