package ru.yandex.calendar;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.yandex.bolts.collection.CollectionF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.definition.LayerPerm;
import ru.yandex.calendar.frontend.webNew.WebNewLayerManager;
import ru.yandex.calendar.frontend.webNew.dto.in.LayerData;
import ru.yandex.calendar.frontend.webNew.dto.out.LayerInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.LayersInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.WebUserInfo;
import ru.yandex.calendar.logic.beans.generated.LayerInvitation;
import ru.yandex.calendar.logic.beans.generated.LayerUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.layer.LayerDao;
import ru.yandex.calendar.logic.layer.LayerInvitationDao;
import ru.yandex.calendar.logic.layer.LayerInvitationManager;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.layer.LayerUserDao;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.util.idlent.YandexUser;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;

import javax.inject.Inject;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class LayerStepDefinitions extends BaseStepDefinitions {
    @Inject
    WebNewLayerManager layerManager;

    @Inject
    LayerUserDao layerUserDao;

    @Inject
    LayerRoutines layerRoutines;

    @Inject
    LayerInvitationDao layerInvitationDao;

    @Inject
    LayerInvitationManager layerInvitationManager;

    @Inject
    LayerDao layerDao;

    @Before
    public void before(Scenario scenario) {
        for (val layer : layerDao.findLayers()) {
            layerManager.deleteLayer(layer.getCreatorUid(), layer.getId(), Option.empty(), Option.empty());
        }
    }

    @Given("layer {string} \\(owned by {string})")
    public void createLayerForUser(String layerName, String ownerLogin) {
        val owner = getUser(ownerLogin);
        val layerData = LayerData.empty();
        layerData.setName(Option.of(layerName));
        layerManager.createLayer(owner.getUid(), layerData);
    }

    @When("share layer {string} \\(owned by {string}) by private token with {string}")
    public void shareLayerViaPrivateToken(String layerName, String ownerLogin, String invitedLogin) {
        val invited = getUser(invitedLogin);
        val owner = getUser(ownerLogin);
        val layer = getExactlyOneLayerIdByLoginAndLayerName(ownerLogin, layerName);

        val privateTokenOpt = Option.of(layerRoutines.obtainPtk(owner.getUid(), layer, false));

        layerManager.shareLayer(invited.getUid(), Option.empty(), privateTokenOpt, Language.RUSSIAN);
    }

    @Then("invitations to layer {string} \\(owned by {string}) should be empty")
    public void layerInvitationsIsEmpty(String layerName, String ownerLogin) {
        val layerInvitations = collectLayerInvitations(ownerLogin, layerName);
        assertThat(layerInvitations).isEmpty();
    }

    @Then("users of layer {string} \\(owned by {string}) should have the following permissions")
    public void layerUserPermsIsExactly(String layerName, String ownerLogin, List<LayerPerm> perms) {
        val realPerms = StreamEx.of(collectLayerUsers(ownerLogin, layerName))
                .mapToEntry(LayerUser::getUid, LayerUser::getPerm)
                .toMap();
        val expectedPerms = constructUidToPermMap(perms);
        assertThat(realPerms).isEqualTo(expectedPerms);
    }

    @Then("invitations to layer {string} \\(owned by {string}) should offer the following permissions")
    public void layerInvitationPermsIsExactly(String layerName, String ownerLogin, List<LayerPerm> perms) {
        val realPerms = StreamEx.of(collectLayerInvitations(ownerLogin, layerName))
                .mapToEntry(LayerInvitation::getUid, LayerInvitation::getPerm)
                .mapKeys(Option::get)
                .toMap();
        val expectedPerms = constructUidToPermMap(perms);
        assertThat(realPerms).isEqualTo(expectedPerms);
    }

    private List<LayerUser> collectLayerUsers(String ownerLogin, String layerName) {
        val layerId = getExactlyOneLayerIdByLoginAndLayerName(ownerLogin, layerName);
        return layerUserDao.findLayerUserByLayerId(layerId);
    }

    private List<LayerInvitation> collectLayerInvitations(String ownerLogin, String layerName) {
        val layerId = getExactlyOneLayerIdByLoginAndLayerName(ownerLogin, layerName);
        return layerInvitationDao.findLayerInvitationByLayerId(layerId);
    }

    private Map<PassportUid, LayerActionClass> constructUidToPermMap(List<LayerPerm> layerPerms) {
        return permissionsEntryStream(layerPerms)
                .mapKeys(YandexUser::getUid)
                .toImmutableMap();
    }

    private Map<Email, LayerActionClass> constructEmailToPermMap(List<LayerPerm> layerPerms) {
        return permissionsEntryStream(layerPerms)
                .mapKeys(YandexUser::getEmail)
                .mapKeys(Option::get)
                .toImmutableMap();
    }

    private EntryStream<YandexUser, LayerActionClass> permissionsEntryStream(List<LayerPerm> layerPerms) {
        return StreamEx.of(layerPerms)
            .mapToEntry(LayerPerm::getLogin, LayerPerm::getPerm)
            .mapKeys(this::getUser);
    }

    private Long getExactlyOneLayerIdByLoginAndLayerName(String login, String layerName) {
        val layerIds = getLayerIdsByLoginAndLayerName(login, layerName);
        if (layerIds.size() > 1) {
            throw new IllegalStateException("Ambiguous layer name: user has more than one layer with name '" + layerName + "'.");
        }
        if (layerIds.isEmpty()) {
            throw new IllegalStateException("Layer with name '" + layerName + "' not found.");
        }
        return layerIds.get(0);
    }

    private List<Long> getLayerIdsByLoginAndLayerName(String login, String layerName) {
        val owner = getUser(login);
        return StreamEx.of(layerManager.getUserLayers(owner.getUid(), Language.RUSSIAN).getLayers())
                .filter(layerInfo -> layerInfo.getName().equals(layerName))
                .map(LayersInfo.LayerInfo::getId)
                .toImmutableList();
    }

    @When("share layer {string} \\(owned by {string}) by layer id with {string}")
    public void shareLayerByLayerId(String layerName, String ownerLogin, String participantLogin) {
        val layerId = getExactlyOneLayerIdByLoginAndLayerName(ownerLogin, layerName);
        val participantUid = getUser(participantLogin).getUid();

        layerManager.shareLayer(participantUid, Option.of(layerId), Option.empty(), Language.RUSSIAN);
    }

    @When("owner {string} revoke all invitations to layer {string}")
    public void shareLayerWithEmptyPermissions(String ownerLogin, String layerName) {
        shareLayerWithPermissions(layerName, ownerLogin, emptyList());
    }

    @When("update invitations to layer {string} \\(owned by {string}) with permissions")
    public void shareLayerWithPermissions(String layerName, String ownerLogin, List<LayerPerm> permissions) {
        val owner = getUserInfo(ownerLogin);
        val permsMap = constructEmailToPermMap(permissions);
        val layerId = getExactlyOneLayerIdByLoginAndLayerName(ownerLogin, layerName);

        layerInvitationManager.updateLayerSharing(owner, layerId, permsMap, false, ActionInfo.webTest());
    }

    @Then("participants returned by get-layer requested by {string} for layer {string} \\(owned by {string}) are")
    public void checkGetLayerParticipantsPermissions(String requesterLogin, String layerName, String ownerLogin, List<LayerPerm> permissions) {
        val layerId = getExactlyOneLayerIdByLoginAndLayerName(ownerLogin, layerName);

        val requester = getUserInfo(requesterLogin);
        val layer = layerManager.getLayer(requester.getUid(), layerId, Language.RUSSIAN, Option.empty());

        val realPermissions = StreamEx.of(layer.getParticipants().toOptional())
                .flatMap(StreamEx::of)
                .mapToEntry(LayerInfo.Participant::getInfo, LayerInfo.Participant::getPermission)
                .mapKeys(WebUserInfo::getEmail)
                .toImmutableMap();
        val expectedPermissions = constructEmailToPermMap(permissions);

        assertThat(realPermissions).isEqualTo(expectedPermissions);
    }

    @Then("participants returned by get-layer requested by {string} for layer {string} \\(owned by {string}) are empty")
    public void checkGetLayerParticipantsPermissionsAreEmpty(String requesterLogin, String layerName, String ownerLogin) {
        checkGetLayerParticipantsPermissions(requesterLogin, layerName, ownerLogin, emptyList());
    }

    @Then("get-layer requested by {string} for layer {string} \\(owned by {string}) responses that the requester is the owner")
    public void getLayerRequesterIsOwner(String requesterLogin, String layerName, String ownerLogin) {
        val layerId = getExactlyOneLayerIdByLoginAndLayerName(ownerLogin, layerName);
        val requester = getUserInfo(requesterLogin);
        val layer = layerManager.getLayer(requester.getUid(), layerId, Language.RUSSIAN, Option.empty());
        assertThat(layer.isOwner()).isTrue();
        assertThat(layer.isParticipant()).isFalse();
        assertThat(layer.getOwner().toOptional()).isEmpty();
    }

    @Then("get-layer requested by {string} for layer {string} \\(owned by {string}) responses that the requester is participant")
    public void getLayerRequesterIsParticipants(String requesterLogin, String layerName, String ownerLogin) {
        val layerId = getExactlyOneLayerIdByLoginAndLayerName(ownerLogin, layerName);
        val requester = getUserInfo(requesterLogin);
        val layer = layerManager.getLayer(requester.getUid(), layerId, Language.RUSSIAN, Option.empty());
        assertThat(layer.isOwner()).isFalse();
        assertThat(layer.isParticipant()).isTrue();
        assertThat(layer.getOwner()).isNotEmpty();
        assertThat(layer.getOwner().get().getLogin()).contains(ownerLogin);
    }

    @When("{string} reply {string} to {string} \\(owned by {string}) layer invitation")
    public void replyToLayerInvitation(String inviteeLogin, String decisionString, String layerName, String ownerLogin) {
        val inviteeUid = getUser(inviteeLogin).getUid();
        val decision = Decision.valueOf(decisionString);

        val layerId = getExactlyOneLayerIdByLoginAndLayerName(ownerLogin, layerName);
        val privateTokenOpt = StreamEx.of(layerInvitationDao.findInvitationByLayerIdAndUid(layerId, inviteeUid).toOptional())
                .map(LayerInvitation::getPrivateToken)
                .flatMap(CollectionF::stream)
                .findAny();

        val privateToken = privateTokenOpt
            .orElseThrow(() -> new IllegalStateException("Unable to find invitation for " + inviteeLogin));
        layerManager.handleLayerReply(inviteeUid, privateToken, decision);
    }
}
