const { ReactEntity } = require('../../../../../../vendors/hermione');
const {
    oneOrg,
    overlayOneOrg,
    overlayIframe,
    overlayOneOrgMap,
    imagesViewer2,
} = require('../../../../Companies.test/Companies.page-object/index@touch-phone');
const { photoTiles } = require('../../../../../../components/PhotoTiles/PhotoTiles.test/PhotoTiles.page-object/index@touch-phone');
const { companiesMapGallery } = require('./index@common');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.companiesMapGallery = companiesMapGallery.copy();
elems.oneOrg.companiesMapGallery.photoTiles = photoTiles.copy();
elems.oneOrg.companiesMapGallery.thumb = new ReactEntity({ block: 'OrgMapGallery', elem: 'ThumbWrap' });
elems.oneOrg.companiesMapGallery.map = new ReactEntity({ block: 'OrgMapGallery', elem: 'MapWrap' });
elems.oneOrg.companiesMapGallery.address = new ReactEntity({ block: 'OrgMapGallery', elem: 'Address' });
elems.oneOrg.companiesMapGallery.copy = new ReactEntity({ block: 'OrgMapGallery', elem: 'Copy' });
elems.oneOrg.companiesMapGallery.expand = new ReactEntity({ block: 'OrgMapGallery', elem: 'MapExpand' });
elems.oneOrg.companiesMapGalleryExpanded = companiesMapGallery.mods({ expanded: true });
elems.overlayOneOrg = overlayOneOrg.copy();
elems.overlayIframe = overlayIframe.copy();
elems.overlayOneOrgMap = overlayOneOrgMap.copy();
elems.imagesViewer2 = imagesViewer2.copy();

module.exports = elems;
