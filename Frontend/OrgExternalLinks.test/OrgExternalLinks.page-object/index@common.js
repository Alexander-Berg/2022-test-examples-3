const { ReactEntity } = require('../../../../../../vendors/hermione');

const elems = {};

elems.OrgExternalLinks = new ReactEntity({ block: 'OrgExternalLinks' });
elems.OrgExternalLinks.item = new ReactEntity({ block: 'OrgExternalLinks', elem: 'Item' });
elems.OrgExternalLinks.secondItem = elems.OrgExternalLinks.item.nthType(2);

module.exports = elems;
