import test from 'ava';

import { addHours } from '../../../src/lib/date';
import { monday } from '../../helpers/date';

const makeHourAddingTest = (hours: number) => {
    test(`Should add ${hours} hours`, (t) => {
        const initialMondayHours = monday.getHours();

        const newDay = addHours(monday, hours);

        const newHours = newDay.getHours();

        const calculatedHours = initialMondayHours + hours;
        t.is(calculatedHours < 0 ? (calculatedHours % 24) + 24 : calculatedHours % 24, newHours);
    });
};

makeHourAddingTest(1);
makeHourAddingTest(2);
makeHourAddingTest(3);
makeHourAddingTest(4);
makeHourAddingTest(5);
makeHourAddingTest(24);
makeHourAddingTest(32);
makeHourAddingTest(100);

makeHourAddingTest(-1);
makeHourAddingTest(-5);
makeHourAddingTest(-10);
makeHourAddingTest(-24);
makeHourAddingTest(-32);
makeHourAddingTest(-100);
