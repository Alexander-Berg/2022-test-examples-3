const { ReactEntity } = require('../../../../../../vendors/hermione');

const { uniSearchHeader } = require('../../../../../../components/UniSearchHeader/UniSearchHeader.test/UniSearchHeader.page-object/index@common');
const { extralinksPopup } = require('../../../../../../components/Extralinks/Extralinks.test/Extralinks.page-object/index@common.js');
const { map } = require('../../../../../../components/StaticMap/StaticMap.test/StaticMap.page-object/index@common');
const { hotelListForm, guestPickerPopup, sideBlockCalendar } = require('../../../../Companies.components/HotelListForm/HotelListForm.test/HotelListForm.page-object/index@touch-phone');
const { hotelsListFilters } = require('../../../../Companies.components/HotelsListFilters/HotelsListFilters.test/HotelsListFilters.page-object/index@touch-phone');
const { hotelList } = require('../../../../Companies.components/HotelList/HotelList.test/HotelList.page-object/index@touch-phone');

const elems = {};

elems.hotelsOrgmn = new ReactEntity({ block: 'HotelsOrgmn' });
elems.hotelsOrgmn.header = uniSearchHeader.copy();
elems.hotelsOrgmn.map = map.copy();
elems.hotelsOrgmn.form = hotelListForm.copy();
elems.hotelsOrgmn.filters = hotelsListFilters.copy();
elems.hotelsOrgmn.list = hotelList.copy();
elems.hotelsOrgmn.progress = new ReactEntity({ block: 'HotelsOrgmn', elem: 'Progress' });
elems.hotelsOrgmn.more = new ReactEntity({ block: 'HotelsOrgmn', elem: 'More' });
elems.hotelsOrgmn.footer = new ReactEntity({ block: 'HotelsOrgmn', elem: 'Footer' });
elems.hotelsOrgmn.footer.conditionsLink = new ReactEntity({ block: 'HotelsOrgmn', elem: 'Conditions' });

elems.extralinksPopup = extralinksPopup.copy();
elems.guestPickerPopup = guestPickerPopup.copy();
elems.sideBlockCalendar = sideBlockCalendar.copy();

module.exports = elems;
