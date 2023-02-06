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

test('delete user agreement draft', async t => {
    const user = await createUser(testUserData);
    const skill = await createSkill({ userId: user.id });
    const draft = await DraftUserAgreement.create({
        skillId: skill.id,
        name: 'name',
        url: 'https://ya.ru',
        order: 0,
    });
    const res = await callApi(
        'delete',
        `/skills/${skill.id}/user-agreements/${draft.id}/draft`,
        t.context,
    ).send();
    t.is(res.status, 200);
    const count = await DraftUserAgreement.count({
        where: {
            id: draft.id,
        },
    });
    t.is(count, 0);
});

test("cannot delete other user's draft user agreement", async t => {
    await createUser(testUserData);
    const otherUser = await createUser({ id: '12345' });
    const skill = await createSkill({ userId: otherUser.id });
    const draft = await DraftUserAgreement.create({
        skillId: skill.id,
        name: 'name',
        url: 'https://ya.ru',
        order: 0,
    });
    const res = await callApi(
        'delete',
        `/skills/${skill.id}/user-agreements/${draft.id}`,
        t.context,
    ).send({});
    t.is(res.status, 404);
});
