const { Entity, ReactEntity } = require('../../../../../../vendors/hermione');
const { oneOrg, overlayOneOrg, swiperModal } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');

const elems = {};

elems.supplyReactBus = new Entity({ block: 'supply', elem: 'react-bus' });

elems.oneOrg = oneOrg.copy();
elems.oneOrg.postsPreview = new ReactEntity({ block: 'OrgPostsPreview' });
elems.oneOrg.postsPreview.title = new ReactEntity({ block: 'OrgPostsPreview', elem: 'Title' });
elems.oneOrg.postsPreview.firstItemLink = new ReactEntity({ block: 'OrgPostsPreview', elem: 'Item' })
    .descendant(new ReactEntity({ block: 'Link' }));

elems.overlayOneOrg = overlayOneOrg.copy();
elems.overlayOneOrg.postsPreview = elems.oneOrg.postsPreview.copy();

elems.swiperModal = swiperModal.copy();
elems.swiperModal.postsSwiperPage = new ReactEntity({ block: 'PostsSwiperPage' });
elems.swiperModal.postsSwiperPage.item = new ReactEntity({ block: 'PostsSwiperPage', elem: 'Item' });
elems.swiperModal.postsSwiperPage.item.more = new ReactEntity({ block: 'PostText', elem: 'More' });
elems.swiperModal.postsSwiperPage.item.textLink = new ReactEntity({ block: 'PostText', elem: 'Link' });
elems.swiperModal.postsSwiperPage.fourthItem = elems.swiperModal.postsSwiperPage.item.nthChild(4);
elems.swiperModal.postsSwiperPage.sixthItem = elems.swiperModal.postsSwiperPage.item.nthChild(6);
elems.swiperModal.postsSwiperPage.autoLoadMore = new ReactEntity({ block: 'PostsSwiperPage', elem: 'AutoLoadMore' });

module.exports = elems;
