import { assert } from 'chai';
import _ from '@lib/lodash';
import type { ISerpAdapterOptions } from '@vendors/taburet/Adapter';
import type { ISerpContext } from '@typings';
import type { OrganicTextChunk, OrganicTextChunkPrice } from '@components/OrganicTextContent/OrganicTextContent.typings';
import type { IOfferInfoSnippet } from '../OfferInfo@common.server';
import { AdapterOfferInfo } from '../OfferInfo@touch-phone.server';

describe('AdapterOfferInfo@touch-phone', () => {
    describe('getPre', () => {
        const createOfferInfoInstance = (snippet?: IOfferInfoSnippet) => {
            const context = {} as unknown as ISerpContext;

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

        const isPrice = (b: unknown): b is OrganicTextChunkPrice => {
            return _.prop(b, 'component') === 'price';
        };

        const isOldPrice = (b: unknown): b is OrganicTextChunkPrice => {
            return isPrice(b) && Boolean(b.props.outdated);
        };

        const hasValue = (b: OrganicTextChunkPrice, value: number): boolean => {
            return b.props.value === value;
        };

        it('показывает скидку, если она есть', () => {
            const offerReviews = createOfferInfoInstance();
            const pre = offerReviews.getPre() as OrganicTextChunk[];

            const hasPrice = pre.some(b => isPrice(b) && hasValue(b, 2000));
            assert.isTrue(hasPrice);

            const hasOldPrice = pre.some(b => isOldPrice(b) && hasValue(b, 2100));
            assert.isTrue(hasOldPrice);
        });

        it('не показывает скидку, если старая цена была такая же', () => {
            const offerReviews = createOfferInfoInstance({
                offer_old_price: 2000,
            } as IOfferInfoSnippet);
            const pre = offerReviews.getPre() as OrganicTextChunk[];

            const hasPrice = pre.some(b => isPrice(b) && hasValue(b, 2000));
            assert.isTrue(hasPrice);

            const hasOldPrice = pre.some(b => isOldPrice(b) && hasValue(b, 2000));
            assert.isFalse(hasOldPrice);
        });

        it('не показывает скидку, если старая цена была меньше', () => {
            const offerReviews = createOfferInfoInstance({
                offer_old_price: 1800,
            } as IOfferInfoSnippet);
            const pre = offerReviews.getPre() as OrganicTextChunk[];

            const hasPrice = pre.some(b => isPrice(b) && hasValue(b, 2000));
            assert.isTrue(hasPrice);

            const hasOldPrice = pre.some(b => isOldPrice(b) && hasValue(b, 1800));
            assert.isFalse(hasOldPrice);
        });
    });
});
