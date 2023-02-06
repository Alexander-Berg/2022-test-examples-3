package ru.yandex.direct.grid.processing.service.client;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterWithAdditionalInformation;
import ru.yandex.direct.core.entity.user.model.AgencyLimRep;
import ru.yandex.direct.core.entity.user.model.DeletedClientRepresentative;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.grid.processing.container.agency.GdAgencyInfo;
import ru.yandex.direct.grid.processing.model.client.GdAgencyLimRepInfo;
import ru.yandex.direct.grid.processing.model.client.GdClientMetrikaCounter;
import ru.yandex.direct.grid.processing.model.client.GdDeletedClientRepresentativeInfo;
import ru.yandex.direct.grid.processing.model.client.GdUserInfo;
import ru.yandex.direct.grid.processing.service.client.converter.ClientDataConverter;
import ru.yandex.direct.rbac.RbacAgencyLimRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.service.client.converter.ClientDataConverter.parseFlags;
import static ru.yandex.direct.grid.processing.service.operator.UserDataConverter.getAvatarUrl;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class ClientDataConverterTest {
    private final static User operator = new User().withRole(RbacRole.CLIENT);

    @Test
    public void toGdAgencyInfo_ClientNull() {
        GdAgencyInfo gdAgencyInfo = ClientDataConverter.toGdAgencyInfo(null, new GdUserInfo(), null, null, true);
        //noinspection ConstantConditions
        assertThat(gdAgencyInfo).isNull();
    }

    @Test
    public void toGdAgencyInfo_AgencyUserInfoNull() {
        GdAgencyInfo gdAgencyInfo = ClientDataConverter.toGdAgencyInfo(new Client(), null, null, null, true);
        //noinspection ConstantConditions
        assertThat(gdAgencyInfo).isNull();
    }

    @Test
    public void toGdAgencyInfo_AgencyInfoNotShow() {
        String name = RandomStringUtils.random(10);
        Client client = new Client().withName(name);

        long userId = RandomNumberUtils.nextPositiveLong();
        long clientId = RandomNumberUtils.nextPositiveLong();
        String email = RandomStringUtils.random(10);
        String login = RandomStringUtils.random(10);
        String phone = RandomStringUtils.randomNumeric(10);
        String representativeName = RandomStringUtils.random(10);

        GdUserInfo gdAgencyUserInfo = new GdUserInfo()
                .withUserId(userId)
                .withClientId(clientId)
                .withEmail(email)
                .withLogin(login)
                .withPhone(phone)
                .withName(representativeName);

        GdAgencyInfo gdAgencyInfo = ClientDataConverter.toGdAgencyInfo(client, gdAgencyUserInfo, gdAgencyUserInfo, null,
                false);
        GdAgencyInfo exceptedGdAgencyInfo = new GdAgencyInfo()
                .withName(name)
                .withRepresentativeName(representativeName)
                .withRepresentativeLogin(login)
                .withUserId(null)
                .withClientId(null)
                .withEmail(null)
                .withLogin(login)
                .withPhone(null)
                .withAvatarUrl(null)
                .withLimitedRepresentatives(List.of())
                .withShowAgencyContacts(false);

        assertThat(gdAgencyInfo).is(matchedBy(beanDiffer(exceptedGdAgencyInfo)));
    }

    @Test
    public void toGdAgencyInfo_NullNameConvertToEmptyString() {
        Client client = new Client();
        GdUserInfo gdAgencyUserInfo = new GdUserInfo();

        GdAgencyInfo gdAgencyInfo = ClientDataConverter.toGdAgencyInfo(client, gdAgencyUserInfo, null, null, true);
        GdAgencyInfo exceptedGdAgencyInfo = new GdAgencyInfo()
                .withName("")
                .withAvatarUrl(getAvatarUrl(null))
                .withLimitedRepresentatives(List.of())
                .withShowAgencyContacts(true);

        assertThat(gdAgencyInfo).isEqualTo(exceptedGdAgencyInfo);
    }

    @Test
    public void toGdAgencyInfo_ConvertToNonNull() {
        String name = RandomStringUtils.random(10);
        Client client = new Client().withName(name);

        long userId = RandomNumberUtils.nextPositiveLong();
        long clientId = RandomNumberUtils.nextPositiveLong();
        String email = RandomStringUtils.random(10);
        String login = RandomStringUtils.random(10);
        String phone = RandomStringUtils.randomNumeric(10);
        String representativeName = RandomStringUtils.random(10);

        GdUserInfo gdAgencyUserInfo = new GdUserInfo()
                .withUserId(userId)
                .withClientId(clientId)
                .withEmail(email)
                .withLogin(login)
                .withPhone(phone)
                .withName(representativeName);

        GdAgencyInfo gdAgencyInfo = ClientDataConverter.toGdAgencyInfo(client, gdAgencyUserInfo, gdAgencyUserInfo, null,
                true);
        GdAgencyInfo exceptedGdAgencyInfo = new GdAgencyInfo()
                .withName(name)
                .withUserId(userId)
                .withClientId(clientId)
                .withEmail(email)
                .withLogin(login)
                .withRepresentativeLogin(login)
                .withPhone(phone)
                .withRepresentativeName(representativeName)
                .withAvatarUrl(getAvatarUrl(userId))
                .withLimitedRepresentatives(List.of())
                .withShowAgencyContacts(true);

        assertThat(gdAgencyInfo).is(matchedBy(beanDiffer(exceptedGdAgencyInfo)));
    }

    @Test
    public void parseFlags_nullFlag() {
        assertThat(parseFlags(null)).isEqualTo(null);
    }

    @Test
    public void parseFlags_singleFlag() {
        String flag = "flag";
        assertThat(parseFlags(flag)).isEqualTo(ImmutableSet.of("flag"));
    }

    @Test
    public void parseFlags_twoFlags() {
        String flag = "flag1,flag2";
        assertThat(parseFlags(flag)).isEqualTo(ImmutableSet.of("flag1", "flag2"));
    }

    @Test
    public void toGdClientMetrikaCountersWithAdditionalInformation() {
        long id = 123L;
        MetrikaCounterWithAdditionalInformation counter = new MetrikaCounterWithAdditionalInformation()
                .withId(id);

        Set<GdClientMetrikaCounter> gdClientMetrikaCounter =
                ClientDataConverter.toGdClientMetrikaCountersWithAdditionalInformation(List.of(counter));
        GdClientMetrikaCounter expectedCounter = new GdClientMetrikaCounter()
                .withId(id)
                .withName("")
                .withIsEditableByOperator(false)
                .withDomain("");
        assertThat(gdClientMetrikaCounter).isEqualTo(Set.of(expectedCounter));
    }

    @Test
    public void toGdAgencyInfo_LimitedRepresentativesTest() {
        String name = RandomStringUtils.random(10);
        Client client = new Client().withName(name);

        long userId = RandomNumberUtils.nextPositiveLong();
        long clientId = RandomNumberUtils.nextPositiveLong();
        String email = RandomStringUtils.random(10);
        String login = RandomStringUtils.random(10);
        String phone = RandomStringUtils.randomNumeric(10);
        String representativeName = RandomStringUtils.random(10);

        GdUserInfo gdAgencyUserInfo = new GdUserInfo()
                .withUserId(userId)
                .withClientId(clientId)
                .withEmail(email)
                .withLogin(login)
                .withPhone(phone)
                .withName(representativeName);

        var gdAgencyLegacyLimRepInfo = getLimRepInfo(RbacAgencyLimRepType.LEGACY, true);
        var gdAgencyChiefLimRepInfo = getLimRepInfo(RbacAgencyLimRepType.CHIEF, false);

        List<GdAgencyLimRepInfo> gdAgencyLimRepInfos = List.of(gdAgencyLegacyLimRepInfo, gdAgencyChiefLimRepInfo);

        GdAgencyInfo gdAgencyInfo = ClientDataConverter.toGdAgencyInfo(client, gdAgencyUserInfo, null, gdAgencyLimRepInfos,
                true);
        GdAgencyInfo exceptedGdAgencyInfo = new GdAgencyInfo()
                .withName(name)
                .withUserId(userId)
                .withClientId(clientId)
                .withEmail(email)
                .withLogin(login)
                .withRepresentativeLogin(login)
                .withPhone(phone)
                .withRepresentativeName(representativeName)
                .withAvatarUrl(getAvatarUrl(userId))
                .withLimitedRepresentatives(gdAgencyLimRepInfos)
                .withShowAgencyContacts(true);

        assertThat(gdAgencyInfo).is(matchedBy(beanDiffer(exceptedGdAgencyInfo)));
    }

    @Test
    public void toGdAgencyLimRepInfoWithShowContactsTest() {
        AgencyLimRep agencyLimRep = new AgencyLimRep()
                .withRepType(RbacAgencyLimRepType.LEGACY);

        GdUserInfo gdAgencyUserInfo = getAgencyUserInfo();

        GdAgencyLimRepInfo gdAgencyLimRepInfo = ClientDataConverter.toGdAgencyLimRepInfo(agencyLimRep, gdAgencyUserInfo, true);
        GdAgencyLimRepInfo exceptedGdAgencyLimRepInfo = new GdAgencyLimRepInfo()
                .withName(gdAgencyUserInfo.getName())
                .withUserId(gdAgencyUserInfo.getUserId())
                .withEmail(gdAgencyUserInfo.getEmail())
                .withLogin(gdAgencyUserInfo.getLogin())
                .withPhone(gdAgencyUserInfo.getPhone())
                .withLimRepType(RbacAgencyLimRepType.LEGACY)
                .withShowContacts(true);

        assertThat(gdAgencyLimRepInfo).is(matchedBy(beanDiffer(exceptedGdAgencyLimRepInfo)));
    }

    @Test
    public void toGdAgencyLimRepInfoWithoutShowContactsTest() {
        AgencyLimRep agencyLimRep = new AgencyLimRep()
                .withRepType(RbacAgencyLimRepType.LEGACY);

        GdUserInfo gdAgencyUserInfo = getAgencyUserInfo();

        GdAgencyLimRepInfo gdAgencyLimRepInfo = ClientDataConverter.toGdAgencyLimRepInfo(agencyLimRep, gdAgencyUserInfo, false);
        GdAgencyLimRepInfo exceptedGdAgencyLimRepInfo = new GdAgencyLimRepInfo()
                .withName(gdAgencyUserInfo.getName())
                .withLogin(gdAgencyUserInfo.getLogin())
                .withLimRepType(RbacAgencyLimRepType.LEGACY)
                .withShowContacts(false);

        assertThat(gdAgencyLimRepInfo).is(matchedBy(beanDiffer(exceptedGdAgencyLimRepInfo)));
    }

    @Test
    public void toGdDeletedClientRepresentativeInfo_NotnullValues() {
        var deletedRep = new DeletedClientRepresentative()
                .withLogin("login1")
                .withUid(1L)
                .withEmail("test@ya.ru")
                .withFio("FIO1")
                .withPhone("+7000123456789");

        var info = ClientDataConverter.toGdDeletedClientRepresentativeInfo(deletedRep);
        var expected = new GdDeletedClientRepresentativeInfo()
                .withUserId(deletedRep.getUid())
                .withLogin(deletedRep.getLogin())
                .withEmail(deletedRep.getEmail())
                .withName(deletedRep.getFio())
                .withPhone(deletedRep.getPhone());

        assertThat(info).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    public void toGdDeletedClientRepresentativeInfo_WithNullValues() {
        var deletedRep = new DeletedClientRepresentative()
                .withLogin("login1")
                .withUid(1L);

        var info = ClientDataConverter.toGdDeletedClientRepresentativeInfo(deletedRep);
        var expected = new GdDeletedClientRepresentativeInfo()
                .withUserId(deletedRep.getUid())
                .withLogin(deletedRep.getLogin())
                .withEmail("")
                .withName("")
                .withPhone(null);

        assertThat(info).usingRecursiveComparison().isEqualTo(expected);
    }

    private GdAgencyLimRepInfo getLimRepInfo(RbacAgencyLimRepType type, boolean isShowContacts) {
        return new GdAgencyLimRepInfo()
                .withLimRepType(RbacAgencyLimRepType.LEGACY)
                .withUserId(RandomNumberUtils.nextPositiveLong())
                .withLogin(RandomStringUtils.random(10))
                .withEmail(RandomStringUtils.random(10))
                .withPhone(RandomStringUtils.randomNumeric(10))
                .withShowContacts(true);
    }

    private GdUserInfo getAgencyUserInfo() {
        return new GdUserInfo()
                .withUserId(RandomNumberUtils.nextPositiveLong())
                .withClientId(RandomNumberUtils.nextPositiveLong())
                .withEmail(RandomStringUtils.random(10))
                .withLogin(RandomStringUtils.random(10))
                .withPhone(RandomStringUtils.randomNumeric(10))
                .withName(RandomStringUtils.random(10));
    }
}
