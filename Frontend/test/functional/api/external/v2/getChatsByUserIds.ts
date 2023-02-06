/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { getServiceTicket } from '../../_helpers';
import { createImage, createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { callApi, respondsWithResult } from './_helpers';
import { ImageType } from '../../../../../db/tables/image';
import { Channel } from '../../../../../db/tables/settings';

const test = anyTest as TestInterface<{ serviceTicket: string }>;

test.before(async t => {
    const serviceTicket = await getServiceTicket();

    Object.assign(t.context, { serviceTicket });
});

test.beforeEach(wipeDatabase);

const route = '/users-chats/bulk/get';

const skillIds = [
    'e7962f78-82f3-45b5-a6aa-3073227fb608',
    '6d29ca48-08ed-447f-b464-bd41bbc8ffdc',
    '6828094f-6b20-4135-bce8-6fbc3442da3f',
    '2b6f82f2-26cf-47c0-9ac1-61f052496573',
    'd84fcd67-1f6d-483f-a913-36023fcbc7e4',
    'a30f2407-0d5a-47ce-bb20-47d419afcf60',
    'cd815f49-8f98-4208-9b0b-755d07d571fe',
];

test('getChatsByUserIds: Return skills list for one user', async t => {
    const skills = [];

    const user = await createUser({ id: '0001' });

    for (let i = 0; i < 5; i++) {
        const skill = await createSkill({
            name: `Skill ${i}`,
            id: skillIds[i],
            userId: user.id,
            onAir: true,
            channel: Channel.OrganizationChat,
        });

        skills.push(skill);
    }

    await createUser({ id: '1234' });
    await createSkill({
        name: 'Skill 5',
        id: skillIds[5],
        userId: '1234',
        onAir: true,
        channel: Channel.OrganizationChat,
    });

    const res = await callApi('post', route, { serviceTicket: t.context.serviceTicket }).send({
        userIds: [user.id],
    });

    t.true(Array.isArray(res.body));

    for (const resSkill of res.body[0]) {
        t.true(skillIds.slice(0, 5).includes(resSkill.id));
    }
});

test('getChatsByUserIds: Return skills list for many users', async t => {
    const skills = [];

    const user1 = await createUser({ id: '0001' });
    const user2 = await createUser({ id: '0002' });

    for (let i = 0; i < 3; i++) {
        const skill = await createSkill({
            name: `Skill ${i}`,
            id: skillIds[i],
            userId: user1.id,
            onAir: true,
            channel: Channel.OrganizationChat,
        });

        skills.push(skill);
    }

    for (let i = 3; i < 6; i++) {
        const skill = await createSkill({
            name: `Skill ${i}`,
            id: skillIds.slice(3, 6)[i],
            userId: user2.id,
            onAir: true,
            channel: Channel.OrganizationChat,
        });

        skills.push(skill);
    }

    await createUser({ id: '1234' });
    await createSkill({
        name: 'Skill 6',
        id: skillIds[6],
        userId: '1234',
        onAir: true,
        channel: Channel.OrganizationChat,
    });

    const res = await callApi('post', route, { serviceTicket: t.context.serviceTicket }).send({
        userIds: [user1.id, user2.id],
    });

    t.true(Array.isArray(res.body));

    for (const resSkill of res.body[0]) {
        t.true(skillIds.slice(0, 3).includes(resSkill.id));
    }

    for (const resSkill of res.body[1]) {
        t.true(skillIds.slice(3, 6).includes(resSkill.id));
    }
});

test('getChatsByUserIds: Return correct skill structure', async t => {
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.OrganizationChat,
        onAir: true,
    });
    const image = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    await skill.update({ logoId: image.id, backendSettings: { uri: 'https://example.com/webhook' } });

    const res = await callApi('post', route, { serviceTicket: t.context.serviceTicket }).send({
        userIds: [user.id],
    });

    respondsWithResult(
        [
            [
                {
                    id: skill.id,
                    channel: 'organizationChat',
                    salt: skill.salt,
                    name: skill.name,
                    useZora: true,
                    onAir: true,
                    botGuid: null,
                    isRecommended: true,
                    isVip: false,
                    logo: {
                        avatarId: '5182/429f04ace2d88763a581',
                    },
                    look: 'external',
                    monitoringType: 'nonmonitored',
                    openInNewTab: true,
                    surfaces: [],
                    storeUrl: 'https://localhost.msup.yandex.ru:8083/skills/',
                    voice: 'good_oksana',
                    useNLU: false,
                    averageRating: 0,
                    ratingHistogram: [0, 0, 0, 0, 0],
                    userReview: null,
                    accountLinking: null,
                    backendUrl: 'https://example.com/webhook',
                    functionId: null,
                    trusted: false,
                    secondaryTitle: '',
                    createdAt: skill.createdAt.getTime(),
                    featureFlags: [],
                    score: 0,
                    userId: skill.userId,
                    accessToSkillTesting: {
                        role: null,
                        hasAccess: false,
                    },
                    useStateStorage: false,
                    editorDescription: '',
                    editorName: '',
                    homepageBadgeTypes: [],
                    tags: [],
                },
            ],
        ],
        res,
        t,
    );
});

test('getChatsByUserIds: Return empty array if user not exists', async t => {
    const res = await callApi('post', route, { serviceTicket: t.context.serviceTicket }).send({
        userIds: ['1488'],
    });

    t.deepEqual(res.body, [[]]);
});
