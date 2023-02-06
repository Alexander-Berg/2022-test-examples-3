/* eslint-disable */
import test from 'ava';
import { EventCounter } from '../../../../lib/pgPool/eventCounter';

test('eventCounter: inc counter', t => {
    const eventCounter = new EventCounter(['a', 'b'], 2);

    eventCounter.inc('a');

    t.is(eventCounter.getCounter('a'), 1);
    t.is(eventCounter.getSubsequentCounter('a'), 1);
    t.is(eventCounter.totalCount, 1);
    t.deepEqual(eventCounter.events, ['a']);
});

test('eventCounter: inc counter over maximum', t => {
    const eventCounter = new EventCounter(['a', 'b'], 2);

    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');

    t.is(eventCounter.getCounter('a'), 2);
    t.is(eventCounter.totalCount, 2);
    t.deepEqual(eventCounter.events, ['a', 'a']);
});

test('eventCounter: inc several counters', t => {
    const eventCounter = new EventCounter(['a', 'b'], 2);

    eventCounter.inc('a');
    eventCounter.inc('b');

    t.is(eventCounter.getCounter('a'), 1);
    t.is(eventCounter.getCounter('b'), 1);
    t.is(eventCounter.totalCount, 2);
    t.deepEqual(eventCounter.events, ['a', 'b']);
});

test('eventCounter: inc several counters over maximum', t => {
    const eventCounter = new EventCounter(['a', 'b'], 2);

    eventCounter.inc('a');
    eventCounter.inc('b');
    eventCounter.inc('a');

    t.is(eventCounter.getCounter('a'), 1);
    t.is(eventCounter.getCounter('b'), 1);
    t.is(eventCounter.totalCount, 2);
    t.deepEqual(eventCounter.events, ['b', 'a']);
});

test('eventCounter: check subsequent count increasing correctly', t => {
    const eventCounter = new EventCounter(['a', 'b'], 10);

    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');

    t.is(eventCounter.getSubsequentCounter('a'), 5);
});

test('eventCounter: check subsequent count do not overcome maximum', t => {
    const eventCounter = new EventCounter(['a', 'b'], 2);

    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');

    t.is(eventCounter.getSubsequentCounter('a'), 2);
});

test('eventCounter: check subsequent counters clears on another event', t => {
    const eventCounter = new EventCounter(['a', 'b'], 5);

    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('b');

    t.is(eventCounter.getSubsequentCounter('a'), 0);
    t.is(eventCounter.getSubsequentCounter('b'), 1);
});

test('eventCounter: check subsequent counters do not clear after reaching maximum', t => {
    const eventCounter = new EventCounter(['a', 'b'], 5);

    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');

    t.is(eventCounter.getSubsequentCounter('a'), 5);

    eventCounter.inc('a');

    t.is(eventCounter.getSubsequentCounter('a'), 5);
});

test('eventCounter: check counters share', t => {
    const eventCounter = new EventCounter(['a', 'b'], 10);

    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('b');

    t.is(eventCounter.getCounterShare('a'), 5 / 6);
    t.is(eventCounter.getCounterShare('b'), 1 / 6);
});

test('eventCounter: events array do not expanding if maxCount is not specified', t => {
    const eventCounter = new EventCounter(['a', 'b']);

    eventCounter.inc('a');

    t.deepEqual(eventCounter.events, []);
});

test('eventCounter: counters increasing normally if maxCount is not specified', t => {
    const eventCounter = new EventCounter(['a', 'b']);

    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('a');
    eventCounter.inc('b');
    eventCounter.inc('b');
    eventCounter.inc('b');

    t.is(eventCounter.getCounter('a'), 5);
    t.is(eventCounter.getCounter('b'), 3);
    t.is(eventCounter.totalCount, 8);
});
