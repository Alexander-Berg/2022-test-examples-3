/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import * as mem from 'mem';
import { getUserTicket, testUser } from '../../_helpers';
import { callApi, respondsWithResult } from '../_helpers';
import * as memento from '../../../../../services/memento';
import { createUser, createSkill, createFeed, wipeDatabase } from '../../../_helpers';
import { Channel } from '../../../../../db/tables/settings';
import { UserInstance } from '../../../../../db/tables/user';
import { UserNewsSourceOption } from '../../../../../types/NewsResource';
import * as utils from '../../../../../services/news/settings/newsSources/utils';
import { NewsFeedSettingsType } from '../../../../../db/tables/newsFeed';
import { getNewsSourceFeeds } from '../../../../../db/entities/news';
import * as blackbox from '../../../../../services/blackbox';

const test = anyTest as TestInterface<{
    userTicket: string;
    user: UserInstance;
}>;

test.before(async t => {
    sinon.restore();

    t.context.userTicket = await getUserTicket(testUser.oauthToken);
});

test.beforeEach(async t => {
    await wipeDatabase();
    // TODO remove after experiment is done
    sinon.replace(blackbox, 'isYandexoid', sinon.fake.resolves(true));

    t.context.user = await createUser({ id: testUser.uid });
    mem.clear(getNewsSourceFeeds);
});

test.afterEach.always(async t => {
    sinon.restore();
});

