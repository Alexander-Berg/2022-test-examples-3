const { create, ReactEntity } = require('../../../../../../vendors/hermione');

const elems = {};

elems.OrgSkiListPP = new ReactEntity({ block: 'OrgSkiListPP' });
elems.OrgSkiListPPItem = new ReactEntity({ block: 'OrgSkiListPP', elem: 'Item' });
elems.OrgSkiListPPItem1 = elems.OrgSkiListPPItem.nthChild(1);
elems.OrgSkiListPPItem1.CollapserLabel = new ReactEntity({ block: 'Collapser', elem: 'Label' });
elems.OrgSkiListPPItem3 = elems.OrgSkiListPPItem.nthChild(3);
elems.OrgSkiListPPItem4 = elems.OrgSkiListPPItem.nthChild(4);
elems.OrgSkiListPPItem4.CollapserContent = new ReactEntity({ block: 'Collapser', elem: 'Content' });
elems.OrgSkiListPP.PhoneMore = new ReactEntity({ block: 'OrgContacts', elem: 'PhoneMore' });
elems.OrgSkiListPP.BuyButton = new ReactEntity({ block: 'OrgSkiPass', elem: 'BuyButton' });
module.exports = create(elems);
