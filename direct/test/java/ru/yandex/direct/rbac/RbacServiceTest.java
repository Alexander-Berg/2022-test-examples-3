package ru.yandex.direct.rbac;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.configuration.RbacConfiguration;
import ru.yandex.direct.utils.PassportUtils;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RbacConfiguration.class})
@Disabled("only for manual runs, because it connects to real database")
public class RbacServiceTest {
    private static final String SUPER = "yndx-p-yakovlev-super";
    private static final String SUPERREADER = "yndx.zhur.superreader";
    private static final String SUPERTEAMLEADER = "msa-super-teamleader";
    private static final String AGENCY_CHIEF = "ra-trinet";
    private static final String AGENCY_REP = "ra-trinet-add2";
    private static final String CLIENT_CHIEF = "zhurs";
    private static final String CLIENT_REP = "zhurs-rep";
    private static final String MANAGER = "zhur-manager";
    private static final String ANOTHER_AGENCY_CHIEF = "zhur-ag2";
    private static final String THIRD_AGENCY_CHIEF = "zhur-cag";
    private static final String SUBCLIENT = "elama-16021052";
    private static final String SUBCLIENT_LIMITED_AG = "direct007fot";
    private static final String ANOTHER_SUBCLIENT = "biznes-kadastr-imiks";
    private static final String SUPER_SUBCLIENT = "reserved-2013";
    private static final String SUBCLIENT_WITH_MULTIPLE_AGENCIES = "prsabidom";
    //perl -Mmy_inc=for,protected -ME -le 'print join ",\n", map { $_."L" } @{RBACDirect::rbac_get_subclients_uids
    // ($rbac, 13239617)}'
    private static final Long[] ANOTHER_AGENCY_SUBCLIENTS_UID = {15281272L,
            16356580L,
            15325369L,
            18730224L,
            16356544L,
            14907978L,
            15266191L,
            13239647L,
            18731758L,
            15281240L};
    // perl -Mmy_inc=for,protected -ME -le 'print join ",\n", map { $_."L" } @{RBACDirect::rbac_get_subclients_uids
    // ($rbac, 13238439)}'
    private static final Long[] THIRD_AGENCY_SUBCLIENTS_UID = {14096701L, 13238472L};

    @Autowired
    RbacService rbacService;

    @Autowired
    ShardHelper shardHelper;

    @Test
    public void getUidRole() throws Exception {
        assertThat(getRole(CLIENT_CHIEF), is(RbacRole.CLIENT));
        assertThat(getRole(SUPERREADER), is(RbacRole.SUPERREADER));
        assertThat(getRole(SUPER), is(RbacRole.SUPER));
    }

    @Test
    public void getChief() throws Exception {
        assertThat(getChief(CLIENT_REP), is(getUid(CLIENT_CHIEF)));
        assertThat(getChief(CLIENT_CHIEF), is(getUid(CLIENT_CHIEF)));
        assertThat(getChief(SUPER), is(getUid(SUPER)));
        assertThat(getChief(AGENCY_REP), is(getUid(AGENCY_CHIEF)));
        assertThat(getChief(AGENCY_CHIEF), is(getUid(AGENCY_CHIEF)));
    }

    @Test
    public void isOwner() throws Exception {
        assertThat(isOwner(AGENCY_CHIEF, AGENCY_CHIEF), is(true));
        assertThat(isOwner(AGENCY_CHIEF, AGENCY_REP), is(true));
        assertThat(isOwner(AGENCY_REP, AGENCY_CHIEF), is(true));

        assertThat(isOwner(AGENCY_CHIEF, SUBCLIENT), is(true));
        assertThat(isOwner(AGENCY_CHIEF, ANOTHER_SUBCLIENT), is(false));

        assertThat(isOwner(AGENCY_CHIEF, CLIENT_CHIEF), is(false));
        assertThat(isOwner(AGENCY_CHIEF, SUPERREADER), is(false));

        assertThat(isOwner(SUPER, SUPER), is(true));
        assertThat(isOwner(SUPER, AGENCY_CHIEF), is(true));

        assertThat(isOwner(CLIENT_CHIEF, CLIENT_REP), is(true));
        assertThat(isOwner(CLIENT_REP, CLIENT_CHIEF), is(true));
    }

