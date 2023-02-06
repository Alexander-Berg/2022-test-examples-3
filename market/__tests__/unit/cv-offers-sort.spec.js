const {
    sortOffersByFirstOffer,
    sortOffersByPrice,
    sortOffersByMedian,
    findMedian,
} = require('../../src/helper/sort-offers');

describe('offers sort', () => {
    test('should sort offers by price', () => {
        const input = [
            { price: { value: '5' } },
            { price: { value: '499' } },
            { price: { value: '4' } },
            { price: { value: '1' } },
        ];

        const output = [
            { price: { value: '1' } },
            { price: { value: '4' } },
            { price: { value: '5' } },
            { price: { value: '499' } },
        ];

        expect(sortOffersByPrice(input)).toEqual(output);
    });

    test('should sort offers by median', () => {
        const priceOnPage = undefined;
        const input = [
            { price: { value: '4' } },
            { price: { value: '3' } },
            { price: { value: '7' } },
            { price: { value: '9' } },
            { price: { value: '1' } },
            { price: { value: '2' } },
            { price: { value: '2' } },
        ];
        const output = [
            { price: { value: '4' } },
            { price: { value: '3' } },
            { price: { value: '2' } },
            { price: { value: '2' } },
            { price: { value: '7' } },
            { price: { value: '9' } },
            { price: { value: '1' } },
        ];

        expect(sortOffersByMedian(input, priceOnPage)).toEqual(output);
    });

    test('should sort offers by most relevant offer price', () => {
        const priceOnPage = undefined;
        const input = [
            { price: { value: '4' } },
            { price: { value: '3' } },
            { price: { value: '7' } },
            { price: { value: '9' } },
            { price: { value: '1' } },
            { price: { value: '2' } },
            { price: { value: '2' } },
        ];
        const output = [
            { price: { value: '4' } },
            { price: { value: '3' } },
            { price: { value: '2' } },
            { price: { value: '2' } },
            { price: { value: '7' } },
            { price: { value: '9' } },
            { price: { value: '1' } },
        ];

        expect(sortOffersByMedian(input, priceOnPage)).toEqual(output);
    });

    test('should sort offers by most relevant offer price', () => {
        const priceOnPage = undefined;
        const input = [
            { price: { value: '3' } },
            { price: { value: '4' } },
            { price: { value: '7' } },
            { price: { value: '9' } },
            { price: { value: '1' } },
            { price: { value: '2' } },
            { price: { value: '2' } },
        ];
        const output = [
            { price: { value: '3' } },
            { price: { value: '4' } },
            { price: { value: '2' } },
            { price: { value: '2' } },
            { price: { value: '7' } },
            { price: { value: '9' } },
            { price: { value: '1' } },
        ];

        expect(sortOffersByFirstOffer(input, priceOnPage)).toEqual(output);
    });

    test('should find median correctly when odd length ', () => {
        const input = [
            { price: { value: '3' } },
            { price: { value: '4' } },
            { price: { value: '7' } },
            { price: { value: '9' } },
            { price: { value: '1' } },
            { price: { value: '2' } },
            { price: { value: '2' } },
        ];

        const output = 3;

        expect(findMedian(input)).toEqual(output);
    });

    test('should find median correctly when even length ', () => {
        const input = [
            { price: { value: '1' } },
            { price: { value: '2' } },
            { price: { value: '3' } },
            { price: { value: '4' } },
            { price: { value: '5' } },
            { price: { value: '6' } },
            { price: { value: '8' } },
            { price: { value: '9' } },
        ];

        const output = 4.5;

        expect(findMedian(input)).toEqual(output);
    });
});
