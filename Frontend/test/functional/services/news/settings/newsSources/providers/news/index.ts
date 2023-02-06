/* eslint-disable */
import test from 'ava';
import * as mem from 'mem';
import { getNewsSourceFeeds } from '../../../../../../../../db/entities/news';
import { NewsFeedSettingsType } from '../../../../../../../../db/tables/newsFeed';
import { Channel } from '../../../../../../../../db/tables/settings';
import { getNewsScenarioSourcesResolver } from '../../../../../../../../services/news/settings/newsSources/providers/news';
import { makeSerializedFeeds } from '../../../../../../../../services/news/settings/newsSources/providers/utils';
import { removePersonalFeed } from '../../../../../../../../services/news/settings/newsSources/utils';
import {
    createFeed,
    createSkill,
    createUser,
    wipeDatabase,
} from '../../../../../../../functional/_helpers';

test.beforeEach(async() => {
    await wipeDatabase();
    mem.clear(getNewsSourceFeeds);
});

test('getUserNewsSourceOptions: get selected feed', async t => {
    await createUser();
    const skill = await createSkill({
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: 'slug1',
    });

    const feed = await createFeed({
        skillId: skill.id,
        topic: 'topic1',
        iconUrl: 'https://example.com',
        settingsTypes: [NewsFeedSettingsType.NEWS_SCENARIO],
    });

    const resolver = getNewsScenarioSourcesResolver({ filterMapper: makeSerializedFeeds });

    const newsSourceOptions = await resolver.getNewsSources([
        {
            newsSource: 'slug1',
            rubric: 'topic1',
        },
    ]);

    t.deepEqual(newsSourceOptions, [
        {
            selected: true,
            source: {
                id: feed.id,
                name: skill.name,
                description: 'Новости радио',
                logoUrl: 'https://example.com',
            },
        },
    ]);
});

test('getUserNewsSourceOptions: do not get feed not specified for news', async t => {
    await createUser();
    const skill = await createSkill({
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: 'slug1',
    });

    await createFeed({
        skillId: skill.id,
        topic: 'topic1',
        iconUrl: 'https://example.com',
        settingsTypes: [],
    });

    const resolver = getNewsScenarioSourcesResolver({ filterMapper: makeSerializedFeeds });

    const newsSourceOptions = await resolver.getNewsSources([
        {
            newsSource: 'slug1',
            rubric: 'topic1',
        },
    ]);

    t.deepEqual(newsSourceOptions, []);
});

test('getUserNewsSourceOptions: get not selected feed', async t => {
    await createUser();
    const skill = await createSkill({
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: 'slug1',
    });

    const feed = await createFeed({
        skillId: skill.id,
        topic: 'topic1',
        iconUrl: 'https://example.com',
        settingsTypes: [NewsFeedSettingsType.NEWS_SCENARIO],
    });

    const resolver = getNewsScenarioSourcesResolver({ filterMapper: makeSerializedFeeds });

    const newsSourceOptions = await resolver.getNewsSources([
        {
            newsSource: 'slug2',
            rubric: 'topic1',
        },
    ]);

    t.deepEqual(newsSourceOptions, [
        {
            selected: false,
            source: {
                id: feed.id,
                name: skill.name,
                description: 'Новости радио',
                logoUrl: 'https://example.com',
            },
        },
    ]);
});

test('getUserNewsSourceOptions: get several feeds with createdAt order', async t => {
    await createUser();
    const skill = await createSkill({
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: 'slug1',
        name: 'test',
    });

    const feed1 = await createFeed({
        skillId: skill.id,
        topic: 'topic1',
        iconUrl: 'https://example.com',
        name: 'test',
        settingsTypes: [NewsFeedSettingsType.NEWS_SCENARIO],
    });

    const feed2 = await createFeed({
        skillId: skill.id,
        topic: 'topic2',
        iconUrl: 'https://example.com',
        settingsTypes: [NewsFeedSettingsType.NEWS_SCENARIO],
    });
    const resolver = getNewsScenarioSourcesResolver({ filterMapper: makeSerializedFeeds });

    const newsSourceOptions = await resolver.getNewsSources([
        {
            newsSource: 'slug1',
            rubric: 'topic1',
        },
    ]);

    t.deepEqual(newsSourceOptions, [
        {
            selected: false,
            source: {
                id: feed2.id,
                name: feed2.name,
                description: 'Новости радио',
                logoUrl: 'https://example.com',
            },
        },
        {
            selected: true,
            source: {
                id: feed1.id,
                name: feed1.name,
                description: 'Новости радио',
                logoUrl: 'https://example.com',
            },
        },
    ]);
});

test('getUserNewsSourceOptions: get feeds with personal feed', async t => {
    await createUser();
    const skill = await createSkill({
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: '6e24a5bb-yandeks-novost',
    });

    const feed = await createFeed({
        skillId: skill.id,
        topic: 'personal',
        iconUrl: 'https://example.com',
        settingsTypes: [NewsFeedSettingsType.NEWS_SCENARIO],
    });

    const resolver = getNewsScenarioSourcesResolver({ filterMapper: makeSerializedFeeds });

    const newsSourceOptions = await resolver.getNewsSources([
        {
            newsSource: '6e24a5bb-yandeks-novost',
            rubric: 'personal',
        },
    ]);

    t.deepEqual(newsSourceOptions, [
        {
            selected: true,
            source: {
                id: feed.id,
                name: skill.name,
                description: 'Новости радио',
                logoUrl: 'https://example.com',
            },
        },
    ]);
});

test('getUserNewsSourceOptions: get feeds without personal feed', async t => {
    await createUser();
    const skill = await createSkill({
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: '6e24a5bb-yandeks-novost',
    });

    await createFeed({
        skillId: skill.id,
        topic: 'personal',
        iconUrl: 'https://example.com',
        settingsTypes: [NewsFeedSettingsType.NEWS_SCENARIO],
    });

    const resolver = getNewsScenarioSourcesResolver({
        filterMapper: sources => makeSerializedFeeds(removePersonalFeed(sources)),
    });

    const newsSourceOptions = await resolver.getNewsSources([]);

    t.deepEqual(newsSourceOptions, []);
});

test('getUserNewsSourceOptions: get feeds with personal unselected feed', async t => {
    await createUser();
    const skill = await createSkill({
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: '6e24a5bb-yandeks-novost',
    });

    const feed = await createFeed({
        skillId: skill.id,
        topic: 'personal',
        iconUrl: 'https://example.com',
        settingsTypes: [NewsFeedSettingsType.NEWS_SCENARIO],
    });

    const resolver = getNewsScenarioSourcesResolver({
        filterMapper: makeSerializedFeeds,
    });

    const newsSourceOptions = await resolver.getNewsSources([]);

    t.deepEqual(newsSourceOptions, [
        {
            selected: false,
            source: {
                id: feed.id,
                name: skill.name,
                logoUrl: 'https://example.com',
                description: 'Новости радио',
            },
        },
    ]);
});
