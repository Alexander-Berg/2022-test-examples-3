const { Entity, ReactEntity } = require('../../../../../../vendors/hermione');
const { scroller } = require('../../../../../../../hermione/page-objects/desktop/blocks');

const elems = {};

elems.oneOrg = new Entity({ block: 't-construct-adapter', elem: 'companies' });
elems.oneOrg.scroller = scroller.copy();
elems.oneOrg.OrgAddPhoto = new ReactEntity({ block: 'OrgAddPhoto' });
elems.addPhotoModal = new ReactEntity({ block: 'AddPhotoModal' });

module.exports = elems;
