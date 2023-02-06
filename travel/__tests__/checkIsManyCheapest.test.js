import times from 'lodash/times';

import {RATION_TO_OFF_BADGES} from '../../constants';

const filterTariffs = jest.fn(segments => segments);

jest.setMock('../filterTariffs', filterTariffs);

const checkIsManyCheapest = require.requireActual(
    '../checkIsManyCheapest',
).default;

const CHEAPEST_1000 = {
    nationalPrice: {
        value: 1000,
    },
};
const EXPENSIVE_2000 = {
    nationalPrice: {
        value: 2000,
    },
};

function getSegment(price) {
    return {
        tariffs: {
            classes: {
                compartment: price,
            },
        },
    };
}

describe('checkIsManyCheapest', () => {
    describe('Позитивные', () => {
        it('Вернет `true`, если >20% тарифов являются самыми дешевыми', () => {
            const segments = [
                ...times(3, () => getSegment(CHEAPEST_1000)),
                getSegment(EXPENSIVE_2000),
            ];

            expect(
                checkIsManyCheapest(segments, 1000, RATION_TO_OFF_BADGES),
            ).toBe(true);
        });

        it('Вернет `true`, если <=20% тарифов являются самыми дешевыми, но больше 3', () => {
            const segments = [
                ...times(4, () => getSegment(CHEAPEST_1000)),
                ...times(17, () => getSegment(EXPENSIVE_2000)),
            ];

            expect(
                checkIsManyCheapest(segments, 1000, RATION_TO_OFF_BADGES),
            ).toBe(true);
        });
    });

    /* Отсутствие каких-либо полей не тестируем, это фильтруется в filterTariffs */
    describe('Негативные', () => {
        it('Вернет `false`, если <=20% тарифов и не больше 3, являются самыми дешевыми', () => {
            const segments = [
                ...times(1, () => getSegment(CHEAPEST_1000)),
                ...times(6, () => getSegment(EXPENSIVE_2000)),
            ];

            expect(
                checkIsManyCheapest(segments, 1000, RATION_TO_OFF_BADGES),
            ).toBe(false);
        });
    });
});
