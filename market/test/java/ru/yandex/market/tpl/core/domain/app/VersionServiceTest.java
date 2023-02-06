package ru.yandex.market.tpl.core.domain.app;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.tpl.api.model.app.VersionDto;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.tpl.api.model.user.UserRole.ADMIN;
import static ru.yandex.market.tpl.api.model.user.UserRole.COURIER;

/**
 * @author aostrikov
 */
@RequiredArgsConstructor
class VersionServiceTest extends TplAbstractTest {

    private final VersionService service;
    private final TestUserHelper userHelper;
    private final Clock clock;

    private Instant now;
    private User user;

    @BeforeEach
    void setUp() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        now = Instant.now(clock);
    }

    @Test
    void shouldCreateTwoVersions() {
        service.addVersion(version("1.0"));
        service.addVersion(version("1.1"));

        assertThat(service.getAllVersions(null).size()).isEqualTo(2);
        assertThat(service.getDistribution(COURIER, false, now).getVersions().keySet())
                .isEqualTo(Set.of(VersionService.UNDEFINED_VERSION));
    }

    @Test
    void couldNotReturnLatestVersionWithoutPublishing() {
        service.addVersion(version("1.1"));

        assertThatThrownBy(() -> service.getLatestVersion(user.getId(), now))
                .isInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void shouldFailOnIncorrectPercent() {
        assertThatThrownBy(() -> service.addVersion(versionCandidate("1.1", 0, now)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.addVersion(versionCandidate("1.1", -10, now)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.addVersion(versionCandidate("1.1", 45, now)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldFailOnNonEmptyPercentForNonCandidate() {
        VersionDto version = version("1.1");
        version.setRolloutPercent(20);

        assertThatThrownBy(() -> service.addVersion(version))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotCreateTwoSimilarVersions() {
        service.addVersion(version("1.1"));

        assertThatThrownBy(() -> service.addVersion(version("1.1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPublishNewVersion() {
        VersionDto version = service.addVersion(version("1.0"));
        service.publishLatestVersion(version.getId());

        assertThat(service.getLatestVersion(user.getId(), now).getId()).isEqualTo(version.getId());
        assertThat(service.getDistribution(COURIER, false, now).getVersions().keySet()).isEqualTo(Set.of("1.0"));
    }

    @Test
    void shouldReplaceLatestVersion() {
        VersionDto version = service.addVersion(version("1.0"));
        service.publishLatestVersion(version.getId());

        VersionDto version2 = service.addVersion(version("1.2"));
        service.publishLatestVersion(version2.getId());

        assertThat(service.getLatestVersion(user.getId(), now).getId()).isEqualTo(version2.getId());
        assertThat(service.getDistribution(COURIER, false, now).getVersions().size()).isEqualTo(1);
    }

    @Test
    void shouldCreateVersionCandidate() {
        VersionDto version = service.addVersion(versionCandidate("1.0", 100));

        assertThat(version.isCandidate()).isTrue();
        assertThat(version.isLatest()).isFalse();

        assertThat(service.getAllVersions(true).size()).isEqualTo(1);
        assertThat(service.getDistribution(COURIER, false, now).getVersions().keySet())
                .isEqualTo(Set.of("1.0"));
    }

    @Test
    void shouldPublishReleaseCandidate() {
        VersionDto version = service.addVersion(versionCandidate("1.1", 50));
        version = service.publishLatestVersion(version.getId());

        assertThat(version.isCandidate()).isFalse();
        assertThat(version.isLatest()).isTrue();

        assertThat(service.getAllVersions(true).size()).isEqualTo(1);
        assertThat(service.getDistribution(COURIER, false, now).getVersions().keySet()).isEqualTo(Set.of("1.1"));
    }

    @Test
    void shouldHaveLatestReleaseCandidateAndNotPublishedVersions() {
        service.addVersion(version("1.0"));
        service.addVersion(versionCandidate("1.3", 50));
        service.publishLatestVersion(service.addVersion(version("1.1")).getId());

        assertThat(service.getAllVersions(null).size()).isEqualTo(3);
        assertThat(service.getAllVersions(true).size()).isEqualTo(2);
        assertThat(service.getAllVersions(false).size()).isEqualTo(1);
    }

    @Test
    void shouldHaveVersionDistributionViaRolloutPercent() {
        service.publishLatestVersion(service.addVersion(version("1.0")).getId());
        service.addVersion(versionCandidate("1.1", 20));

        for (int i = 0; i < 9; i++) {
            userHelper.findOrCreateUser(i, LocalDate.now(clock));
        }

        assertThat(service.getDistribution(COURIER, true, now).getVersions().keySet()).isEqualTo(emptySet());
        assertThat(service.getDistribution(ADMIN, false, now).getVersions().keySet()).isEqualTo(emptySet());
        assertThat(service.getDistribution(ADMIN, true, now).getVersions().keySet()).isEqualTo(emptySet());

        assertThat(service.getDistribution(COURIER, false, now).getVersions().size()).isEqualTo(2);
        assertThat(service.getDistribution(COURIER, false, now).getVersions().get("1.1").size()).isLessThan(5);
        assertThat(service.getDistribution(COURIER, false, now).getVersions().get("1.0").size()).isGreaterThan(5);
    }

    @Test
    void shouldHaveAutoRolloutVersion() {
        service.publishLatestVersion(service.addVersion(version("1.0")).getId());
        service.addVersion(versionCandidate("1.1", null, now.minus(2, DAYS)));

        assertThat(service.getLatestVersion(user.getId(), now).getVersion()).isEqualTo("1.1");
    }

    @Test
    void shouldHaveOldVersionBeforeAutoRollout() {
        service.publishLatestVersion(service.addVersion(version("1.0")).getId());
        service.addVersion(versionCandidate("1.1", null, now.plus(1, DAYS)));

        assertThat(service.getLatestVersion(user.getId(), now).getVersion()).isEqualTo("1.0");
    }

    @Test
    void shouldAddVersionWithHuaweiUrl() {
        var huaweiUrl = "https://yadi.sk/d/123456";
        VersionDto version = service.addVersion(versionCandidate("1.1", "https://yadi.sk/d/1", huaweiUrl));

        assertThat(version.getHuaweiUrl()).isEqualTo(huaweiUrl);

    }

    @Test
    void shouldPublishVersionWithHuaweiUrl() {
        var huaweiUrl = "https://yadi.sk/d/123456";

        VersionDto version1 = service.addVersion(versionCandidate("1.1", "https://yadi.sk/d/1", huaweiUrl));
        service.publishLatestVersion(version1.getId());
        VersionDto version2 = service.getLatestVersion(user.getId(), now);

        assertThat(version2.getVersion()).isEqualTo("1.1");
        assertThat(version2.getId()).isEqualTo(version1.getId());
        assertThat(version2.getHuaweiUrl()).isEqualTo(huaweiUrl);
    }

    @Test
    void shouldNotReplaceUrlWithoutDeviceType() {
        var mainUrl = "https://yadi.sk/d/123456";
        var huaweiUrl = "https://yadi.sk/d/654321";

        VersionDto version = service.addVersion(versionCandidate("1.1", mainUrl, huaweiUrl));
        service.publishLatestVersion(version.getId());
        VersionDto version1 = service.getLatestVersionForDevice(user.getId(), now, null);
        VersionDto version2 = service.getLatestVersionForDevice(user.getId(), now, "");

        assertThat(version1.getUrl()).isEqualTo(mainUrl);
        assertThat(version2.getUrl()).isEqualTo(mainUrl);
    }

    @Test
    void shouldNotReplaceUrlWithNotHuaweiDeviceType() {
        var mainUrl = "https://yadi.sk/d/123456";
        var huaweiUrl = "https://yadi.sk/d/654321";

        VersionDto version = service.addVersion(versionCandidate("1.1", mainUrl, huaweiUrl));
        service.publishLatestVersion(version.getId());
        version = service.getLatestVersionForDevice(user.getId(), now, "GOOGLE");

        assertThat(version.getUrl()).isEqualTo(mainUrl);
    }

    @Test
    void shouldNotReplaceUrlWithHuaweiDeviceTypeAndEmptyHuaweiUrl() {
        VersionDto version1 = service.addVersion(versionCandidate("1.1", 50));
        version1 = service.publishLatestVersion(version1.getId());
        VersionDto version2 = service.getLatestVersionForDevice(user.getId(), now, "HUAWEI");

        assertThat(version1.getUrl()).isEqualTo(version2.getUrl());
    }

    @Test
    void shouldReplaceUrlByHuaweiUrl() {
        var mainUrl = "https://yadi.sk/d/123456";
        var huaweiUrl = "https://yadi.sk/d/654321";

        VersionDto version = service.addVersion(versionCandidate("1.1", mainUrl, huaweiUrl));
        service.publishLatestVersion(version.getId());
        version = service.getLatestVersionForDevice(user.getId(), now, "HUAWEI");

        assertThat(version.getUrl()).isEqualTo(huaweiUrl);
    }

    @DisplayName("Версия приложения из 2х секций xx.yy")
    @Test
    void shouldPublishVersionWithDoubleSection() {
        String version = "2.51";
        VersionDto versionDto = service.addVersion(version(version));

        service.publishLatestVersion(versionDto.getId());

        assertThat(service.getAllVersions(null).size()).isEqualTo(1);
        assertThat(service.getDistribution(COURIER, false, now).getVersions().keySet())
                .isEqualTo(Set.of(version));
        assertThat(version).matches(VersionDto.APP_VERSION_REGEXP);
    }

    @DisplayName("Версия приложения из 3х секций xx.yy.zz")
    @Test
    void shouldPublishVersionWithTripleSection() {
        String version = "2.51.01";
        VersionDto versionDto = service.addVersion(version(version));

        service.publishLatestVersion(versionDto.getId());

        assertThat(service.getAllVersions(null).size()).isEqualTo(1);
        assertThat(service.getDistribution(COURIER, false, now).getVersions().keySet())
                .isEqualTo(Set.of(version));
        assertThat(version).matches(VersionDto.APP_VERSION_REGEXP);
    }

    @Test
    @Disabled//MARKETTPL-8069
    void shouldHaveAutoRolloutVersionWithRolloutPercents() {
        service.publishLatestVersion(service.addVersion(version("1.0")).getId());
        VersionDto versionWithRolloutPercents = versionCandidateWithRolloutPercents(
                "1.1",
                List.of(30L, 50L, 80L),
                now.minus(1, DAYS)
        );
        service.addVersion(versionWithRolloutPercents);

        assertThat(service.getLatestVersion(user.getId(), now).getVersion()).isEqualTo("1.1");
    }

    private VersionDto version(String version) {
        return VersionDto.builder()
                .version(version)
                .description("boo")
                .url("https://yadi.sk/d/" + new Random().nextInt())
                .huaweiUrl("https://yadi.sk/d/" + new Random().nextInt())
                .updateAfter(now)
                .updateRequired(true)
                .latest(false)
                .candidate(false)
                .build();
    }

    private VersionDto versionCandidateWithRolloutPercents(
            String version,
            List<Long> rolloutPercents,
            Instant minus
    ) {
        return VersionDto.builder()
                .version(version)
                .description("boo")
                .url("https://yadi.sk/d/" + new Random().nextInt())
                .updateAfter(minus)
                .updateRequired(true)
                .latest(false)
                .candidate(true)
                .rolloutPercents(rolloutPercents)
                .build();
    }

    private VersionDto versionCandidate(String version, Integer percent) {
        return versionCandidate(version, percent, now);
    }

    private VersionDto versionCandidate(String version, Integer percent, Instant updateAfter) {
        return VersionDto.builder()
                .version(version)
                .description("boo")
                .url("https://yadi.sk/d/" + new Random().nextInt())
                .updateAfter(updateAfter)
                .updateRequired(true)
                .latest(false)
                .candidate(true)
                .rolloutPercent(percent)
                .build();
    }

    private VersionDto versionCandidate(String version, String mainUrl, String huaweiUrl) {
        return VersionDto.builder()
                .version(version)
                .description("boo")
                .url(mainUrl)
                .huaweiUrl(huaweiUrl)
                .updateAfter(now)
                .updateRequired(true)
                .latest(false)
                .candidate(true)
                .rolloutPercent(50)
                .build();
    }

}
