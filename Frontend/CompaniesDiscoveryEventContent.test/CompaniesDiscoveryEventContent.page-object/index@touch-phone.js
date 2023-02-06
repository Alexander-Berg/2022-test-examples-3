const { ReactEntity } = require('../../../../../../vendors/hermione');

const elems = {};

elems.CompaniesDiscoveryEventContent = new ReactEntity({ block: 'CompaniesDiscoveryEventContent' });
elems.CompaniesDiscoveryEventContent.Map = new ReactEntity({ block: 'CompaniesDiscoveryEventContent', elem: 'Map' });
elems.CompaniesDiscoveryEventContent.Button = new ReactEntity({ block: 'CompaniesDiscoveryEventContent', elem: 'Button' });

module.exports = elems;
