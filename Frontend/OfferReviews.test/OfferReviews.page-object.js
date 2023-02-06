'use strict';

const { Entity: El, create } = require('../../../vendors/hermione/index');
const { extralinksPopup, extralinks } = require('../../../components/Extralinks/Extralinks.test/Extralinks.page-object/index@common');
const { relatedAbove } = require('../../../features/Related/_above/Related_above.test/Related_above.page-object/index@common');

const PO = { extralinksPopup };

PO.serpItemWithReviews = new El({ block: 't-construct-adapter', elem: 'offer-reviews' });

PO.serpItemWithReviews1 = PO.serpItemWithReviews.nthType(1);
PO.serpItemWithReviews1.subtitle = new El({ block: 'organic', elem: 'subtitle' });
PO.serpItemWithReviews2 = PO.serpItemWithReviews.nthType(2);
PO.serpItemWithReviews2.subtitle = new El({ block: 'organic', elem: 'subtitle' });

PO.serpItem = new El({ block: 'serp-item' });
PO.serpItem.title = new El({ block: 'organic', elem: 'url' });
PO.serpItem.subtitle = new El({ block: 'organic', elem: 'subtitle' });
PO.serpItem.reviews = new El({ block: 'organic', elem: 'reviews' });
PO.serpItem.reviews.a11yHiddenText = new El({ block: 'A11yHidden' });
PO.serpItem.reviews.a11yLabel = PO.serpItem.reviews.a11yHiddenText.nthType(1);
PO.serpItem.reviews.a11yTooltip = PO.serpItem.reviews.a11yHiddenText.nthType(2);
PO.serpItem.reviews.a11yTooltip.link = new El({ block: 'Link' });
PO.serpItem.reviews.a11yTooltip.text = new El(({ block: 'OrganicReviews-HiddenTooltipText' }));
PO.serpItem.reviews.link = new El({ block: 'organic', elem: 'reviews-link' });
PO.offerReviewsTooltip = new El({ block: 'ReviewsTooltip' });
PO.offerReviewsTooltip.link = new El({ block: 'Link' });

PO.serpItem.extralinks = extralinks.copy();
PO.serpItem.extralinks.offerReviews = new El({ block: 'OfferReviews' });

PO.relatedAbove = relatedAbove.copy();

module.exports = create(PO);
