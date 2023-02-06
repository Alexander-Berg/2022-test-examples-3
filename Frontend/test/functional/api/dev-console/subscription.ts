/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import sinon = require('sinon');
import { createUser, wipeDatabase } from '../../_helpers';
import { getUserById, getUserTicket, testUser } from '../_helpers';
import { callApi } from './_helpers';
import * as senderService from '../../../../services/sender';
import * as mailService from '../../../../services/mail';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.beforeEach(wipeDatabase);

test.before(async t => {
    t.context.userTicket = await getUserTicket(testUser.oauthToken);
});

const id = testUser.uid;
const notificationPath = '/subscription';
const newsPath = '/news-subscription';
const succesSenderResponce = {
    params: {
        email: '----test---@yandex.ru',
    },
    result: {
        status: 'ok',
    },
};

test('Subscription: Subscribe on email notification success', async t => {
    await createUser({ hasSubscription: false, id });
    const res = await callApi('post', notificationPath, t.context);
    t.is(res.body.result.status, 'ok');
    t.true((await getUserById(id)).hasSubscription);
});

test('Subscription: Unsubscribe from email notification success', async t => {
    await createUser({ hasSubscription: true, id });
    const res = await callApi('delete', notificationPath, t.context);
    t.is(res.body.result.status, 'ok');
    const userAfter = await getUserById(id);
    t.true(!userAfter.hasSubscription);
});

test('Subscription: Unsubscribe from email news success', async t => {
    sinon.stub(mailService, 'fetchEmail').value(async() => '----test---@yandex.ru');

    sinon.stub(senderService, 'removeUserFromSubscribeList').value(async() => {
        return succesSenderResponce;
    });
    await createUser({ hasNewsSubscription: true, id });

    const res = await callApi('delete', newsPath, t.context);
    t.is(res.body.result.status, 'ok');
    t.true(!(await getUserById(id)).hasNewsSubscription);
});

test('Subscription: Subscribe on email news success', async t => {
    sinon.stub(mailService, 'fetchEmail').value(async() => '----test---@yandex.ru');

    sinon.stub(senderService, 'addUserToSubscribeList').value(async() => {
        return succesSenderResponce;
    });
    await createUser({ hasNewsSubscription: false, id });

    const res = await callApi('post', newsPath, t.context);
    t.is(res.body.result.status, 'ok');
    t.true((await getUserById(id)).hasNewsSubscription);
});

test('Subscription: Unsubscribe from email news: sender error', async t => {
    sinon.stub(mailService, 'fetchEmail').value(async() => 'test@yandex.ru');
    sinon.stub(senderService, 'removeUserFromSubscribeList').throws(new Error());
    await createUser({ hasNewsSubscription: true, id });
    await callApi('delete', newsPath, t.context);
    const userAfter = await getUserById(id);
    t.true(userAfter.hasNewsSubscription);
});

test('Subscription: Subscribe on email news: sender error', async t => {
    sinon.stub(mailService, 'fetchEmail').value(async() => {
        return 'test@yandex.ru';
    });
    sinon.stub(senderService, 'addUserToSubscribeList').throws(new Error());
    await createUser({ hasNewsSubscription: false, id });
    await callApi('post', newsPath, t.context);
    const userAfter = await getUserById(id);
    t.true(!userAfter.hasNewsSubscription);
});
