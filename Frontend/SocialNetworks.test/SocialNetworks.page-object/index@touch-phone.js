const { ReactEntity } = require('../../../../../../vendors/hermione');
const { oneOrg } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');

const elems = {};

elems.OrgSocialNetworks = new ReactEntity({ block: 'OrgSocialNetworks' });
elems.OrgSocialNetworks.item = new ReactEntity({ block: 'OrgSocialNetworks', elem: 'Item' });
elems.OrgSocialNetworks.firstItem = elems.OrgSocialNetworks.item.firstChild();

elems.oneOrg = oneOrg.copy();
elems.oneOrg.OrgSocialNetworks = elems.OrgSocialNetworks.copy();

module.exports = elems;
