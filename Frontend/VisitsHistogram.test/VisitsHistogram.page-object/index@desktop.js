const { oneOrg, popup } = require('../../../../Companies.test/Companies.page-object/index@desktop');
const { visitsHistogram } = require('./index@common');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.visitsHistogram = visitsHistogram.copy();

elems.popup = popup.copy();
elems.popup.oneOrg.visitsHistogram = elems.oneOrg.visitsHistogram.copy();

module.exports = elems;
