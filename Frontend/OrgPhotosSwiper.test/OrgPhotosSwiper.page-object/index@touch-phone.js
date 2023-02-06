const { Entity, ReactEntity } = require('../../../../../../vendors/hermione');
const { oneOrg, swiperModal } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');
const { photoTiles } = require('../../../../../../components/PhotoTiles/PhotoTiles.test/PhotoTiles.page-object/index@touch-phone');

const elems = {};

elems.supplyReactBus = new Entity({ block: 'supply', elem: 'react-bus' });

elems.oneOrg = oneOrg.copy();
elems.oneOrg.photoTiles = photoTiles.copy();
elems.swiperModal = swiperModal.copy();
elems.swiperModal.photosPage = new ReactEntity({ block: 'PhotosSwiperPage' });
elems.swiperModal.photosPage.photo = new ReactEntity({ block: 'PhotosSwiperPage', elem: 'Photo' });

module.exports = elems;
