import { assert } from 'chai';
import _ from '../../../lib/lodash';
import type { ISerpAdapterOptions } from '../../../vendors/taburet/Adapter';
import type { ISerpContext } from '../../../typings';
import type { IOfferInfoSnippet } from '../OfferInfo@common.server';
import { AdapterOfferInfo } from '../OfferInfo@desktop.server';

describe('AdapterOfferInfo@desktop', () => {
    describe('getMeta', () => {
        const createOfferInfoInstance = (snippet?: IOfferInfoSnippet) => {
            const context = {
                device: {
                    BrowserName: 'Chrome',
                    BrowserVersionRaw: 87,
                },
            } as unknown as ISerpContext;

            const options = {
                context: context,
                snippet: {
                    currency: 'RUR',
                    offer_price: 2000,
                    offer_old_price: 2100,
                    price: 1900,
                    delivery: 'доставка',
                    ...snippet,
                },
            } as unknown as ISerpAdapterOptions<IOfferInfoSnippet>;

            return new AdapterOfferInfo(options);
        };

        const isPrice = (b: unknown): boolean => {
            return _.prop(b, 'block') === 'price';
        };

        const isOldPrice = (b: unknown): boolean => {
            return isPrice(b) && _.prop(b, 'outdated');
        };

        const hasValue = (b: unknown, value: number): boolean => {
            return _.prop(b, 'value') === value;
        };

        it('показывает скидку, если она есть', () => {
            const offerReviews = createOfferInfoInstance();
            // eslint-disable-next-line
            const items = offerReviews.getMeta()!.items[0].items[0];

            const hasPrice = Array.isArray(items) && items.some(b => isPrice(b) && hasValue(b, 2000));
            assert.isTrue(hasPrice);

            const hasOldPrice = Array.isArray(items) && items.some(b => isOldPrice(b) && hasValue(b, 2100));
            assert.isTrue(hasOldPrice);
        });

        it('не показывает скидку, если старая цена была такая же', () => {
            const offerReviews = createOfferInfoInstance({
                offer_old_price: 2000,
            } as IOfferInfoSnippet);
            // eslint-disable-next-line
            const items = offerReviews.getMeta()!.items[0].items[0];

            const hasPrice = Array.isArray(items) && items.some(b => isPrice(b) && hasValue(b, 2000));
            assert.isTrue(hasPrice);

            const hasOldPrice = Array.isArray(items) && items.some(b => isOldPrice(b) && hasValue(b, 2000));
            assert.isFalse(hasOldPrice);
        });

        it('не показывает скидку, если старая цена была меньше', () => {
            const offerReviews = createOfferInfoInstance({
                offer_old_price: 1800,
            } as IOfferInfoSnippet);
            // eslint-disable-next-line
            const items = offerReviews.getMeta()!.items[0].items[0];

            const hasPrice = Array.isArray(items) && items.some(b => isPrice(b) && hasValue(b, 2000));
            assert.isTrue(hasPrice);

            const hasOldPrice = Array.isArray(items) && items.some(b => isOldPrice(b) && hasValue(b, 1800));
            assert.isFalse(hasOldPrice);
        });
    });
});
