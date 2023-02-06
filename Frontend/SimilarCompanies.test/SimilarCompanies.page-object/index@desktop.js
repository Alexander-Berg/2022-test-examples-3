const { Entity } = require('../../../../../../vendors/hermione');
const { oneOrg, hotelOrg, popup } = require('../../../../Companies.test/Companies.page-object/index@desktop');
const { similarCompanies } = require('./index@common');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.similarCompanies = similarCompanies.copy();

elems.hotelOrg = hotelOrg.copy();
elems.hotelOrg.similarCompanies = hotelOrg.tabsPanes.pane.mods({ 't-mod': 'about' })
    .descendant(similarCompanies.copy());

elems.popup = popup.copy();
elems.popup.oneOrg.similarCompanies = similarCompanies.copy();
elems.popup.oneOrg.tabRooms = new Entity({ block: 'tabs-panes', elem: 'pane' }).mods({ 't-mod': 'rooms' });
elems.popup.oneOrg.tabRooms.similarCompanies = similarCompanies.copy();

module.exports = elems;
