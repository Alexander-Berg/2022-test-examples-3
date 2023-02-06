'use strict';

const { Entity, ReactEntity, create } = require('../../../vendors/hermione');
const bemPO = require('../../../../hermione/page-objects/common/index');
const { scroller } = require('../../../components/Scroller/Scroller.test/Scroller.page-object/index@common');
const { productCardsShowcase, productCardsShowcaseExtended } = require('../../../components/ProductCardsShowcase/ProductCardsShowcase.test/ProductCardsShowcase.page-object/index@common');
const { Collapser } = require('../../../components/Collapser/Collapser.test/Collapser.page-object/index@common');
const { productCard, productCard2 } = require('../../../components/ProductCard/ProductCard.test/ProductCard.page-object/index@common');
const { organic } = require('../../../components/SerpOrganic/SerpOrganic.test/SerpOrganic.page-object/index@common');
const { extralinks, extralinksPopup } = require('../../../components/Extralinks/Extralinks.test/Extralinks.page-object/index@common');
const { button } = require('../../../components/Button/Button.test/Button.page-object/index@common');
const { sitelinks } = require('../../../components/Sitelinks/Sitelinks.test/Sitelinks.page-object/index@common');
const { tabsMenu } = require('../../../components/TabsMenu/TabsMenu.test/TabsMenu.page-object/index@common');
const { review } = require('../../../components/Review/Review.test/Review.page-object/index@common');
const { reviewCount } = require('../../../components/ReviewCount/ReviewCount.test/ReviewCount.page-object/index@common');
const { more } = require('../../../components/LinkMore/LinkMore.test/LinkMore.page-object/index@common');
const { link } = require('../../../components/Link/Link.test/Link.page-object/index@common');
const { titleIncut } = require('../../../features/Market/Market.components/TitleIncut/TitleIncut.page-object');
const { videoThumb } = require('../../../components/VideoThumb/VideoThumb.test/VideoThumb.page-object/index@common');
const { textCut } = require('../../../components/TextCut/TextCut.test/TextCut.page-object/index@common');
const { thumb } = require('../../../components/Thumb/Thumb.test/Thumb.page-object/index@common');

productCard.bestPrice = new ReactEntity({ block: 'ProductCard', elem: 'BestPrice' });

const MarketCarousel = new ReactEntity({ block: 'ECommerceCarousel' });
const MarketCarouselInteracted = MarketCarousel.mods({ interacted: true });
MarketCarousel.scroller = scroller.copy();
MarketCarousel.productCard = productCard.copy();
MarketCarousel.secondProductCard = productCard.copy().nthChild(2);
MarketCarousel.directProduct = productCard.mods({ 't-mod': 'direct' });
MarketCarousel.marketProduct = productCard.mods({ 't-mod': 'market' });
MarketCarousel.moreLink = productCard.mods({ more: true });
MarketCarousel.moreLink.link = link.copy();
MarketCarousel.serpBkCounter = new Entity({ block: 'serp-bk-counter' });
const MarketCarouselHitCounter = new Entity({ block: 'serp-bk-counter' }).mods({ type: 'gallery' });
MarketCarousel.extralinks = extralinks.copy();
MarketCarousel.label = new ReactEntity({ block: 'Label' });
MarketCarousel.header = new ReactEntity({ block: 'ECommerceCarousel', elem: 'Header' });
MarketCarousel.header.link = link.copy();

const MarketImplicitModel = new ReactEntity({ block: 'ECommerceImplicitModel' });
MarketImplicitModel.marketItems = new ReactEntity({ block: 'ECommerceImplicitModel', elem: 'MarketItemsContainer' });
MarketImplicitModel.marketItems.productCardsShowcase = productCardsShowcase.copy();
MarketImplicitModel.productCardsShowcase = productCardsShowcase.copy();
MarketImplicitModel.productCard = productCard.copy();
MarketImplicitModel.productCard.reviewCount = reviewCount.copy();
MarketImplicitModel.productCard.reviewAvatars = new Entity({ block: 'ReviewCount-ImageItem' });
MarketImplicitModel.productCardLoader = productCard.mods({ loader: true });
MarketImplicitModel.organic = organic.copy();
MarketImplicitModel.titleWithIcon = new Entity({ block: 'TitleWithIcon' });
MarketImplicitModel.titleWithIcon.link = link.copy();
MarketImplicitModel.titleIncut = titleIncut.copy();
MarketImplicitModel.titleIncut.link = link.copy();
MarketImplicitModel.firstProductCard = productCard.nthChild(1);