    private boolean isOwner(String operator, String client) {
        return rbacService.isOwner(getUid(operator), getUid(client));
    }

    @Test
    public void isUnderAgency() throws Exception {

    }

    @Test
    public void isUnderManager() throws Exception {

    }

    @Test
    public void getAgencyRepsOfSubclient() {
        assertThat(rbacService.getAgencyRepsOfSubclient(getUid(CLIENT_CHIEF)), empty());
        assertThat(rbacService.getAgencyRepsOfSubclient(getUid(SUBCLIENT)), not(empty()));
    }

    @Test
    public void getManagersOfUser() {
        assertThat(rbacService.getManagersOfUser(getUid(SUBCLIENT)), empty());
        assertThat(rbacService.getManagersOfUser(getUid(CLIENT_CHIEF)), containsInAnyOrder(getUid(MANAGER)));
        assertThat(rbacService.getManagersOfUser(getUid(CLIENT_REP)), containsInAnyOrder(getUid(MANAGER)));
        assertThat(rbacService.getManagersOfUser(getUid(MANAGER)), containsInAnyOrder(getUid(MANAGER)));
        assertThat(rbacService.getManagersOfUser(getUid(SUPERTEAMLEADER)), not(empty()));
        assertThat(rbacService.getManagersOfUser(getUid(SUPERREADER)), not(empty()));
    }

    private RbacRole getRole(String login) {
        return rbacService.getUidRole(getUid(login));
    }

    private Long getUid(String login) {
        return shardHelper.getUidByLogin(PassportUtils.normalizeLogin(login));
    }

    private ClientId getClientId(String login) {
        return ClientId.fromLong(shardHelper.getClientIdByUid(getUid(login)));
    }

    private List<Long> getReps(String login) {
        Long clientId = shardHelper.getClientIdByUid(getUid(login));
        return shardHelper.getUidsByClientId(clientId);
    }

    private long getChief(String login) {
        return rbacService.getChief(getUid(login));
    }

    @Test
    public void isSuperSubclient() {
        assertThat(rbacService.isSuperSubclient(getClientId(SUBCLIENT)), is(false));
        assertThat(rbacService.isSuperSubclient(getClientId(ANOTHER_SUBCLIENT)), is(true));
        assertThat(rbacService.isSuperSubclient(getClientId(SUPER_SUBCLIENT)), is(true));
    }

    @Test
    public void canImportXLSIntoNewCampaign() {
        assertThat(rbacService
                        .canImportXLSIntoNewCampaign(getUid(AGENCY_CHIEF), getUid(SUBCLIENT), getClientId(SUBCLIENT)),
                is(false));

        assertThat(rbacService
                        .canImportXLSIntoNewCampaign(getUid(SUPER_SUBCLIENT), getUid(SUPER_SUBCLIENT),
                                getClientId(SUPER_SUBCLIENT)),
                is(true));
    }

    @Test
    public void getAgencySubclients() {
        assertThat(rbacService.getAgencySubclients(getUid(AGENCY_CHIEF)), hasItems(getUid(SUBCLIENT)));
    }

    @Test
    public void getAgencySubclientsExact() {
        assertThat(rbacService.getAgencySubclients(getUid(ANOTHER_AGENCY_CHIEF)),
                containsInAnyOrder(ANOTHER_AGENCY_SUBCLIENTS_UID));
    }

    @Test
    public void getAgencySubclientsExact2() {
        assertThat(rbacService.getAgencySubclients(getUid(THIRD_AGENCY_CHIEF)),
                containsInAnyOrder(THIRD_AGENCY_SUBCLIENTS_UID));
    }

    @Test
    public void getCampaignsWaitForServicing() {
        Assertions.assertThat(
                        rbacService.getCampaignsWaitForServicing(asList(685396L, 760326L, 1280295L, 1182606L)))
                .isEqualTo(
                        ImmutableMap.of(
                                685396L, true, 760326L, true,
                                1280295L, false, 1182606L, false)
                );
    }
}
