/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { callApi } from './_helpers';
import { createSkill, createUser, wipeDatabase } from '../../_helpers';

const test = anyTest as TestInterface<{ clock: sinon.SinonFakeTimers }>;

test.beforeEach(async() => {
    await wipeDatabase();
});

test('FTS returns 400 if q not specified', async t => {
    const response = await callApi('get', '/dialogs/search');
    t.truthy(response.status === 400);
});

test('FTS returns valid response on empty q', async(t: any) => {
    await createUser();
    await createSkill();

    const response = await callApi('get', '/dialogs/search?q=');
    t.truthy(response.status === 200);
    t.deepEqual(response.body, {
        result: {
            hasMore: false,
            items: [],
            total: 0,
        },
    });
});
