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

test('getUserAgreements returns ordered user agreememt drafts', async t => {
    const user = await createUser(testUserData);
    const skill = await createSkill({ userId: user.id });
    const agreement1 = await DraftUserAgreement.create({
        skillId: skill.id,
        url: 'https://yandex.ru',
        name: 'name',
        order: 1,
    });
    const agreement2 = await DraftUserAgreement.create({
        skillId: skill.id,
        url: 'https://yandex.ru/2',
        name: 'name 2',
        order: 0,
    });
    const res = await callApi(
        'get',
        `/skills/${skill.id}/user-agreements/drafts`,
        t.context,
    ).send();
    t.is(res.status, 200);
    t.is(res.body.result.length, 2);
    assertSerializedUserAgreementEqualsModel(agreement2, res.body.result[0], t);
    assertSerializedUserAgreementEqualsModel(agreement1, res.body.result[1], t);
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
    const draft = await DraftUserAgreement.create({
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
    t.is(res.status, 404);
});
