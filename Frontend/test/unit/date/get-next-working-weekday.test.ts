import { WeekOpeningHours } from '../../../src/types/OpeningHours';
import test from 'ava';

import { getNextWorkingWeekday } from '../../../src/lib/date';
import { sunday, saturday, monday, defaultOpeningHours, friday, badOpeningHours } from '../../helpers/date';

test('Should get Monday after Sunday', (t) => {
    t.is(getNextWorkingWeekday(sunday, defaultOpeningHours), 'Monday');
});

test('Should get Monday after Saturday', (t) => {
    t.is(getNextWorkingWeekday(saturday, defaultOpeningHours), 'Monday');
});

test('Should get Monday after Friday', (t) => {
    t.is(getNextWorkingWeekday(friday, defaultOpeningHours), 'Monday');
});

test('Should get Tuesday after Monday', (t) => {
    t.is(getNextWorkingWeekday(monday, defaultOpeningHours), 'Tuesday');
});

test('Should stop if no working days', (t) => {
    t.is(getNextWorkingWeekday(monday, badOpeningHours), undefined);
});

const openingHours: WeekOpeningHours = {
    ...defaultOpeningHours,
    Tuesday: null,
};

test('Should get Wednesday after Monday', (t) => {
    t.is(getNextWorkingWeekday(monday, openingHours), 'Wednesday');
});
