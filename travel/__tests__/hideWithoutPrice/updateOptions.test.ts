import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import hideWithoutPrice from '../../hideWithoutPrice';

// @ts-ignore
const segmentWithPrice = {
    tariffs: {
        classes: {
            platzkarte: '1000 roubles',
        },
    },
} as ITrainsTariffApiSegment;

const segmentWithoutPrice = {} as ITrainsTariffApiSegment;

describe('hideWithoutPrice.updateOptions', () => {
    it('update default options by segment with price', () => {
        const newOptions = hideWithoutPrice.updateOptions(
            hideWithoutPrice.getDefaultOptions(),
            segmentWithPrice,
        );

        expect(newOptions).toEqual({
            withPrice: true,
            withoutPrice: false,
        });
    });

    it('update default options by segment without price', () => {
        const options = hideWithoutPrice.updateOptions(
            hideWithoutPrice.getDefaultOptions(),
            segmentWithoutPrice,
        );

        expect(options).toEqual({
            withPrice: false,
            withoutPrice: true,
        });
    });

    it('update options (withPrice == true, withoutPrice == false) by segment with price', () => {
        const options = hideWithoutPrice.updateOptions(
            {withPrice: true, withoutPrice: false},
            segmentWithPrice,
        );

        expect(options).toEqual({
            withPrice: true,
            withoutPrice: false,
        });
    });

    it('update options (withPrice == false, withoutPrice == true) by segment with price', () => {
        const options = hideWithoutPrice.updateOptions(
            {withPrice: false, withoutPrice: true},
            segmentWithPrice,
        );

        expect(options).toEqual({
            withPrice: true,
            withoutPrice: true,
        });
    });

    it('update options (withPrice == true, withoutPrice == false) by segment without price', () => {
        const options = hideWithoutPrice.updateOptions(
            {withPrice: true, withoutPrice: false},
            segmentWithoutPrice,
        );

        expect(options).toEqual({
            withPrice: true,
            withoutPrice: true,
        });
    });

    it('update options (withPrice == false, withoutPrice == true) by segment without price', () => {
        const options = hideWithoutPrice.updateOptions(
            {withPrice: false, withoutPrice: true},
            segmentWithoutPrice,
        );

        expect(options).toEqual({
            withPrice: false,
            withoutPrice: true,
        });
    });

    it('update options (withPrice == true, withoutPrice == true) by segment with price', () => {
        const options = hideWithoutPrice.updateOptions(
            {withPrice: true, withoutPrice: true},
            segmentWithPrice,
        );

        expect(options).toEqual({
            withPrice: true,
            withoutPrice: true,
        });
    });

    it('update options (withPrice == true, withoutPrice == true) by segment without price', () => {
        const options = hideWithoutPrice.updateOptions(
            {withPrice: true, withoutPrice: true},
            segmentWithoutPrice,
        );

        expect(options).toEqual({
            withPrice: true,
            withoutPrice: true,
        });
    });
});
