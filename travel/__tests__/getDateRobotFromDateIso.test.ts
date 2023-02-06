import TDateIso from 'types/common/date/TDateIso';

import getDateRobotFromDateIso from '../getDateRobotFromDateIso';

describe('getDateRobotFromDateIso', () => {
    it('Для строки типа TDateMoment вернет время в TDateRobot', () => {
        expect(
            getDateRobotFromDateIso('2020-03-11T15:40:00+03:00' as TDateIso),
        ).toBe('2020-03-11');
    });
});
