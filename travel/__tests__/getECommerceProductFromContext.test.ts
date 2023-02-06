import {ITrainsFilledSearchContext} from 'reducers/trains/context/types';

import {CHAR_EM_DASH} from 'utilities/strings/charCodes';

import getECommerceProductFromContext from '../getECommerceProductFromContext';

describe('getECommerceProductFromContext', () => {
    it('Вернёт объект продукта по поисковому контексту', () => {
        const context = {
            from: {
                key: 'c213',
                title: 'Москва',
            },
            to: {
                key: 'c54',
                title: 'Екатеринбург',
            },
        };

        expect(
            getECommerceProductFromContext(
                context as ITrainsFilledSearchContext,
            ),
        ).toEqual({
            id: 'c213-c54',
            name: `ЖД-билеты Москва ${CHAR_EM_DASH} Екатеринбург`,
            category: 'ЖД-билеты/Москва/Екатеринбург',
        });
    });
});
