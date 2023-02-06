// @flow

import {FILTER_IDS} from '@self/project/src/constants/filters';
import {getFilterValueId} from '@self/platform/entities/filter/helpers';

import {EXPRESS_FILTER_VALUE_ID} from './getFilterValueId';

describe('Хелпер getFilterValueId', () => {
    it('возвращает идентификатор, если он есть в фильтре', () => {
        const FILTER_ID = FILTER_IDS.PRICE;
        const FILTER_VALUE = {
            id: '10798550',
            initialFound: 137,
            found: 4,
            value: 'GiGwi',
            values: 'GiGwi',
            priceMin: {currency: 'RUR', value: '40'},
        };

        const result = getFilterValueId(FILTER_ID, FILTER_VALUE);
        const expected = FILTER_VALUE.id;

        expect(result).toBe(expected);
    });

    it('берет идентификатор из значения фильтра, если не прописан идентификатор', () => {
        const FILTER_ID = FILTER_IDS.CPA;
        const FILTER_VALUE = {
            value: '1',
            found: 10,
            checked: true,
        };

        // $FlowFixMe: потому что бывают без id ¯\_(ツ)_/¯
        const result = getFilterValueId(FILTER_ID, FILTER_VALUE);
        const expected = FILTER_VALUE.value;

        expect(result).toBe(expected);
    });

    it('возвращает кастомный идентификатор для экспресса', () => {
        const FILTER_ID = FILTER_IDS.EXPRESS;
        const FILTER_VALUE = {
            value: 'Доставка за 2 часа',
            initialFound: 4,
            found: 4,
        };

        // $FlowFixMe: потому что бывают без id ¯\_(ツ)_/¯
        const result = getFilterValueId(FILTER_ID, FILTER_VALUE);
        const expected = EXPRESS_FILTER_VALUE_ID;

        expect(result).toBe(expected);
    });
});
