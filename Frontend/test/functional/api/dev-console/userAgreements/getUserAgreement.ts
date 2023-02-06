/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as uuid from 'uuid';
import { getUserTicket, testUser } from '../../_helpers';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { callApi } from '../_helpers';
import { DraftUserAgreement, PublishedUserAgreement } from '../../../../../db';
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

test('getUserAgreement returns 404 for nonexisting uuid', async t => {
    const user = await createUser(testUserData);
    const skill = await createSkill({ userId: user.id });
    const res = await callApi(
        'get',
        `/skills/${skill.id}/user-agreements/${uuid.v4()}/drafts`,
        t.context,
    ).send();
    t.is(res.status, 404);
});

test('get draft user agreement', async t => {
    const user = await createUser(testUserData);
    const skill = await createSkill({ userId: user.id });
    const draft = await DraftUserAgreement.create({
        skillId: skill.id,
        name: 'name',
        url: 'https://ya.ru',
        order: 0,
    });
    const res = await callApi(
        'get',
        `/skills/${skill.id}/user-agreements/${draft.id}/draft`,
        t.context,
    ).send();
    t.is(res.status, 200);
    assertSerializedUserAgreementEqualsModel(draft, res.body.result, t);
});

test('get published user agreement', async t => {
    const user = await createUser(testUserData);
    const skill = await createSkill({ userId: user.id });
    const draft = await PublishedUserAgreement.create({
        id: uuid.v4(),
        skillId: skill.id,
        name: 'name',
        url: 'https://ya.ru',
        order: 0,
    });
    const res = await callApi(
        'get',
        `/skills/${skill.id}/user-agreements/${draft.id}`,
        t.context,
    ).send();
    t.is(res.status, 200);
    assertSerializedUserAgreementEqualsModel(draft, res.body.result, t);
});

test("getUserAgreement returns 404 for other user's draft user agreement", async t => {
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
        'get',
        `/skills/${skill.id}/user-agreements/${draft.id}/draft`,
        t.context,
    ).send();
    t.is(res.status, 404);
});

test("getUserAgreement returns 404 for other user's published user agreement", async t => {
    await createUser(testUserData);
    const otherUser = await createUser({ id: '12345' });
    const skill = await createSkill({ userId: otherUser.id });
    const userAgreement = await PublishedUserAgreement.create({
        id: uuid.v4(),
        skillId: skill.id,
        name: 'name',
        url: 'https://ya.ru',
        order: 0,
    });
    const res = await callApi(
        'get',
        `/skills/${skill.id}/user-agreements/${userAgreement.id}/draft`,
        t.context,
    ).send();
    t.is(res.status, 404);
});
