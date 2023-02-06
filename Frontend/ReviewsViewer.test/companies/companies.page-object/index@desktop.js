const { create } = require('../../../../../vendors/hermione');
const { oneOrg } = require('../../../../Companies/Companies.test/Companies.page-object/index@desktop');
const { reviewViewerModal } = require('../../ReviewsViewer.page-object/index@desktop');

const elems = {
    oneOrg: oneOrg.copy(),
    reviewViewerModal: reviewViewerModal.copy(),
};

module.exports = create(elems);
