import test from 'ava';

import { addDays } from '../../../src/lib/date';
import { monday } from '../../helpers/date';

const makeBasicAddDayTest = (day: Date, days: number) => {
    test(`Should add ${days} days`, (t) => {
        const newDay = addDays(day, days);

        t.is((day.getDay() + days) % 7, newDay.getDay());
    });
};

makeBasicAddDayTest(monday, 1);
makeBasicAddDayTest(monday, 2);
makeBasicAddDayTest(monday, 5);
makeBasicAddDayTest(monday, 7);
makeBasicAddDayTest(monday, 14);
makeBasicAddDayTest(monday, 136);
