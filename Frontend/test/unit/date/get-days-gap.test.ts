import test from 'ava';

import { getOneWeekDaysGapBetween } from '../../../src/lib/date';
import { Day } from '../../../src/types/OpeningHours';

const makeGetDaysGapTest = (day1: Day, day2: Day, answer: ReturnType<typeof getOneWeekDaysGapBetween>) => {
    test(`Should work for ${day1} and ${day2} with ${answer} gap`, (t) => {
        t.is(getOneWeekDaysGapBetween(day1, day2), answer);
    });
};

makeGetDaysGapTest('Monday', 'Monday', 7);
makeGetDaysGapTest('Tuesday', 'Monday', 6);
makeGetDaysGapTest('Monday', 'Tuesday', 1);
makeGetDaysGapTest('Wednesday', 'Saturday', 3);
makeGetDaysGapTest('Saturday', 'Sunday', 1);
makeGetDaysGapTest('Saturday', 'Monday', 2);
makeGetDaysGapTest('Friday', 'Monday', 3);
