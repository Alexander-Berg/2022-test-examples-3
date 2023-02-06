package ru.yandex.direct.intapi.entity.display.canvas.service;

import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.creative.service.CreativeService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.configuration.IntapiConfiguration;
import ru.yandex.direct.intapi.entity.display.canvas.model.ActionType;
import ru.yandex.direct.intapi.entity.display.canvas.model.AuthResponse;
import ru.yandex.direct.intapi.entity.display.canvas.model.FeatureType;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.test.utils.data.Logins;
import ru.yandex.direct.utils.PassportUtils;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.intapi.entity.display.canvas.model.ActionType.CREATIVE_CREATE;
import static ru.yandex.direct.intapi.entity.display.canvas.model.ActionType.CREATIVE_DELETE;
import static ru.yandex.direct.intapi.entity.display.canvas.model.ActionType.CREATIVE_EDIT;
import static ru.yandex.direct.intapi.entity.display.canvas.model.ActionType.CREATIVE_GET;
import static ru.yandex.direct.intapi.entity.display.canvas.model.FeatureType.HTML5_CREATIVES;
import static ru.yandex.direct.intapi.entity.display.canvas.model.FeatureType.INTERNAL_USER;
import static ru.yandex.direct.intapi.entity.display.canvas.model.FeatureType.TURBO_LANDINGS;
import static ru.yandex.direct.intapi.entity.display.canvas.model.FeatureType.VIDEO_ADDITION;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntapiConfiguration.class})
@Ignore("only for manual runs, because it connects to real database")
@SuppressWarnings("unchecked")
public class DisplayCanvasAuthServiceTest {

    private static final String SUPER = "at-direct-super";
    private static final String SUPERREADER = "at-direct-super-reader";
    private static final String AGENCY_CHIEF = "ra-trinet";
    private static final String CLIENT_CHIEF = "yndx-irakr-light";
    private static final String VIDEO_CLIENT = "bioshlang";
    private static final String TURBOLANDINGS_CLIENT = "bioshlang";
    private static final String OTHER_MANAGER = Logins.ANOTHER_MANAGER;
    private static final String SUPPORT = Logins.SUPPORT;

    private static final String SUBCLIENT = "elama-16021052";
    private static final String ANOTHER_SUBCLIENT = "biznes-kadastr-imiks";
    private static final Matcher<Iterable<? extends ActionType>> ALL_RIGHTS =
            containsInAnyOrder(is(CREATIVE_CREATE), is(CREATIVE_GET), is(CREATIVE_EDIT), is(CREATIVE_DELETE));
    private static final Matcher<Iterable<? extends ActionType>> READ_RIGHTS = contains(is(CREATIVE_GET));
    private static final Matcher<Iterable<? extends ActionType>> CREATE_READ_UPDATE_RIGHTS =
            containsInAnyOrder(is(CREATIVE_CREATE), is(CREATIVE_GET), is(CREATIVE_EDIT));
    private static final Matcher<? super List<ActionType>> NO_RIGHTS = empty();
    private static final Matcher<? super List<FeatureType>> VIDEO_EXIST = hasItem(is(VIDEO_ADDITION));
    private static final Matcher<? super List<FeatureType>> NO_TURBOLANDINGS = not(hasItem(is(TURBO_LANDINGS)));
    private static final Matcher<? super List<FeatureType>> TURBOLANDINGS_EXIST = hasItem(is(TURBO_LANDINGS));
    private static final Matcher<? super List<FeatureType>> NO_HTML5_CREATIVES = not(hasItem(is(HTML5_CREATIVES)));
    private static final Matcher<? super List<FeatureType>> HTML5_CREATIVES_EXISTS = hasItem(is(HTML5_CREATIVES));

    @Autowired
    ShardHelper shardHelper;

    @Autowired
    RbacService rbacService;

    @Autowired
    ClientService clientService;

    @Autowired
    AuthCreativeValidationService authCreativeValidationService;

    @Autowired
    CreativeService creativeService;

    @Autowired
    FeatureService featureService;

    private DisplayCanvasAuthService displayCanvasAuthService;

    @Before
    public void setup() {
        displayCanvasAuthService = new DisplayCanvasAuthService(rbacService, clientService,
                authCreativeValidationService, creativeService, featureService,
                shardHelper);
    }

    @Test
    public void auth_SuperHasAccessToClient() {
        ClientId clientId = getClientId(CLIENT_CHIEF);
        Long operatorUid = getUid(SUPER);
        AuthResponse grants = displayCanvasAuthService.auth(operatorUid, clientId);

        assertThat(grants.getAvailableActions(), ALL_RIGHTS);
    }

    @Test
    public void auth_UnknownUidsHaveNoRights() {
        ClientId clientId = getClientId(SUPERREADER);
        Long operatorUid = getUid(SUPER);
        AuthResponse grants = displayCanvasAuthService.auth(operatorUid, clientId);

        assertThat(grants.getAvailableActions(), NO_RIGHTS);
    }

