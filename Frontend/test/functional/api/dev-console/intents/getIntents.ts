/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { getUserTicket, testUser } from '../../_helpers';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { callApi } from '../_helpers';
import { DraftIntent, PublishedIntent } from '../../../../../db';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
});

test('Get empty published intents', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const res = await callApi('get', `/skills/${skill.id}/intents`, t.context);
    t.is(res.status, 200);
    t.deepEqual(res.body, { result: [] });
});

test('Get empty draft intents', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const res = await callApi('get', `/skills/${skill.id}/intents/drafts`, t.context);
    t.is(res.status, 200);
    t.deepEqual(res.body, { result: [] });
});

test('Get one intent', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const intent = await PublishedIntent.create({
        skillId: skill.id,
        humanReadableName: 'Название формы',
        formName: 'paskills.test',
    });
    const res = await callApi('get', `/skills/${skill.id}/intents`, t.context);
    t.is(res.status, 200);
    t.deepEqual(res.body, {
        result: [
            {
                id: intent.id,
                humanReadableName: 'Название формы',
                isActivation: false,
                status: 'INVALID_GRAMMAR',
            },
        ],
    });

    const getDraftsResponse = await callApi('get', `/skills/${skill.id}/intents/drafts`, t.context);
    t.is(getDraftsResponse.status, 200);
    t.deepEqual(getDraftsResponse.body, { result: [] });
});

test('Get one draft intent', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const draftIntent = await DraftIntent.create({
        skillId: skill.id,
        humanReadableName: 'Название формы',
        formName: 'paskills.test',
    });
    const res = await callApi('get', `/skills/${skill.id}/intents`, t.context);
    t.is(res.status, 200);
    t.deepEqual(res.body, {
        result: [],
    });

    const getDraftsResponse = await callApi('get', `/skills/${skill.id}/intents/drafts`, t.context);
    t.is(getDraftsResponse.status, 200);
    t.deepEqual(getDraftsResponse.body, {
        result: [
            {
                id: draftIntent.id,
                humanReadableName: 'Название формы',
                isActivation: false,
                status: 'INVALID_GRAMMAR',
            },
        ],
    });
});

test('Intents are sorted alphabetically', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const intentLatin = await PublishedIntent.create({ skillId: skill.id, humanReadableName: 'latin' });
    const intentOrange = await PublishedIntent.create({ skillId: skill.id, humanReadableName: 'Апельсин' });
    const intentApple = await PublishedIntent.create({ skillId: skill.id, humanReadableName: 'Яблоко' });
    const res = await callApi('get', `/skills/${skill.id}/intents`, t.context);
    t.is(res.status, 200);
    t.deepEqual(res.body, {
        result: [
            {
                id: intentLatin.id,
                humanReadableName: 'latin',
                isActivation: false,
                status: 'INVALID_GRAMMAR',
            },
            {
                id: intentOrange.id,
                humanReadableName: 'Апельсин',
                isActivation: false,
                status: 'INVALID_GRAMMAR',
            },
            {
                id: intentApple.id,
                humanReadableName: 'Яблоко',
                isActivation: false,
                status: 'INVALID_GRAMMAR',
            },
        ],
    });
});

test('Created intent drafts without settings has "NEW" status', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const intent = await DraftIntent.create({ skillId: skill.id });
    const res = await callApi('get', `/skills/${skill.id}/intents/drafts`, t.context);
    t.is(res.status, 200);
    t.deepEqual(res.body, {
        result: [
            {
                id: intent.id,
                humanReadableName: '',
                isActivation: false,
                status: 'NEW',
            },
        ],
    });
});

test('Created intent drafts with filled settings and without base64 has "INVALID_GRAMMAR" status', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const intent = await DraftIntent.create({ skillId: skill.id, formName: 'name' });
    const res = await callApi('get', `/skills/${skill.id}/intents/drafts`, t.context);
    t.is(res.status, 200);

    t.deepEqual(res.body, {
        result: [
            {
                id: intent.id,
                humanReadableName: '',
                isActivation: false,
                status: 'INVALID_GRAMMAR',
            },
        ],
    });
});

test('Created intent drafts with filled settings and with base64 has "OK" status', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const intent = await DraftIntent.create({ skillId: skill.id, formName: 'name', base64: 'base64' });
    const res = await callApi('get', `/skills/${skill.id}/intents/drafts`, t.context);
    t.is(res.status, 200);

    t.deepEqual(res.body, {
        result: [
            {
                id: intent.id,
                humanReadableName: '',
                isActivation: false,
                status: 'OK',
            },
        ],
    });
});
