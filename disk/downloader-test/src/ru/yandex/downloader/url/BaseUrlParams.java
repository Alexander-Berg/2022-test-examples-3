package ru.yandex.downloader.url;

import org.joda.time.Duration;
import org.joda.time.Instant;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.PassportUid;

/**
 * @author akirakozov
 */
public class BaseUrlParams {
    public Instant timestamp = Instant.now().plus(Duration.standardHours(2));
    public TargetReference targetRef;
    public Option<PassportUid> uid = Option.none();
    public String fileName = "result";
    public String contentType = "application/octet-stream";
    public Disposition disposition = Disposition.INLINE;
    public Option<String> hash = Option.none();
    public Option<Boolean> limit = Option.none();
    public Option<Boolean> autoLogin = Option.none();
    public Option<String> src = Option.none();

    public ListF<String> getParamsForTokenAsListOfStrings() {
        ListF<String> res = Cf.arrayList();
        res.add(getHexTimestamp());
        res.add(targetRef.getValue());
        res.add(getUidAsString());
        res.add(fileName);
        res.add(contentType);
        res.add(disposition.name().toLowerCase());
        res.add(hash.getOrElse(""));
        res.add(getLimitAsString());
        return res;
    }

    public String getUidAsString() {
        return uid.map(PassportUid::getUid).getOrElse(0L).toString();
    }

    public String getHexTimestamp() {
        return Long.toHexString(timestamp.getMillis() / 1000);
    }

    public String getLimitAsString() {
        return getBooleanAsString(limit);
    }

    public String getAutoLoginAsString() {
        return getBooleanAsString(autoLogin);
    }

    public static String getBooleanAsString(Option<Boolean> field) {
        return field.map(v -> v ? "1" : "0").getOrElse("");
    }
}