test('Get news source when newsSource and rubric is specified in memento', async t => {
    sinon.stub(memento, 'getUserNewsConfig').resolves({
        selectedNews: [
            {
                newsSource: 'slug1',
                rubric: 'topic1',
            },
        ],
    });

    const skill = await createSkill({
        userId: t.context.user.id,
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

    const res = await callApi('get', '/personal/news/config/options', t.context);

    respondsWithResult(
        {
            newsSourceOptions: [
                {
                    source: {
                        id: feed.id,
                        name: skill.name,
                        logoUrl: 'https://example.com',
                        description: 'Новости радио',
                    },
                    selected: true,
                } as UserNewsSourceOption,
            ],
        },
        res,
        t,
        200,
    );
});

test('Do not get feed if it is not specified for news scenario', async t => {
    sinon.stub(memento, 'getUserNewsConfig').resolves({
        selectedNews: [
            {
                newsSource: 'slug1',
                rubric: 'topic1',
            },
        ],
    });

    const skill = await createSkill({
        userId: t.context.user.id,
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

    const res = await callApi('get', '/personal/news/config/options', t.context);

    respondsWithResult(
        {
            newsSourceOptions: [],
        },
        res,
        t,
        200,
    );
});

test('Get news source when rubric is omited in memento and skill has default feed', async t => {
    sinon.stub(memento, 'getUserNewsConfig').resolves({
        selectedNews: [
            {
                newsSource: 'slug1',
            },
        ],
    });

    const skill = await createSkill({
        userId: t.context.user.id,
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

    skill.backendSettings = { ...skill.backendSettings, defaultFeed: feed.id };
    await skill.save();

    const res = await callApi('get', '/personal/news/config/options', t.context);

    respondsWithResult(
        {
            newsSourceOptions: [
                {
                    source: {
                        id: feed.id,
                        name: skill.name,
                        logoUrl: 'https://example.com',
                        description: 'Новости радио',
                    },
                    selected: true,
                } as UserNewsSourceOption,
            ],
        },
        res,
        t,
        200,
    );
});

test('Get several news sources (flashbriefingType = news) when newsSource and rubric is specified in memento', async t => {
    sinon.stub(memento, 'getUserNewsConfig').resolves({
        selectedNews: [
            {
                newsSource: 'slug1',
                rubric: 'topic1',
            },
        ],
    });

    const skill = await createSkill({
        userId: t.context.user.id,
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
    const feed2 = await createFeed({
        skillId: skill.id,
        topic: 'topic2',
        iconUrl: 'https://example.com',
        settingsTypes: [NewsFeedSettingsType.NEWS_SCENARIO],
    });

    const res = await callApi('get', '/personal/news/config/options', t.context);

    respondsWithResult(
        {
            newsSourceOptions: [
                {
                    source: {
                        id: feed2.id,
                        name: feed2.name,
                        description: 'Новости радио',
                        logoUrl: 'https://example.com',
                    },
                    selected: false,
                } as UserNewsSourceOption,
                {
                    source: {
                        id: feed.id,
                        name: feed.name,
                        description: 'Новости радио',
                        logoUrl: 'https://example.com',
                    },
                    selected: true,
                } as UserNewsSourceOption,
            ],
        },
        res,
        t,
        200,
    );
});

test('Get several news sources (flashbriefingType = text_news) when newsSource and rubric is specified in memento', async t => {
    sinon.stub(memento, 'getUserNewsConfig').resolves({
        selectedNews: [
            {
                newsSource: 'slug1',
                rubric: 'topic1',
            },
        ],
    });

    const skill = await createSkill({
        userId: t.context.user.id,
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'text_news',
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
    const feed2 = await createFeed({
        skillId: skill.id,
        topic: 'topic2',
        iconUrl: 'https://example.com',
        settingsTypes: [NewsFeedSettingsType.NEWS_SCENARIO],
    });

    const res = await callApi('get', '/personal/news/config/options', t.context);

    respondsWithResult(
        {
            newsSourceOptions: [
                {
                    source: {
                        id: feed2.id,
                        name: feed2.name,
                        description: skill.name,
                        logoUrl: 'https://example.com',
                    },
                    selected: false,
                } as UserNewsSourceOption,
                {
                    source: {
                        id: feed.id,
                        name: feed.name,
                        description: skill.name,
                        logoUrl: 'https://example.com',
                    },
                    selected: true,
                } as UserNewsSourceOption,
            ],
        },
        res,
        t,
        200,
    );
});

test('Should get personal source when it selected despite check function returns false', async t => {
    sinon.stub(memento, 'getUserNewsConfig').resolves({
        selectedNews: [
            {
                newsSource: '6e24a5bb-yandeks-novost',
                rubric: 'personal',
            },
        ],
    });

    sinon.stub(utils, 'shouldDisplayPersonalNewsSource').resolves(false);

    const skill = await createSkill({
        userId: t.context.user.id,
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

    const res = await callApi('get', '/personal/news/config/options', t.context);

    respondsWithResult(
        {
            newsSourceOptions: [
                {
                    source: {
                        id: feed.id,
                        name: skill.name,
                        logoUrl: 'https://example.com',
                        description: 'Новости радио',
                    },
                    selected: true,
                } as UserNewsSourceOption,
            ],
        },
        res,
        t,
        200,
    );
});

test('Should not get personal source when check function returns false', async t => {
    sinon.stub(memento, 'getUserNewsConfig').resolves({
        selectedNews: [
            {
                newsSource: 'slug1',
                rubric: 'personal',
            },
        ],
    });

    sinon.stub(utils, 'shouldDisplayPersonalNewsSource').resolves(false);

    const skill = await createSkill({
        userId: t.context.user.id,
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

    const res = await callApi('get', '/personal/news/config/options', t.context);

    respondsWithResult(
        {
            newsSourceOptions: [],
        },
        res,
        t,
        200,
    );
});

test('Should get personal source when check function returns true', async t => {
    sinon.stub(memento, 'getUserNewsConfig').resolves({
        selectedNews: [
            {
                newsSource: '6e24a5bb-yandeks-novost',
                rubric: 'topic2',
            },
        ],
    });

    sinon.stub(utils, 'shouldDisplayPersonalNewsSource').resolves(true);

    const skill = await createSkill({
        userId: t.context.user.id,
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

    const feed2 = await createFeed({
        skillId: skill.id,
        topic: 'topic2',
        iconUrl: 'https://example.com',
        settingsTypes: [NewsFeedSettingsType.NEWS_SCENARIO],
    });

    const res = await callApi('get', '/personal/news/config/options', t.context);

    respondsWithResult(
        {
            newsSourceOptions: [
                {
                    source: {
                        id: feed2.id,
                        name: feed2.name,
                        description: 'Новости радио',
                        logoUrl: 'https://example.com',
                    },
                    selected: true,
                } as UserNewsSourceOption,
                {
                    source: {
                        id: feed.id,
                        name: feed.name,
                        description: 'Новости радио',
                        logoUrl: 'https://example.com',
                    },
                    selected: false,
                } as UserNewsSourceOption,
            ],
        },
        res,
        t,
        200,
    );
});
