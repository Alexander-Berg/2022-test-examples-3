'use strict';

const { Entity: El, create, ReactEntity } = require('../../../vendors/hermione/index');
const blocks = require('../../../../hermione/page-objects/common/blocks');

const PO = {};

PO.a11yHidden = new El({ block: 'a11y-hidden' });
PO.A11yHidden = new ReactEntity({ block: 'A11yHidden' });

PO.serpList = blocks.serpList.copy();
PO.serpItem = blocks.serpItem.copy();

PO.serpItem.url = new El({ block: 'organic', elem: 'url' });
PO.serpItem.price = new ReactEntity({ block: 'Price-CurrentValue' });
PO.serpItem.price.a11yHidden = PO.A11yHidden.copy();
PO.serpItem.price.Rub = new ReactEntity({ block: 'Rub' });
PO.serpItem.priceRange = new ReactEntity({ block: 'PriceRange' });
PO.serpItem.priceRange.a11yHidden = PO.A11yHidden.copy();
PO.serpItem.priceRange.Rub = new ReactEntity({ block: 'Rub' });
PO.serpItem.priceOutdated = new ReactEntity({ block: 'Price_outdated' });
PO.serpItem.priceOutdated.currentValue = new ReactEntity({ block: 'Price', elem: 'CurrentValue' });
PO.serpItem.priceOutdated.currentValue.a11yHidden = PO.A11yHidden.copy();
PO.serpItem.priceOutdated.currentValue.Rub = PO.serpItem.priceRange.Rub.copy();
PO.serpItem.thumb = new ReactEntity({ block: 'Thumb' });
PO.serpItem.oldThumb = blocks.organic.thumb.copy();
PO.serpItem.oldThumb.link = blocks.link.copy();
PO.serpItem.title = blocks.organic.title.copy();
PO.serpItem.title.offerReviews = new ReactEntity({ block: 'OfferReviews' });

PO.serpItem.scroller = blocks.scroller.copy();
PO.serpItem.scroller.topic = blocks.topic.nthType(1);
PO.serpItem.scroller.topic.link = blocks.topic.link.copy();
PO.serpItem.scroller.topic.labelDiscount = new El({ block: 'thumb', elem: 'label_discount' });
PO.serpItem.scroller.topic.labelDiscount.content = new El({ block: 'label', elem: 'content' });
PO.serpItem.scroller.topic.content = blocks.topic.content.copy();
PO.serpItem.scroller.topic.content.cutted = blocks.cutted.copy();
PO.serpItem.scroller.topic.content.cutted.textContainer = blocks.textContainer.copy();
PO.serpItem.scroller.topic.content.price = new El({ block: 'offer-price-group' });
PO.serpItem.scroller.topic.content.price.a11yHidden = PO.a11yHidden.copy();

module.exports = create(PO);
