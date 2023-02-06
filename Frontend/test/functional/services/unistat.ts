/* eslint-disable */
import test from 'ava';
import {
    HistogramBucket,
    HistogramName,
    resetMetrics,
    serialize,
    SerializedHistogram,
    writeHistogramValue,
} from '../../../services/unistat';

test.beforeEach(async() => {
    resetMetrics();
});

test('serialize empty getSkill timer histogram', async t => {
    const serialized = serialize();
    const hist: [string, HistogramBucket[]] = serialized.filter(
        metric => metric[0] === 'DB_getSkillStatus_time_hgram',
    )[0] as SerializedHistogram;
    t.truthy(hist !== undefined);
    t.is(hist[0], 'DB_getSkillStatus_time_hgram');
    for (const [, value] of hist[1]) {
        t.is(value, 0);
    }
});

test('write and read zero to getSkill timer', async t => {
    writeHistogramValue(HistogramName.DB_getSkillStatus_time, 0);
    const serialized = serialize();
    const hist = serialized.filter(metric => metric[0] === 'DB_getSkillStatus_time_hgram')[0] as SerializedHistogram;
    for (const [boundary, value] of hist[1]) {
        if (boundary === 0) {
            t.is(value, 1);
        } else {
            t.is(value, 0);
        }
    }
});

test('writing a value lower than left boundary is ignored', async t => {
    writeHistogramValue(HistogramName.DB_getSkillStatus_time, -1);
    const serialized = serialize();
    const hist = serialized.filter(metric => metric[0] === 'DB_getSkillStatus_time_hgram')[0] as SerializedHistogram;
    for (const [, value] of hist[1]) {
        t.is(value, 0);
    }
});

test('right bucket boundary is not inclusive', async t => {
    // writeHistogramValue() should increment second bucket
    writeHistogramValue(HistogramName.DB_getSkillStatus_time, 0.01);
    const serialized = serialize();
    const hist = serialized.filter(metric => metric[0] === 'DB_getSkillStatus_time_hgram')[0] as SerializedHistogram;
    for (const [boundary, value] of hist[1]) {
        if (boundary === 0.01) {
            t.is(value, 1);
        } else {
            t.is(value, 0);
        }
    }
});

test('large values are put to last bucket', async t => {
    // writeHistogramValue() should increment second bucket
    writeHistogramValue(HistogramName.DB_getSkillStatus_time, 100500);
    const serialized = serialize();
    const hist = serialized.filter(metric => metric[0] === 'DB_getSkillStatus_time_hgram')[0] as SerializedHistogram;
    for (const [boundary, value] of hist[1]) {
        if (boundary === 10) {
            t.is(value, 1);
        } else {
            t.is(value, 0);
        }
    }
});
