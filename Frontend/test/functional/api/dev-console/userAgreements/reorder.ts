/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { getUserTicket, testUser } from '../../_helpers';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { callApi } from '../_helpers';
import { DraftUserAgreement } from '../../../../../db';

const test = anyTest as TestInterface<{ userTicket: string }>;

const testUserData = {
    id: testUser.uid,
    featureFlags: {
        enableUserAgreements: true,
    },
};

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
});

test('1', async t => {
    const user = await createUser(testUserData);
    const skill = await createSkill({ userId: user.id });
    const firstDraft = await DraftUserAgreement.create({
        skillId: skill.id,
        name: 'name',
        url: 'https://ya.ru/first',
        order: 0,
    });
    const secondDraft = await DraftUserAgreement.create({
        skillId: skill.id,
        name: 'name 2',
        url: 'https://ya.ru/second',
        order: 1,
    });
    const res = await callApi(
        'post',
        `/skills/${skill.id}/user-agreements/drafts/reorder`,
        t.context,
    ).send({
        [firstDraft.id]: 1,
        [secondDraft.id]: 0,
    });
    t.is(res.status, 201);
    await firstDraft.reload();
    t.is(firstDraft.order, 1);
    await secondDraft.reload();
    t.is(secondDraft.order, 0);
});

test("cannot reoder other user's draft user agreement", async t => {
    await createUser(testUserData);
    const otherUser = await createUser({ id: '12345' });
    const skill = await createSkill({ userId: otherUser.id });
    const firstDraft = await DraftUserAgreement.create({
        skillId: skill.id,
        name: 'name',
        url: 'https://ya.ru/first',
        order: 0,
    });
    const secondDraft = await DraftUserAgreement.create({
        skillId: skill.id,
        name: 'name 2',
        url: 'https://ya.ru/second',
        order: 1,
    });
    const res = await callApi(
        'patch',
        `/skills/${skill.id}/user-agreements/drafts/reorder`,
        t.context,
    ).send({
        [firstDraft.id]: 1,
        [secondDraft.id]: 0,
    });
    t.is(res.status, 404);
});
