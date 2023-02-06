const { ReactEntity } = require('../../../../../../vendors/hermione');
const { reviewPreview } = require('../../../../../../components/ReviewPreview/ReviewPreview.test/ReviewPreview.page-object/index@common');

const elems = {};

elems.reviewsPreview = new ReactEntity({ block: 'OrgReviewsPreview' });
elems.reviewsPreview.title = new ReactEntity({ block: 'ReviewsTitle' });
elems.reviewsPreview.title.link = new ReactEntity({ block: 'ReviewsTitle', elem: 'Link' });
elems.reviewsPreview.readAll = new ReactEntity({ block: 'LinkMore' });
elems.reviewsPreview.list = new ReactEntity({ block: 'Reviews', elem: 'List' });
elems.reviewsPreview.list.firstReview = reviewPreview.nthChild(1);
elems.reviewsPreview.list.secondReview = reviewPreview.nthChild(2);
elems.reviewsPreview.aspectsList = new ReactEntity({ block: 'ReviewsAspectCardList' });
elems.reviewsPreview.aspectsList.card = new ReactEntity({ block: 'ReviewsAspectCard' });
elems.reviewsPreview.aspectsList.card.a11y = new ReactEntity({ block: 'A11yHidden' });
elems.reviewsPreview.aspectsList.firstCard = elems.reviewsPreview.aspectsList.card.nthChild(1);

module.exports = elems;
