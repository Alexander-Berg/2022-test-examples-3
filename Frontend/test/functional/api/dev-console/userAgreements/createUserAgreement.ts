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

test('create user agreement draft fails without name', async t => {
    const user = await createUser(testUserData);
    const skill = await createSkill({ userId: user.id });
    const url = 'https://ya.ru';
    const data = {
        url,
    };
    const res = await callApi('post', `/skills/${skill.id}/user-agreements/drafts`, t.context).send(
        data,
    );
    t.is(res.status, 400);
});

test('create user agreement draft', async t => {
    const user = await createUser(testUserData);
    const skill = await createSkill({ userId: user.id });
    const name = 'User Agreement';
    const url = 'https://ya.ru';
    const data = {
        url,
        name,
    };
    const res = await callApi('post', `/skills/${skill.id}/user-agreements/drafts`, t.context).send(
        data,
    );
    t.is(res.status, 201);
    const draft = await DraftUserAgreement.findByPk(res.body.result.id);
    t.not(draft, null);
    t.is(draft!.url, url);
    t.is(draft!.order, 0);
    t.is(draft!.name, name);
});

test('order is increased on every draft', async t => {
    const user = await createUser(testUserData);
    const skill = await createSkill({ userId: user.id });
    const urls = ['https://ya.ru', 'https://yandex.ru'];
    let expectedOrder = 0;
    for (const url of urls) {
        const res = await callApi(
            'post',
            `/skills/${skill.id}/user-agreements/drafts`,
            t.context,
        ).send({ url, name: 'name ' + url });
        t.is(res.status, 201);
        t.assert(res.body.result.order === expectedOrder);
        expectedOrder++;
    }
});

test('create user agreement draft fails with http link', async t => {
    const user = await createUser(testUserData);
    const skill = await createSkill({ userId: user.id });
    const data = {
        url: 'http://ya.ru',
    };
    const res = await callApi('post', `/skills/${skill.id}/user-agreements/drafts`, t.context).send(
        data,
    );
    t.is(res.status, 400);
});

test('create user agreement draft fails without url', async t => {
    const user = await createUser(testUserData);
    const skill = await createSkill({ userId: user.id });
    const res = await callApi('post', `/skills/${skill.id}/user-agreements/drafts`, t.context).send(
        {},
    );
    t.is(res.status, 400);
});

test('create user agreement draft fails user feature flag', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const res = await callApi('post', `/skills/${skill.id}/user-agreements/drafts`, t.context).send(
        {},
    );
    t.is(res.status, 404);
});

test("cannot create user agreement on other users's skill", async t => {
    await createUser({ id: testUser.uid });
    const otherUser = await createUser({ id: '12345' });
    const skill = await createSkill({ userId: otherUser.id });
    const res = await callApi('post', `/skills/${skill.id}/user-agreements/drafts`, t.context).send(
        {},
    );
    t.is(res.status, 404);
});
