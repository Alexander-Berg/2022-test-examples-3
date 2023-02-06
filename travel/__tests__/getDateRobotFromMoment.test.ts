import moment from 'moment';

import getDateRobotFromMoment from 'utilities/dateUtils/getDateRobotFromMoment';
import isDateRobot from 'utilities/dateUtils/isDateRobot';

describe('getDateRobotFromMoment', () => {
    it('Вернет TDateRobot для текущего времени', () => {
        const now = moment();
        const dateRobot = getDateRobotFromMoment(now);

        expect(isDateRobot(dateRobot)).toBe(true);
    });
});
