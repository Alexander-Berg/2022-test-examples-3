package ru.yandex.chemodan.app.psbilling.core.users;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserInfo;
import ru.yandex.chemodan.test.ConcurrentTestUtils;
import ru.yandex.devtools.test.annotations.YaExternal;
import ru.yandex.inside.geobase.Geobase;
import ru.yandex.inside.geobase.RegionNode;
import ru.yandex.inside.geobase.RegionType;
import ru.yandex.inside.passport.PassportUid;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class UserInfoServiceTest extends AbstractPsBillingCoreTest {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private Geobase geobase;

    @Before
    public void setUp() {
        when(geobase.getRegionIdsByType(any())).thenReturn(Cf.list(225));
        when(geobase.getRegionById(225)).thenReturn(
                Option.of(
                        new RegionNode("bla", "ru", Option.empty(), Option.empty(), 225, RegionType.COUNTRY, true, null,
                                Option.empty())));
        userInfoService.updateCountriesCache();
    }

    @Test
    @YaExternal
    public void test() {
        UserInfo userInfo = userInfoService.findOrCreateUserInfo(PassportUid.cons(3000185708L));
        Assert.assertEquals(Option.of("225"), userInfo.getRegionId());
        Assert.assertEquals("3000185708", userInfo.getUid());
        Assert.assertNotNull(userInfo.getId());
        Assert.assertNotNull(userInfo.getCreatedAt());
    }

    @Test
    public void testConcurrentCreation() {
        ConcurrentTestUtils.testConcurrency(() -> this::test);
    }
}
