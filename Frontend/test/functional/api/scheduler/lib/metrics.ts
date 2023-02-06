/* eslint-disable */
import '../../_helpers';
import test from 'ava';
import { Channel } from '../../../../../db/tables/settings';
import { findRow, sensorsFromRow } from '../../../../../routes/scheduler/v1/lib/metrics';
import { DeployRequestedChannelStat, Percentiles } from '../../../../../db/entities';

function rowFactory(
    channel: Channel,
    deployedToAlice: boolean,
    deployedToOrganizationChats: boolean,
    count: number,
    overdueCount: number,
    delayPercentiles: Percentiles,
): DeployRequestedChannelStat {
    return {
        channel,
        deployedToAlice,
        deployedToOrganizationChats,
        count,
        overdueCount,
        delayPercentiles,
    };
}

test('findRow returns default on empty rows list', async t => {
    const row = findRow([], Channel.AliceSkill, true, true);

    t.deepEqual(row, {
        channel: Channel.AliceSkill,
        deployedToOrganizationChats: true,
        deployedToAlice: true,
        count: 0,
        overdueCount: 0,
        delayPercentiles: [0, 0, 0, 0, 0, 0],
    });
});

test('check SkillChannel filter', async t => {
    const row = rowFactory(Channel.AliceSkill, true, false, 1, 1, [1, 2, 3, 4, 5, 6]);
    const rows = [row, rowFactory(Channel.OrganizationChat, true, false, 0, 0, [0, 0, 0, 0, 0, 0])];

    const foundRow = findRow(rows, Channel.AliceSkill, true, false);

    t.deepEqual(row, foundRow);
});

test('check deployedToAlice filter', async t => {
    const row = rowFactory(Channel.AliceSkill, true, false, 1, 1, [1, 2, 3, 4, 5, 6]);
    const rows = [row, rowFactory(Channel.AliceSkill, false, false, 0, 0, [0, 0, 0, 0, 0, 0])];

    const foundRow = findRow(rows, Channel.AliceSkill, true, false);

    t.deepEqual(row, foundRow);
});

test('check deployedToOrganizationChats filter', async t => {
    const row = rowFactory(Channel.AliceSkill, false, true, 1, 1, [1, 2, 3, 4, 5, 6]);
    const rows = [row, rowFactory(Channel.AliceSkill, false, false, 0, 0, [0, 0, 0, 0, 0, 0])];

    const foundRow = findRow(rows, Channel.AliceSkill, false, true);

    t.deepEqual(row, foundRow);
});

test('sensorsFromRow extract correct values', async t => {
    const row = rowFactory(Channel.AliceSkill, true, false, 10, 20, [1, 2, 3, 4, 5, 6]);
    const ts = new Date().toISOString();

    const sensors = sensorsFromRow(row, ts);

    t.truthy(sensors.length === 8);

    t.deepEqual(sensors.filter(s => s.labels.sensor === 'deployRequested_count')[0], {
        labels: {
            channel: row.channel,
            deployedToAlice: String(row.deployedToAlice),
            deployedToOrganizationChats: String(row.deployedToOrganizationChats),
            sensor: 'deployRequested_count',
        },
        ts,
        value: 10,
    });

    t.deepEqual(sensors.filter(s => s.labels.sensor === 'deployRequested_overdueCount')[0], {
        labels: {
            channel: row.channel,
            deployedToAlice: String(row.deployedToAlice),
            deployedToOrganizationChats: String(row.deployedToOrganizationChats),
            sensor: 'deployRequested_overdueCount',
        },
        ts,
        value: 20,
    });

    t.deepEqual(sensors.filter(s => s.labels.sensor === 'deployRequested_delayPercentile_50')[0], {
        labels: {
            channel: row.channel,
            deployedToAlice: String(row.deployedToAlice),
            deployedToOrganizationChats: String(row.deployedToOrganizationChats),
            sensor: 'deployRequested_delayPercentile_50',
        },
        ts,
        value: 1,
    });

    t.deepEqual(sensors.filter(s => s.labels.sensor === 'deployRequested_delayPercentile_75')[0], {
        labels: {
            channel: row.channel,
            deployedToAlice: String(row.deployedToAlice),
            deployedToOrganizationChats: String(row.deployedToOrganizationChats),
            sensor: 'deployRequested_delayPercentile_75',
        },
        ts,
        value: 2,
    });

    t.deepEqual(sensors.filter(s => s.labels.sensor === 'deployRequested_delayPercentile_90')[0], {
        labels: {
            channel: row.channel,
            deployedToAlice: String(row.deployedToAlice),
            deployedToOrganizationChats: String(row.deployedToOrganizationChats),
            sensor: 'deployRequested_delayPercentile_90',
        },
        ts,
        value: 3,
    });

    t.deepEqual(sensors.filter(s => s.labels.sensor === 'deployRequested_delayPercentile_95')[0], {
        labels: {
            channel: row.channel,
            deployedToAlice: String(row.deployedToAlice),
            deployedToOrganizationChats: String(row.deployedToOrganizationChats),
            sensor: 'deployRequested_delayPercentile_95',
        },
        ts,
        value: 4,
    });

    t.deepEqual(sensors.filter(s => s.labels.sensor === 'deployRequested_delayPercentile_99')[0], {
        labels: {
            channel: row.channel,
            deployedToAlice: String(row.deployedToAlice),
            deployedToOrganizationChats: String(row.deployedToOrganizationChats),
            sensor: 'deployRequested_delayPercentile_99',
        },
        ts,
        value: 5,
    });

    t.deepEqual(sensors.filter(s => s.labels.sensor === 'deployRequested_delayPercentile_100')[0], {
        labels: {
            channel: row.channel,
            deployedToAlice: String(row.deployedToAlice),
            deployedToOrganizationChats: String(row.deployedToOrganizationChats),
            sensor: 'deployRequested_delayPercentile_100',
        },
        ts,
        value: 6,
    });
});
