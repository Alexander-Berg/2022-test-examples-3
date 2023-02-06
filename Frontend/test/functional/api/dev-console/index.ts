/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { getUserTicket, testUser } from '../_helpers';
import { createUser, wipeDatabase } from '../../_helpers';
import { callApi, respondsWithError } from './_helpers';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
});

test('Unauthorized access denied', async t => {
    const res = await callApi('get', '/use');

    respondsWithError(
        {
            code: 403,
            message: 'Forbidden (no credentials)',
        },
        res,
        t,
    );
});

test('Unknown method handling', async t => {
    await createUser({ id: testUser.uid });
    const res = await callApi('get', '/foo', { userTicket: t.context.userTicket });

    respondsWithError(
        {
            code: 404,
            message: 'Resource not found',
        },
        res,
        t,
    );
});
