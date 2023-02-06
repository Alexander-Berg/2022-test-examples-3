const { ReactEntity } = require('../../../../../../vendors/hermione');
const { oneOrg, popup } = require('../../../../Companies.test/Companies.page-object/index@desktop');
const { orgPrices } = require('./index@common');

const elems = {
    oneOrg: oneOrg.copy(),
    orgPrices: orgPrices.copy(),
};

elems.orgPrices.priceList = new ReactEntity({ block: 'OrgPriceList' });
elems.oneOrg.orgPrices = orgPrices.copy();
elems.oneOrg.reviewsTitle = new ReactEntity({ block: 'Reviews', elem: 'Title' });
elems.popup = popup.copy();
elems.popup.oneOrg.orgPrices = orgPrices.copy();

module.exports = elems;
