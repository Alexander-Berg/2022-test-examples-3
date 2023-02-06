const { Entity, ReactEntity } = require('../../../../../../vendors/hermione');
const { hotelOfferLite } = require('../../../../Companies.components/HotelOfferLite/HotelOfferLite.test/HotelOfferLite.page-object/index@common');

const oneOrg = new Entity({ block: 'serp-item' }).descendant(new Entity({ block: 'composite' }).mods({ 't-mod': '1org' }));

oneOrg.hotelFilters = new Entity({ block: 'hotels-filters' });
oneOrg.hotelFilters.submit = new Entity({ block: 'hotels-filters', elem: 'submit' });
oneOrg.list = new ReactEntity({ block: 'OrgHotelOffers' });
oneOrg.list.offer = hotelOfferLite.copy();

const overlayPanel = new Entity({ block: 'overlay', elem: 'panel', modName: 'opened', modVal: 'yes' });
overlayPanel.back = new Entity({ block: 'navigation-bar', elem: 'action', modName: 'name', modVal: 'back' });
overlayPanel.oneOrgTabs = new Entity({ block: 'one-org-tabs', modName: 'type', modVal: 'sideblock' });
overlayPanel.tabsPanes = new Entity({ block: 'tabs-panes' });
overlayPanel.tabsPanes.pane = new Entity({ block: 'tabs-panes', elem: 'pane' });
overlayPanel.tabsPanes.rooms = overlayPanel.tabsPanes.pane.mods({ 't-mod': 'rooms' });
overlayPanel.tabsPanes.rooms.list = new Entity({ block: 'hotels-form', elem: 'list' });

module.exports = {
    oneOrg,
    overlayPanel,
};
