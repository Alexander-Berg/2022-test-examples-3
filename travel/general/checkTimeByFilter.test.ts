/* eslint-disable no-bitwise */

import {EAviaSearchTimeFilter} from 'reducers/avia/search/results/filters/reducer';

import {checkTimeByFilter} from './checkTimeByFilter';

const {DAY, EVENING, MORNING, NIGHT} = EAviaSearchTimeFilter;

describe('checkTimeByFilter', () => {
    test.each`
        time                     | filters            | expected
        ${'2019-06-29T05:59:00'} | ${0}               | ${true}
        ${'2019-06-29T05:59:00'} | ${NIGHT}           | ${true}
        ${'2019-06-29T05:59:00'} | ${MORNING}         | ${false}
        ${'2019-06-29T05:59:00'} | ${DAY}             | ${false}
        ${'2019-06-29T05:59:00'} | ${EVENING}         | ${false}
        ${'2019-06-29T06:00:00'} | ${0}               | ${true}
        ${'2019-06-29T06:00:00'} | ${NIGHT}           | ${false}
        ${'2019-06-29T06:00:00'} | ${MORNING}         | ${true}
        ${'2019-06-29T06:00:00'} | ${DAY}             | ${false}
        ${'2019-06-29T06:00:00'} | ${EVENING}         | ${false}
        ${'2019-06-29T06:29:00'} | ${0}               | ${true}
        ${'2019-06-29T06:29:00'} | ${NIGHT}           | ${false}
        ${'2019-06-29T06:29:00'} | ${MORNING}         | ${true}
        ${'2019-06-29T06:29:00'} | ${DAY}             | ${false}
        ${'2019-06-29T06:29:00'} | ${EVENING}         | ${false}
        ${'2019-06-29T12:00:00'} | ${0}               | ${true}
        ${'2019-06-29T12:00:00'} | ${NIGHT}           | ${false}
        ${'2019-06-29T12:00:00'} | ${MORNING}         | ${false}
        ${'2019-06-29T12:00:00'} | ${DAY}             | ${true}
        ${'2019-06-29T12:00:00'} | ${EVENING}         | ${false}
        ${'2019-06-29T15:00:00'} | ${0}               | ${true}
        ${'2019-06-29T15:00:00'} | ${NIGHT}           | ${false}
        ${'2019-06-29T15:00:00'} | ${MORNING}         | ${false}
        ${'2019-06-29T15:00:00'} | ${DAY}             | ${true}
        ${'2019-06-29T15:00:00'} | ${EVENING}         | ${false}
        ${'2019-06-29T18:00:00'} | ${0}               | ${true}
        ${'2019-06-29T18:00:00'} | ${NIGHT}           | ${false}
        ${'2019-06-29T18:00:00'} | ${MORNING}         | ${false}
        ${'2019-06-29T18:00:00'} | ${DAY}             | ${false}
        ${'2019-06-29T18:00:00'} | ${EVENING}         | ${true}
        ${'2019-06-29T22:00:00'} | ${0}               | ${true}
        ${'2019-06-29T22:00:00'} | ${NIGHT}           | ${false}
        ${'2019-06-29T22:00:00'} | ${MORNING}         | ${false}
        ${'2019-06-29T22:00:00'} | ${DAY}             | ${false}
        ${'2019-06-29T22:00:00'} | ${EVENING}         | ${true}
        ${'2019-06-29T00:00:00'} | ${0}               | ${true}
        ${'2019-06-29T00:00:00'} | ${NIGHT}           | ${true}
        ${'2019-06-29T00:00:00'} | ${MORNING}         | ${false}
        ${'2019-06-29T00:00:00'} | ${DAY}             | ${false}
        ${'2019-06-29T00:00:00'} | ${EVENING}         | ${false}
        ${'2019-06-29T00:00:00'} | ${NIGHT | MORNING} | ${true}
        ${'2019-06-29T00:00:00'} | ${MORNING | NIGHT} | ${true}
        ${'2019-06-29T00:00:00'} | ${DAY | NIGHT}     | ${true}
        ${'2019-06-29T00:00:00'} | ${EVENING | NIGHT} | ${true}
    `(
        'returns $expected when time is $time and filters are $filters',
        ({time, filters, expected}) => {
            expect(checkTimeByFilter(time, filters)).toBe(expected);
        },
    );
});
