/* eslint-disable */
import test from 'ava';
import * as sinon from 'sinon';
import * as saasService from '../../../../services/saas';
import { callApi } from './_helpers';

test('"force" parameter is optional', async t => {
    const uploadToFerryman = sinon.stub(saasService, 'uploadToFerryman');
    const response = await callApi('/saas/upload-activation-phrases');
    t.deepEqual(response.status, 200);
    t.true(uploadToFerryman.calledOnceWith(undefined));
    uploadToFerryman.restore();
});

test('"force" parameter is passed to ferryman upload', async t => {
    const uploadToFerryman = sinon.stub(saasService, 'uploadToFerryman');
    const response = await callApi('/saas/upload-activation-phrases', { force: true });
    t.deepEqual(response.status, 200);
    t.true(uploadToFerryman.calledOnceWith(true));
    uploadToFerryman.restore();
});

test('"force"  doens\'t accept strings', async t => {
    const uploadToFerryman = sinon.stub(saasService, 'uploadToFerryman');
    const response = await callApi('/saas/upload-activation-phrases', { force: 'true' });
    t.deepEqual(response.status, 400);
    t.deepEqual(uploadToFerryman.callCount, 0);
    uploadToFerryman.restore();
});

test('"force"  doens\'t accept numbers', async t => {
    const uploadToFerryman = sinon.stub(saasService, 'uploadToFerryman');
    const response = await callApi('/saas/upload-activation-phrases', { force: 1 });
    t.deepEqual(response.status, 400);
    t.deepEqual(uploadToFerryman.callCount, 0);
    uploadToFerryman.restore();
});

test('"force"  doens\'t accept null', async t => {
    const uploadToFerryman = sinon.stub(saasService, 'uploadToFerryman');
    const response = await callApi('/saas/upload-activation-phrases', { force: null });
    t.deepEqual(response.status, 400);
    t.deepEqual(uploadToFerryman.callCount, 0);
    uploadToFerryman.restore();
});

test('"force"  doens\'t accept objects', async t => {
    const uploadToFerryman = sinon.stub(saasService, 'uploadToFerryman');
    const response = await callApi('/saas/upload-activation-phrases', { force: { force: true } });
    t.deepEqual(response.status, 400);
    t.deepEqual(uploadToFerryman.callCount, 0);
    uploadToFerryman.restore();
});

test('"force"  doens\'t accept arrays', async t => {
    const uploadToFerryman = sinon.stub(saasService, 'uploadToFerryman');
    const response = await callApi('/saas/upload-activation-phrases', { force: [] });
    t.deepEqual(response.status, 400);
    t.deepEqual(uploadToFerryman.callCount, 0);
    uploadToFerryman.restore();
});
