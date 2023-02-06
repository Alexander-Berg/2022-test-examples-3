const { oneOrg, overlayOneOrg } =
    require('../../../../Companies.test/Companies.page-object/index@touch-phone');
const { photoViewer } = require('../../../../../../components/PhotoViewer/PhotoViewer.test/PhotoViewer.page-object/index@common');
const { posts } = require('./index@common');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.posts = posts.copy();

elems.oneOrgOverlay = overlayOneOrg.copy();
elems.oneOrgOverlay.posts = posts.copy();

elems.photoViewer = photoViewer.copy();

module.exports = elems;
