// @flow

import {FILTER_IDS} from '@self/project/src/constants/filters';
import {getUpdatedParams} from '@self/platform/entities/filter/helpers';

const COMMON_PARAMS = {
    slug: 'igrushki-dlia-koshek-i-sobak',
    nid: '73259',
    hid: '15959385',
    glfilter: ['12719164:12719186', '15959401:15959420'],
    rt: '11',
    was_redir: '1',
    srnum: '139',
    suggest_text: 'дразнилка для кошек',
    onstock: '1',
    'local-offers-first': '0',
};

describe('Хелпер getUpdatedParams', () => {
    it('актуализирует параметры, когда включены' +
            'быстрофильтры маркетплейса и экспресса', () => {
        const FILTER_ID = FILTER_IDS.CPA;
        const PARAMS = {
            ...COMMON_PARAMS,
            [FILTER_IDS.CPA]: '1',
            [FILTER_IDS.EXPRESS]: '1',
        };

        const result = getUpdatedParams(FILTER_ID, PARAMS);
        const expected = {
            ...COMMON_PARAMS,
            [FILTER_IDS.CPA]: '1',
        };

        expect(result).toEqual(expect.objectContaining(expected));
    });

    it('не актуализирует параметры в остальных случаях', () => {
        const FILTER_ID = 'smth';
        const PARAMS = COMMON_PARAMS;

        const result = getUpdatedParams(FILTER_ID, PARAMS);
        const expected = PARAMS;

        expect(result).toEqual(expect.objectContaining(expected));
    });
});
