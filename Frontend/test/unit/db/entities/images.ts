/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import sinon = require('sinon');
import { buildImageUrlFromExternalId } from '../../../../db/entities/images';
import config from '../../../../services/config';

const test = anyTest as TestInterface<{ stubs: sinon.SinonStub[] }>;

test.beforeEach(async t => {
    t.context.stubs = [];
});

test.afterEach.always(async t => {
    t.context.stubs.map(s => s.restore());
});

test('test buildImageUrlFromExternalId builds correct image url', async t => {
    t.context.stubs.push(sinon.stub(config.mds.avatar, 'readUrl').value('https://avatar-unit-test'));

    t.deepEqual(buildImageUrlFromExternalId('1/2'), 'https://avatar-unit-test/get-dialogs-skill-card/1/2/orig');
});

test('test buildImageUrlFromExternalId ignores trailing slash on avatar url', async t => {
    t.context.stubs.push(sinon.stub(config.mds.avatar, 'readUrl').value('https://avatar-unit-test/'));

    t.deepEqual(buildImageUrlFromExternalId('1/2'), 'https://avatar-unit-test/get-dialogs-skill-card/1/2/orig');
});

test('test buildImageUrlFromExternalId uses skill card namespace name', async t => {
    t.context.stubs.push(sinon.stub(config.mds.avatar, 'readUrl').value('https://avatar-unit-test'));
    t.context.stubs.push(sinon.stub(config.mds.avatar.namespaces, 'skillCard').value('card'));

    t.deepEqual(buildImageUrlFromExternalId('1/2'), 'https://avatar-unit-test/get-card/1/2/orig');
});
