'use strict';

jest.dontMock('../../baseFilterManager');
const pricePresence = require.requireActual('../../pricePresence').default;

const segmentWithPrice = {
    title: 'Moscow - Omsk',
    tariffs: {
        classes: {
            business: '1000 roubles',
        },
    },
};

const segmentWithoutPrice = {
    title: 'Moscow - Omsk',
};

describe('pricePresence', () => {
    describe('updateOptions', () => {
        it('update default options by segment with price', () => {
            const newOptions = pricePresence.updateOptions(
                pricePresence.getDefaultOptions(),
                segmentWithPrice,
            );

            expect(newOptions).toEqual({
                withPrice: true,
                withoutPrice: false,
            });
        });

        it('update default options by segment without price', () => {
            const options = pricePresence.updateOptions(
                pricePresence.getDefaultOptions(),
                segmentWithoutPrice,
            );

            expect(options).toEqual({
                withPrice: false,
                withoutPrice: true,
            });
        });

        it('update options (withPrice == true, withoutPrice == false) by segment with price', () => {
            const options = pricePresence.updateOptions(
                {withPrice: true, withoutPrice: false},
                segmentWithPrice,
            );

            expect(options).toEqual({
                withPrice: true,
                withoutPrice: false,
            });
        });

        it('update options (withPrice == false, withoutPrice == true) by segment with price', () => {
            const options = pricePresence.updateOptions(
                {withPrice: false, withoutPrice: true},
                segmentWithPrice,
            );

            expect(options).toEqual({
                withPrice: true,
                withoutPrice: true,
            });
        });

        it('update options (withPrice == true, withoutPrice == false) by segment without price', () => {
            const options = pricePresence.updateOptions(
                {withPrice: true, withoutPrice: false},
                segmentWithoutPrice,
            );

            expect(options).toEqual({
                withPrice: true,
                withoutPrice: true,
            });
        });

        it('update options (withPrice == false, withoutPrice == true) by segment without price', () => {
            const options = pricePresence.updateOptions(
                {withPrice: false, withoutPrice: true},
                segmentWithoutPrice,
            );

            expect(options).toEqual({
                withPrice: false,
                withoutPrice: true,
            });
        });

        it('update options (withPrice == true, withoutPrice == true) by segment with price', () => {
            const options = pricePresence.updateOptions(
                {withPrice: true, withoutPrice: true},
                segmentWithPrice,
            );

            expect(options).toEqual({
                withPrice: true,
                withoutPrice: true,
            });
        });

        it('update options (withPrice == true, withoutPrice == true) by segment without price', () => {
            const options = pricePresence.updateOptions(
                {withPrice: true, withoutPrice: true},
                segmentWithoutPrice,
            );

            expect(options).toEqual({
                withPrice: true,
                withoutPrice: true,
            });
        });
    });
});
