package ru.yandex.market.adv.b2bmonetization.programs.yt.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.inside.yt.kosher.impl.ytree.object.NullSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeKeyField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;

/**
 * Тестовая сущность для таблицы категорий YT.
 * Date: 12.07.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@YTreeObject(nullSerializationStrategy = NullSerializationStrategy.SERIALIZE_NULL_TO_EMPTY)
public class TestCategory {
    @YTreeKeyField
    @YTreeField(key = "shop_id")
    private int shopId;

    @YTreeKeyField
    @YTreeField(key = "feed_id")
    private int feedId;

    @YTreeKeyField
    @YTreeField(key = "category_id")
    private long categoryId;

    private String name;

    private int status;
}
