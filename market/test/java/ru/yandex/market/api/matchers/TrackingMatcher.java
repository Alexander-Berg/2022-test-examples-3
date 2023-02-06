package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import ru.yandex.market.api.user.order.checkout.Checkpoint;
import ru.yandex.market.api.user.order.checkout.Tracking;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.collectionToStr;
import static ru.yandex.market.api.ApiMatchers.map;

public class TrackingMatcher {
    public static Matcher<Tracking> tracking(Matcher<Tracking> ... matchers) {
        return allOf(matchers);
    }

    public static Matcher<Tracking> id(long id) {
        return map(
            Tracking::getId,
            "'id'",
            is(id),
            TrackingMatcher::toStr
        );
    }

    public static Matcher<Tracking> code(String code) {
        return map(
            Tracking::getCode,
            "'code'",
            is(code),
            TrackingMatcher::toStr
        );
    }

    public static Matcher<Tracking> checkpoints(Matcher<Iterable<? extends Checkpoint>> ... checkpoints) {
        return map(
            Tracking::getCheckpoints,
            "'checkpoints'",
            allOf(checkpoints),
            TrackingMatcher::toStr
        );
    }

    private static String toStr(Tracking tracking) {
        if (null == tracking) {
            return "null";
        }
        return MoreObjects.toStringHelper(Tracking.class)
            .add("id", tracking.getId())
            .add("code", tracking.getCode())
            .add(
                "checkpoints",
                collectionToStr(tracking.getCheckpoints(), TrackingMatcher::toStr)
            )
            .toString();
    }

    public static Matcher<Checkpoint> checkpoint(Matcher<Checkpoint> ... matchers) {
        return allOf(matchers);
    }

    public static Matcher<Checkpoint> checkpointId(long id) {
        return map(
            Checkpoint::getId,
            "'id'",
            is(id),
            TrackingMatcher::toStr
        );
    }

    public static Matcher<Checkpoint> country(String country) {
        return map(
            Checkpoint::getCountry,
            "'country'",
            is(country),
            TrackingMatcher::toStr
        );
    }


    public static Matcher<Checkpoint> location(String location) {
        return map(
            Checkpoint::getLocation,
            "'location'",
            is(location),
            TrackingMatcher::toStr
        );
    }


    public static Matcher<Checkpoint> message(String message) {
        return map(
            Checkpoint::getMessage,
            "'message'",
            is(message),
            TrackingMatcher::toStr
        );
    }


    public static Matcher<Checkpoint> status(Checkpoint.Status status) {
        return map(
            Checkpoint::getStatus,
            "'status'",
            is(status),
            TrackingMatcher::toStr
        );
    }


    public static Matcher<Checkpoint> deliveryStatus(int deliveryStatus) {
        return map(
            Checkpoint::getDeliveryStatus,
            "'deliveryStatus'",
            is(deliveryStatus),
            TrackingMatcher::toStr
        );
    }

    public static Matcher<Checkpoint> time(long time) {
        return map(
            Checkpoint::getTime,
            "'time'",
            is(time),
            TrackingMatcher::toStr
        );
    }

    private static String toStr(Checkpoint checkpoint) {
        if (null == checkpoint) {
            return "null";
        }
        return MoreObjects.toStringHelper(Checkpoint.class)
            .add("id", checkpoint.getId())
            .add("country", checkpoint.getCountry())
            .add("location", checkpoint.getLocation())
            .add("message", checkpoint.getMessage())
            .add("status", checkpoint.getStatus())
            .add("deliveryStatus", checkpoint.getDeliveryStatus())
            .add("time", checkpoint.getTime())
            .toString();
    }
}
