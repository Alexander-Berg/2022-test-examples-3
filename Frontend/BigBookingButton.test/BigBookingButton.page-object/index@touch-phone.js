const { ReactEntity } = require('../../../../../../vendors/hermione');
const rootPO = require('../../../../../../../hermione/page-objects/touch-phone');
const formPO = require('../../../../Companies.components/HotelForm/HotelForm.test/HotelForm.page-object/index@touch-phone.js');

const elems = {};

elems.bigBookingButton = new ReactEntity({ block: 'BigBookingButton' });
elems.bigBookingButton.price = new ReactEntity({ block: 'BigBookingButton', elem: 'Price' });

elems.companies = rootPO.companiesComposite.copy();
elems.companies.bigBookingButton = elems.bigBookingButton.copy();
elems.overlayBack = rootPO.overlayPanel.back.copy();
elems.overlay = rootPO.overlayPanel.oneOrgTabs.copy();
elems.overlay.bigBookingButton = elems.bigBookingButton.copy();
elems.overlay.tabsPanes.rooms.form = formPO.hotelForm.copy();
elems.overlay.tabsPanes.rooms.list = new ReactEntity({ block: 'HotelRoomsList' });
elems.sideBlockCalendar = rootPO.sideBlockCalendar.copy();

module.exports = elems;
