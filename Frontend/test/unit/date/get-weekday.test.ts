import test from 'ava';

import { getWeekday } from '../../../src/lib/date';
import * as date from '../../helpers/date';

test('Should return Monday', (t) => {
    t.is(getWeekday(date.monday), 'Monday');
});

test('Should return Tuesday', (t) => {
    t.is(getWeekday(date.tuesday), 'Tuesday');
});

test('Should return Wednesday', (t) => {
    t.is(getWeekday(date.wednesday), 'Wednesday');
});

test('Should return Thursday', (t) => {
    t.is(getWeekday(date.thursday), 'Thursday');
});

test('Should return Friday', (t) => {
    t.is(getWeekday(date.friday), 'Friday');
});

test('Should return Saturday', (t) => {
    t.is(getWeekday(date.saturday), 'Saturday');
});

test('Should return Sunday', (t) => {
    t.is(getWeekday(date.sunday), 'Sunday');
});
