package ru.yandex.direct.intapi.mobilecontent.utils;

import java.util.List;

import javax.annotation.Nullable;

import ru.yandex.direct.intapi.utils.ColumnInfo;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static java.util.Arrays.asList;

public class ApiMobileContentYTRecord {
    static final List<ColumnInfo> YT_COLUMNS = asList(
            new ColumnInfo("app_id", "string", true),
            new ColumnInfo("lang", "string", true),
            new ColumnInfo("bundle", "string", false),
            new ColumnInfo("name", "string", false),
            new ColumnInfo("os_version", "string", false),
            new ColumnInfo("adult", "string", false),
            new ColumnInfo("reviews", "string", false),
            new ColumnInfo("genres", "string", false),
            new ColumnInfo("icon", "string", false),
            new ColumnInfo("date_release_ts", "string", false),
            new ColumnInfo("rating_count", "int64", false),
            new ColumnInfo("currency", "string", false),
            new ColumnInfo("publisher", "string", false),
            new ColumnInfo("price", "double", false),
            new ColumnInfo("icon_ex", "string", false),
            new ColumnInfo("website", "string", false),
            new ColumnInfo("screens", "string", false),
            new ColumnInfo("rating", "double", false),
            new ColumnInfo("release_date", "string", false),
            new ColumnInfo("rating_value", "double", false),
            new ColumnInfo("rating_votes", "double", false)
    );

    private String appId;
    private String lang;
    private String bundle;
    private String name;
    private String osVersion;
    private String adult;
    private String reviews;
    private String genres;
    private String icon;
    private String dateReleaseTs;
    private Long ratingCount;
    private String currency;
    private String publisher;
    private Double price;
    private String iconEx;
    private String website;
    private String screens;
    private Double rating;
    private String releaseDate;
    private Double ratingValue;
    private Double ratingVotes;

    YTreeMapNode buildMapNode() {
        YTreeBuilder builder = YTree.mapBuilder();
        setColumn(builder, "app_id", appId);
        setColumn(builder, "lang", lang);
        setColumn(builder, "bundle", bundle);
        setColumn(builder, "name", name);
        setColumn(builder, "os_version", osVersion);
        setColumn(builder, "adult", adult);
        setColumn(builder, "reviews", reviews);
        setColumn(builder, "genres", genres);
        setColumn(builder, "icon", icon);
        setColumn(builder, "date_release_ts", dateReleaseTs);
        setColumn(builder, "rating_count", ratingCount);
        setColumn(builder, "currency", currency);
        setColumn(builder, "publisher", publisher);
        setColumn(builder, "price", price);
        setColumn(builder, "icon_ex", iconEx);
        setColumn(builder, "website", website);
        setColumn(builder, "screens", screens);
        setColumn(builder, "rating", rating);
        setColumn(builder, "release_date", releaseDate);
        setColumn(builder, "rating_value", ratingValue);
        setColumn(builder, "rating_votes", ratingVotes);
        return builder.buildMap();
    }

    private <T> void setColumn(YTreeBuilder builder, String field, @Nullable T value) {
        if (value == null) {
            builder.key(field).entity();
        } else {
            builder.key(field).value(value);
        }
    }

    public ApiMobileContentYTRecord withAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public ApiMobileContentYTRecord withLang(String lang) {
        this.lang = lang;
        return this;
    }

    public ApiMobileContentYTRecord withBundle(String bundle) {
        this.bundle = bundle;
        return this;
    }

    public ApiMobileContentYTRecord withName(String name) {
        this.name = name;
        return this;
    }

    public ApiMobileContentYTRecord withIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public ApiMobileContentYTRecord withRatingCount(Long ratingCount) {
        this.ratingCount = ratingCount;
        return this;
    }

    public ApiMobileContentYTRecord withCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public ApiMobileContentYTRecord withPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    public ApiMobileContentYTRecord withWebsite(String website) {
        this.website = website;
        return this;
    }
}
