/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import * as s3 from '../../../services/s3';

const test = anyTest as TestInterface<{ clock: sinon.SinonFakeTimers }>;

test('upload called with valid body', async t => {
    const upload = sinon.stub(s3, 'upload');
    const key = 'key';
    const xmlContent = '<xml></xml>';

    await s3.uploadXML(key, '<xml></xml>');
    t.true(upload.calledWith(key, xmlContent, 'application/xml'));

    const jsonContent = { a: 1 };
    await s3.uploadJSON(key, jsonContent);

    t.true(upload.calledWith(key, JSON.stringify(jsonContent, null, 2), 'application/json'));

    upload.restore();
});
