import DateMoment from '../../../interfaces/date/DateMoment';

import getTimeFromDateMoment from '../getTimeFromDateMoment';

describe('getTimeFromDateMoment', () => {
    it('Для строки типа DateMoment ернет время', () => {
        expect(
            getTimeFromDateMoment('2020-03-11T15:40:00+03:00' as DateMoment),
        ).toBe('15:40');
    });
});
