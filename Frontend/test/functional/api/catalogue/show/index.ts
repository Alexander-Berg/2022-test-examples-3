/* eslint-disable */
import anyTest, {ExecutionContext, TestInterface} from 'ava';
import {getUserTicket, testUser} from '../../_helpers';
import {callApi, respondsWithResult} from '../_helpers';
import * as memento from '../../../../../services/memento';
import * as sinon from 'sinon';
import {
    createFeed,
    createImageForSkill,
    createShareForSkill,
    createShowFeed,
    createSkill,
    createUser,
    wipeDatabase,
} from '../../../_helpers';
import {Channel, FlashBriefingType, SkillAccess} from '../../../../../db/tables/settings';
import {UserInstance} from '../../../../../db/tables/user';
import {UserNewsSourceOption} from '../../../../../types/NewsResource';
import * as fixtures from '../../../../../fixtures/show';
import {ShowThemeOption} from '../../../../../fixtures/show';
import {NewsPiece} from '../../../../../types/UserNewsConfig';
import {NewsFeedSettingsType} from '../../../../../db/tables/newsFeed';
import {getNewsSourceFeeds} from '../../../../../db/entities/news';
import * as mem from 'mem';
import * as blackbox from '../../../../../services/blackbox';
import {
    MorningShowSkillFeed,
    MorningShowSkillFeedOption,
    MorningShowSkillFeedSerialized,
} from '../../../../../fixtures/morningShowSkills';
import {SkillsPiece} from '../../../../../types/UserSkillsConfig';
import {ShowFeedType} from "../../../../../db/tables/showFeed";
import uuid = require('uuid');

interface Context {
    userTicket: string;
    user: UserInstance;
}

const test = anyTest as TestInterface<Context>;

test.before(async (t) => {
    sinon.restore();

    t.context.userTicket = await getUserTicket(testUser.oauthToken);
});

test.beforeEach(async (t) => {
    await wipeDatabase();

    // TODO remove after experiment is done
    sinon.replace(blackbox, 'isYandexoid', sinon.fake.resolves(true));

    t.context.user = await createUser({id: testUser.uid});

    mem.clear(getNewsSourceFeeds);
});

test.afterEach.always(async (t) => {
    sinon.restore();
});

const skillSlug = 'slug1';
const skillName = 'name1';
const feedTopic = 'topic1';
const feedId = uuid();

const defaultExpectedNewsWithNotSelected = [
    {
        source: {
            id: feedId,
            name: skillName,
            logoUrl: 'https://example.com',
            description: 'Новости радио',
        },
        selected: false,
    },
];

const defaultExpectedNewsWithSelected = [
    {
        source: {
            id: feedId,
            name: skillName,
            logoUrl: 'https://example.com',
            description: 'Новости радио',
        },
        selected: true,
    },
];

const defaultExpectedTextNewsWithSelected = [
    {
        source: {
            id: feedId,
            name: skillName,
            logoUrl: 'https://example.com',
        },
        selected: true,
    },
];

const defaultExpectedThemesWithNotSelected = [
    {
        theme: {
            name: 'Фантастика',
            id: 'sci_fi',
            logoUrl: 'https://avatars.mds.yandex.net/get-dialogs/1530877/af2da34d88c3382979aa/orig',
        },
        selected: false,
    },
];

const defaultExpectedThemesWithSelected = [
    {
        theme: {
            name: 'Фантастика',
            id: 'sci_fi',
            logoUrl: 'https://avatars.mds.yandex.net/get-dialogs/1530877/af2da34d88c3382979aa/orig',
        },
        selected: true,
    },
];

