const { ReactEntity } = require('../../../../../../vendors/hermione');

const elems = {};

elems.orgPrices = new ReactEntity({ block: 'OrgPrices' });
elems.orgPrices.title = new ReactEntity({ block: 'OrgPrices', elem: 'Title' });
elems.orgPrices.list = new ReactEntity({ block: 'OrgPriceList' });
elems.orgPrices.more = new ReactEntity({ block: 'OrgPrices', elem: 'More' });

module.exports = elems;
