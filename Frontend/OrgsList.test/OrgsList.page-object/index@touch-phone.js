const { ReactEntity, Entity } = require('../../../../../../vendors/hermione');
const { orgRelated } = require('../../../../../../components/OrgRelated/OrgRelated.react-test/OrgRelated.page-object/index@touch-phone');
const { overlayOneOrg } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');

const elems = {};

elems.OrgsList = new ReactEntity({ block: 'OrgsList' });
elems.OrgsList.Item = new ReactEntity({ block: 'OrgsList', elem: 'Item' });
elems.OrgsList.Item.OverlayHandler = new ReactEntity({ block: 'OrgMinibadge', elem: 'OverlayHandler' });
elems.OrgsList.Item.PhoneButton = new ReactEntity({ block: 'OrgMinibadge', elem: 'PhoneButton' });
elems.OrgsList.Item.SiteButton = new ReactEntity({ block: 'OrgMinibadge', elem: 'SiteButton' });
elems.OrgsList.Item.Content = new ReactEntity({ block: 'OrgMinibadge', elem: 'Content' });
elems.OrgsList.FirstItem = elems.OrgsList.Item.nthChild(1);
elems.OrgsList.SecondItem = elems.OrgsList.Item.nthChild(2);
elems.OrgsList.ThirdItem = elems.OrgsList.Item.nthChild(3);
elems.OrgsList.lastItem = elems.OrgsList.Item.pseudo('last-of-type');
elems.OrgsList.more = new ReactEntity({ block: 'OrgsList', elem: 'More' });
elems.OrgsList.orgRelated = orgRelated.copy();

elems.overlay = new Entity({ block: 'overlay' });
elems.overlay.content = new Entity({ block: 'overlay', elem: 'content' });

elems.overlayOneOrg = overlayOneOrg.copy();

module.exports = elems;
