package ru.yandex.market.crm.campaign.test.utils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.yt.paths.CrmYtTables;
import ru.yandex.market.crm.yt.client.YtClient;

/**
 * @author apershukov
 */
@Component
public class CryptaProfilesTestHelper {

    public static class ProfileBuilder {

        private final YTreeBuilder yTreeBuilder = YTree.mapBuilder();

        ProfileBuilder yuid(long yuid) {
            yTreeBuilder.key("yandexuid").value(yuid);
            return this;
        }

        public ProfileBuilder exactSocdem(YTreeNode exactSocdem) {
            yTreeBuilder.key("exact_socdem").value(exactSocdem);
            return this;
        }

        public ProfileBuilder heuristicCommon(List<Integer> heuristicCommon) {
            yTreeBuilder.key("heuristic_common").value(heuristicCommon);
            return this;
        }

        public ProfileBuilder audienceSegments(List<Integer> audienceSegments) {
            yTreeBuilder.key("audience_segments").value(audienceSegments);
            return this;
        }

        public ProfileBuilder heuristicPrivate(List<Integer> heuristicPrivate) {
            yTreeBuilder.key("heuristic_private").value(heuristicPrivate);
            return this;
        }

        public ProfileBuilder lalInternal(List<Integer> lalInternal) {
            yTreeBuilder.key("lal_internal").value(
                toProbabiltyMap(lalInternal)
            );

            return this;
        }

        public ProfileBuilder lalCommon(List<Integer> lalCommon) {
            yTreeBuilder.key("lal_common").value(
                toProbabiltyMap(lalCommon)
            );

            return this;
        }

        public ProfileBuilder longtermInterests(Integer... longtermInterests) {
            yTreeBuilder.key("longterm_interests").value(Arrays.asList(longtermInterests));
            return this;
        }

        public ProfileBuilder shorttermInterests(Integer... shorttermInterests) {
            yTreeBuilder.key("shortterm_interests").value(
                Stream.of(shorttermInterests)
                    .collect(Collectors.toMap(
                        String::valueOf,
                        x -> Instant.now().getEpochSecond()
                    ))
            );
            return this;
        }

        public ProfileBuilder heuristicSegments(Integer... heuristicSegments) {
            yTreeBuilder.key("heuristic_segments").value(
                toStaticMap(heuristicSegments)
            );

            return this;
        }

        public ProfileBuilder marketingSegments(Integer... marketingSegments) {
            yTreeBuilder.key("marketing_segments").value(
                toStaticMap(marketingSegments)
            );
            return this;
        }

        public YTreeMapNode build() {
            return yTreeBuilder.buildMap();
        }
    }

    public static ProfileBuilder profile(long yuid) {
        return new ProfileBuilder().yuid(yuid);
    }

    public static ProfileBuilder profile(String yuid) {
        return profile(Long.parseLong(yuid));
    }

    public static YTreeMapNode exactSocdem(String ageSegment,
                                           String income5Segment,
                                           String incomeSegment,
                                           String gender) {
        YTreeBuilder builder = YTree.mapBuilder();

        if (ageSegment != null) {
            builder.key("age_segment").value(ageSegment);
        }

        if (income5Segment != null) {
            builder.key("income_5_segment").value(income5Segment);
        }

        if (incomeSegment != null) {
            builder.key("income_segment").value(incomeSegment);
        }

        if (gender != null) {
            builder.key("gender").value(gender);
        }

        return builder.buildMap();
    }

    private static Map<String, Double> toProbabiltyMap(List<Integer> segments) {
        return segments.stream()
                .collect(Collectors.toMap(
                        String::valueOf,
                        x -> RandomUtils.nextDouble(0, .99)
                ));
    }

    private static Map<String, Integer> toStaticMap(Integer... segments) {
        return Stream.of(segments)
                .collect(Collectors.toMap(
                        String::valueOf,
                        x -> 1
                ));
    }

    private final CrmYtTables ytTables;
    private final YtClient ytClient;
    private final YtSchemaTestHelper ytSchemaTestHelper;

    public CryptaProfilesTestHelper(CrmYtTables ytTables, YtClient ytClient, YtSchemaTestHelper ytSchemaTestHelper) {
        this.ytTables = ytTables;
        this.ytClient = ytClient;
        this.ytSchemaTestHelper = ytSchemaTestHelper;
    }

    public void prepareProfiles(ProfileBuilder... profileBuilders) {
        ytSchemaTestHelper.createTable(ytTables.getCryptaProfiles(), "crypta_profiles.yson");

        List<YTreeMapNode> rows = Stream.of(profileBuilders)
                .map(ProfileBuilder::build)
                .collect(Collectors.toList());

        ytClient.write(ytTables.getCryptaProfiles(), YTableEntryTypes.YSON, rows);
    }
}
