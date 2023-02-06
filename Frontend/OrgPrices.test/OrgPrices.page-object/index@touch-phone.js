const { ReactEntity } = require('../../../../../../vendors/hermione');
const { oneOrg, overlayOneOrg } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');
const { orgPrices } = require('./index@common');

const elems = {
    oneOrg: oneOrg.copy(),
    orgPrices: orgPrices.copy(),
};

elems.orgPrices.priceList = new ReactEntity({ block: 'OrgPhotoPriceList' });
elems.oneOrg.orgPrices = orgPrices.copy();
elems.overlayOneOrg = overlayOneOrg.copy();
elems.overlayOneOrg.orgPrices = orgPrices.copy();

module.exports = elems;
