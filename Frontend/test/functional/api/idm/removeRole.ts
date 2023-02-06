/* eslint-disable */
import test from 'ava';
import * as serviceDb from '../../../../db/entities';
import * as serviceBlackbox from '../../../../services/blackbox';
import { createUser, wipeDatabase } from '../../_helpers';
import { getUserById, makePostRequest } from './_heplers';
import sinon = require('sinon');

const id = '0001';
const name = 'user0001';

test.beforeEach(wipeDatabase);

test.beforeEach(() => {
    sinon.stub(serviceBlackbox, 'getUserInfo').value(async() => {
        return {
            body: { users: [{ id: '0001' }] },
        };
    });
});

test.afterEach.always(async() => {
    sinon.restore();
});

test('Wrong request props(without login)', async t => {
    const res = await await makePostRequest('remove-role', {
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, fatal: 'Неверные входные параметры.' });
});

test('Wrong request props(without role)', async t => {
    const res = await makePostRequest('remove-role', {
        login: 'user',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, fatal: 'Неверные входные параметры.' });
});

test('Wrong request props(without fields)', async t => {
    const res = await makePostRequest('remove-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
    });
    t.deepEqual(res.body, { code: 1, fatal: 'Неверные входные параметры.' });
});

test('Wrong request props(wrong slug)', async t => {
    const res = await makePostRequest('remove-role', {
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
    const res = await makePostRequest('remove-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "unkwon-user"}',
    });
    console.log(createUser, getUserById);
    t.deepEqual(res.body, { code: 1, fatal: 'Неизвестный passport login.' });
});

test('Wrong request props(nonexistent role)', async t => {
    const res = await makePostRequest('remove-role', {
        login: 'user',
        role: '{"paskills": "__unknow__"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, fatal: 'Нет такой роли.' });
});

test('Remove role from a nonexistent user', async t => {
    const res = await makePostRequest('remove-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, error: 'Нет связи с базой данных.' });
});
test('Remove role from existent user with this role', async t => {
    await createUser({ id, name, roles: ['admin'] });
    const res = await makePostRequest('remove-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    const user = await getUserById(id);
    t.deepEqual(res.body, { code: 0 });
    t.deepEqual(user.roles, []);
});
test('Remove role from existent user without this role', async t => {
    await createUser({ id, name, roles: [] });
    const res = await makePostRequest('remove-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    const user = await getUserById(id);
    t.deepEqual(res.body, { code: 0, warning: 'У сотрудника уже нет такого доступа.' });
    t.deepEqual(user.roles, []);
});

test('Problems with dataBase(can not find or create user)', async t => {
    sinon.stub(serviceDb, 'findOrCreateUser').throwsException();
    const res = await makePostRequest('remove-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, error: 'Нет связи с базой данных.' });
});
test('Problems with dataBase(can not add role)', async t => {
    await createUser({ id, name, roles: ['admin'] });
    sinon.stub(serviceDb, 'removeRole').throwsException();
    const res = await makePostRequest('remove-role', {
        login: 'user',
        role: '{"paskills": "admin"}',
        fields: '{ "passport-login": "test-pl"}',
    });
    t.deepEqual(res.body, { code: 1, error: 'Нет связи с базой данных.' });
});
