const { create, Entity } = require('bem-page-object');

const PO = {};

PO.spinner = new Entity({ block: 'approves', elem: 'spin' });

PO.resourcesTable = new Entity({ block: 'abc-resources-table', elem: 'table' });
PO.resourcesTable.testYPResource = new Entity('[title="loc:VLA-seg:default-cpu:0.918-mem:0-hdd:0-ssd:0-ip4:0-net:0-io_ssd:0-io_hdd:0"]');
PO.resourcesTable.firstRowInTable = new Entity({ block: 'abc-resources-table', elem: 'tr_type_body' }).firstChild();
PO.resourcesTable.firstRowInTable.time = new Entity({ block: 'abc-resources-table', elem: 'td_type_modification-time' });
PO.resourcesTable.firstRowInTable.testServiceRecord = new Entity('[href="/services/autotest-for-resources-requests/"]');
PO.resourcesTable.firstRowInTable.kebabMenu = new Entity({ block: 'abc-resources-table', elem: 'actions' });

PO.resourcesActionsMenu = new Entity('.popup2_visible_yes.abc-resources-table__popup');
PO.resourcesActionsMenu.firstMenuGroup = new Entity({ block: 'menu', elem: 'group' }).firstChild();
PO.resourcesActionsMenu.secondMenuGroup = new Entity({ block: 'menu', elem: 'group' }).nthChild(2);
PO.resourcesActionsMenu.firstMenuGroup.approveButton = new Entity('.menu__item:nth-child(2)');
PO.resourcesActionsMenu.firstMenuGroup.declineButton = new Entity('.menu__item:nth-child(3)');
PO.resourcesActionsMenu.secondMenuGroup.editButton = new Entity('.menu__item:nth-child(1)');
PO.resourcesActionsMenu.secondMenuGroup.watchButton = new Entity('.menu__item:nth-child(2)');

PO.visibleModal = new Entity({ block: 'modal_visible_yes' });
PO.modal = new Entity({ block: 'modal', elem: 'content' });
PO.modal.resourseEditForm = new Entity({ block: 'abc-resource-editor' });
PO.modal.resourseEditForm.spinner = new Entity({ block: 'abc-resource-editor-form', elem: 'spin' });

PO.modal.resourceView = new Entity({ block: 'abc-resource-view' });
PO.modal.resourceView.buttons = new Entity({ block: 'abc-resource-view', elem: 'controls' });
PO.modal.resourceView.buttons.rejectButton = new Entity({ block: 'abc-resource-view', elem: 'reject' });
PO.modal.resourceView.spinner = new Entity({ block: 'abc-resource-view', elem: 'spin' });

PO.resourceSpinner = new Entity({ block: 'resources', elem: 'spin' });

PO.approvesContent = new Entity({ block: 'approves', elem: 'content' });

PO.approvesContent.selectedDirectButton = new Entity('.radio-button__radio_side_left.radio-button__radio_checked_yes');
PO.approvesContent.inheritedButton = new Entity({ block: 'radio-button', elem: 'radio_side_right' });
PO.approvesContent.selectedInheritedButton = new Entity('.radio-button__radio_side_right.radio-button__radio_checked_yes');

PO.approvesTable = new Entity({ block: 'approves', elem: 'table' });
PO.approvesTable.roleRequestForUser = new Entity({ block: 'approves', elem: 'td_type_person' }).nthChild(1);
PO.approvesTable.roleRequestForUser.forRobotAbc004 = new Entity('[href="https://staff.yandex-team.ru/robot-abc-004"]');
PO.approvesTable.declineRoleButton = new Entity({ block: 'approves', elem: 'decline' });
PO.approvesTable.approveRoleButton = new Entity({ block: 'approves', elem: 'approve' });
PO.emptyTable = new Entity({ block: 'approves', elem: 'empty' });
PO.approvesTable.outgoingRequestForDecliningTestService = new Entity('.approves__td_type_service-outgoing [href="/services/autotestservicedeclinemov/"]');
PO.approvesTable.outgoingRequestForAccseptingTestService = new Entity('.approves__td_type_service-outgoing [href="/services/autotestserviceforapprove/"]');
PO.approvesTable.decisionButtons = new Entity({ block: 'approves', elem: 'td_type_approver-incoming' });
PO.approvesTable.decisionButtons.declineButton = new Entity({ block: 'approves', elem: 'decline' });
PO.approvesTable.decisionButtons.approveButton = new Entity({ block: 'approves', elem: 'approve' });

PO.serviceMoveRequest = new Entity({ block: 'service-move-requests' });
PO.serviceMoveRequest.openButton = new Entity({ block: 'service-move-requests-group', elem: 'toggler' });
PO.serviceMoveRequest.requestMoreInfo = new Entity({ block: 'service-move-request', elem: 'footer' });
PO.serviceParentBlock = new Entity({ block: '.abc-service', elem: 'parent' });
PO.serviceParentBlock.testParentLink = new Entity('[href="/services/gov_ua/"]');

PO.serviceTeamSpinner = new Entity({ block: 'loader', elem: 'spinner' });
PO.ROLabel = new Entity({ block: 'service-name-state', elem: 'ro' });

module.exports = create(PO);
