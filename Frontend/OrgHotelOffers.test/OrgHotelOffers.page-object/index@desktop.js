const { Entity, ReactEntity } = require('../../../../../../vendors/hermione');
const { hotelOfferLite } = require('../../../../Companies.components/HotelOfferLite/HotelOfferLite.test/HotelOfferLite.page-object/index@common');
const button2 = new ReactEntity({ block: 'Button2' });

const hotelsReactFormDatePickerControl = new ReactEntity({ block: 'HotelForm', elem: 'FirstBlock' })
    .descendant(new ReactEntity({ block: 'DatePicker' }))
    .descendant(new ReactEntity({ block: 'DatePickerControl' }));
const hotelsReactFormGuestsDropdown = new ReactEntity({ block: 'HotelForm', elem: 'LastBlock' })
    .descendant(new ReactEntity({ block: 'GuestsDropdown' }));

const hotelsReactFilters = new ReactEntity({ block: 'HotelForm' });
hotelsReactFilters.submit = new ReactEntity({ block: 'HotelForm', elem: 'Submit' });
hotelsReactFilters.dateAt = hotelsReactFormDatePickerControl.nthType(1).descendant(button2.copy());
hotelsReactFilters.dateTo = hotelsReactFormDatePickerControl.nthType(2).descendant(button2.copy());
hotelsReactFilters.personDropdown = hotelsReactFormGuestsDropdown.descendant(button2.copy());

const hotelsReactForm = new ReactEntity({ block: 'OrgHotelOffers' });
hotelsReactForm.list = new ReactEntity({ block: 'OrgHotelOffers', elem: 'List' });
hotelsReactForm.list.item = hotelOfferLite.copy();
hotelsReactForm.list.first = hotelsReactForm.list.item.nthType(1);
hotelsReactForm.list.second = hotelsReactForm.list.item.nthType(2);
hotelsReactForm.all = new ReactEntity({ block: 'OrgHotelOffers', elem: 'More' });
hotelsReactForm.filters = hotelsReactFilters.copy();

const composite = new Entity({ block: 'composite' });
const hotelsFormTitle = composite.mods({ 't-mod': 'hotel-offers-results' })
    .descendant(new ReactEntity({ block: 'OneOrgSection', elem: 'Title' }));

const OrgHotelOffersList = new ReactEntity({ block: 'OrgHotelOffers', elem: 'List' });
OrgHotelOffersList.offer = hotelOfferLite.copy();
OrgHotelOffersList.firstOffer = hotelOfferLite.nthType(1);

module.exports = {
    hotelsReactFilters, hotelsReactForm, hotelsFormTitle,
};
