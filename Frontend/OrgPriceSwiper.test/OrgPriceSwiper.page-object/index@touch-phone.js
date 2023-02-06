const { Entity } = require('../../../../../../vendors/hermione');
const { oneOrg, overlayOneOrg, swiperModal } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');
const { orgPrices } = require('../../../OrgPrices/OrgPrices.test/OrgPrices.page-object/index@touch-phone');

const elems = {};

elems.supplyReactBus = new Entity({ block: 'supply', elem: 'react-bus' });

elems.oneOrg = oneOrg.copy();
elems.oneOrg.orgPrices = orgPrices.copy();
elems.overlayOneOrg = overlayOneOrg.copy();

elems.swiperModal = swiperModal.copy();

module.exports = elems;
