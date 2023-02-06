package ru.yandex.market.tsum.pipelines.ott.resources;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class OttReleaseVersionPredicatesTest {
    private final List<String> versions;
    private final String currentVersion;
    private final String previousVersion;
    private final String releaseInfoVersion;

    public OttReleaseVersionPredicatesTest(
        List<String> versions,
        String currentVersion,
        String previousVersion,
        String releaseInfoVersion
    ) {
        this.versions = versions;
        this.currentVersion = currentVersion;
        this.previousVersion = previousVersion;
        this.releaseInfoVersion = releaseInfoVersion;
    }

    @Parameterized.Parameters()
    public static Collection<Object[]> parameters() {
        return List.of(
            // new release
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342"
                ),
                "release_20200610_OTT-18342",
                "release_20200610_OTT-17342",
                "release_20200610_OTT-17342"
            },
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342",
                    "release_20200610_OTT-17342_hf1"
                ),
                "release_20200610_OTT-18342",
                "release_20200610_OTT-17342",
                "release_20200610_OTT-17342_hf1"
            },
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342",
                    "release_20200610_OTT-17342_rn1"
                ),
                "release_20200610_OTT-18342",
                "release_20200610_OTT-17342",
                "release_20200610_OTT-17342_rn1"
            },
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342",
                    "release_20200610_OTT-17342_rn1",
                    "release_20200610_OTT-17342_rn1_hf1"
                ),
                "release_20200610_OTT-18342",
                "release_20200610_OTT-17342",
                "release_20200610_OTT-17342_rn1_hf1"
            },
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342",
                    "release_20200610_OTT-17342_rn1",
                    "release_20200610_OTT-17342_rn1_hf1",
                    "release_20200610_OTT-17342_rn1_hf1_rn1"
                ),
                "release_20200610_OTT-18342",
                "release_20200610_OTT-17342",
                "release_20200610_OTT-17342_rn1_hf1_rn1"
            },
            // restart release
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342",
                    "release_20200610_OTT-18342"
                ),
                "release_20200610_OTT-18342_rn1",
                "release_20200610_OTT-17342",
                "release_20200610_OTT-17342"
            },
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342",
                    "release_20200610_OTT-18342",
                    "release_20200610_OTT-18342_rn1"
                ),
                "release_20200610_OTT-18342_rn2",
                "release_20200610_OTT-17342",
                "release_20200610_OTT-17342"
            },
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342",
                    "release_20200610_OTT-17342_hf1",
                    "release_20200610_OTT-18342"
                ),
                "release_20200610_OTT-18342_rn1",
                "release_20200610_OTT-17342",
                "release_20200610_OTT-17342_hf1"
            },
            // new hotfix
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342",
                    "release_20200610_OTT-18342"
                ),
                "release_20200610_OTT-17342_hf1",
                "release_20200610_OTT-17342",
                "release_20200610_OTT-17342"
            },
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342_rn1",
                    "release_20200610_OTT-18342"
                ),
                "release_20200610_OTT-17342_rn1_hf1",
                "release_20200610_OTT-17342_rn1",
                "release_20200610_OTT-17342_rn1"
            },
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342_rn1",
                    "release_20200610_OTT-17342_rn1_hf1",
                    "release_20200610_OTT-18342"
                ),
                "release_20200610_OTT-17342_rn1_hf2",
                "release_20200610_OTT-17342_rn1_hf1",
                "release_20200610_OTT-17342_rn1_hf1"
            },
            // restart hotfix
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342_rn1",
                    "release_20200610_OTT-17342_rn2",
                    "release_20200610_OTT-17342_rn2_hf1",
                    "release_20200610_OTT-18342"
                ),
                "release_20200610_OTT-17342_rn2_hf1_rn1",
                "release_20200610_OTT-17342_rn2",
                "release_20200610_OTT-17342_rn2"
            },
            new Object[]{
                List.of(
                    "release_20200610_OTT-16342",
                    "release_20200610_OTT-17342_rn1",
                    "release_20200610_OTT-17342_rn2",
                    "release_20200610_OTT-17342_rn2_hf1",
                    "release_20200610_OTT-17342_rn2_hf1_rn1",
                    "release_20200610_OTT-17342_rn2_hf1_rn2",
                    "release_20200610_OTT-18342"
                ),
                "release_20200610_OTT-17342_rn2_hf1_rn3",
                "release_20200610_OTT-17342_rn2",
                "release_20200610_OTT-17342_rn2"
            }
        );
    }

    @Test
    public void previousVersionPredicateTest() {
        OttReleaseVersion current = OttReleaseVersion.parse(currentVersion);
        String previous = findVersion(versions, current.previousVersionPredicate());
        assertEquals(previousVersion, previous);
    }

    @Test
    public void releaseInfoPredicateTest() {
        OttReleaseVersion current = OttReleaseVersion.parse(currentVersion);
        String releaseInfo = findVersion(versions, current.releaseInfoPredicate());
        assertEquals(releaseInfoVersion, releaseInfo);
    }

    private String findVersion(List<String> versions, Predicate<OttReleaseVersion> predicate) {
        return versions.stream()
            .map(OttReleaseVersion::parse)
            .sorted(Comparator.reverseOrder())
            .filter(predicate)
            .findFirst()
            .map(OttReleaseVersion::toString)
            .orElseThrow(() -> new IllegalStateException("Can not find previous release"));
    }
}
