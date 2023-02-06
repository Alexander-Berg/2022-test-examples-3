const { ReactEntity } = require('../../../../../../vendors/hermione');
const elems = {};

elems.orgCollections = new ReactEntity({ block: 'OrgCollections' });
elems.orgCollections.item = new ReactEntity({ block: 'OrgCollections', elem: 'Item' });
elems.orgCollections.firstItem = elems.orgCollections.item.copy().firstChild();
elems.orgCollections.secondItem = elems.orgCollections.item.copy().nthChild(2);

module.exports = elems;
