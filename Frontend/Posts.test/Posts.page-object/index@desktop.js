const { oneOrg, popup } = require('../../../../Companies.test/Companies.page-object/index@desktop');
const { photoViewer } = require('../../../../../../components/PhotoViewer/PhotoViewer.test/PhotoViewer.page-object/index@common');
const { posts } = require('./index@common');
const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.posts = posts.copy();

elems.popup = popup.copy();
elems.popup.oneOrg.posts = posts.copy();

elems.photoViewer = photoViewer.copy();

module.exports = elems;
