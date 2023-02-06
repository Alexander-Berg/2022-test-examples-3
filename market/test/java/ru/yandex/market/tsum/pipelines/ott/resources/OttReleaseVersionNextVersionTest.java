package ru.yandex.market.tsum.pipelines.ott.resources;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;


@RunWith(Parameterized.class)
public class OttReleaseVersionNextVersionTest {
    private final String nextHotfixVersion;
    private final String nextRestartVersion;
    private final String originalVersion;
    private final List<String> allRestarts;
    private final OttReleaseVersion releaseVersion;

    public OttReleaseVersionNextVersionTest(
        String currentVersion,
        String nextHotfixVersion,
        String nextRestartVersion,
        String originalVersion,
        List<String> allRestarts
    ) {
        this.releaseVersion = OttReleaseVersion.parse(currentVersion);
        this.nextHotfixVersion = nextHotfixVersion;
        this.nextRestartVersion = nextRestartVersion;
        this.originalVersion = originalVersion;
        this.allRestarts = allRestarts;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return List.of(
            new Object[]{
                "release_20200610_OTT-17342",
                "release_20200610_OTT-17342_hf1",
                "release_20200610_OTT-17342_rn1",
                "release_20200610_OTT-17342",
                List.of(
                    "release_20200610_OTT-17342"
                )
            },
            new Object[]{
                "release_20200610_OTT-17342_rn1",
                "release_20200610_OTT-17342_rn1_hf1",
                "release_20200610_OTT-17342_rn2",
                "release_20200610_OTT-17342",
                List.of(
                    "release_20200610_OTT-17342",
                    "release_20200610_OTT-17342_rn1"
                )
            },
            new Object[]{
                "release_20200610_OTT-17342_hf1",
                "release_20200610_OTT-17342_hf2",
                "release_20200610_OTT-17342_hf1_rn1",
                "release_20200610_OTT-17342_hf1",
                List.of(
                    "release_20200610_OTT-17342_hf1"
                )
            },
            new Object[]{
                "release_20200610_OTT-17342_hf2_rn2",
                "release_20200610_OTT-17342_hf3",
                "release_20200610_OTT-17342_hf2_rn3",
                "release_20200610_OTT-17342_hf2",
                List.of(
                    "release_20200610_OTT-17342_hf2",
                    "release_20200610_OTT-17342_hf2_rn1",
                    "release_20200610_OTT-17342_hf2_rn2"
                )
            },
            new Object[]{
                "release_20200610_OTT-17342_rn3_hf2",
                "release_20200610_OTT-17342_rn3_hf3",
                "release_20200610_OTT-17342_rn3_hf2_rn1",
                "release_20200610_OTT-17342_rn3_hf2",
                List.of(
                    "release_20200610_OTT-17342_rn3_hf2"
                )
            },
            new Object[]{
                "release_20200610_OTT-17342_rn3_hf3_rn2",
                "release_20200610_OTT-17342_rn3_hf4",
                "release_20200610_OTT-17342_rn3_hf3_rn3",
                "release_20200610_OTT-17342_rn3_hf3",
                List.of(
                    "release_20200610_OTT-17342_rn3_hf3",
                    "release_20200610_OTT-17342_rn3_hf3_rn1",
                    "release_20200610_OTT-17342_rn3_hf3_rn2"
                )
            }
        );
    }

    @Test
    public void nextHotfixVersionTest() {
        assertEquals(nextHotfixVersion, releaseVersion.newHotfixVersion().toString());
    }

    @Test
    public void nextRestartVersion() {
        assertEquals(nextRestartVersion, releaseVersion.newRestartVersion().toString());
    }

    @Test
    public void originalVersionTest() {
        assertEquals(originalVersion, releaseVersion.originalVersion().toString());
    }

    @Test
    public void allRestartsTest() {
        List<String> restarts = releaseVersion.allRestarts()
            .stream()
            .map(OttReleaseVersion::toString)
            .collect(Collectors.toList());
        assertThat(restarts).containsExactly(allRestarts.toArray(new String[]{}));
    }
}
