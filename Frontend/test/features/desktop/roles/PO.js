const { create, Entity } = require('bem-page-object');
const PO = {};

PO.addTeamMember = new Entity({ block: 'service-team', elem: 'button' }).mods({ role: 'add-users' });

PO.team = new Entity({ block: 'service-team' });
PO.team.allUserPics = new Entity({ block: 'person', elem: 'userpic' });
PO.team.popup = new Entity({ block: 'service-team', elem: 'popup' });
PO.team.header = new Entity({ block: 'service-team', elem: 'header' });
PO.team.lastScope = new Entity({ block: 'service-team-scope' }).lastOfType();
PO.team.lastScope.moreLink = new Entity({ block: 'service-team-scope', elem: 'more-link' });
PO.team.lastScope.scopeLink = new Entity({ block: 'service-team-scope', elem: 'scope-link' });
PO.team.lastScope.allMembers = new Entity({ block: 'service-team-member' });
PO.team.lastScope.lastMember = new Entity({ block: 'service-team-member' }).lastOfType();
PO.team.lastScope.selectedLastMember = new Entity('.service-team-member:last-of-type.service-team-member:hover');
PO.team.lastScope.lastMember.removeButton = new Entity({ block: 'service-team-member', elem: 'button' }).mods({ role: 'remove' });
PO.team.lastScope.lastMember.idmIcon = new Entity({ block: 'icon_type_idm' });
PO.team.lastScope.lastMember.spin = new Entity({ block: 'service-team-member', elem: 'spin' });
PO.team.lastScope.lastMember.userpic = new Entity({ block: 'person', elem: 'userpic' });
PO.team.analyticsScope = new Entity('.service-team-scope[data-name="Аналитика"]');
PO.deleteDepartmentButtons = new Entity({ block: 'service-team-department', elem: 'button_role_remove' });
PO.team.serviceTeamMoreSpinner = new Entity({ block: 'service-team-scope', elem: 'more-spinner' });
PO.team.firstScope = new Entity({ block: 'service-team-scope' }).firstOfType();
PO.team.firstScope.scopeLink = new Entity({ block: 'service-team-scope', elem: 'scope-link' });

PO.team.unapproved = new Entity({ block: 'service-team', elem: 'unapproved' });
PO.team.unapproved.user3370 = new Entity('.username .link[href$="/user3370"]');
PO.team.unapproved.matroskinUser = new Entity('.username .link[href$="/robot-cat-matroskin"]');
PO.team.unapproved.bayunUser = new Entity('.username .link[href$="/robot-cat-bayun"]');
PO.team.unapproved.role = new Entity({ block: '.service-team-member', elem: 'role-text-cell' });
PO.team.unapproved.approveButton = new Entity({ block: 'service-team-member', elem: 'button' }).mods({ role: 'approve' });
PO.team.unapproved.declineButton = new Entity({ block: 'service-team-member', elem: 'button' }).mods({ role: 'decline' });
PO.team.unapproved.approvedIcon = new Entity({ block: 'service-team-member', elem: 'state-icon' }).mods({ state: 'approved' });
PO.unapprovedBlockSpinner = new Entity({ block: 'service-team-member', elem: 'spin' });
PO.team.unapproved.departmentName = new Entity({ block: 'service-team-department', elem: 'title' });

PO.serviceTeamScopes = new Entity({ block: 'service-team', elem: 'scopes' });
PO.serviceTeam = new Entity({ block: 'abc-service', elem: 'team' });

PO.checkedRadioButton = new Entity({ block: 'radio-button', elem: 'radio' }).mods({ checked: 'yes' });
PO.checkedRadioButton.control = new Entity({ block: 'radio-button', elem: 'control' });

