package ru.yandex.market.crm.campaign.services.segments;

import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.UidPair;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;

import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.userLoginsFilter;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.passportProfile;

/**
 * @author apershukov
 */
public class PassportAccountsFilterTest extends AbstractServiceLargeTest {

    private static YTreeMapNode profile(long puid, @Nullable String userDefinedLogin, String login) {
        return passportProfile(puid, "m", null, null, userDefinedLogin, login);
    }

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.preparePassportProfilesTable();
        ytSchemaTestHelper.prepareEmailOwnershipFactsTable();
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();
    }

    @Test
    public void testReturnPuids() throws Exception {
        userTestHelper.addPassportProfiles(
                profile(111, "first.user", "first_user"),
                profile(222, "second.user", "second_user"),
                profile(333, "tHiRd.UsEr", "third_user")
        );

        Segment segment = segment(
                userLoginsFilter("\r\nfirst.user\r\n", "\rThIrD.uSeR\r")
        );

        Set<UidPair> expected = ImmutableSet.of(
                pair(Uid.asPuid(111L)),
                pair(Uid.asPuid(333L))
        );

        assertSegment(segment, expected);
    }

    /**
     * В случае если user_defined_login не заполнен смотрим в поле login
     */
    @Test
    public void testCheckLoginIfUserDefinedLoginMissing() throws Exception {
        userTestHelper.addPassportProfiles(
                profile(111, null, "first_user"),
                profile(222, null, "second_user")
        );

        Segment segment = segment(
                userLoginsFilter("first_user")
        );

        Set<UidPair> expected = ImmutableSet.of(
                pair(Uid.asPuid(111L))
        );

        assertSegment(segment, expected);
    }

    private void assertSegment(Segment segment, Set<UidPair> expected) throws Exception {
        segmentatorTestHelper.assertSegmentPairs(expected, LinkingMode.NONE, Set.of(UidType.PUID), segment);
    }
}
