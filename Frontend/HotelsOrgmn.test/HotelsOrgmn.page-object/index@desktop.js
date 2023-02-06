const El = require('@yandex-int/bem-page-object').Entity;
const { ReactEntity } = require('../../../../../../vendors/hermione');
const { modalPopup } = require('../../../../../../../hermione/page-objects/desktop/index.js');
const { hotelsReactFilters } = require('../../../OrgHotelOffers/OrgHotelOffers.test/OrgHotelOffers.page-object')('desktop');
const { companyCardRichWithOffers } = require('../../../../../../components/CompanyCardRichWithOffers/CompanyCardRichWithOffers.test/CompanyCardRichWithOffers.page-object/index@desktop.js');
const HotelListFormPO = require('../../../../Companies.components/HotelListForm/HotelListForm.test/HotelListForm.page-object/index@desktop.js');
const { datePickerPopup } = require('../../../../../../components/DatePicker/DatePicker.test/DatePicker.page-object/index@desktop.js');
const { map } = require('../../../../../../components/DynamicMap/DynamicMap.test/DynamicMap.page-object/index.js');
const elems = {};

elems.hotelsOrgmn = new ReactEntity({ block: 'HotelsOrgmn' });
elems.hotelsOrgmn.footerLink = new ReactEntity('').child(new ReactEntity({ block: 'Composite' }))
    .child(new ReactEntity({ block: 'Composite', elem: 'Item' })).lastChild()
    .child(new ReactEntity({ block: 'Link' }));

elems.hotelsOrgmn.hotelListForm = HotelListFormPO.hotelListForm.copy();
elems.hotelsOrgmn.hotelCard = companyCardRichWithOffers.copy();

elems.hotelsOrgmn.firstCard = elems.hotelsOrgmn.hotelCard.nthChild(1);
elems.hotelsOrgmn.secondCard = elems.hotelsOrgmn.hotelCard.nthChild(2);
elems.hotelsOrgmn.spin = new ReactEntity({ block: 'Spin2' });
elems.hotelsOrgmn.hotelCardsContainer = new ReactEntity({ block: 'Scroller', elem: 'Wrap' });

elems.popup = modalPopup.copy();
elems.popup.hotelsFilters = hotelsReactFilters.copy();

elems.hotelsOrgmn.map = map.copy();

elems.hotelsOrgmn.map.firstPin = map.pin.nthChild(1);
// Определяет активный пин
elems.hotelsOrgmn.map.pin.activeIconLayout = new ReactEntity({ block: 'HotelIconLayout' }).mods({ active: true });

elems.datePickerPopup = datePickerPopup.copy();
elems.guestsDropdownPopup = HotelListFormPO.guestsDropdownPopup.copy();

elems.feedback = new El({ block: 'feedback' });

module.exports = elems;
