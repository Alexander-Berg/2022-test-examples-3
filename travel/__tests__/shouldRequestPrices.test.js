jest.disableAutomock();

import {TRAIN_TYPE} from '../../transportType';

import shouldRequestPrices from '../shouldRequestPrices';

describe('shouldRequestPrices', () => {
    it('Если поиск на прошедшую дату - не нужно запрашивать цены', () => {
        expect(shouldRequestPrices({searchForPastDate: true})).toBe(false);
    });

    it('Поиск на будущую дату и тип транспорта не электричка - нужно запрашивать цены', () => {
        expect(
            shouldRequestPrices({
                searchForPastDate: false,
                transportType: TRAIN_TYPE,
            }),
        ).toBe(true);
    });
});
