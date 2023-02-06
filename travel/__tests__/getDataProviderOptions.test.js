import {STATION} from '../../constants/suggestOptionTypes';

import getDataProviderOptions from '../getDataProviderOptions';

describe('getGenericDataProviderOptions', () => {
    it('должен вернуть undefined если отсутствуют параметры', () => {
        expect(getDataProviderOptions({})).toBeUndefined();
    });

    it('должен вернуть соответствующий ответ при заданных параметрах', () => {
        const params = {
            suggests: {
                url: 'some.url',
            },
            nationalVersion: 'ru',
            settlement: {
                geo_id: '1',
            },
            language: 'ru',
            path: STATION,
        };

        const expectedResult = {
            url: 'some.url',
            path: STATION,
            query: {
                lang: 'ru',
                format: 'old',
                national_version: 'ru',
                client_city: '1',
            },
        };

        expect(getDataProviderOptions(params)).toEqual(expectedResult);
    });
});