// test data, can be used in tests
const testMorningShowSkillFeeds: MorningShowSkillFeed[] = [
    {
        id: '47718ad7-ee3e-4e05-94ec-dbbd8e5c5cf7',
        name: 'Утреннее шоу Олега Дулина',
        description: 'Тестовое шоу',
        skill: {
            skillId: '47718ad7-ee3e-4e05-94ec-dbbd8e5c5cf7',
            skillName: 'Олег Дулин',
            skillslug: '20de2906-oleg-dulin',
            logoUrl: 'https://avatars.mds.yandex.net/get-dialogs/1525540/bd0e3727e55a2eb476a3/orig',
        },
    },
    {
        id: '6a6537aa-66c6-40e2-b9ab-7ac680aaa5a5',
        name: 'Нейрогороскоп',
        description: 'Cлушайте каждый день гороскоп, сгенерированный нейросетью',
        skill: {
            skillId: '6a6537aa-66c6-40e2-b9ab-7ac680aaa5a5',
            skillslug: 'd35efb90-pustyshka-pervy',
            logoUrl: 'https://avatars.mds.yandex.net/get-dialogs/1027858/a6d3dec5720abe091d62/orig',
            skillName: 'Нейрогороскоп',
        },
    },
    {
        id: 'b0b6255d-b1e7-4542-ab66-cd36a41ddb51',
        name: 'Слово дня от SkyEng',
        description: 'Cлушайте слово дня От SkyEng',
        skill: {
            skillId: 'b0b6255d-b1e7-4542-ab66-cd36a41ddb51',
            skillslug: 'c07d154a-skyeng-dev',
            logoUrl: 'https://avatars.mds.yandex.net/get-dialogs/1027858/f6a48463480048234f75/orig',
            skillName: 'Skyeng Beta',
        },
    },
    {
        id: '987f867f-76f8-477a-82d2-25b6b03ed1e0',
        name: 'Шпагат за месяц',
        description:
            'Следуя упражнениям и рекомендациям навыка вы сможете сесть на шпагат за 30 дней. Помимо плана тренировок на каждый день навык может дать лайфхаки как правильно садиться на шпагат какие есть правила и противопоказания, а так же будет следить за вашим прогрессом.',
        skill: {
            skillId: '987f867f-76f8-477a-82d2-25b6b03ed1e0',
            skillslug: '6b87feed-shpagat-za-mesya',
            logoUrl: 'https://avatars.mds.yandex.net/get-dialogs/1017510/c88c20bc158830681a16/orig',
            skillName: 'Шпагат за месяц',
        },
    },
];

const mapFromSkillFeedToFeedSerialized = (
    feed: MorningShowSkillFeed,
    idMap: Map<string, string> = new Map(),
): MorningShowSkillFeedSerialized => ({
    id: idMap.get(feed.id ?? feed.skill.skillId) ?? feed.id ?? feed.skill.skillId,
    skillName: feed.skill.skillName,
    name: feed.name,
    description: feed.description,
    logoUrl: feed.skill.logoUrl,
});

const oneSelectedExpectedSkillFeedOption: MorningShowSkillFeedOption[] = [
    {
        feed: mapFromSkillFeedToFeedSerialized(testMorningShowSkillFeeds[0]),
        selected: true,
    },
    {
        feed: mapFromSkillFeedToFeedSerialized(testMorningShowSkillFeeds[1]),
        selected: false,
    },
];

const allSkillFeedOptions = testMorningShowSkillFeeds.slice(0, 2).map((feed) => ({
    skillslug: feed.skill.skillslug,
    feedid: feed.id,
}));

const oneSkillFeedOptions = [
    {
        skillslug: testMorningShowSkillFeeds[0].skill.skillslug,
        feedid: testMorningShowSkillFeeds[0].id,
    },
];

const allSelectedExpectedSkillFeedOptions = oneSelectedExpectedSkillFeedOption.map((opt) => ({
    feed: opt.feed,
    selected: true,
}));

