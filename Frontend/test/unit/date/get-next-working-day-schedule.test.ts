import { WeekOpeningHours } from '../../../src/types/OpeningHours';
import test from 'ava';

import { getNextWorkingDaySchedule } from '../../../src/lib/date';
import { saturday, monday, friday, badOpeningHours } from '../../helpers/date';

const openingHours: WeekOpeningHours = {
    Monday: {
        from: '9:00',
        to: '10:00',
    },
    Tuesday: {
        from: '9:00',
        to: '11:00',
    },
    Wednesday: {
        from: '9:00',
        to: '12:00',
    },
    Thursday: {
        from: '9:00',
        to: '13:00',
    },
    Friday: {
        from: '9:00',
        to: '14:00',
    },
    Saturday: null,
    Sunday: {
        from: '9:00',
        to: '16:00',
    },
};

test('Should get working hours for Tuesday', (t) => {
    t.deepEqual(getNextWorkingDaySchedule(monday, openingHours), openingHours.Tuesday);
});

test('Should get working hours for Sunday', (t) => {
    t.deepEqual(getNextWorkingDaySchedule(friday, openingHours), openingHours.Sunday);
});

test('Should get working hours for Sunday 2', (t) => {
    t.deepEqual(getNextWorkingDaySchedule(saturday, openingHours), openingHours.Sunday);
});

test('Should get undefined', (t) => {
    t.deepEqual(getNextWorkingDaySchedule(saturday, badOpeningHours), undefined);
});
