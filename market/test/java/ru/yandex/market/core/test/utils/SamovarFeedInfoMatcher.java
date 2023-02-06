package ru.yandex.market.core.test.utils;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeedInfo;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarUtils;

public class SamovarFeedInfoMatcher extends BaseMatcher<SamovarFeedInfo> {

    private final SamovarFeedInfo expected;

    private SamovarFeedInfoMatcher(SamovarFeedInfo expected) {
        this.expected = expected;
    }

    public static SamovarFeedInfoMatcher from(SamovarFeedInfo expected) {
        return new SamovarFeedInfoMatcher(expected);
    }

    @Override
    public boolean matches(Object item) {
        var actual = (SamovarFeedInfo) item;

        return Objects.equals(expected.getPartnerId(), actual.getPartnerId())
                && Objects.equals(expected.getCampaignType(), actual.getCampaignType())
                && Objects.equals(expected.getFeedId(), actual.getFeedId())
                && Objects.equals(expected.getResource(), actual.getResource())
                && Objects.equals(expected.getTimeout(), actual.getTimeout())
                && Objects.equals(expected.getPeriod(), actual.getPeriod())
                && Objects.equals(expected.getForcedReparseIntervalMinutes(), actual.getForcedReparseIntervalMinutes());
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        description.appendText("was ").appendText(toString((SamovarFeedInfo) item));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(toString(expected));
    }

    static String toString(SamovarFeedInfo item) {
        return new ToStringBuilder(item, ToStringStyle.JSON_STYLE)
                .append("url", item.getResource().url())
                .append("partnerId", item.getPartnerId())
                .append("campaignType", item.getCampaignType())
                .append("feedId", item.getFeedId())
                .append("auth", item.getResource().credentials()
                        .map(SamovarUtils::buildAuthString).orElse(null)
                )
                .append("timeout", item.getTimeout())
                .append("period", item.getPeriod())
                .append("forcedPeriodMinutes", item.getForcedReparseIntervalMinutes())
//                .append("context", JsonFormat.printToString(context))
                .toString();
    }
}