PO.teamEditor = new Entity({ block: 'service-team-editor' });
PO.teamEditor.addMemberField = new Entity({ block: 'service-team-editor', elem: 'input' }).mods({ role: 'add' });
PO.teamEditor.addMemberField.control = new Entity({ block: 'input', elem: 'control' });
PO.teamEditor.firstMemberEditor = new Entity({ block: 'service-team-editor-member' }).firstChild();
PO.teamEditor.firstMemberEditor.person = new Entity({ block: 'service-team-editor-member', elem: 'person' });
PO.teamEditor.firstMemberEditor.person.username = new Entity({ block: 'person', elem: 'username' });
PO.teamEditor.firstMemberEditor.role = new Entity({ block: 'service-team-editor-member', elem: 'role' });
PO.teamEditor.firstMemberEditor.role.control = new Entity({ block: 'input', elem: 'control' });
PO.teamEditor.firstMemberEditor.role.clear = new Entity({ block: 'input', elem: 'clear' });
PO.teamEditor.expiration = new Entity({ block: 'service-team-editor', elem: 'expiration' });
PO.teamEditor.expiration.checkedOption = PO.checkedRadioButton;
PO.teamEditor.submit = new Entity({ block: 'service-team-editor', elem: 'button' }).mods({ role: 'add' });
PO.teamEditor.rolesWarning = new Entity({ block: 'service-team-editor__warning' }).firstChild();
PO.teamEditor.accessRoleIcon = new Entity({ block: 'AccessRoles-Item' });
PO.teamEditor.departmentEditor = new Entity({ block: 'service-team-editor', elem: 'departments' });
PO.teamEditor.departmentEditor.firstDepartment = new Entity({ block: 'service-team-editor-department' }).firstChild();
PO.teamEditor.departmentEditor.firstDepartment.depName = new Entity({
    block: 'service-team-editor-department', elem: 'name' });
PO.teamEditor.departmentEditor.firstDepartment.control = new Entity({
    block: 'service-team-editor-department', elem: 'controls',
});
PO.teamEditor.departmentEditor.firstDepartment.control.input = new Entity('input');
PO.teamEditor.departmentEditor.firstDepartment.control.role = new Entity({
    block: 'service-team-editor-department', elem: 'role',
});

PO.popupElem = new Entity({ block: 'input', elem: 'popup' });
PO.visiblePopup = new Entity({ block: 'popup' }).mods({ visibility: 'visible' }).mix(PO.popupElem);
PO.visiblePopup.firstStaffItem = new Entity({ block: 'b-autocomplete-item' }).mods({ type: 'staff' }).firstChild();
PO.visiblePopup.firstDepItem = new Entity({ block: 'b-autocomplete-item' }).mods({ type: 'department' }).firstChild();
PO.visiblePopup.firstStaffItem.username = new Entity({ block: 'm-username' });
PO.visiblePopup.firstDepartmentItem = new Entity({
    block: 'b-autocomplete-item' }).mods({ type: 'department' }).firstChild();
PO.visiblePopup.firstDepartmentItem.departmentName = new Entity({ block: 'department-name' });
PO.visiblePopup.roleItem = new Entity({ block: 'b-autocomplete-item' }).mods({ type: 'role' });
PO.visiblePopup.newRoleButton = new Entity({ block: 'b-autocomplete-item' }).mods({ type: 'new-role' });
PO.visiblePopup.analystRole = new Entity('.b-autocomplete-item[data-data*="Analyst"]');

PO.popup2Elem = new Entity({ block: 'popup2' });
PO.visiblePopup2 = PO.popup2Elem.mods({ visible: 'yes' });
PO.visiblePopup2.content = new Entity({ block: 'tooltip__content' });
PO.select2Popup = PO.popup2Elem.mix({ block: 'select2', elem: 'popup' });

PO.newRolePopup = PO.visiblePopup2.mix(new Entity({ block: 'input', elem: 'new-role-popup' }));

PO.newRoleEditor = new Entity({ block: 'abc-new-role' });
PO.newRoleEditor.input = new Entity({ block: 'abc-new-role', elem: 'input' });
PO.newRoleEditor.scope = PO.newRoleEditor.input.mods({ role: 'scope' });
PO.newRoleEditor.scope.selectButton = new Entity({ block: 'select2', elem: 'button' });
PO.newRoleEditor.nameRu = PO.newRoleEditor.input.mods({ role: 'name-ru' });
PO.newRoleEditor.nameRu.control = new Entity({ block: 'textinput', elem: 'control' });
PO.newRoleEditor.nameEn = PO.newRoleEditor.input.mods({ role: 'name-en' });
PO.newRoleEditor.nameEn.control = new Entity({ block: 'textinput', elem: 'control' });
PO.newRoleEditor.code = PO.newRoleEditor.input.mods({ role: 'code' });
PO.newRoleEditor.code.control = new Entity({ block: 'textinput', elem: 'control' });
PO.newRoleEditor.submit = new Entity({ block: 'abc-new-role', elem: 'submit' });

module.exports = create(PO);
