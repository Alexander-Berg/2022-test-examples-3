package ru.yandex.market.adv.yt.test.extension;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeKeyField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

/**
 * Date: 13.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@YTreeObject
public class Table {

    @YTreeKeyField
    private int id;

    private String name;

    private Map<String, YTreeNode> map;
}