const MarketImplicitModelRightIncut = new ReactEntity({ block: 'ECommerceImplicitModelRightIncut' });
MarketImplicitModelRightIncut.moreLink = productCard.mods({ more: true });
MarketImplicitModelRightIncut.scroller = scroller.copy();
MarketImplicitModelRightIncut.productCard = productCard.copy();
MarketImplicitModelRightIncut.serpOrganic = organic.copy();
MarketImplicitModelRightIncut.sitelinks = button.copy();
MarketImplicitModelRightIncut.productCard.reviewAvatars = new Entity({ block: 'ReviewCount-ImageItem' });
MarketImplicitModelRightIncut.organic = organic.copy();
MarketImplicitModelRightIncut.titleWithIcon = new Entity({ block: 'TitleWithIcon' });
MarketImplicitModelRightIncut.titleWithIcon.link = link.copy();
MarketImplicitModelRightIncut.titleIncut = titleIncut.copy();
MarketImplicitModelRightIncut.titleIncut.link = link.copy();
MarketImplicitModelRightIncut.firstProductCard = productCard.nthChild(1);

const MarketImplicitModelRightIncutWithMarketplace = new Entity({ block: 'ECommerceImplicitModelRightIncutWithMarketplace' });
MarketImplicitModelRightIncutWithMarketplace.scroller = scroller.copy();
MarketImplicitModelRightIncutWithMarketplace.productCard = productCard.copy();
MarketImplicitModelRightIncutWithMarketplace.productCard.reviewAvatars = new Entity({ block: 'ReviewCount-ImageItem' });
MarketImplicitModelRightIncutWithMarketplace.organic = organic.copy();
MarketImplicitModelRightIncutWithMarketplace.titleWithIcon = new Entity({ block: 'TitleWithIcon' });
MarketImplicitModelRightIncutWithMarketplace.titleWithIcon.link = link.copy();
MarketImplicitModelRightIncutWithMarketplace.titleIncut = titleIncut.copy();
MarketImplicitModelRightIncutWithMarketplace.titleIncut.link = link.copy();

const MarketImplicitModelText = new ReactEntity({ block: 'ECommerceImplicitModelText' });
MarketImplicitModelText.organic = organic.copy();
MarketImplicitModelText.sitelinks = sitelinks.mix(new ReactEntity({ block: 'ECommerceImplicitModelText', elem: 'Sitelinks' }));
MarketImplicitModelText.textCut = textCut.copy();

const MarketModel = new Entity({ block: 'ECommerceModel' });

