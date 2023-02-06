/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { createUser, wipeDatabase } from '../../_helpers';
import {
    callApi,
    respondsWithCreatedModelContains,
    respondsWithError,
    respondsWithExistingModelContains,
} from './_helpers';
import { getUserTicket, testUser } from '../_helpers';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.beforeEach(async() => {
    await wipeDatabase();
});

test.before(async t => {
    t.context.userTicket = await getUserTicket(testUser.oauthToken);
});

test.skip('Unknown user', async t => {
    const res = await callApi('get', '/user');

    respondsWithError(
        {
            code: 404,
            message: 'Not Found',
        },
        res,
        t,
    );
});

test.skip('Known user', async t => {
    await createUser();
    const res = await callApi('get', '/user');

    respondsWithExistingModelContains(
        {
            id: '0001',
            name: 'user',
        },
        res,
        t,
    );
});

test.skip('User creation', async t => {
    const res = await callApi('put', '/user').send({ name: 'user' });

    respondsWithCreatedModelContains(
        {
            id: '0001',
            name: 'user',
        },
        res,
        t,
    );
});

test.skip('User recreation', async t => {
    await createUser();
    const res = await callApi('put', '/user').send({ name: 'user2' });

    respondsWithCreatedModelContains(
        {
            id: '0001',
            name: 'user2',
        },
        res,
        t,
    );
});

const id = '4000242097';

test('put admin user with flag isAdmin', async t => {
    await createUser({ id, name: 'user' });
    const res = await callApi('put', '/user', t.context).send({ login: 'user' });
    t.deepEqual(res.body.result.isAdmin, true);
});

test('put admin user with admin role', async t => {
    await createUser({ roles: ['admin'], id, name: 'user' });
    const res = await callApi('put', '/user', t.context).send({ login: 'user' });
    t.deepEqual(res.body.result.isAdmin, true);
});

test('put user', async t => {
    await createUser({ id, name: 'user' });
    const res = await callApi('put', '/user', t.context).send({ login: 'user' });
    t.deepEqual(res.body.result.isAdmin, false);
});
