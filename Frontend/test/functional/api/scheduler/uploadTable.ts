/* eslint-disable */
import test from 'ava';
import * as sinon from 'sinon';
import { callApi } from './_helpers';
import * as yt from '../../../../services/yt';

test('upload table returns 400 on invalid table', async t => {
    const response = await callApi('/table/upload', {
        table: 'abc',
    });
    t.deepEqual(response.status, 400);
});

test('upload table returns 200 on valid table', async t => {
    const tables = ['skills', 'operations', 'images', 'operations', 'drafts', 'userReviews'];
    const stub = sinon.stub(yt, 'uploadPostgreSQLTable').resolves(0);
    for (const table of tables) {
        const response = await callApi('/table/upload', {
            table,
        });
        t.deepEqual(response.status, 200);
        t.true(stub.calledOnceWith('hahn', table));
        stub.reset();
    }
    stub.restore();
});

test('upload table returns 500 on upload error', async t => {
    const stub = sinon.stub(yt, 'uploadPostgreSQLTable').throws();
    const table = 'skills';
    const response = await callApi('/table/upload', {
        table,
    });
    t.deepEqual(response.status, 500);
    t.true(stub.calledOnceWith('hahn', table));
    stub.restore();
});
