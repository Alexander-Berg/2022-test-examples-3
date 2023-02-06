package ru.yandex.market.pers.notify.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static ru.yandex.market.pers.notify.model.NotificationType.ACCESSORIES;
import static ru.yandex.market.pers.notify.model.NotificationType.ADVERTISING;
import static ru.yandex.market.pers.notify.model.NotificationType.ALL;
import static ru.yandex.market.pers.notify.model.NotificationType.BT_STORE_ADVERTISING;
import static ru.yandex.market.pers.notify.model.NotificationType.CART;
import static ru.yandex.market.pers.notify.model.NotificationType.COUPON;
import static ru.yandex.market.pers.notify.model.NotificationType.FORUM;
import static ru.yandex.market.pers.notify.model.NotificationType.GRADE_AFTER_CPA;
import static ru.yandex.market.pers.notify.model.NotificationType.GRADE_AFTER_ORDER;
import static ru.yandex.market.pers.notify.model.NotificationType.JOURNAL;
import static ru.yandex.market.pers.notify.model.NotificationType.JOURNAL_COMMENTS_REJECTION;
import static ru.yandex.market.pers.notify.model.NotificationType.JOURNAL_MODERATION_RESULTS;
import static ru.yandex.market.pers.notify.model.NotificationType.JOURNAL_NEW_ANSWERS_ON_COMMENTS;
import static ru.yandex.market.pers.notify.model.NotificationType.JOURNAL_NEW_COMMENTS;
import static ru.yandex.market.pers.notify.model.NotificationType.MODEL_GRADE;
import static ru.yandex.market.pers.notify.model.NotificationType.PA_ON_SALE;
import static ru.yandex.market.pers.notify.model.NotificationType.PRICE_DROP;
import static ru.yandex.market.pers.notify.model.NotificationType.QA_AGITATION;
import static ru.yandex.market.pers.notify.model.NotificationType.SBER_STORE_ADVERTISING;
import static ru.yandex.market.pers.notify.model.NotificationType.SHOP_GRADE;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_ADVERTISING;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_ADVERTISING_WELCOME;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_CART;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_GENERAL_INFORMATION;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_WISHLIST;
import static ru.yandex.market.pers.notify.model.NotificationType.SUCCESSFUL_GRADE;
import static ru.yandex.market.pers.notify.model.NotificationType.TRANSBOUNDARY_TRADING_ADVERTISING;
import static ru.yandex.market.pers.notify.model.NotificationType.TRANSBOUNDARY_TRADING_ADVERTISING_NEW;
import static ru.yandex.market.pers.notify.model.NotificationType.TRANSBOUNDARY_TRADING_ADVERTISING_PUSH;
import static ru.yandex.market.pers.notify.model.NotificationType.TRANSBOUNDARY_TRADING_TRANSACTION;
import static ru.yandex.market.pers.notify.model.NotificationType.TRANSBOUNDARY_TRADING_TRANSACTION_PUSH;
import static ru.yandex.market.pers.notify.model.NotificationType.TRANSBOUNDARY_TRADING_TRIGGER;
import static ru.yandex.market.pers.notify.model.NotificationType.TRANSBOUNDARY_TRADING_TRIGGER_PUSH;
import static ru.yandex.market.pers.notify.model.NotificationType.UGC_AUTHOR_REPORT;
import static ru.yandex.market.pers.notify.model.NotificationType.VIDEO_NEW_COMMENTS;
import static ru.yandex.market.pers.notify.model.NotificationType.WISHLIST;

/**
 * @author vtarasoff
 * @since 19.07.2021
 */
