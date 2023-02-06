const { ReactEntity, Entity } = require('../../../../../../vendors/hermione');
const GuestsDropdownPO = require('../../../../../../components/GuestsDropdown/GuestsDropdown.test/GuestsDropdown.page-object/index@desktop.js');
const DatePickerPO = require('../../../../../../components/DatePicker/DatePicker.test/DatePicker.page-object');
const SelectPO = require('../../../../../../components/Select/Select.test/Select.page-object');
const CarouselPricePO = require('../../../../../../components/CarouselPriceRange/CarouselPriceRange.test/CarouselPriceRange.page-object');
const HotelsCarouselListPO = require('../../../HotelsCarouselList/HotelsCarouselList.test/HotelsCarouselList.page-object');

const elems = {};

elems.hotelsCarouselForm = new ReactEntity({ block: 'HotelsCarouselForm-Form' });
elems.hotelsCarouselForm.datePicker = DatePickerPO.datePicker.copy();
elems.hotelsCarouselForm.GuestsDropdown = GuestsDropdownPO.GuestsDropdown.copy();
elems.hotelsCarouselForm.sort = SelectPO.select.copy();
elems.hotelsCarouselForm.carouselPrice = CarouselPricePO.carouselPrice.copy();
elems.hotelsCarouselForm.GuestsDropdown = GuestsDropdownPO.GuestsDropdown.copy();
elems.hotelsCarouselForm.sortPopup = SelectPO.selectPopup.copy();

elems.companiesMapList = HotelsCarouselListPO.companiesMapList.copy();

elems.companiesMapList.progress = new Entity({ block: 'companies-map-list', elem: 'progress' });
elems.companiesMapList.map = new Entity({ block: 'companies-map-list', elem: 'map' });
elems.companiesMapList.map.map2 = new Entity({ block: 'map2' });
elems.companiesMapList.map.map2.closeButton = new Entity({ block: 'map2', elem: 'close' });
elems.companiesMapList.map.map2.pinTitle = new Entity({ block: 'map2', elem: 'pin-title' });

elems.companiesMapList.hotelsFilters = elems.hotelsCarouselForm.copy();

elems.datePickerPopup = DatePickerPO.datePickerPopup.copy();
elems.GuestsDropdownPopup = GuestsDropdownPO.GuestsDropdownPopup.copy();

elems.header = new Entity({ block: 'companies-map-list', elem: 'header' });
elems.header.link = new Entity({ block: 'link' });
elems.openMapBtn = new ReactEntity({ block: 'MapLinks', elem: 'GoToLink' });

module.exports = elems;
