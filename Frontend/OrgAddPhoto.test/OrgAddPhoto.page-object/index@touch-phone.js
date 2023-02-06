const { ReactEntity } = require('../../../../../../vendors/hermione');
const { oneOrg, overlayOneOrg } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');
const { scroller } = require('../../../../../../../hermione/page-objects/touch-phone/blocks');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.scroller = scroller.copy();
elems.oneOrg.OrgAddPhoto = new ReactEntity({ block: 'OrgAddPhoto' });
elems.overlayOneOrg = overlayOneOrg.copy();
elems.overlayOneOrg.OrgAddPhoto = new ReactEntity({ block: 'OrgAddPhoto' });

module.exports = elems;
