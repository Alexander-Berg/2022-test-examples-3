package ru.yandex.market.yql_test;

import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

class YtColumnDefinition {
    String name;
    YtType type;
    YtMetaType metaType;
    YTreeNode yTreeNode;

    YtColumnDefinition(String name, YtType type, YtMetaType metaType, YTreeNode yTreeNode) {
        this.name = name;
        this.type = type;
        this.metaType = metaType;
        this.yTreeNode = yTreeNode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public YtType getType() {
        return type;
    }

    public void setType(YtType type) {
        this.type = type;
    }

    public YtMetaType getMetaType() {
        return metaType;
    }

    public void setMetaType(YtMetaType metaType) {
        this.metaType = metaType;
    }

    public YTreeNode getYTreeNode() {
        return yTreeNode;
    }

    public void setyTreeNode(YTreeNode yTreeNode) {
        this.yTreeNode = yTreeNode;
    }

    @Override
    public String toString() {
        return "YtColumnDefinition{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", metaType=" + metaType +
                ", yTreeNode=" + yTreeNode +
                '}';
    }
}
