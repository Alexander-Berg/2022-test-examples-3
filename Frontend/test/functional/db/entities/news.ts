/* eslint-disable */
import test from 'ava';
import * as mem from 'mem';
import { getNewsSourceFeeds } from '../../../../db/entities/news';
import { wipeDatabase, createSkill, createUser, createFeed } from '../../_helpers';
import { Channel } from '../../../../db/tables/settings';

test.beforeEach(async() => {
    await wipeDatabase();
    mem.clear(getNewsSourceFeeds);
});

test('getNewsSourceFeeds: works', async t => {
    const feeds = await getNewsSourceFeeds();

    t.is(feeds.length, 0);
});

test('getNewsSourceFeeds: gets feeds', async t => {
    await createUser();
    const skill = await createSkill({
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
    });

    await createFeed({ skillId: skill.id });

    const feeds = await getNewsSourceFeeds();

    t.is(feeds.length, 1);
});

test('getNewsSourceFeeds: not get disabled feeds', async t => {
    await createUser();
    const skill = await createSkill({
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
    });

    await createFeed({ skillId: skill.id, enabled: false });

    const feeds = await getNewsSourceFeeds();

    t.is(feeds.length, 0);
});

test('getNewsSourceFeeds: do not get disabled feeds but get enabled from same skill', async t => {
    await createUser();
    const skill = await createSkill({
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
    });

    await createFeed({ skillId: skill.id });
    await createFeed({ skillId: skill.id, enabled: false });

    const feeds = await getNewsSourceFeeds();

    t.is(feeds.length, 1);
    t.true(feeds[0].enabled);
});

test("getNewsSourceFeeds: don't get feeds from skill without flashBriefingType", async t => {
    await createUser();
    const skill = await createSkill({
        channel: Channel.NewsSkill,
        backendSettings: {},
        onAir: true,
    });

    await createFeed({ skillId: skill.id });

    const feeds = await getNewsSourceFeeds();

    t.is(feeds.length, 0);
});

test('getNewsSourceFeeds: get specified feeds', async t => {
    await createUser();
    const skill = await createSkill({
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
    });

    const feed = await createFeed({
        skillId: skill.id,
        topic: 'b',
    });
    await createFeed({
        skillId: skill.id,
        topic: 'a',
    });

    const feeds = await getNewsSourceFeeds({ ids: [feed.id] });

    t.is(feeds.length, 1);
    t.is(feeds[0].id, feed.id);
});
