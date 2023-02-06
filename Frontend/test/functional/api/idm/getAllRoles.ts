/* eslint-disable */
import test from 'ava';
import * as serviceDb from '../../../../db/entities';
import { createUser, wipeDatabase } from '../../_helpers';
import { makeGetRequest } from './_heplers';
import sinon = require('sinon');

test.beforeEach(wipeDatabase);

test('Get all roles', async t => {
    await createUser({ id: '0001', name: 'user0001', roles: ['admin'], yandexTeamLogin: 'YTuser0001' });
    await createUser({ id: '0002', name: 'user0002', roles: [], yandexTeamLogin: 'user0002' });
    await createUser({ id: '0003', name: 'user0003', roles: ['admin'], yandexTeamLogin: 'YTuser0003' });
    const res = await makeGetRequest('get-all-roles');

    t.deepEqual(res.body, {
        code: 0,
        users: [
            {
                login: 'YTuser0001',
                roles: [[{ paskills: 'admin' }, { 'passport-login': 'user0001' }]],
            },
            {
                login: 'YTuser0003',
                roles: [[{ paskills: 'admin' }, { 'passport-login': 'user0003' }]],
            },
        ],
    });
});
test('Get all roles from empty users query', async t => {
    const res = await makeGetRequest('get-all-roles');
    t.deepEqual(res.body, {
        code: 0,
        users: [],
    });
});

test('Get all roles with database problems', async t => {
    sinon.stub(serviceDb, 'getUsersWithRoles').throwsException();
    const res = await makeGetRequest('get-all-roles');
    t.deepEqual(res.body, {
        code: 1,
        error: 'Нет связи с базой данных.',
    });
});
