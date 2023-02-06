const { ReactEntity } = require('../../../../vendors/hermione');

const elems = {};

elems.reviewViewerModal = new ReactEntity({ block: 'OrgReviewsViewerModal' });
elems.reviewViewerModal.reviewItem = new ReactEntity({ block: 'Review' });
elems.reviewViewerModal.page = new ReactEntity({ block: 'OrgReviewsViewerPage' });

module.exports = elems;
