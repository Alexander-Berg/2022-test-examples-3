const { Entity } = require('../../../../../../vendors/hermione');
const { popupVisible } = require('../../../../../../components/Popup/Popup.test/Popup.page-object/index@common');
const { oneOrg } = require('../../../../Companies.test/Companies.page-object/index@desktop');
const { OrgContacts } = require('./index@common');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.OrgContacts = OrgContacts.copy();
elems.RealtyPopup = popupVisible.copy();
elems.oneOrgModal = new Entity({ block: 'modal', modName: 'visible', modVal: 'yes' })
    .descendant(new Entity({ block: 'adaptive-org' }));
elems.oneOrgModal.mainTab = new Entity({ block: 'tabs-menu', elem: 'tab', elemMods: { name: 'about' } });
elems.oneOrgModal.about = new Entity({ block: 'tabs-panes', elem: 'pane', modName: 't-mod', modVal: 'about' });
elems.oneOrgModal.about.OrgContacts = OrgContacts.copy();

module.exports = elems;
