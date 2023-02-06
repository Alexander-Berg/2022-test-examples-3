import DateMoment from '../../../interfaces/date/DateMoment';

import getDateRobotFromDateMoment from '../getDateRobotFromDateMoment';

describe('getDateRobotFromDateMoment', () => {
    it('Для строки типа DateMoment ернет время', () => {
        expect(
            getDateRobotFromDateMoment(
                '2020-03-11T15:40:00+03:00' as DateMoment,
            ),
        ).toBe('2020-03-11');
    });
});