const createMorningShowSkillsForTests = async (
    morningShowSkillFeeds: MorningShowSkillFeed[],
    userId: string,
    skillAccess: SkillAccess = SkillAccess.Public,
    onAir: boolean = true,
    shareWithUsers: UserInstance[][] = [],
) => {
    const mapToCreateAndCreatedSkills: Map<string, string> = new Map<string, string>();
    for (let i = 0; i < morningShowSkillFeeds.length; i++) {
        const showToCreate = morningShowSkillFeeds[i];
        const skillInstance = await createSkill({
            id: showToCreate.skill.skillId,
            name: showToCreate.skill.skillName,
            userId,
            skillAccess,
            channel: Channel.MorningShow,
            onAir,
            slug: showToCreate.skill.skillslug,
        });
        skillInstance.logoId = (
            await createImageForSkill(skillInstance, showToCreate.skill.logoUrl)
        ).id;
        await skillInstance.save();
        for (const userInstance of shareWithUsers[i] ?? []) {
            await createShareForSkill(skillInstance, userInstance);
        }
        await createShowFeed({
            id: showToCreate.id,
            skillId: skillInstance.id,
            name: showToCreate.name,
            description: showToCreate.description,
            type: ShowFeedType.MORNING,
            onAir: true
        })
        mapToCreateAndCreatedSkills.set(showToCreate.skill.skillId, skillInstance.id);
    }
    return mapToCreateAndCreatedSkills;
};

const configureGetOptionsTest = (
    selectedNews: NewsPiece[],
    expectedNews: UserNewsSourceOption[],
    newsOff: boolean,
    selectedSkillFeeds: SkillsPiece[],
    expectedSkillFeeds: MorningShowSkillFeedOption[],
    skillFeedsOff: boolean,
    selectedThemes: string[],
    expectedThemes: ShowThemeOption[],
    themesOff: boolean,
    flashBriefingType: FlashBriefingType = 'news',
    settingsType: NewsFeedSettingsType = NewsFeedSettingsType.MORNING_SHOW_SCENARIO,
    existingSkills: MorningShowSkillFeed[] = [],
) => async (t: ExecutionContext<Context>) => {
    const skill = await createSkill({
        name: skillName,
        userId: t.context.user.id,
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType,
        },
        onAir: true,
        slug: skillSlug,
    });

    await createFeed({
        id: feedId,
        skillId: skill.id,
        topic: feedTopic,
        iconUrl: 'https://example.com',
        settingsTypes: [settingsType],
    });

    const mapToCreateAndCreatedSkills: Map<string, string> = await createMorningShowSkillsForTests(
        existingSkills,
        t.context.user.id,
    );

    expectedSkillFeeds = expectedSkillFeeds.map((option) => ({
        feed: {
            id: mapToCreateAndCreatedSkills.get(option.feed.id)!,
            skillName: option.feed.skillName,
            name: option.feed.name,
            description: option.feed.description,
            logoUrl: option.feed.logoUrl,
        },
        selected: option.selected,
    }));

    selectedSkillFeeds = selectedSkillFeeds.map((skillFeed) => ({
        skillslug: skillFeed.skillslug,
        feedid: mapToCreateAndCreatedSkills.get(skillFeed.feedid) ?? skillFeed.feedid,
    }));

    sinon.stub(memento, 'getUserShowConfig').resolves({
        selectedNews,
        selectedSkillFeeds,
        selectedThemes,
        newsOff,
        skillFeedsOff,
        themesOff,
    });

    sinon.replace(fixtures, 'showThemeItems', [defaultExpectedThemesWithNotSelected[0].theme]);

    const res = await callApi('get', '/personal/show/config/options', t.context);

    respondsWithResult(
        {
            newsSourceOptions: expectedNews,
            showThemeOptions: expectedThemes,
            skillFeedOptions: expectedSkillFeeds,
            newsOff: false,
            skillFeedsOff: false,
            themesOff: false,
        },
        res,
        t,
        200,
    );
};

