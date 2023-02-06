const { ReactEntity, Entity } = require('../../../../../../vendors/hermione');
const { ModalVisible } = require('../../../../../../components/Modal/Modal.test/Modal.page-object/index@common');
const { OrgContacts } = require('./index@common');

const elems = {};

elems.OrgContacts = OrgContacts.copy();
elems.OrgContacts.PhoneItem = OrgContacts.PhoneItem.copy();
elems.OrgContacts.PhoneItem.More = new ReactEntity({ block: 'OrgContacts', elem: 'PhoneMore' });
elems.OrgContacts.Title = new ReactEntity({ block: 'OrgContacts', elem: 'Title' });
elems.RealtyPopup = ModalVisible.mix(new ReactEntity({ block: 'RealtyPopup' }));
elems.RealtyPopup.Button = new ReactEntity({ block: 'RealtyPopup', elem: 'Phone' });

elems.overlay = new Entity({ block: 'overlay', elem: 'panel', modName: 'opened', modVal: 'yes' });
elems.overlay.navBar = new Entity({ block: 'navigation-bar' });
elems.overlay.navBar.back = new Entity({ block: 'navigation-bar', elem: 'action', modName: 'name', modVal: 'back' });

module.exports = elems;