public class NotificationTypeTest {
    private static final Set<NotificationType> DEFAULT_TYPES = new HashSet<>(Arrays.asList(
            VIDEO_NEW_COMMENTS,
            JOURNAL_NEW_COMMENTS,
            SUCCESSFUL_GRADE,
            STORE_WISHLIST,
            STORE_CART,
            UGC_AUTHOR_REPORT,
            QA_AGITATION,
            MODEL_GRADE,
            CART,
            STORE_GENERAL_INFORMATION,
            TRANSBOUNDARY_TRADING_TRANSACTION_PUSH,
            TRANSBOUNDARY_TRADING_TRANSACTION,
            JOURNAL_COMMENTS_REJECTION,
            COUPON,
            SHOP_GRADE,
            JOURNAL_NEW_ANSWERS_ON_COMMENTS,
            GRADE_AFTER_CPA,
            JOURNAL_MODERATION_RESULTS,
            WISHLIST,
            GRADE_AFTER_ORDER,
            ACCESSORIES,
            FORUM,
            STORE_ADVERTISING
    ));

    private static final Set<NotificationType> NON_PARAMETRIC_TYPES = new HashSet<>(Arrays.asList(
            VIDEO_NEW_COMMENTS,
            SBER_STORE_ADVERTISING,
            TRANSBOUNDARY_TRADING_ADVERTISING_PUSH,
            STORE_WISHLIST,
            STORE_CART,
            TRANSBOUNDARY_TRADING_TRIGGER_PUSH,
            TRANSBOUNDARY_TRADING_TRIGGER,
            UGC_AUTHOR_REPORT,
            COUPON,
            ACCESSORIES,
            JOURNAL_NEW_COMMENTS,
            JOURNAL_NEW_ANSWERS_ON_COMMENTS,
            TRANSBOUNDARY_TRADING_ADVERTISING,
            SUCCESSFUL_GRADE,
            JOURNAL,
            WISHLIST,
            BT_STORE_ADVERTISING,
            TRANSBOUNDARY_TRADING_TRANSACTION,
            JOURNAL_COMMENTS_REJECTION,
            STORE_GENERAL_INFORMATION,
            TRANSBOUNDARY_TRADING_TRANSACTION_PUSH,
            FORUM,
            STORE_ADVERTISING_WELCOME,
            SHOP_GRADE,
            TRANSBOUNDARY_TRADING_ADVERTISING_NEW,
            GRADE_AFTER_CPA,
            JOURNAL_MODERATION_RESULTS,
            GRADE_AFTER_ORDER,
            STORE_ADVERTISING,
            QA_AGITATION,
            MODEL_GRADE,
            CART,
            ADVERTISING
    ));

    private static final Set<NotificationType> CONFIRMABLE_TYPES = new HashSet<>(Arrays.asList(
            ADVERTISING, STORE_ADVERTISING, PA_ON_SALE, PRICE_DROP, JOURNAL
    ));

    @Test
    void testConfirmableTypes() {
        assertThat(NotificationType.getConfirmableTypes(), equalTo(CONFIRMABLE_TYPES));
    }

    @Test
    void testDefaultTypes() {
        assertThat(NotificationType.getDefaultTypes(), equalTo(DEFAULT_TYPES));
    }

    @Test
    void testDefaultType() {
        Arrays.asList(NotificationType.values())
                .stream()
                .filter(type -> type != ALL)
                .forEach(type -> assertThat(NotificationType.isDefault(type), is(DEFAULT_TYPES.contains(type))));
    }

    @Test
    void testNonParametricTypes() {
        assertThat(NotificationType.getNonParametricTypes(), equalTo(NON_PARAMETRIC_TYPES));
    }

    @Test
    void testParametricType() {
        Arrays.asList(NotificationType.values())
                .stream()
                .filter(type -> type != ALL)
                .forEach(type -> assertThat(
                        NotificationType.isParametric(type),
                        is(!NON_PARAMETRIC_TYPES.contains(type)))
                );
    }

    @Test
    void testAllNotificationType() {
        assertFalse(NotificationType.getConfirmableTypes().contains(ALL));
        assertFalse(NotificationType.getDefaultTypes().contains(ALL));
        assertFalse(NotificationType.isDefault(ALL));
        assertFalse(NotificationType.getNonParametricTypes().contains(ALL));
        assertFalse(NotificationType.isParametric(ALL));
    }
}