    @Test
    public void auth_ManagerHasNoAccessToOtherClient() {
        ClientId clientId = getClientId(CLIENT_CHIEF);
        Long operatorUid = getUid(OTHER_MANAGER);
        AuthResponse grants = displayCanvasAuthService.auth(operatorUid, clientId);

        assertThat(grants.getAvailableActions(), NO_RIGHTS);
    }

    @Test
    public void auth_AgencyHasAccessToItsSubclient() {
        ClientId clientId = getClientId(SUBCLIENT);
        Long operatorUid = getUid(AGENCY_CHIEF);
        AuthResponse grants = displayCanvasAuthService.auth(operatorUid, clientId);

        assertThat(grants.getAvailableActions(), ALL_RIGHTS);
    }

    @Test
    public void auth_AgencyHasNoAccessToOtherSubclient() {
        ClientId clientId = getClientId(ANOTHER_SUBCLIENT);
        Long operatorUid = getUid(AGENCY_CHIEF);
        AuthResponse grants = displayCanvasAuthService.auth(operatorUid, clientId);

        assertThat(grants.getAvailableActions(), NO_RIGHTS);
    }

    @Test
    public void auth_SuperreaderHasReadAccessForClient() {
        ClientId clientId = getClientId(CLIENT_CHIEF);
        Long operatorUid = getUid(SUPERREADER);
        AuthResponse grants = displayCanvasAuthService.auth(operatorUid, clientId);

        assertThat(grants.getAvailableActions(), READ_RIGHTS);
    }

    @Test
    public void auth_SupportHasReadAccessForClient() {
        ClientId clientId = getClientId(CLIENT_CHIEF);
        Long operatorUid = getUid(SUPPORT);
        AuthResponse grants = displayCanvasAuthService.auth(operatorUid, clientId);

        assertThat(grants.getAvailableActions(), CREATE_READ_UPDATE_RIGHTS);
    }

    @Test
    public void auth_ClientHasAccessToItsClient() {
        ClientId clientId = getClientId(CLIENT_CHIEF);
        Long operatorUid = getUid(CLIENT_CHIEF);
        AuthResponse grants = displayCanvasAuthService.auth(operatorUid, clientId);

        assertThat(grants.getAvailableActions(), ALL_RIGHTS);
    }

    @Test
    public void auth_ClientHasNoAccessToOtherClient() {
        ClientId clientId = getClientId(ANOTHER_SUBCLIENT);
        Long operatorUid = getUid(CLIENT_CHIEF);
        AuthResponse grants = displayCanvasAuthService.auth(operatorUid, clientId);

        assertThat(grants.getAvailableActions(), NO_RIGHTS);
    }

    private Long getUid(String login) {
        return shardHelper.getUidByLogin(PassportUtils.normalizeLogin(login));
    }

    private ClientId getClientId(String login) {
        return ClientId.fromLong(shardHelper.getClientIdByUid(getUid(login)));
    }

    private List<FeatureType> getFeatures(String clientLogin, String operatorLogin) {
        ClientId clientId = getClientId(clientLogin);
        Long operatorUid = getUid(operatorLogin);
        AuthResponse grants = displayCanvasAuthService.auth(operatorUid, clientId);

        return grants.getAvailableFeatures();
    }

    @Test
    public void auth_clientHasAccessToVideo() {
        assertThat(getFeatures(VIDEO_CLIENT, CLIENT_CHIEF), VIDEO_EXIST);
    }

    @Test
    public void auth_agencyHasNoAccessToTurbolandings() {
        assertThat(getFeatures(CLIENT_CHIEF, AGENCY_CHIEF), NO_TURBOLANDINGS);
    }

    @Test
    public void auth_clientHasNoAccessToTurbolandings() {
        assertThat(getFeatures(CLIENT_CHIEF, ANOTHER_SUBCLIENT), NO_TURBOLANDINGS);
    }

    @Test
    public void auth_clientHasAccessToTurbolandings() {
        assertThat(getFeatures(TURBOLANDINGS_CLIENT, CLIENT_CHIEF), TURBOLANDINGS_EXIST);
    }

    @Test
    public void auth_clientHasNoAccessToHtml5Creatives() {
        assertThat(getFeatures(CLIENT_CHIEF, ANOTHER_SUBCLIENT), NO_HTML5_CREATIVES);
    }

    @Test
    public void auth_superHasAccessToHtml5Creatives() {
        assertThat(getFeatures(ANOTHER_SUBCLIENT, SUPER), HTML5_CREATIVES_EXISTS);
    }

    @Test
    public void auth_superIsInternalUser() {
        assertThat(getFeatures(CLIENT_CHIEF, SUPER), hasItem(is(INTERNAL_USER)));
    }

    @Test
    public void auth_clientIsNotInternalUser() {
        assertThat(getFeatures(CLIENT_CHIEF, CLIENT_CHIEF), not(hasItem(is(INTERNAL_USER))));
    }
}
