import {
    COMMON_STORE,
    storeCommon,
} from './common.actions';

import { CommonDataType } from './common.types';

describe('storeCommon', () => {
    it('Should create payload', () => {
        const serviceData = {
            id: 989,
            slug: 'abc',
            name: {
                ru: 'ABC (Каталог)',
                en: 'ABC (Catalogue)',
            },
        };

        expect(storeCommon(CommonDataType.services, 989, serviceData)).toEqual({
            type: COMMON_STORE,
            payload: serviceData,
            meta: {
                type: CommonDataType.services,
                id: 989,
            },
        });
    });
});
