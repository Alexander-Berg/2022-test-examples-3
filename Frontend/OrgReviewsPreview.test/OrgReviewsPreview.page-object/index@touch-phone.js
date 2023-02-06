const { ReactEntity, Entity } = require('../../../../../../vendors/hermione');
const { reviews } = require('../../../../../../features/Reviews/Reviews.test/Reviews.page-object/index@common');
const { reviewsPreview: reviewsPreviewBase } = require('./index@common');

const elems = {};

elems.orgReviewsPreview = reviewsPreviewBase.copy();
elems.orgReviewsPreview.list.firstReview.visibleText = new ReactEntity({ block: 'Cut-Visible' });
elems.orgReviewsPreview.reviewsRankingSelector = new ReactEntity({ block: 'ReviewsRankingSelector' });
elems.orgReviewsPreview.reviewsRankingSelector.button = new ReactEntity({ block: 'Button2' });
elems.orgReviewsPreview.reviewsRankingSelector.default =
    elems.orgReviewsPreview.reviewsRankingSelector.button.nthChild(1);
elems.orgReviewsPreview.reviewsRankingSelector.positive =
    elems.orgReviewsPreview.reviewsRankingSelector.button.nthChild(2);
elems.orgReviewsPreview.reviewsRankingSelector.negative =
    elems.orgReviewsPreview.reviewsRankingSelector.button.nthChild(3);

elems.oneOrg = new Entity({ block: 'serp-item' }).descendant(new Entity({ block: 'composite' }).mods({ 't-mod': '1org' }));
elems.oneOrg.reviewsPreview = elems.orgReviewsPreview.copy();

elems.overlay = new Entity({ block: 'overlay', elem: 'panel' });
elems.overlay.reviews = reviews.copy();
elems.overlay.reviews.sortingSelect.text = new ReactEntity({ block: 'Button2', elem: 'Text' });

module.exports = elems;
