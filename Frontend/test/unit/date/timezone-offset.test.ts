import test from 'ava';

import { getTimezoneOffset } from '../../../src/lib/date';

test('Should get timezone offset correctly 1', (t) => {
    t.is(getTimezoneOffset('Pacific/Midway'), -11);
});

test('Should get timezone offset correctly 2', (t) => {
    t.is(getTimezoneOffset('Pacific/Tahiti'), -10);
});

test('Should get timezone offset correctly 3', (t) => {
    t.is(getTimezoneOffset('Pacific/Gambier'), -9);
});

test('Should get timezone offset correctly 4', (t) => {
    t.is(getTimezoneOffset('Pacific/Pitcairn'), -8);
});

test('Should get timezone offset correctly 5', (t) => {
    t.is(getTimezoneOffset('America/Ojinaga'), -7);
});

test('Should get timezone offset correctly 6', (t) => {
    t.is(getTimezoneOffset('America/Costa_Rica'), -6);
});

test('Should get timezone offset correctly 7', (t) => {
    t.is(getTimezoneOffset('America/Jamaica'), -5);
});

test('Should get timezone offset correctly 8', (t) => {
    t.is(getTimezoneOffset('America/Antigua'), -4);
});

test('Should get timezone offset correctly 9', (t) => {
    t.is(getTimezoneOffset('America/Argentina/Catamarca'), -3);
});

test('Should get timezone offset correctly 10', (t) => {
    t.is(getTimezoneOffset('Atlantic/South_Georgia'), -2);
});

test('Should get timezone offset correctly 11', (t) => {
    t.is(getTimezoneOffset('America/Scoresbysund'), -1);
});

test('Should get timezone offset correctly 12', (t) => {
    t.is(getTimezoneOffset('America/Danmarkshavn'), 0);
});

test('Should get timezone offset correctly 13', (t) => {
    t.is(getTimezoneOffset('Africa/Ceuta'), 1);
});

test('Should get timezone offset correctly 14', (t) => {
    t.is(getTimezoneOffset('Asia/Amman'), 2);
});

test('Should get timezone offset correctly 15', (t) => {
    t.is(getTimezoneOffset('Antarctica/Syowa'), 3);
});

test('Should get timezone offset correctly 16', (t) => {
    t.is(getTimezoneOffset('Europe/Ulyanovsk'), 4);
});

test('Should get timezone offset correctly 17', (t) => {
    t.is(getTimezoneOffset('Asia/Atyrau'), 5);
});

test('Should get timezone offset correctly 18', (t) => {
    t.is(getTimezoneOffset('Indian/Chagos'), 6);
});

test('Should get timezone offset correctly 19', (t) => {
    t.is(getTimezoneOffset('Asia/Novosibirsk'), 7);
});

test('Should get timezone offset correctly 20', (t) => {
    t.is(getTimezoneOffset('Asia/Choibalsan'), 8);
});

test('Should get timezone offset correctly 21', (t) => {
    t.is(getTimezoneOffset('Pacific/Palau'), 9);
});

test('Should get timezone offset correctly 22', (t) => {
    t.is(getTimezoneOffset('Australia/Lindeman'), 10);
});

test('Should get timezone offset correctly 23', (t) => {
    t.is(getTimezoneOffset('Australia/Lord_Howe'), 11);
});

test('Should get timezone offset correctly 24', (t) => {
    t.is(getTimezoneOffset('Pacific/Tarawa'), 12);
});
