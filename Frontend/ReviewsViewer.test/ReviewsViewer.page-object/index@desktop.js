const { ReactEntity } = require('../../../../vendors/hermione');
const modalPO = require('../../../../components/Modal/Modal.test/Modal.page-object/index@common');
const reviewPO = require('../../../../components/Review/Review.test/Review.page-object/index@common');
const { photoViewer } = require('../../../../components/PhotoViewer/PhotoViewer.test/PhotoViewer.page-object/index@common');
const { myReview } = require('../../../../components/MyReview/MyReview.test/MyReview.page-object/index@common');

const elems = {};

elems.reviewViewerModal = modalPO.Modal.mix({ block: 'OrgReviewsViewerModal' });
elems.reviewViewerModal.close = new ReactEntity({ block: 'TabsPanesModal', elem: 'CloseButton' });
elems.reviewViewerModal.reviewItem = reviewPO.review.copy();

elems.reviewViewerModal.tab = new ReactEntity({ block: 'TabsMenu', elem: 'Tab' });
elems.reviewViewerModal.secondTab = elems.reviewViewerModal.tab.nthChild(2);
elems.reviewViewerModal.moreTab = elems.reviewViewerModal.tab.mods({ more: true });
elems.reviewViewerModal.moreTab.item = new ReactEntity({ block: 'Menu', elem: 'Item' });

elems.reviewViewerModal.page = new ReactEntity({ block: 'OrgReviewsViewerPage' });
elems.reviewViewerModal.page.photos = new ReactEntity({ block: 'ReviewsPage', elem: 'Photos' });
elems.reviewViewerModal.page.photos.scrollerWrap = new ReactEntity({ block: 'Scroller', elem: 'Wrap' });
elems.reviewViewerModal.page.photo = new ReactEntity({ block: 'ReviewsPage', elem: 'Photo' });
elems.reviewViewerModal.page.photoViewer = photoViewer.copy();

elems.reviewViewerModal.page.sorting = new ReactEntity({ block: 'ReviewsPage', elem: 'SortingSelect' });
elems.reviewViewerModal.page.sorting.item = new ReactEntity({ block: 'Menu', elem: 'Item' });
elems.reviewViewerModal.page.sorting.secondItem = elems.reviewViewerModal.page.sorting.item.nthChild(2);

elems.reviewViewerModal.page.loginButton = new ReactEntity({ block: 'LoginButton', elem: 'Button' });
elems.reviewViewerModal.myReview = myReview.copy();

module.exports = elems;
