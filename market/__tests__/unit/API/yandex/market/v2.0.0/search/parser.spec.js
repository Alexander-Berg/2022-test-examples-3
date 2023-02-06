'use strict';

const parser = require('./../../../../../../../src/API/yandex/market/v2.0.0/search/parser');

const correctResponseWithSeveralItems = require('./data/200-response-with-several-items.stub');
const correctResponseWithSeveralItemsModels = require('./data/200-response-with-several-items-models.stub');
const correctResponseWithSeveralItemsOffers = require('./data/200-response-with-several-items-offers.stub');
const correctResponseWithSeveralContext = require('./data/200-response-with-several-context.stub');
const correctResponseWithSeveralCategories = require('./data/200-response-with-several-categories.stub');
const correctResponseWithoutItems = require('./data/200-response-without-items.stub');

describe('search parser', () => {
    it('should return an empty array of \'items\' if response is an empty object', () => {
        const { response, expected } = correctResponseWithoutItems;
        const actual = parser(response);
        const { items } = actual;

        expect(items).toBeInstanceOf(Array);
        expect(items).toHaveLength(0);
        expect(items).toEqual(expected);
    });

    it('should return an empty array of \'categories\' if response is an empty object', () => {
        const { response, expected } = correctResponseWithoutItems;
        const actual = parser(response);
        const { categories } = actual;

        expect(categories).toBeInstanceOf(Array);
        expect(categories).toHaveLength(0);
        expect(categories).toEqual(expected);
    });

    it('should return \'items\' array of objects with specific keys', () => {
        const { response, expectedItems } = correctResponseWithSeveralItems;
        const actual = parser(response);
        const { context, items, items: [firstItem] } = actual;

        expect(items).toBeInstanceOf(Array);
        expect(Object.keys(firstItem)).toEqual([
            '__type',
            'id',
            'wareMd5',
            'name',
            'description',
            'price',
            'cpa',
            'url',
            'shop',
            'model',
            'phone',
            'delivery',
            'category',
            'vendor',
            'warranty',
            'recommended',
            'link',
            'paymentOptions',
            'bigPhoto',
            'photos',
            'previewPhotos',
            'context',
            'isOffer'
        ]);

        expect(Object.keys(context)).toEqual([
            'region',
            'currency',
            'page',
            'processingOptions',
            'id',
            'time',
            'link',
            'marketUrl'
        ]);
    });

    it('should return \'models\' array', () => {
        const { response, expectedModels } = correctResponseWithSeveralItemsModels;
        const actual = parser(response);
        const { items } = actual;

        const actualModels = items
            .map((item) => {
                if (item.__type === 'model') {
                    return item;
                }
            })
            .filter((item) => item);

        expect(actualModels).toEqual(expectedModels);
    });

    it('should return \'offers\' array', () => {
        const { response, expectedOffers } = correctResponseWithSeveralItemsOffers;
        const actual = parser(response);
        const { items } = actual;

        const actualOffers = items
            .map((item, index) => {
                if (item.__type === 'offer') {
                    return item;
                }
            })
            .filter((item) => item);

        expect(actualOffers).toEqual(expectedOffers);
    });

    it('should return \'context\' object', () => {
        const { response, expectedContext } = correctResponseWithSeveralContext;
        const actual = parser(response);
        const { context } = actual;

        expect(context).toEqual(expectedContext);
    });

    it('should return \'categories\' array', () => {
        const { response, expectedCategories } = correctResponseWithSeveralCategories;
        const actual = parser(response);
        const { categories } = actual;

        expect(categories).toEqual(expectedCategories);
    });
});
