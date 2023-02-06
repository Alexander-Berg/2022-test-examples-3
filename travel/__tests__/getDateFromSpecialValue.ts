import moment, {Moment} from 'moment';

import {WHEN_SPECIAL_VALUE} from 'types/common/When';

import DateMock from 'utilities/testUtils/DateMock';
import {ROBOT} from 'utilities/dateUtils/formats';

import {getDateFromSpecialValue} from '../index';

describe('getDateFromSpecialValue(when)', () => {
    test('Должен вернуть сегодняшнюю дату для today', () => {
        DateMock.mock('2019-02-11');

        const date = getDateFromSpecialValue(
            WHEN_SPECIAL_VALUE.TODAY,
        ) as Moment;

        expect(date.format(ROBOT)).toEqual(moment('2019-02-11').format(ROBOT));
        DateMock.restore();
    });

    test('Должен вернуть завтрашнюю дату для tomorrow', () => {
        DateMock.mock('2019-02-11');

        const date = getDateFromSpecialValue(
            WHEN_SPECIAL_VALUE.TOMORROW,
        ) as Moment;

        expect(date.format(ROBOT)).toEqual(moment('2019-02-12').format(ROBOT));
        DateMock.restore();
    });

    test('Должен вернуть undefined для all-days', () => {
        expect(getDateFromSpecialValue(WHEN_SPECIAL_VALUE.ALL_DAYS)).toBe(
            undefined,
        );
    });
});
