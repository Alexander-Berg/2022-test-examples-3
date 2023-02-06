const { companiesComposite, overlayPanel, images, entitySearch, serpList } = require('../../../../../../hermione/page-objects/touch-phone');
const common = require('./index@common');

const elems = { ...common };

elems.companiesComposite = companiesComposite.copy();
elems.overlayPanel = overlayPanel.copy();
elems.images = images.copy();
elems.entitySearch = entitySearch.copy();
elems.serpList = serpList.copy();

module.exports = elems;
