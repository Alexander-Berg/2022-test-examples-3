const { ReactEntity, Entity } = require('../../../../../../vendors/hermione');
const { oneOrg, overlayOneOrg } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');
const { reviews } = require('../../../../../Reviews/Reviews.test/Reviews.page-object/index@common');
const { similarCompanies } = require('./index@common');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.similarCompanies = similarCompanies.copy();

elems.overlayOneOrg = overlayOneOrg.copy();
elems.overlayOneOrg.similarCompanies = similarCompanies.copy();
elems.overlayOneOrg.visitsHistogram = new ReactEntity({ block: 'OrgVisitsHistogram' });
elems.overlayOneOrg.tabRooms = new Entity({ block: 'tabs-panes', elem: 'pane' }).mods({ 't-mod': 'rooms' });
elems.overlayOneOrg.tabRooms.similarCompanies = similarCompanies.copy();
elems.overlayOneOrg.tabAbout = new Entity({ block: 'tabs-panes', elem: 'pane' }).mods({ 't-mod': 'about' });
elems.overlayOneOrg.tabAbout.similarCompanies = similarCompanies.copy();
elems.overlayOneOrg.reviewsList = reviews.list.copy();

module.exports = elems;
