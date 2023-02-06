package ru.yandex.market.crm.core.test.utils;

import java.util.List;
import java.util.Set;

import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType.Channel;

/**
 * @author apershukov
 */
public class SubscriptionTypes {

    public static final SubscriptionType ALL = new SubscriptionType(
            0, "ALL", "Фейковый тип для отписки от всех типов рассылок", false, false, Set.of(Channel.EMAIL));
    public static final SubscriptionType ADVERTISING = new SubscriptionType(
            2, "ADVERTISING", "Рассылки о скидках и акциях", false, false, Set.of(Channel.EMAIL));
    public static final SubscriptionType STORE_ADVERTISING = new SubscriptionType(
            34, "STORE_ADVERTISING", "Синий Маркет. Подписка на рекламную рассылку", true, true, Set.of(Channel.EMAIL));
    public static final SubscriptionType GRADE_AFTER_CPA = new SubscriptionType(
            4, "GRADE_AFTER_CPA", "Просьба оставить отзыв после заказа", true, false, Set.of(Channel.EMAIL));
    public static final SubscriptionType WISHLIST = new SubscriptionType(
            5, "WISHLIST", "Добавление в список покупок", true, false, Set.of(Channel.EMAIL));
    public static final SubscriptionType STORE_CART = new SubscriptionType(
            36, "STORE_CART", "Синий Маркет. Письма \"Брошеная корзина\"", true, true, Set.of(Channel.EMAIL));

    public static final SubscriptionType STORE_PUSH_ORDER_STATUS = new SubscriptionType(
            63, "STORE_PUSH_ORDER_STATUS", "Push. Статус заказа", true, false, Set.of(Channel.PUSH));
    public static final SubscriptionType STORE_PUSH_GENERAL_ADVERTISING = new SubscriptionType(
            64, "STORE_PUSH_GENERAL_ADVERTISING", "Push. Скидки и акции общие", false, false, Set.of(Channel.PUSH));
    public static final SubscriptionType STORE_PUSH_PERSONAL_ADVERTISING = new SubscriptionType(
            65, "STORE_PUSH_PERSONAL_ADVERTISING", "Push. Персональные предложения", false, false, Set.of(Channel.PUSH)
    );

    public static List<SubscriptionType> TYPES = List.of(
            ALL, ADVERTISING, STORE_ADVERTISING, GRADE_AFTER_CPA, WISHLIST, STORE_CART,
            STORE_PUSH_ORDER_STATUS, STORE_PUSH_GENERAL_ADVERTISING, STORE_PUSH_PERSONAL_ADVERTISING
    );

    private SubscriptionTypes() {
        // Utility class
    }
}
