const { ReactEntity, Entity } = require('../../../../../../vendors/hermione');
const StaticMapPO = require('../../../../../../components/StaticMap/StaticMap.test/StaticMap.page-object/index@common');
const { companyCardRichWithOffers } = require('../../../../../../components/CompanyCardRichWithOffers/CompanyCardRichWithOffers.test/CompanyCardRichWithOffers.page-object/index@desktop');
const SelectPO = require('../../../../../../components/Select/Select.test/Select.page-object/index.js');

const elems = {};

elems.HotelsCarouselList = new ReactEntity({ block: 'HotelsCarouselList' });
elems.HotelsCarouselList.Scroller = new ReactEntity({ block: 'Scroller' });
elems.HotelsCarouselList.Scroller.Wrap = new ReactEntity({ block: 'Scroller', elem: 'Wrap' });
elems.HotelsCarouselList.MapCard = StaticMapPO.map.copy();
elems.HotelsCarouselList.HotelCard = companyCardRichWithOffers.copy().mix(new ReactEntity({ block: 'HotelsCarouselList', elem: 'Card' }));
elems.HotelsCarouselList.HotelCard.TopicLink = new ReactEntity({ block: 'Topic', elem: 'Link' });
elems.HotelsCarouselList.HotelCard.PriceValues = new ReactEntity({ block: 'PriceValue' });
elems.HotelsCarouselList.LastHotelCard = elems.HotelsCarouselList.HotelCard.pseudo('last-child');
elems.HotelsCarouselList.Spin = new ReactEntity({ block: 'Spin2' });
elems.HotelsCarouselList.FooterLink = new ReactEntity({ block: 'HotelsCarouselList', elem: 'FooterLink' });

for (let i = 1; i < 10; i++) {
    elems.HotelsCarouselList[`HotelCard-${i}`] = elems.HotelsCarouselList.HotelCard.nthType(i);
}

elems.companiesMapList = new Entity({ block: 'companies-map-list' });
elems.companiesMapList.HotelsCarouselList = elems.HotelsCarouselList.copy();

elems.modal = new Entity({ block: 'modal' });
elems.modal.oneOrg = new Entity({ block: 't-construct-adapter', elem: 'companies' });

elems.hotelsCarouselForm = new ReactEntity({ block: 'HotelsCarouselForm' });
elems.hotelsCarouselForm.servicesSelect = SelectPO.select.copy();
elems.hotelsCarouselForm.servicesPopup = SelectPO.selectPopup.copy();

module.exports = elems;
