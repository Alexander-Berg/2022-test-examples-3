package ru.yandex.direct.grid.processing.service.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.user.model.AgencyLimRep;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.container.agency.GdAgencyInfo;
import ru.yandex.direct.grid.processing.model.client.GdAgencyLimRepInfo;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.client.GdUserInfo;
import ru.yandex.direct.rbac.RbacAgencyLimRepType;
import ru.yandex.direct.regions.GeoTreeType;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.client.model.PhoneVerificationStatus.VERIFIED;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.service.client.converter.ClientDataConverter.toGdAgencyInfo;
import static ru.yandex.direct.grid.processing.service.operator.UserDataConverter.toGdUserInfo;
import static ru.yandex.direct.grid.processing.util.UserHelper.defaultClientNds;

@GridProcessingTest
@ParametersAreNonnullByDefault
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientDataConverterCreateClientInfoTest {

    private static final int SHARD = 1;
    private static final Long OTHER_MANAGER = null;

    @Autowired
    private UserSteps userSteps;

    private Map<Long, Boolean> showAgencyContactsByUids;
    private Map<Long, Client> relatedClientsById;
    private Map<Long, GdUserInfo> userByUids;
    private Long agencyChiefUid;
    private Long agencyRepresentativeUid;
    private Client client;
    private Client agencyClient;
    private User operator;
    private AgencyLimRep agencyLimRep1;
    private AgencyLimRep agencyLimRep2;
    private AgencyLimRep agencyLimRep3;
    private UserInfo agencyLimRep1UserInfo;
    private UserInfo agencyLimRep2UserInfo;
    private UserInfo agencyLimRep3UserInfo;

    @Before
    public void init() {
        showAgencyContactsByUids = new HashMap<>();
        relatedClientsById = new HashMap<>();
        userByUids = new HashMap<>();
        UserInfo userInfo = userSteps.createUser(generateNewUser());
        UserInfo agencyChiefUser = userSteps.createUser(generateNewUser());
        UserInfo agencyRepresentativeUser =
                userSteps.createUser(generateNewUser().withClientId(agencyChiefUser.getClientInfo().getClientId()));

        agencyChiefUid = agencyChiefUser.getUid();
        agencyRepresentativeUid = agencyRepresentativeUser.getUid();

        client = userInfo.getClientInfo().getClient();
        client.setAgencyUserId(agencyRepresentativeUid);
        operator = userInfo.getUser();
        var agencyClientInfo = agencyChiefUser.getClientInfo();
        agencyClient = agencyClientInfo.getClient();

        agencyLimRep1 = new AgencyLimRep().withRepType(RbacAgencyLimRepType.LEGACY);
        agencyLimRep2 = new AgencyLimRep().withRepType(RbacAgencyLimRepType.CHIEF).withGroupId(RandomNumberUtils.nextPositiveLong());
        agencyLimRep3 = new AgencyLimRep().withRepType(RbacAgencyLimRepType.MAIN).withGroupId(agencyLimRep2.getGroupId());

        agencyLimRep1UserInfo = userSteps.createAgencyLimRep(agencyClientInfo, agencyLimRep1);
        agencyLimRep2UserInfo = userSteps.createAgencyLimRep(agencyClientInfo, agencyLimRep2);
        agencyLimRep3UserInfo = userSteps.createAgencyLimRep(agencyClientInfo, agencyLimRep3);
        Stream.of(agencyLimRep1UserInfo, agencyLimRep2UserInfo, agencyLimRep3UserInfo).forEach(
                o -> userByUids.put(o.getUid(), toGdUserInfo(o.getUser()))
        );

        relatedClientsById.put(userInfo.getClientInfo().getClientId().asLong(), client);
        relatedClientsById.put(agencyChiefUser.getClientInfo().getClientId().asLong(), agencyClient);

        userByUids.put(userInfo.getUid(), toGdUserInfo(userInfo.getUser()));
        userByUids.put(agencyRepresentativeUid, toGdUserInfo(agencyRepresentativeUser.getUser()));
        userByUids.put(agencyChiefUid, toGdUserInfo(agencyChiefUser.getUser()));
    }

    @Test
    public void createClientInfo_showRepresentativeContacts() {
        showAgencyContactsByUids.put(agencyChiefUid, true);
        showAgencyContactsByUids.put(agencyRepresentativeUid, true);
        verify(agencyRepresentativeUid, -1L);
    }

    @Test
    public void createClientInfo_LimitedRepresentativesWithContactsTest() {
        showAgencyContactsByUids.put(agencyChiefUid, true);
        showAgencyContactsByUids.put(agencyRepresentativeUid, true);
        showAgencyContactsByUids.put(agencyLimRep1.getUid(), true);
        showAgencyContactsByUids.put(agencyLimRep2.getUid(), true);
        var limitedRepresentativesInfo = List.of(
                getGdAgencyLimRepInfo(agencyLimRep1, agencyLimRep1UserInfo.getUser(), true),
                getGdAgencyLimRepInfo(agencyLimRep2, agencyLimRep2UserInfo.getUser(), true)
        );
        var agencyLimRepsByClientIds = Map.ofEntries(
                Map.entry(client.getClientId(), Set.of(agencyLimRep1, agencyLimRep2))
        );
        verify(agencyRepresentativeUid, -1L, agencyLimRepsByClientIds,  limitedRepresentativesInfo);
    }

    @Test
    public void createClientInfo_LimitedRepresentativesWithoutContactsTest() {
        showAgencyContactsByUids.put(agencyChiefUid, true);
        showAgencyContactsByUids.put(agencyRepresentativeUid, true);
        showAgencyContactsByUids.put(agencyLimRep1.getUid(), false);
        showAgencyContactsByUids.put(agencyLimRep2.getUid(), false);
        var limitedRepresentativesInfo = List.of(
                getGdAgencyLimRepInfo(agencyLimRep1, agencyLimRep1UserInfo.getUser(), false),
                getGdAgencyLimRepInfo(agencyLimRep2, agencyLimRep2UserInfo.getUser(), false)
        );
        var agencyLimRepsByClientIds = Map.ofEntries(
                Map.entry(client.getClientId(), Set.of(agencyLimRep1, agencyLimRep2))
        );
        verify(agencyRepresentativeUid, -1L, agencyLimRepsByClientIds,  limitedRepresentativesInfo);
    }

    @Test
    public void createClientInfo_showNoContacts() {
        showAgencyContactsByUids.put(agencyChiefUid, false);
        showAgencyContactsByUids.put(agencyRepresentativeUid, false);
        verify(agencyChiefUid, agencyRepresentativeUid);
    }

    @Test
    public void createClientInfo_showChiefContacts() {
        showAgencyContactsByUids.put(agencyChiefUid, true);
        showAgencyContactsByUids.put(agencyRepresentativeUid, false);
        verify(agencyChiefUid, agencyRepresentativeUid);
    }

    private GdAgencyLimRepInfo getGdAgencyLimRepInfo(AgencyLimRep agencyLimRep, User agencyLimRepUser, boolean isShowContacts) {
        var gdAgencyLimRepInfo =  new GdAgencyLimRepInfo()
                .withLimRepType(agencyLimRep.getRepType())
                .withName(agencyLimRepUser.getFio())
                .withLogin(agencyLimRepUser.getLogin())
                .withShowContacts(isShowContacts);
        if (isShowContacts) {
            gdAgencyLimRepInfo
                    .withUserId(agencyLimRepUser.getUid())
                    .withEmail(agencyLimRepUser.getEmail())
                    .withPhone(agencyLimRepUser.getPhone());
        }

        return gdAgencyLimRepInfo;
    }

    private void verify(Long expectedAgencyInfoUid, Long expectedAgencyRepresentativeUid) {
        verify(expectedAgencyInfoUid, expectedAgencyRepresentativeUid, emptyMap(), null);
    }

    private void verify(Long expectedAgencyInfoUid, Long expectedAgencyRepresentativeUid,
                        Map<Long, Set<AgencyLimRep>> agencyLimRepsByClientIds, Collection<GdAgencyLimRepInfo> agencyLimRepsInfo) {
        GdClientInfo clientInfo =
                ClientDataService.createClientInfo(SHARD, client, userByUids, OTHER_MANAGER, relatedClientsById,
                        showAgencyContactsByUids, defaultClientNds(client.getId()), GeoTreeType.GLOBAL, null,
                        emptySet(), emptySet(), false, emptyMap(), agencyLimRepsByClientIds, false, VERIFIED, false);
        GdAgencyInfo expectedAgencyInfo =
                toGdAgencyInfo(agencyClient, userByUids.get(expectedAgencyInfoUid),
                        userByUids.get(expectedAgencyRepresentativeUid), agencyLimRepsInfo,
                        showAgencyContactsByUids.get(expectedAgencyInfoUid));
        assertThat(clientInfo.getAgencyInfo()).isEqualTo(expectedAgencyInfo);
    }

}
