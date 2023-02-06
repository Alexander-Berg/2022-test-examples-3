const { Entity, ReactEntity } = require('../../../../../../vendors/hermione');
const { oneOrgSection } = require('../../../../Companies.components/OneOrgSection/OneOrgSection.test/OneOrgSection.page-object/index@common');
const { oneOrg, overlayOneOrg } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');
const { textFeatures } = require('./index@common');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.textFeatures = textFeatures.copy();

elems.overlayOneOrg = overlayOneOrg.copy();
elems.overlayOneOrg.textFeatures = textFeatures.copy();
elems.overlayOneOrg.textFeatures.title = oneOrgSection.title.mix({ block: 'TextFeatures' });
elems.overlayOneOrg.textFacts = textFeatures.mods({ type: 'full' });
elems.overlayOneOrg.contacts = new ReactEntity({ block: 'OrgContacts' });
elems.overlayOneOrg.realtyOffers = new ReactEntity({ block: 'RealtyOffers' });

elems.overlayIframe = new Entity({ block: 'overlay-iframe' });

module.exports = elems;