test(
    'Get show config when newsSource and themes are specified in memento',
    configureGetOptionsTest(
        [
            {
                newsSource: 'slug1',
                rubric: 'topic1',
            },
        ],
        defaultExpectedNewsWithSelected,
        false,
        [],
        [],
        false,
        ['sci_fi'],
        defaultExpectedThemesWithSelected,
        false,
    ),
);

test(
    'Get show config when newsSource, skillFeeds and themes are specified in memento',
    configureGetOptionsTest(
        [
            {
                newsSource: 'slug1',
                rubric: 'topic1',
            },
        ],
        defaultExpectedNewsWithSelected,
        false,
        allSkillFeedOptions,
        allSelectedExpectedSkillFeedOptions,
        false,
        ['sci_fi'],
        defaultExpectedThemesWithSelected,
        false,
        'news',
        NewsFeedSettingsType.MORNING_SHOW_SCENARIO,
        testMorningShowSkillFeeds.slice(0, 2),
    ),
);

test(
    'Do not include radionews title if skill provides text news',
    configureGetOptionsTest(
        [
            {
                newsSource: 'slug1',
                rubric: 'topic1',
            },
        ],
        defaultExpectedTextNewsWithSelected,
        false,
        [],
        [],
        false,
        ['sci_fi'],
        defaultExpectedThemesWithSelected,
        false,
        'text_news',
    ),
);

test(
    'Get show config when only themes are specified in memento',
    configureGetOptionsTest(
        [],
        defaultExpectedNewsWithNotSelected,
        false,
        [],
        [],
        false,
        ['sci_fi'],
        defaultExpectedThemesWithSelected,
        false,
    ),
);

test(
    'Get show config when only news specified in memento',
    configureGetOptionsTest(
        [
            {
                newsSource: 'slug1',
                rubric: 'topic1',
            },
        ],
        defaultExpectedNewsWithSelected,
        false,
        [],
        [],
        false,
        [],
        defaultExpectedThemesWithNotSelected,
        false,
    ),
);

test(
    'Get show config when only skill feeds are specified in memento',
    configureGetOptionsTest(
        [],
        defaultExpectedNewsWithNotSelected,
        false,
        allSkillFeedOptions,
        allSelectedExpectedSkillFeedOptions,
        false,
        [],
        defaultExpectedThemesWithNotSelected,
        false,
        'news',
        NewsFeedSettingsType.MORNING_SHOW_SCENARIO,
        testMorningShowSkillFeeds.slice(0, 2),
    ),
);

test(
    'Get show config when some skill feeds from memento are missing',
    configureGetOptionsTest(
        [],
        [
            {
                source: {
                    id: feedId,
                    name: skillName,
                    logoUrl: 'https://example.com',
                    description: 'Новости радио',
                },
                selected: false,
            },
        ],
        false,
        allSkillFeedOptions.concat({
            skillslug: 'foo',
            feedid: 'bar',
        }),
        allSelectedExpectedSkillFeedOptions,
        false,
        [],
        defaultExpectedThemesWithNotSelected,
        false,
        'news',
        NewsFeedSettingsType.MORNING_SHOW_SCENARIO,
        testMorningShowSkillFeeds.slice(0, 2),
    ),
);

test(
    'Get show config when only part of skill feeds are specified in memento',
    configureGetOptionsTest(
        [],
        defaultExpectedNewsWithNotSelected,
        false,
        oneSkillFeedOptions,
        oneSelectedExpectedSkillFeedOption,
        false,
        [],
        defaultExpectedThemesWithNotSelected,
        false,
        'news',
        NewsFeedSettingsType.MORNING_SHOW_SCENARIO,
        testMorningShowSkillFeeds.slice(0, 2),
    ),
);

test(
    'Get show config when nothing is specified in memento',
    configureGetOptionsTest(
        [],
        defaultExpectedNewsWithNotSelected,
        false,
        [],
        [],
        false,
        [],
        defaultExpectedThemesWithNotSelected,
        false,
    ),
);

