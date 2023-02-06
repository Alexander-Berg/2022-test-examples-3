package ru.yandex.market.mbo.gwt.models.visual;

import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author s-ermakov
 */
public class TovarCategoryBuilder {

    private int id;
    private long hid;
    private long parentHid;
    private int tovarId;
    private String name;
    private List<CommonModel.Source> showModelTypes = new ArrayList<>();
    private long guruCategoryId;
    private boolean isGroup;

    private boolean published = true;
    private boolean leaf = true;

    public static TovarCategoryBuilder newBuilder() {
        return new TovarCategoryBuilder();
    }

    public static TovarCategoryBuilder newBuilder(int id, long hid) {
        return newBuilder()
                .setId(id)
                .setHid(hid);
    }

    public TovarCategoryBuilder setId(int id) {
        this.id = id;
        return this;
    }

    public TovarCategoryBuilder setHid(long hid) {
        this.hid = hid;
        return this;
    }

    public TovarCategoryBuilder setParentHid(long parentHid) {
        this.parentHid = parentHid;
        return this;
    }

    public TovarCategoryBuilder setPublished(boolean published) {
        this.published = published;
        return this;
    }

    public TovarCategoryBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public TovarCategoryBuilder addShowModelType(CommonModel.Source type) {
        this.showModelTypes.add(type);
        return this;
    }

    public TovarCategoryBuilder setLeaf(boolean leaf) {
        this.leaf = leaf;
        return this;
    }

    public TovarCategoryBuilder setGuruCategoryId(long guruCategoryId) {
        this.guruCategoryId = guruCategoryId;
        return this;
    }

    public TovarCategoryBuilder setGroup(boolean isGroup) {
        this.isGroup = isGroup;
        return this;
    }

    public TovarCategoryBuilder setTovarId(int tovarId) {
        this.tovarId = tovarId;
        return this;
    }

    public TovarCategory create() {
        TovarCategory tovarCategory = new TovarCategory();
        tovarCategory.setHid(hid);
        tovarCategory.setName(name);
        tovarCategory.setId(id);
        tovarCategory.setParentHid(parentHid);
        tovarCategory.setPublished(published);
        tovarCategory.setName(name);
        tovarCategory.setShowModelTypes(showModelTypes);
        tovarCategory.setLeaf(leaf);
        tovarCategory.setGuruCategoryId(guruCategoryId);
        tovarCategory.setGroup(isGroup);
        tovarCategory.setTovarId(tovarId);
        return tovarCategory;
    }
}
