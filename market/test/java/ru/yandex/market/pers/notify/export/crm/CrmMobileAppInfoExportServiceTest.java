package ru.yandex.market.pers.notify.export.crm;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.pers.notify.export.ChangedMobileAppInfoDAO;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author apershukov
 */
public class CrmMobileAppInfoExportServiceTest extends MarketMailerMockedDbTest {

    private static class InfoMatcher extends BaseMatcher<MobileAppInfo> {

        private final MobileAppInfo expected;

        InfoMatcher(MobileAppInfo expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Object o) {
            if (!(o instanceof MobileAppInfo)) {
                return false;
            }
            MobileAppInfo info = (MobileAppInfo) o;

            return Objects.equals(expected.getUuid(), info.getUuid()) &&
                    Objects.equals(expected.getPushToken(), info.getPushToken()) &&
                    Objects.equals(expected.isUnregistered(), info.isUnregistered()) &&
                    Objects.equals(expected.getModificationTime(), info.getModificationTime()) &&
                    Objects.equals(expected.getPlatform(), info.getPlatform()) &&
                    Objects.equals(expected.getAppName(), info.getAppName()) &&
                    Objects.equals(expected.getUid(), info.getUid()) &&
                    Objects.equals(expected.getYandexUid(), info.getYandexUid()) &&
                    Objects.equals(expected.getGeoId(), info.getGeoId()) &&
                    Objects.equals(expected.getMuid(), info.getMuid()) &&
                    Objects.equals(expected.getDisabledBySystem(), info.getDisabledBySystem());
        }

        @Override
        public void describeTo(Description description) {
            description.appendValueList(
                    expected.getUuid(),
                    expected.getPushToken(),
                    String.valueOf(expected.isUnregistered())
            );
        }
    }

    private static Matcher<MobileAppInfo> hasValues(MobileAppInfo info) {
        return new InfoMatcher(info);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ChangedMobileAppInfoDAO changedMobileAppInfoDAO;

    private TestCrmMobileAppInfoTskvWriter writer;
    private CrmMobileAppInfoExportService service;

    @BeforeEach
    public void setUp() throws Exception {
        writer = new TestCrmMobileAppInfoTskvWriter();
        service = new CrmMobileAppInfoExportService(jdbcTemplate, writer, changedMobileAppInfoDAO);
    }

    @Test
    public void testExportAll() {
        List<MobileAppInfo> infos = registerInfo();

        service.exportAll();

        List<MobileAppInfo> items = writer.getItems();
        assertThat(items, hasSize(3));

        assertThat(items.get(0), hasValues(infos.get(0)));
        assertThat(items.get(1), hasValues(infos.get(1)));
        assertThat(items.get(2), hasValues(infos.get(2)));
    }

    @Test
    public void testExportChangedOnly() {
        List<MobileAppInfo> infos = registerInfo();

        changedMobileAppInfoDAO.markAsChanged(infos.get(0).getUuid());
        changedMobileAppInfoDAO.markAsChanged(infos.get(2).getUuid());

        service.exportChanged();

        List<MobileAppInfo> items = writer.getItems();
        assertThat(items, hasSize(2));

        assertThat(items.get(0), hasValues(infos.get(0)));
        assertThat(items.get(1), hasValues(infos.get(2)));

        Long changedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM CRM_CHANGED_MOBILE_APP_INFO",
                Long.class
        );
        assertEquals(Long.valueOf(0), changedCount);
    }

    @Test
    public void testExportWithDisabledBySystemAsNull() {
        MobileAppInfo mobileAppInfo = getMobileAppInfo(1);
        mobileAppInfo.setDisabledBySystem(false);

        jdbcTemplate.update(
                "INSERT INTO MOBILE_APP_INFO (\n" +
                "  UUID, \n" +
                "  PUSH_TOKEN, \n" +
                "  UNREGISTERED, \n" +
                "  MODIFICATION_TIME, \n" +
                "  PLATFORM, \n" +
                "  APP_NAME, \n" +
                "  UID,\n" +
                "  YANDEX_UID,\n" +
                "  GEO_ID,\n" +
                "  DISABLED_BY_SYSTEM,\n" +
                "  MUID\n" +
                ")\n" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                mobileAppInfo.getUuid(),
                mobileAppInfo.getPushToken(),
                mobileAppInfo.isUnregistered() ? 1 : 0,
                mobileAppInfo.getModificationTime(),
                mobileAppInfo.getPlatform().name(),
                mobileAppInfo.getAppName(),
                mobileAppInfo.getUid(),
                mobileAppInfo.getYandexUid(),
                mobileAppInfo.getGeoId(),
                null,
                mobileAppInfo.getMuid()
        );

        changedMobileAppInfoDAO.markAsChanged(mobileAppInfo.getUuid());
        service.exportChanged();

        List<MobileAppInfo> items = writer.getItems();
        assertThat(items, hasSize(1));

        assertThat(items.get(0), hasValues(mobileAppInfo));

        Long changedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM CRM_CHANGED_MOBILE_APP_INFO",
                Long.class
        );
        assertEquals(Long.valueOf(0), changedCount);
    }

    private List<MobileAppInfo> registerInfo() {
        return IntStream.range(0, 3)
                .mapToObj(this::getMobileAppInfo)
                .peek(this::register)
                .collect(Collectors.toList());
    }

    @NotNull
    private MobileAppInfo getMobileAppInfo(int randomizedNumber) {
        MobileAppInfo info = new MobileAppInfo();
        info.setUuid("uuid-" + randomizedNumber);
        info.setPushToken("iddqd-" + randomizedNumber);
        info.setModificationTime(Date.from(
                LocalDateTime.of(2017, 12, 1, 12, randomizedNumber)
                        .toInstant(ZoneOffset.UTC)
        ));
        info.setPlatform(MobilePlatform.values()[randomizedNumber]);
        info.setAppName("ru.yandex.test.market.app" + randomizedNumber);
        info.setUid(1000L * randomizedNumber);
        info.setYandexUid(String.valueOf(randomizedNumber * 10_000));
        info.setGeoId(50L + randomizedNumber);
        info.setMuid(100_000L * randomizedNumber);
        info.setDisabledBySystem(randomizedNumber % 2 == 0);
        return info;
    }

    private void register(MobileAppInfo mobileAppInfo) {
        jdbcTemplate.update(
                "INSERT INTO MOBILE_APP_INFO (\n" +
                        "  UUID, \n" +
                        "  PUSH_TOKEN, \n" +
                        "  UNREGISTERED, \n" +
                        "  MODIFICATION_TIME, \n" +
                        "  PLATFORM, \n" +
                        "  APP_NAME, \n" +
                        "  UID,\n" +
                        "  YANDEX_UID,\n" +
                        "  GEO_ID,\n" +
                        "  DISABLED_BY_SYSTEM,\n" +
                        "  MUID\n" +
                        ")\n" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                mobileAppInfo.getUuid(),
                mobileAppInfo.getPushToken(),
                mobileAppInfo.isUnregistered() ? 1 : 0,
                mobileAppInfo.getModificationTime(),
                mobileAppInfo.getPlatform().name(),
                mobileAppInfo.getAppName(),
                mobileAppInfo.getUid(),
                mobileAppInfo.getYandexUid(),
                mobileAppInfo.getGeoId(),
                mobileAppInfo.getDisabledBySystem() ? 1 : 0,
                mobileAppInfo.getMuid()
        );
    }
}