test(
    'Do not get news source specified for another settings type',
    configureGetOptionsTest(
        [],
        [],
        false,
        [],
        [],
        false,
        [],
        defaultExpectedThemesWithNotSelected,
        false,
        'news',
        NewsFeedSettingsType.NEWS_SCENARIO,
    ),
);

test('Save news to memento', async (t) => {
    const stub = sinon.stub(memento, 'setUserShowConfig').resolves();

    sinon.replace(fixtures, 'showThemeItems', [
        {
            name: 'Фантастика',
            id: 'sci_fi',
            logoUrl: 'https://avatars.mds.yandex.net/get-dialogs/1530877/af2da34d88c3382979aa/orig',
        },
    ]);

    const skill = await createSkill({
        userId: t.context.user.id,
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: 'slug',
    });

    const feed = await createFeed({
        id: feedId,
        skillId: skill.id,
        topic: 'topic',
        iconUrl: 'https://example.com',
    });

    const res = await callApi('post', '/personal/show/config', t.context).send({
        selectedNewsSources: [feed.id],
        selectedShowThemes: [],
    });

    t.true(res.ok);
    t.true(
        stub.calledWith({
            userTicket: t.context.userTicket,
            showConfig: {
                selectedNews: [
                    {
                        newsSource: skill.slug,
                        rubric: feed.topic,
                    },
                ],
                selectedSkillFeeds: [],
                selectedThemes: [],
                newsOff: false,
                skillFeedsOff: false,
                themesOff: false,
            },
        }),
    );
});

test('Save themes to memento', async (t) => {
    const stub = sinon.stub(memento, 'setUserShowConfig').resolves();

    sinon.replace(fixtures, 'showThemeItems', [
        {
            name: 'Фантастика',
            id: 'sci_fi',
            logoUrl: 'https://avatars.mds.yandex.net/get-dialogs/1530877/af2da34d88c3382979aa/orig',
        },
    ]);

    const skill = await createSkill({
        userId: t.context.user.id,
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: 'slug',
    });

    const feed = await createFeed({
        id: feedId,
        skillId: skill.id,
        topic: 'topic',
        iconUrl: 'https://example.com',
    });

    const res = await callApi('post', '/personal/show/config', t.context).send({
        selectedNewsSources: [feed.id],
        selectedShowThemes: ['sci_fi'],
    });

    t.true(res.ok);
    t.true(
        stub.calledWith({
            userTicket: t.context.userTicket,
            showConfig: {
                selectedNews: [
                    {
                        newsSource: skill.slug,
                        rubric: feed.topic,
                    },
                ],
                selectedSkillFeeds: [],
                selectedThemes: ['sci_fi'],
                newsOff: false,
                skillFeedsOff: false,
                themesOff: false,
            },
        }),
    );
});

test('Save skill feeeds to memento', async (t) => {
    const stub = sinon.stub(memento, 'setUserShowConfig').resolves();

    sinon.replace(fixtures, 'showThemeItems', [
        {
            name: 'Фантастика',
            id: 'sci_fi',
            logoUrl: 'https://avatars.mds.yandex.net/get-dialogs/1530877/af2da34d88c3382979aa/orig',
        },
    ]);

    const skill = await createSkill({
        userId: t.context.user.id,
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: 'slug',
    });

    const feed = await createFeed({
        id: feedId,
        skillId: skill.id,
        topic: 'topic',
        iconUrl: 'https://example.com',
    });

    const skillMap = await createMorningShowSkillsForTests(
        testMorningShowSkillFeeds.slice(0, 2),
        t.context.user.id,
    );
    const selectedSkillFeeds = [skillMap.get(testMorningShowSkillFeeds[0].id)!];

    const res = await callApi('post', '/personal/show/config', t.context).send({
        selectedNewsSources: [feed.id],
        selectedShowThemes: ['sci_fi'],
        selectedSkillFeeds,
    });

    t.true(res.ok);
    t.true(
        stub.calledWith({
            userTicket: t.context.userTicket,
            showConfig: {
                selectedNews: [
                    {
                        newsSource: skill.slug,
                        rubric: feed.topic,
                    },
                ],
                selectedSkillFeeds: oneSkillFeedOptions.map((option) => ({
                    skillslug: option.skillslug,
                    feedid: skillMap.get(option.feedid)!,
                })),
                selectedThemes: ['sci_fi'],
                newsOff: false,
                skillFeedsOff: false,
                themesOff: false,
            },
        }),
    );
});

