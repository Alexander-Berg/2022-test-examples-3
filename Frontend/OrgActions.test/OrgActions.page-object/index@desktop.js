const { Entity, ReactEntity } = require('../../../../../../vendors/hermione');
const { frame } = require('../../../../../../../hermione/page-objects/common/blocks');
const { oneOrg: oneOrgCommon, oneOrgLeft: oneOrgLeftBase, popup } = require('../../../../Companies.test/Companies.page-object/index@desktop');
const { popupVisible } = require('../../../../../../components/Popup/Popup.test/Popup.page-object/index@common');

const buttons = new ReactEntity({ block: 'OrgActions' });
buttons.advert = new ReactEntity({ block: 'OrgActions', elem: 'Item', modName: 't-mod', modVal: 'advert' });
buttons.airport = new ReactEntity({ block: 'OrgActions', elem: 'Item', modName: 't-mod', modVal: 'airport' });
buttons.delivery = new ReactEntity({ block: 'OrgActions', elem: 'Item', modName: 't-mod', modVal: 'delivery' });
buttons.site = new ReactEntity({ block: 'OrgActions', elem: 'Item', modName: 't-mod', modVal: 'site' });
buttons.route = new ReactEntity({ block: 'OrgActions', elem: 'Item', modName: 't-mod', modVal: 'route' });
buttons.reviews = new ReactEntity({ block: 'OrgActions', elem: 'Item', modName: 't-mod', modVal: 'reviews' });
buttons.realty = new ReactEntity({ block: 'OrgActions', elem: 'Item', modName: 't-mod', modVal: 'realty-phone' });
buttons.booking = new ReactEntity({ block: 'OrgActions', elem: 'Item', modName: 't-mod', modVal: 'booking' });

const elems = { buttons };

elems.oneOrg = new Entity({ block: 'serp-list', modName: 'right', modVal: 'yes' })
    .descendant(oneOrgCommon);
elems.oneOrg.buttons = buttons.copy();
elems.oneOrgLeft = oneOrgLeftBase.copy();
elems.oneOrgLeft.buttons = buttons.copy();

elems.modal = popup.copy();
elems.modal.oneOrg.buttons = buttons.copy();
elems.frame = frame.copy();

elems.realtyPopup = popupVisible.mix(new ReactEntity({ block: 'RealtyPopup' }));

elems.bookingModal = new ReactEntity({ block: 'OrgBookingModal' });
elems.bookingModal.bookingIframe = new ReactEntity({ block: 'OrgBookingModal', elem: 'Iframe' });
elems.bookingModal.close = new ReactEntity({ block: 'OrgBookingModal', elem: 'Close' });

module.exports = elems;