const MarketModelRight = new ReactEntity({ block: 'ECommerceModelRight' });
MarketModelRight.gallery = new ReactEntity({ block: 'ECommerceModelRight', elem: 'Gallery' });
MarketModelRight.gallery.scroller = scroller.copy();
MarketModelRight.reviews = new ReactEntity({ block: 'ECommerceModelRight', elem: 'Reviews' });
MarketModelRight.reviews.scroller = scroller.copy();
MarketModelRight.reviews.scroller.moreItem = more.copy();
MarketModelRight.reviews.item = review.copy();
MarketModelRight.reviews.firstItem = review.nthChild(1);
MarketModelRight.reviews.authorImage = new ReactEntity({ block: 'ReviewAuthor', elem: 'Image' });
MarketModelRight.offers = new ReactEntity({ block: 'ECommerceModelRight', elem: 'Offers' });
MarketModelRight.videos = new ReactEntity({ block: 'ECommerceModelRight', elem: 'Videos' });
MarketModelRight.videos.scroller = scroller.copy();
MarketModelRight.videos.scroller.videoThumb = videoThumb.copy();
MarketModelRight.videos.scroller.moreItem = more.copy();
MarketModelRight.articles = new ReactEntity({ block: 'ECommerceModelRight', elem: 'Articles' });
MarketModelRight.articles.scroller = scroller.copy();
MarketModelRight.articles.scroller.thumb = thumb.copy();
MarketModelRight.articles.scroller.moreItem = more.copy();
MarketModelRight.products = new ReactEntity({ block: 'ECommerceModelRight', elem: 'Products' });
MarketModelRight.products.scroller = scroller.copy();
MarketModelRight.offers = new ReactEntity({ block: 'ECommerceModelRight', elem: 'Offers' });
MarketModelRight.tabsMenu = tabsMenu.copy();
MarketModelRight.tabReviews = new ReactEntity({ block: 'ECommerceModelRight', elem: 'TabReviews' });
MarketModelRight.tabReviews.firstItem = review.nthChild(1);
MarketModelRight.tabReviews.item = review.copy();
MarketModelRight.tabSpecs = new ReactEntity({ block: 'ECommerceModelRight', elem: 'TabSpecs' });
MarketModelRight.hiddenBlock = new ReactEntity({ block: 'ECommerceModelRight', elem: 'HiddenBlock' });
MarketModelRight.hiddenBlock.label = Collapser.label.copy();
MarketModelRight.hiddenBlockReviews = MarketModelRight.hiddenBlock.mods({ 't-mod': 'Reviews' });
MarketModelRight.hiddenBlockArticles = MarketModelRight.hiddenBlock.mods({ 't-mod': 'Articles' });
MarketModelRight.hiddenBlockProducts = MarketModelRight.hiddenBlock.mods({ 't-mod': 'Products' });

const MarketOffersWizard = new ReactEntity({ block: 'ECommerceOffersUniSearch' });
MarketOffersWizard.marketItemsContainer = new ReactEntity({ block: 'ECommerceOffersUniSearch', elem: 'ItemsContainer' });
MarketOffersWizard.marketItemsContainer.scroller = scroller.copy();
MarketOffersWizard.marketItemsContainer.scroller.moreItem = more.copy();
MarketOffersWizard.productCardsShowcase = productCardsShowcase.copy();
MarketOffersWizard.productCardsShowcase.showAll = new ReactEntity({ block: 'ProductCardsShowcase', elem: 'ShowAll' });
MarketOffersWizard.productCard = productCard.copy();
MarketOffersWizard.productCard2 = productCard2.copy();
MarketOffersWizard.productCardLoader = productCard.mods({ loader: true });
MarketOffersWizard.scroller = scroller.copy();
MarketOffersWizard.extralinks = extralinks.copy();
MarketOffersWizard.titleWithIcon = new Entity({ block: 'TitleWithIcon' });
MarketOffersWizard.titleWithIcon.link = link.copy();
MarketOffersWizard.titleIncut = titleIncut.copy();
MarketOffersWizard.titleIncut.link = link.copy();
MarketOffersWizard.buttonMore = new Entity({ block: 'ECommerceOffersUniSearch-MoreBtn' });
MarketOffersWizard.firstProductCard = productCard.nthChild(1);
// Добавлено экспериментом SERP-137449
MarketOffersWizard.debrandingTitle = new ReactEntity({ block: 'ECommerceOffersUniSearch', elem: 'DebrandingTitle' });
// Добавлено экспериментом SERP-149812
MarketOffersWizard.debrandingSubtitle = new ReactEntity({ block: 'ECommerceOffersUniSearch', elem: 'DebrandingSubtitle' });

const MarketOffersWizardText = new ReactEntity({ block: 'ECommerceOffersUniSearchText' });
MarketOffersWizardText.organic = organic.copy();
MarketOffersWizardText.sitelinks = sitelinks.mix(new ReactEntity({ block: 'ECommerceOffersUniSearchText', elem: 'Sitelinks' }));
MarketOffersWizardText.textCut = textCut.copy();