test('Do not save unknown themes', async (t) => {
    const stub = sinon.stub(memento, 'setUserShowConfig').resolves();

    sinon.replace(fixtures, 'showThemeItems', [
        {
            name: 'Фантастика',
            id: 'sci_fi',
            logoUrl: 'https://avatars.mds.yandex.net/get-dialogs/1530877/af2da34d88c3382979aa/orig',
        },
    ]);

    const skill = await createSkill({
        userId: t.context.user.id,
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: 'slug',
    });

    const feed = await createFeed({
        id: feedId,
        skillId: skill.id,
        topic: 'topic',
        iconUrl: 'https://example.com',
    });

    const res = await callApi('post', '/personal/show/config', t.context).send({
        selectedNewsSources: [feed.id],
        selectedShowThemes: ['lol'],
    });

    t.true(res.ok);
    t.true(
        stub.calledWith({
            userTicket: t.context.userTicket,
            showConfig: {
                selectedNews: [
                    {
                        newsSource: skill.slug,
                        rubric: feed.topic,
                    },
                ],
                selectedSkillFeeds: [],
                selectedThemes: [],
                newsOff: false,
                skillFeedsOff: false,
                themesOff: false,
            },
        }),
    );
});

test('Disable news in memento', async (t) => {
    const stub = sinon.stub(memento, 'setUserShowConfig').resolves();

    sinon.replace(fixtures, 'showThemeItems', [
        {
            name: 'Фантастика',
            id: 'sci_fi',
            logoUrl: 'https://avatars.mds.yandex.net/get-dialogs/1530877/af2da34d88c3382979aa/orig',
        },
    ]);

    const skill = await createSkill({
        userId: t.context.user.id,
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: 'slug',
    });

    const feed = await createFeed({
        id: feedId,
        skillId: skill.id,
        topic: 'topic',
        iconUrl: 'https://example.com',
    });

    const res = await callApi('post', '/personal/show/config', t.context).send({
        selectedNewsSources: [feed.id],
        selectedShowThemes: ['sci_fi'],
        themesOff: false,
        newsOff: true,
    });

    t.true(res.ok);
    t.true(
        stub.calledWith({
            userTicket: t.context.userTicket,
            showConfig: {
                selectedNews: [
                    {
                        newsSource: skill.slug,
                        rubric: feed.topic,
                    },
                ],
                selectedSkillFeeds: [],
                selectedThemes: ['sci_fi'],
                themesOff: false,
                skillFeedsOff: false,
                newsOff: true,
            },
        }),
    );
});

test('Disable themes in memento', async (t) => {
    const stub = sinon.stub(memento, 'setUserShowConfig').resolves();

    sinon.replace(fixtures, 'showThemeItems', [
        {
            name: 'Фантастика',
            id: 'sci_fi',
            logoUrl: 'https://avatars.mds.yandex.net/get-dialogs/1530877/af2da34d88c3382979aa/orig',
        },
    ]);

    const skill = await createSkill({
        userId: t.context.user.id,
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: 'slug',
    });

    const feed = await createFeed({
        id: feedId,
        skillId: skill.id,
        topic: 'topic',
        iconUrl: 'https://example.com',
    });

    const res = await callApi('post', '/personal/show/config', t.context).send({
        selectedNewsSources: [feed.id],
        selectedShowThemes: ['sci_fi'],
        themesOff: true,
        newsOff: false,
    });

    t.true(res.ok);
    t.true(
        stub.calledWith({
            userTicket: t.context.userTicket,
            showConfig: {
                selectedNews: [
                    {
                        newsSource: skill.slug,
                        rubric: feed.topic,
                    },
                ],
                selectedSkillFeeds: [],
                selectedThemes: ['sci_fi'],
                themesOff: true,
                skillFeedsOff: false,
                newsOff: false,
            },
        }),
    );
});

