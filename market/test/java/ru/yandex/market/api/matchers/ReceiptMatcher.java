package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.checkout.Receipt;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class ReceiptMatcher {
    public static Matcher<Receipt> receipt(Matcher<Receipt> ... matchers) {
        return allOf(matchers);
    }

    public static Matcher<Receipt> id(long id) {
        return ApiMatchers.map(
            Receipt::getId,
            "'id'",
            is(id),
            ReceiptMatcher::toStr
        );
    }

    public static Matcher<Receipt> type(Receipt.Type type) {
        return ApiMatchers.map(
            Receipt::getType,
            "'type'",
            is(type),
            ReceiptMatcher::toStr
        );
    }

    public static Matcher<Receipt> status(Receipt.Status status) {
        return ApiMatchers.map(
            Receipt::getStatus,
            "'status'",
            is(status),
            ReceiptMatcher::toStr
        );
    }

    public static Matcher<Receipt> creationDate(ZonedDateTime creationDate) {
        return ApiMatchers.map(
            Receipt::getCreationDate,
            "'creationDate'",
            is(creationDate),
            ReceiptMatcher::toStr
        );
    }

    private static String toStr(Receipt receipt) {
        if (null == receipt) {
            return "null";
        }
        return MoreObjects.toStringHelper(Receipt.class)
            .add("id", receipt.getId())
            .add("type", receipt.getType())
            .add("status", receipt.getStatus())
            .add("creationDate", receipt.getCreationDate().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .toString();
    }

}
