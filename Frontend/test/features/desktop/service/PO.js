const inherit = require('inherit');
const { create, Entity } = require('bem-page-object');

const PO = {};
const ReactEntity = inherit(Entity, null, { preset: 'react' });

PO.bpage = new Entity({ block: 'b-page' });
PO.bpage.content = new Entity({ block: 'b-page', elem: 'content' });
PO.service = new Entity({ block: 'abc-service' });

PO.service.onDuty = new Entity({ block: 'OnDuty' });
PO.service.onDuty.loading = PO.service.onDuty.mods({ loading: 'true' });
PO.service.onDuty.cutRedrawPending = new ReactEntity({ block: 'Cut', redrawpending: true });

const input = new Entity({ block: 'input' });
const popup = new Entity({ block: 'popup' });
const popupVisible = popup.mods({ visibility: 'visible' });

input.control = new Entity({ block: 'input', elem: 'control' });
input.popup = new Entity({ block: 'input', elem: 'popup' }).mix(popupVisible);
input.popup.items = new Entity({ block: 'input', elem: 'popup-items' });
input.popup.items.first = new Entity({ block: 'b-autocomplete-item' }).firstChild();

PO.popup = new Entity({ block: 'popup', elem: 'control' });

PO.serviceManagers = new Entity({ block: 'service-responsible' });
PO.serviceManagers.spin = new Entity({ block: 'service-responsible', elem: 'spin' });
PO.serviceManagers.users = new Entity({ block: 'service-responsible', elem: 'users' });
PO.serviceManagers.editButton = new Entity({ block: 'service-responsible', elem: 'change' });
PO.serviceManagersModal = new Entity({ block: 'service-responsible', elem: 'modal' });
PO.serviceManagersModal.content = new Entity({ block: 'modal', elem: 'content' });

PO.operationalLink = new Entity({ block: 'OperationalLink' });
PO.operationalLink.spin = new Entity({ block: 'Spin2' });

PO.serviceManagersEditor = new Entity({ block: 'service-responsible-editor' });
PO.serviceManagersEditor.submitButton = new Entity({ block: 'service-responsible-editor', elem: 'submit' });
PO.serviceManagersEditor.item = new Entity({ block: 'service-responsible-editor', elem: 'item' });

PO.serviceManagersEditor.users = new Entity({ block: 'service-responsible-editor', elem: 'layout-group' }).mods({ role: 'users' });
PO.serviceManagersEditor.users.itemFirst = new Entity({ block: 'service-responsible-editor', elem: 'item' }).firstOfType();
PO.serviceManagersEditor.users.itemFirst.removeLink = new Entity({ block: 'service-responsible-member', elem: 'button' }).mods({ role: 'remove' });

PO.serviceManagersEditor.addForm = new Entity({ block: 'service-responsible-editor', elem: 'link-form' });
PO.serviceManagersEditor.addForm.input = input;

PO.inputPopup = input.popup;

PO.serviceTeamScope = new Entity({ block: 'service-team-scope' });
PO.serviceTeam = new Entity({ block: 'abc-service', elem: 'team' });
PO.serviceTeamHeadSpin = new Entity({ block: 'service-team', elem: 'head-spin' });
PO.serviceTeamTitle = new Entity({ block: 'service-team', elem: 'title' });
PO.serviceTeamTitle.spin = new Entity({ block: 'statuses-spin' }).mods({ loading: 'yes' });

PO.service.resources = new Entity({ block: 'abc-service', elem: 'resources' });
PO.service.resources.robot = new Entity({ block: 'service-resources', elem: 'resource' }).firstOfType();
PO.service.resources.robot.requestLink = new Entity({ block: 'service-resources', elem: 'resource-link' });

PO.resourceRequestForm = new Entity({ block: 'resource-editor' });

PO.serviceTeamScopes = new Entity({ block: 'service-team', elem: 'scopes' });

PO.serviceDescription = new Entity({ block: 'abc-service', elem: 'description' });
PO.serviceDescription.content = new Entity({ block: 'service-description', elem: 'content' });
PO.serviceDescription.emptyDescription = new Entity({ block: 'service-description', elem: 'empty' });
PO.serviceDescription.header = new Entity({ block: 'service-description', elem: 'header' });
PO.serviceDescription.header.editButton = new Entity({ block: 'service-description', elem: 'edit' });

PO.serviceActivity = new Entity({ block: 'abc-service', elem: 'activity' });
PO.serviceActivity.content = new Entity({ block: 'service-activity', elem: 'content' });

PO.infrastructureBlock = new Entity({ block: 'abc-service', elem: 'infrastructure' });
PO.infrastructureBlock.content = new Entity({ block: 'service-infrastructure', elem: 'content' });
PO.infrastructureBlock.emptyBlock = new Entity({ block: 'service-infrastructure', elem: 'empty' });
PO.infrastructureBlock.icons = new Entity({ block: 'service-infrastructure', elem: 'icon' });
PO.infrastructureBlock.warden = new Entity({ block: 'service-infrastructure', elem: 'contact_type_warden-url' });
PO.infrastructureBlock.warden.url = new Entity('a');
PO.infrastructureBlock.spiChat = new Entity({ block: 'service-infrastructure', elem: 'contact_type_spi-chat' });
PO.infrastructureBlock.spiChat.url = new Entity('a');
PO.infrastructureBlock.infra = new Entity({ block: 'service-infrastructure', elem: 'contact_type_infra-preset' });
PO.infrastructureBlock.infra.url = new Entity('a');
PO.infrastructureBlock.arcadia = new Entity({ block: 'service-infrastructure', elem: 'contact_type_arcadia-url' });
PO.infrastructureBlock.arcadia.frontUrl = new Entity('a[href*="frontend/services/abc/"]');
PO.infrastructureBlock.arcadia.backUrl = new Entity('a[href*="/intranet/plan/"]');

module.exports = create(PO);