test('Get public and private skill feeds of user', async (t) => {
    const skillMap: Map<string, string> = new Map();
    const user1 = await createUser({id: '001'});

    await createMorningShowSkillsForTests(
        testMorningShowSkillFeeds.slice(0, 1),
        t.context.user.id,
        SkillAccess.Public,
    ).then((map) => map.forEach((val, key) => skillMap.set(key, val)));

    await createMorningShowSkillsForTests(
        testMorningShowSkillFeeds.slice(1, 2),
        t.context.user.id,
        SkillAccess.Private,
    ).then((map) => map.forEach((val, key) => skillMap.set(key, val)));

    await createMorningShowSkillsForTests(
        testMorningShowSkillFeeds.slice(2, 3),
        user1.id,
        SkillAccess.Public,
    ).then((map) => map.forEach((val, key) => skillMap.set(key, val)));

    await createMorningShowSkillsForTests(
        testMorningShowSkillFeeds.slice(3, 4),
        user1.id,
        SkillAccess.Private,
    ).then((map) => map.forEach((val, key) => skillMap.set(key, val)));

    const skill = await createSkill({
        name: skillName,
        userId: t.context.user.id,
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: skillSlug,
    });

    await createFeed({
        id: feedId,
        skillId: skill.id,
        topic: feedTopic,
        iconUrl: 'https://example.com',
        settingsTypes: [NewsFeedSettingsType.MORNING_SHOW_SCENARIO],
    });

    sinon.stub(memento, 'getUserShowConfig').resolves({
        selectedNews: [],
        selectedSkillFeeds: [],
        selectedThemes: [],
        newsOff: false,
        themesOff: false,
        skillFeedsOff: false,
    });

    sinon.replace(fixtures, 'showThemeItems', [defaultExpectedThemesWithNotSelected[0].theme]);

    const res = await callApi('get', '/personal/show/config/options', t.context);
    respondsWithResult(
        {
            newsSourceOptions: defaultExpectedNewsWithNotSelected,
            showThemeOptions: defaultExpectedThemesWithNotSelected,
            skillFeedOptions: testMorningShowSkillFeeds.slice(0, 3).map((feed) => ({
                feed: mapFromSkillFeedToFeedSerialized(feed, skillMap),
                selected: false,
            })),
            newsOff: false,
            skillFeedsOff: false,
            themesOff: false,
        },
        res,
        t,
        200,
    );
});

