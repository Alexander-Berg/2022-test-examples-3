import { assert } from 'chai';
import type { IOfferReviewsRaw } from '@features/OfferReviews/OfferReviews.typings';
import { AdapterOfferReviews } from '@features/OfferReviews/OfferReviews@common.server';
import type { ISerpAdapterOptions } from '../../vendors/taburet/Adapter';

describe('AdapterOfferReviews@touch-phone', () => {
    const createOfferReviewsInstance = (snippet?: IOfferReviewsRaw) => {
        const options = {
            context: {},
            snippet: {
                ...snippet,
            },
        } as unknown as ISerpAdapterOptions<IOfferReviewsRaw>;

        return new AdapterOfferReviews(options);
    };

    describe('getRating', () => {
        it('должен приводить данные в один формат', () => {
            const offerReviews = createOfferReviewsInstance();

            assert.equal(offerReviews.getRating(4.3), '4,3');
            assert.equal(offerReviews.getRating('4.2'), '4,2');
            assert.equal(offerReviews.getRating('4,1'), '4,1');
        });

        it('должен показывать одну цифру после запятой', () => {
            const offerReviews = createOfferReviewsInstance();

            assert.equal(offerReviews.getRating(4), '4,0');
        });
    });

    describe('getReviewsText', () => {
        it('правильно склоняет слово "отзывов"', () => {
            const offerReviews = createOfferReviewsInstance();

            assert.equal(offerReviews.getReviewsText(101), '101 отзыв');
            assert.equal(offerReviews.getReviewsText(102), '102 отзыва');
            assert.equal(offerReviews.getReviewsText(105), '105 отзывов');
        });

        it('учитывает пробелы в форматированном значении числа', () => {
            const offerReviews = createOfferReviewsInstance();

            assert.equal(offerReviews.getReviewsText(17101), '17 101 отзыв');
            assert.equal(offerReviews.getReviewsText(21100), '21 100 отзывов');
        });
    });
});
