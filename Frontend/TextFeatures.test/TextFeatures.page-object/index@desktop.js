const { oneOrg, hotelOrg, popup } = require('../../../../Companies.test/Companies.page-object/index@desktop');
const { textFeatures } = require('./index@common');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.textFeatures = textFeatures.copy();

elems.hotelOrg = hotelOrg.copy();
elems.hotelOrg.textFeatures = textFeatures.copy();

elems.popup = popup.copy();
elems.popup.oneOrg.textFeatures = textFeatures.copy();
elems.popup.oneOrg.textFeaturesOnlyUrls = textFeatures.mods({ onlyUrls: true });

module.exports = elems;
