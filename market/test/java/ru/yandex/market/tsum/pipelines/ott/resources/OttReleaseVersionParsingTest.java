package ru.yandex.market.tsum.pipelines.ott.resources;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static junit.framework.Assert.assertEquals;

@RunWith(Parameterized.class)
public class OttReleaseVersionParsingTest {
    private final String version;
    private final String releaseTicket;
    private final LocalDate releaseDate;
    private final int hotfixNumber;
    private final int releaseRestartNumber;
    private final int hotfixRestartNumber;

    public OttReleaseVersionParsingTest(
        String version,
        String releaseTicket,
        LocalDate releaseDate,
        int hotfixNumber,
        int releaseRestartNumber,
        int hotfixRestartNumber
    ) {
        this.version = version;
        this.releaseTicket = releaseTicket;
        this.releaseDate = releaseDate;
        this.hotfixNumber = hotfixNumber;
        this.releaseRestartNumber = releaseRestartNumber;
        this.hotfixRestartNumber = hotfixRestartNumber;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return List.of(
            new Object[]{"release_20200610_OTT-17342", "OTT-17342", LocalDate.of(2020, 6, 10), 0, 0, 0},
            new Object[]{"release_20200611_OTT-17343_rn1", "OTT-17343", LocalDate.of(2020, 6, 11), 0, 1, 0},
            new Object[]{"release_20200611_OTT-17343_hf1", "OTT-17343", LocalDate.of(2020, 6, 11), 1, 0, 0},
            new Object[]{"release_20200611_OTT-17343_rn2_hf3", "OTT-17343", LocalDate.of(2020, 6, 11), 3, 2, 0},
            new Object[]{"release_20200611_OTT-17343_hf4_rn5", "OTT-17343", LocalDate.of(2020, 6, 11), 4, 0, 5},
            new Object[]{"release_20200611_OTT-17343_rn2_hf3_rn4", "OTT-17343", LocalDate.of(2020, 6, 11), 3, 2, 4}
        );
    }

    @Test
    public void versionParsingTest() {
        OttReleaseVersion releaseVersion = OttReleaseVersion.parse(version);
        assertEquals(releaseTicket, releaseVersion.getReleaseTicket());
        assertEquals(releaseDate, releaseVersion.getReleaseDate());
        assertEquals(hotfixNumber, releaseVersion.getHotfixNumber());
        assertEquals(releaseRestartNumber, releaseVersion.getReleaseRestartNumber());
        assertEquals(hotfixRestartNumber, releaseVersion.getHotfixRestartNumber());
        assertEquals(version, releaseVersion.toString());
    }
}
