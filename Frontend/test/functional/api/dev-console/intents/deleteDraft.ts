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

test('Delete existing intent', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const draft = await DraftIntent.create({ skillId: skill.id });
    const published = await PublishedIntent.create({ skillId: skill.id, id: draft.id });
    const res = await callApi('delete', `/skills/${skill.id}/intents/${draft.id}/draft`, t.context);
    t.is(res.status, 200);
    t.deepEqual(res.body, { result: 'ok' });
    await published.reload();
    const draftCount = await DraftIntent.count({
        where: {
            skillId: skill.id,
        },
    });
    t.is(draftCount, 0);
});

test('Delete nonexisting intent', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const res = await callApi(
        'delete',
        `/skills/${skill.id}/intents/5718fb4b-ed21-4e98-9cc2-c8fcaf6cc0b9/draft`,
        t.context,
    );
    t.is(res.status, 200);
    t.deepEqual(res.body, { result: 'ok' });
});