const MarketOffersWizardRightIncut = new Entity({ block: 'ECommerceOffersUniSearchRightIncut' });
MarketOffersWizardRightIncut.moreLink = productCard.mods({ more: true });
MarketOffersWizardRightIncut.moreLink.link = link.copy();
MarketOffersWizardRightIncut.productCard = productCard.copy();
MarketOffersWizardRightIncut.productCardLoader = productCard.mods({ loader: true });
MarketOffersWizardRightIncut.extralinks = extralinks.copy();
MarketOffersWizardRightIncut.scroller = scroller.copy();
MarketOffersWizardRightIncut.titleWithIcon = new Entity({ block: 'TitleWithIcon' });
MarketOffersWizardRightIncut.titleWithIcon.link = link.copy();
MarketOffersWizardRightIncut.titleIncut = titleIncut.copy();
MarketOffersWizardRightIncut.titleIncut.link = link.copy();
MarketOffersWizardRightIncut.buttonMore = new Entity({ block: 'ECommerceOffersUniSearchRightIncut-MoreBtn' });
MarketOffersWizardRightIncut.firstProductCard = productCard.nthChild(1);
// Добавлено экспериментом SERP-137449
MarketOffersWizardRightIncut.debrandingTitle = new ReactEntity({ block: 'ECommerceOffersUniSearchRightIncut', elem: 'DebrandingTitle' });

const MarketplaceCarousel = new Entity({ block: 'ECommercePlaceCarousel' });
MarketplaceCarousel.productCardsShowcase = productCardsShowcase.copy();
// Для эксперимента MARKET_cpa_carousel_with_extend
MarketplaceCarousel.productCardsShowcaseExtended = productCardsShowcaseExtended.copy();
MarketplaceCarousel.scroller = scroller.copy();
MarketplaceCarousel.productCard = productCard.copy();
MarketplaceCarousel.firstProductCard = productCard.nthChild(1);

const MarketModelOffersWizard = new Entity({ block: 'ECommerceModelOffersWizard' });
MarketModelOffersWizard.organic = organic.copy();
MarketModelOffersWizard.textCut = textCut.copy();
MarketModelOffersWizard.titleWithIcon = new Entity({ block: 'TitleWithIcon' });
MarketModelOffersWizard.titleWithIcon.link = link.copy();
MarketModelOffersWizard.productCard = productCard.copy();
MarketModelOffersWizard.titleIncut = titleIncut.copy();
MarketModelOffersWizard.titleIncut.link = link.copy();

const Page = new Entity({ block: 'b-page' });
const ExtralinksPopup = extralinksPopup.copy();
const Label = new ReactEntity({ block: 'Label' });

// Добавлено экспериментом SERP-137449
const ProductCardsModal = new Entity({ block: 'ProductCardsModal' });
ProductCardsModal.scrollContainer = new Entity({ block: 'ProductCardsModal-ScrollContainer' });

// https://st.yandex-team.ru/SERP-148788
MarketCarousel.header.conditions = new ReactEntity({ block: 'ECommerceCarousel', elem: 'HeaderExtralink' }).nthChild(3);
MarketCarousel.header.feedback = new ReactEntity({ block: 'ECommerceCarousel', elem: 'HeaderExtralink' }).nthChild(4);

module.exports = {
    MarketCarousel: create(MarketCarousel),
    MarketCarouselInteracted: create(MarketCarouselInteracted),
    MarketCarouselHitCounter: create(MarketCarouselHitCounter),
    MarketImplicitModel: create(MarketImplicitModel),
    MarketImplicitModelRightIncut: create(MarketImplicitModelRightIncut),
    MarketImplicitModelText: create(MarketImplicitModelText),
    MarketModel: create(MarketModel),
    MarketModelRight: create(MarketModelRight),
    MarketOffersWizard: create(MarketOffersWizard),
    MarketOffersWizardText: create(MarketOffersWizardText),
    MarketOffersWizardRightIncut: create(MarketOffersWizardRightIncut),
    MarketplaceCarousel: create(MarketplaceCarousel),
    MarketImplicitModelRightIncutWithMarketplace: create(MarketImplicitModelRightIncutWithMarketplace),
    MarketModelOffersWizard: create(MarketModelOffersWizard),

    ProductCardsShowcase: create(productCardsShowcase),
    TitleIncut: create(titleIncut),

    Page: create(Page),
    ExtralinksPopup: create(ExtralinksPopup),
    Label: create(Label),
    ProductCardsModal: create(ProductCardsModal),
    bemPO,
};
