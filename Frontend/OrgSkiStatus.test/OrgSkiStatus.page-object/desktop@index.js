const { create, ReactEntity } = require('../../../../../../vendors/hermione');

const elems = {};

elems.Gallery = new ReactEntity({ block: 'gallery' });
elems.OrgSkiSeasons = new ReactEntity({ block: 'OrgSkiSeasons' });
elems.NoticeStripe = new ReactEntity({ block: 'NoticeStripe' });

module.exports = create(elems);
