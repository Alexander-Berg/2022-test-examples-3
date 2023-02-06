const { ReactEntity } = require('../../../../../../vendors/hermione');
const { oneOrg, overlayOneOrg } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');

const elems = {};

elems.OrgAbout = new ReactEntity({ block: 'OrgAbout' });
elems.OrgAbout.more = new ReactEntity({ block: 'OrgAbout', elem: 'More' });

elems.oneOrg = oneOrg.copy();
elems.oneOrg.OrgAbout = elems.OrgAbout.copy();

elems.overlayOneOrg = overlayOneOrg.copy();
elems.overlayOneOrg.OrgAbout = elems.OrgAbout.copy();

module.exports = elems;
