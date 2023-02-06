package ru.yandex.direct.version;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class DirectVersionTest {
    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @After
    public void after() {
        System.clearProperty(DirectVersion.BUILD_VERSION_PROPERTY);
    }

    @Test
    public void parseNoVersionInfo() {
        DirectVersion.Version version = new DirectVersion.Version(-1, "");
        softly.assertThat(version.major).isEqualTo(0);
        softly.assertThat(version.minor).isEqualTo(-1);
        softly.assertThat(version.version).isEqualTo(DirectVersion.UNKNOWN_VERSION);
    }

    @Test
    public void parseTrunkUrl() {
        DirectVersion.Version version = new DirectVersion.Version(
                3036440, "svn+ssh://zomb-sandbox-rw@arcadia.yandex.ru/arc/trunk/arcadia");
        softly.assertThat(version.major).isEqualTo(3036440);
        softly.assertThat(version.minor).isEqualTo(3036440);
        softly.assertThat(version.version).isEqualTo("1.3036440-1");
    }

    @Test
    public void parseReleaseUrl() {
        DirectVersion.Version version = new DirectVersion.Version(
                3036440,
                "svn+ssh://zomb-sandbox-rw@arcadia.yandex.ru/arc/branches/direct/release/java-api5/2884798/arcadia");
        softly.assertThat(version.major).isEqualTo(2884798);
        softly.assertThat(version.minor).isEqualTo(3036440);
        softly.assertThat(version.version).isEqualTo("1.2884798.3036440-1");
    }

    @Test
    public void parseArcVersion() {
        DirectVersion.Version version = new DirectVersion.Version(9601582, "");
        softly.assertThat(version.major).isEqualTo(0);
        softly.assertThat(version.minor).isEqualTo(9601582);
        softly.assertThat(version.version).isEqualTo("unknown");
    }

    @Test
    public void parseCiVersion() {
        System.setProperty(DirectVersion.BUILD_VERSION_PROPERTY, "63");
        DirectVersion.Version version = new DirectVersion.Version(9601582, "");
        softly.assertThat(version.major).isEqualTo(63);
        softly.assertThat(version.minor).isEqualTo(0);
        softly.assertThat(version.version).isEqualTo("63");
    }

    @Test
    public void parseCiBranchVersion() {
        System.setProperty(DirectVersion.BUILD_VERSION_PROPERTY, "63.3");
        DirectVersion.Version version = new DirectVersion.Version(9601582, "");
        softly.assertThat(version.major).isEqualTo(63);
        softly.assertThat(version.minor).isEqualTo(3);
        softly.assertThat(version.version).isEqualTo("63.3");
    }

    @Test
    public void parseCiCachedVersion() {
        System.setProperty(DirectVersion.BUILD_VERSION_PROPERTY, "64");
        DirectVersion.Version version =
                new DirectVersion.Version(9645318, "svn://arcadia.yandex.ru/arc/trunk/arcadia");
        softly.assertThat(version.major).isEqualTo(64);
        softly.assertThat(version.minor).isEqualTo(0);
        softly.assertThat(version.version).isEqualTo("64");
    }
}

