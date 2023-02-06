const { ReactEntity } = require('../../../../../../vendors/hermione');

const elems = {};

elems.OrgContacts = new ReactEntity({ block: 'OrgContacts' });
elems.OrgContacts.AddressItem = new ReactEntity({ block: 'OrgContacts', elem: 'Item' }).mods({ type: 'address' });
elems.OrgContacts.AddressItem.Link = new ReactEntity({ block: 'Link' });
elems.OrgContacts.PhoneItem = new ReactEntity({ block: 'OrgContacts', elem: 'Item' }).mods({ type: 'phone' });
elems.OrgContacts.PhoneItem.Root = new ReactEntity({ block: 'Root' });
elems.OrgContacts.PhoneItem.Text = new ReactEntity({ block: 'OrgContacts', elem: 'ItemText' });
elems.OrgContacts.PhoneItem.Phone = new ReactEntity({ block: 'OrgContacts', elem: 'Phone' });
elems.OrgContacts.PhoneItem.Link = new ReactEntity({ block: 'Link' });
elems.OrgContacts.PhoneItem.More = new ReactEntity({ block: 'OrgContacts', elem: 'PhoneMore' });
elems.OrgContacts.Phone = new ReactEntity({ block: 'OrgContacts', elem: 'Phone' });
elems.OrgContacts.SiteItem = new ReactEntity({ block: 'OrgContacts', elem: 'Item' }).mods({ type: 'site' });
elems.OrgContacts.SiteItem.Link = new ReactEntity({ block: 'Link' });
elems.OrgContacts.MetroItem = new ReactEntity({ block: 'OrgContacts', elem: 'Item' }).mods({ type: 'metro' });

module.exports = elems;