test('Get private skills by share', async (t) => {
    const skillMap: Map<string, string> = new Map();
    const user1 = await createUser({id: '001'});
    const user2 = await createUser({id: '002'});
    const user = t.context.user;

    await createMorningShowSkillsForTests(
        testMorningShowSkillFeeds.slice(0, 1),
        user1.id,
        SkillAccess.Private,
        true,
        [[]],
    ).then((map) => map.forEach((val, key) => skillMap.set(key, val)));

    await createMorningShowSkillsForTests(
        testMorningShowSkillFeeds.slice(1, 2),
        user1.id,
        SkillAccess.Private,
        true,
        [[user]],
    ).then((map) => map.forEach((val, key) => skillMap.set(key, val)));

    await createMorningShowSkillsForTests(
        testMorningShowSkillFeeds.slice(2, 3),
        user1.id,
        SkillAccess.Private,
        true,
        [[user2, user]],
    ).then((map) => map.forEach((val, key) => skillMap.set(key, val)));

    await createMorningShowSkillsForTests(
        testMorningShowSkillFeeds.slice(3, 4),
        user1.id,
        SkillAccess.Private,
        true,
        [[user2]],
    ).then((map) => map.forEach((val, key) => skillMap.set(key, val)));

    const skill = await createSkill({
        name: skillName,
        userId: t.context.user.id,
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: skillSlug,
    });

    await createFeed({
        id: feedId,
        skillId: skill.id,
        topic: feedTopic,
        iconUrl: 'https://example.com',
        settingsTypes: [NewsFeedSettingsType.MORNING_SHOW_SCENARIO],
    });

    sinon.stub(memento, 'getUserShowConfig').resolves({
        selectedNews: [],
        selectedSkillFeeds: [],
        selectedThemes: [],
        newsOff: false,
        themesOff: false,
        skillFeedsOff: false,
    });

    sinon.replace(fixtures, 'showThemeItems', [defaultExpectedThemesWithNotSelected[0].theme]);

    const res = await callApi('get', '/personal/show/config/options', t.context);
    respondsWithResult(
        {
            newsSourceOptions: defaultExpectedNewsWithNotSelected,
            showThemeOptions: defaultExpectedThemesWithNotSelected,
            skillFeedOptions: testMorningShowSkillFeeds.slice(1, 3).map((feed) => ({
                feed: mapFromSkillFeedToFeedSerialized(feed, skillMap),
                selected: false,
            })),
            newsOff: false,
            skillFeedsOff: false,
            themesOff: false,
        },
        res,
        t,
        200,
    );
});

test('Test sorting of skill feed options', async (t) => {
    const skillMap: Map<string, string> = new Map();
    const user = t.context.user;

    let seq = [2, 0, 3, 1]
    for (let i = 0; i < seq.length; i++) {
        let idx = seq[i];
        await createMorningShowSkillsForTests(
            testMorningShowSkillFeeds.slice(idx, idx + 1),
            user.id,
            SkillAccess.Public,
            true,
            [[]],
        ).then((map) => map.forEach((val, key) => skillMap.set(key, val)));
    }

    sinon.stub(memento, 'getUserShowConfig').resolves({
        selectedNews: [],
        selectedSkillFeeds: [],
        selectedThemes: [],
        newsOff: false,
        themesOff: false,
        skillFeedsOff: false,
    });

    const skill = await createSkill({
        name: skillName,
        userId: t.context.user.id,
        channel: Channel.NewsSkill,
        backendSettings: {
            flashBriefingType: 'news',
        },
        onAir: true,
        slug: skillSlug,
    });

    await createFeed({
        id: feedId,
        skillId: skill.id,
        topic: feedTopic,
        iconUrl: 'https://example.com',
        settingsTypes: [NewsFeedSettingsType.MORNING_SHOW_SCENARIO],
    });

    sinon.replace(fixtures, 'showThemeItems', [defaultExpectedThemesWithNotSelected[0].theme]);

    const res = await callApi('get', '/personal/show/config/options', t.context);

    const expectedSkillFeedOptions = seq.map(idx => ({
        feed: mapFromSkillFeedToFeedSerialized(testMorningShowSkillFeeds[idx], skillMap),
        selected: false
    }))

    // just to check wrong
    // const expectedSkillFeedOptions = testMorningShowSkillFeeds.map(feed => ({
    //     feed: mapFromSkillFeedToFeedSerialized(feed, skillMap),
    //     selected: false
    // }))

    respondsWithResult(
        {
            newsSourceOptions: defaultExpectedNewsWithNotSelected,
            showThemeOptions: defaultExpectedThemesWithNotSelected,
            skillFeedOptions: expectedSkillFeedOptions,
            newsOff: false,
            skillFeedsOff: false,
            themesOff: false,
        },
        res,
        t,
        200,
    );
});


