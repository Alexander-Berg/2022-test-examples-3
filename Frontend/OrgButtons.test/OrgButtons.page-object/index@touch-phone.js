const { Entity, ReactEntity } = require('../../../../../../vendors/hermione');
const { OrgContacts } = require('../../../OrgContacts/OrgContacts.test/OrgContacts.page-object/index@touch-phone');
const { oneOrg } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');

const wideButton = new Entity({ block: 'button2', modName: 'width', modVal: 'max' });
const wideAdvertButton = new Entity({ block: 'button2', modName: 't-mod', modVal: 'advert' }).mix(wideButton);

const buttons = new Entity({ block: 'serp-item-actions' });
buttons.phone = new Entity({ block: 'button2', modName: 't-mod', modVal: 'phone' });
buttons.realtyPhone = new Entity({ block: 'covered-phone' });
buttons.reviews = new Entity({ block: 'button2', modName: 't-mod', modVal: 'reviews' });
buttons.route = new Entity({ block: 'button2', modName: 't-mod', modVal: 'route' });
buttons.site = new Entity({ block: 'button2', modName: 't-mod', modVal: 'site' });

const elems = {};

elems.bcardSideBlock = new Entity({ block: 'side-block-bcard' });
elems.bcardSideBlock.contacts = OrgContacts.copy();
elems.bcardSideBlock.buttons = buttons.copy();
elems.bcardSideBlock.wideAdvertButton = wideAdvertButton.copy();

elems.oneOrg = oneOrg.copy();
elems.oneOrg.buttons = buttons.copy();
elems.oneOrg.wideAdvertButton = wideAdvertButton.copy();
elems.oneOrg.wideAirportButton = new Entity({ block: 'button2', modName: 't-mod', modVal: 'airport' }).mix(wideButton);
elems.oneOrg.wideDeliveryButton = new Entity({ block: 'button2', modName: 't-mod', modVal: 'delivery' }).mix(wideButton);
elems.oneOrg.favoriteButton = new Entity({ block: 'favorite-button', elem: 'button' });
elems.oneOrg.wideBookingButton = new ReactEntity({ block: 'BigBookingButton' });

elems.oneOrg.contacts = OrgContacts.copy();

module.exports = elems;
