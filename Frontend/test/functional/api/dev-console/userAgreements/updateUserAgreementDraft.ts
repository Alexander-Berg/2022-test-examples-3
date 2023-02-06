/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { getUserTicket, testUser } from '../../_helpers';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { callApi } from '../_helpers';
import { DraftUserAgreement } from '../../../../../db';
import { assertSerializedUserAgreementEqualsModel } from './_utils';

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

test('update user agreement draft', async t => {
    const user = await createUser(testUserData);
    const skill = await createSkill({ userId: user.id });
    const draft = await DraftUserAgreement.create({
        skillId: skill.id,
        name: 'name',
        url: 'https://ya.ru',
        order: 0,
    });
    const newUrl = 'https://ya.ru/new';
    const newName = 'name';
    const res = await callApi(
        'patch',
        `/skills/${skill.id}/user-agreements/${draft.id}/draft`,
        t.context,
    ).send({
        url: newUrl,
        name: newName,
    });
    t.is(res.status, 200);
    await draft.reload();
    t.is(draft.url, newUrl);
    t.is(draft.name, newName);
    assertSerializedUserAgreementEqualsModel(draft, res.body.result, t);
});

test("cannot patch other user's draft user agreement", async t => {
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
        'patch',
        `/skills/${skill.id}/user-agreements/${draft.id}`,
        t.context,
    ).send({
        url: 'https://ya.ru/new',
        name: 'name',
    });
    t.is(res.status, 404);
});
