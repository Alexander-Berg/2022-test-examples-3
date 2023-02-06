/* eslint-disable */
import test from 'ava';
import sinon = require('sinon');
import { makePostRequest, getUserById } from './_heplers';
import * as serviceBlackbox from '../../../../services/blackbox';
import * as serviceDb from '../../../../db/entities';
import { wipeDatabase, createUser } from '../../_helpers';

test.beforeEach(wipeDatabase);

test.beforeEach(() => {
    sinon.stub(serviceBlackbox, 'getUserInfo').value(async() => {
        return {
            body: { users: [{ id: '0001' }] },
        };
    });
});

const id = '0001';
const name = 'user0001';
const yandexTeamLogin = 'user0001';

test.afterEach.always(async() => {
    sinon.restore();
});

test('Wrong request props(without login)', async t => {
    const res = await await makePostRequest('add-role', {
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, fatal: 'Неверные входные параметры.' });
});

test('Wrong request props(without role)', async t => {
    const res = await makePostRequest('add-role', {
        login: 'user',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, fatal: 'Неверные входные параметры.' });
});

test('Wrong request props(without fields)', async t => {
    const res = await makePostRequest('add-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
    });
    t.deepEqual(res.body, { code: 1, fatal: 'Неверные входные параметры.' });
});

test('Wrong request props(wrong slug)', async t => {
    const res = await makePostRequest('add-role', {
        login: 'user',
        role: '{"__unknow__": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, fatal: 'Неверные входные параметры.' });
});

test('Wrong request props(wrong fields)', async t => {
    sinon.stub(serviceBlackbox, 'getUserInfo').value(async() => {
        return {
            body: { users: [{ id: '' }] },
        };
    });
    const res = await makePostRequest('add-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "unkwon-user"}',
    });
    t.deepEqual(res.body, { code: 1, fatal: 'Неизвестный passport login.' });
});

test('Wrong request props(nonexistent role)', async t => {
    const res = await makePostRequest('add-role', {
        login: 'user',
        role: '{"paskills": "__unknow__"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, fatal: 'Нет такой роли.' });
});

test('Add role to a nonexistent user', async t => {
    const res = await makePostRequest('add-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 0, data: { 'passport-login': 'test-pl' } });
});

test('Add role to a existent user', async t => {
    await createUser({ id, name, yandexTeamLogin });
    const res = await makePostRequest('add-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    const user = await getUserById(id);
    t.deepEqual(res.body, { code: 0, data: { 'passport-login': 'test-pl' } });
    t.deepEqual(user.roles, ['admin']);
});

test('Add already existing role', async t => {
    sinon.restore();
    sinon.stub(serviceBlackbox, 'getUserInfo').value(async() => {
        return {
            body: { users: [{ id }] },
        };
    });
    await createUser({ id, name, yandexTeamLogin, roles: ['admin'] });
    const res = await makePostRequest('add-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    const user = await getUserById(id);
    t.deepEqual(res.body, { code: 1, warning: 'Пользователь уже имеет эту роль.' });
    t.deepEqual(user.roles, ['admin']);
});

test('Problems with blackbox', async t => {
    sinon.stub(serviceBlackbox, 'getUserInfo').throwsException();
    const res = await makePostRequest('add-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, error: 'Проблемы с доступом к blackbox.' });
});

test('Problems with dataBase(can not find or create user)', async t => {
    sinon.stub(serviceDb, 'findOrCreateUser').throwsException();
    const res = await makePostRequest('add-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, error: 'Нет связи с базой данных.' });
});

test('Problems with dataBase(can not add role)', async t => {
    await createUser({ id, name, yandexTeamLogin, roles: ['admin'] });
    sinon.stub(serviceDb, 'removeRole').throwsException();
    const res = await makePostRequest('remove-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, error: 'Нет связи с базой данных.' });
});
