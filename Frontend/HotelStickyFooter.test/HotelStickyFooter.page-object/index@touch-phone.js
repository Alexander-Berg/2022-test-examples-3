const { ReactEntity } = require('../../../../../../vendors/hermione');

const elems = {};

elems.hotelStickyFooter = new ReactEntity({ block: 'HotelStickyFooter' });
elems.hotelStickyFooter.text = new ReactEntity({ block: 'HotelStickyFooter', elem: 'Text' });

module.exports = elems;
