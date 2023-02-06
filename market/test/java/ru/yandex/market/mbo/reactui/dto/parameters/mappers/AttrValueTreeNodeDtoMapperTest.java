package ru.yandex.market.mbo.reactui.dto.parameters.mappers;

import org.junit.Test;

import ru.yandex.market.mbo.gwt.models.params.AttrValueInfo;
import ru.yandex.market.mbo.gwt.models.params.AttrValueTreeNode;
import ru.yandex.market.mbo.reactui.dto.parameters.AttrValueTreeNodeDto;

import static org.junit.Assert.assertEquals;

public class AttrValueTreeNodeDtoMapperTest {

    public static final int CHILD_HID = 54321;
    public static final String CHILD_CATEGORY = "Test category";
    public static final int ROOT_HID = 12345;
    public static final String ROOT_CATEGORY = "All category";

    @Test
    public void toAttrValueTreeNodeDto() {
        AttrValueTreeNodeDtoMapper mapper = new AttrValueTreeNodeDtoMapper() {
        };
        final AttrValueTreeNodeDto attrValueTreeNodeDto = mapper.toAttrValueTreeNodeDto(buildTree());
        final AttrValueInfo<?> data = attrValueTreeNodeDto.getData();
        assertEquals(data.getCategoryHid(), ROOT_HID);
        assertEquals(data.getCategoryName(), ROOT_CATEGORY);

        final AttrValueTreeNodeDto children = attrValueTreeNodeDto.getChildren().get(0);
        final AttrValueInfo<?> childrenData = children.getData();
        assertEquals(childrenData.getCategoryHid(), CHILD_HID);
        assertEquals(childrenData.getCategoryName(), CHILD_CATEGORY);
    }

    private AttrValueTreeNode<?> buildTree() {
        AttrValueTreeNode<Boolean> children = new AttrValueTreeNode<>();
        children.setData(new AttrValueInfo<>(CHILD_CATEGORY, CHILD_HID, true));

        AttrValueTreeNode<Boolean> root = new AttrValueTreeNode<>();
        root.setData(new AttrValueInfo<>(ROOT_CATEGORY, ROOT_HID, null));
        root.addChild(children);
        return root;
    }
}
