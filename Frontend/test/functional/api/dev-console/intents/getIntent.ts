/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { getUserTicket, testUser } from '../../_helpers';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { callApi, respondsWithError } from '../_helpers';
import { DraftIntent, PublishedIntent } from '../../../../../db';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
});

test('Get existing draft intent', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const draft = await DraftIntent.create({ skillId: skill.id, formName: 'draft' });
    await PublishedIntent.create({ id: draft.id, skillId: skill.id, formName: 'published' });
    const res = await callApi('get', `/skills/${skill.id}/intents/${draft.id}/draft`, t.context);
    t.is(res.status, 200);
    t.deepEqual(res.body, {
        result: {
            id: draft.id,
            humanReadableName: '',
            formName: 'draft',
            positiveTests: '',
            negativeTests: '',
            sourceText: '',
            status: 'INVALID_GRAMMAR',
            isActivation: false,
        },
    });
});

test('Get existing published intent', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const draft = await DraftIntent.create({ skillId: skill.id, formName: 'draft' });
    const published = await PublishedIntent.create({ id: draft.id, skillId: skill.id, formName: 'published' });
    const res = await callApi('get', `/skills/${skill.id}/intents/${draft.id}`, t.context);
    t.is(res.status, 200);
    t.deepEqual(res.body, {
        result: {
            id: published.id,
            humanReadableName: '',
            formName: 'published',
            positiveTests: '',
            negativeTests: '',
            sourceText: '',
            status: 'INVALID_GRAMMAR',
            isActivation: false,
        },
    });
});

test('Get nonexisting published intent', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const res = await callApi(
        'get',
        `/skills/${skill.id}/intents/2b827588-f66d-4e74-b20d-e01b5b3f5af8/draft`,
        t.context,
    );
    t.is(res.status, 404);
});

test("Get other user's intent", async t => {
    const otherUser = await createUser({ id: '12345' });
    const skill = await createSkill({ userId: otherUser.id });
    const draft = await DraftIntent.create({ skillId: skill.id, formName: 'draft' });
    const res = await callApi('get', `/skills/${skill.id}/intents/${draft.id}/draft`, t.context);

    respondsWithError(
        {
            code: 404,
            message: 'Resource not found',
        },
        res,
        t,
    );
});
