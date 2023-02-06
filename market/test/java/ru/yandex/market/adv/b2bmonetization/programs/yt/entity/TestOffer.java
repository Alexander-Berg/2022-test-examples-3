package ru.yandex.market.adv.b2bmonetization.programs.yt.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.inside.yt.kosher.impl.ytree.object.NullSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeKeyField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;

/**
 * Тестовая сущность для таблицы товарных предложений YT.
 * Date: 12.07.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@YTreeObject(nullSerializationStrategy = NullSerializationStrategy.SERIALIZE_NULL_TO_EMPTY)
public class TestOffer {
    @YTreeKeyField
    @YTreeField(key = "shop_id")
    private int shopId;

    @YTreeKeyField
    @YTreeField(key = "feed_id")
    private int feedId;

    @YTreeKeyField
    @YTreeField(key = "offer_id")
    private String offerId;

    @YTreeField(key = "category_id")
    private long categoryId;

    @YTreeField(key = "market_category_id")
    private int marketCategoryId;

    private int status;

    @YTreeField(key = "ssku_offer")
    private boolean sskuOffer;

    @YTreeField(key = "app_autostrategy_id")
    private long plCampaignId;

    @YTreeField(key = "strategy_type")
    private int campaignType;
}
