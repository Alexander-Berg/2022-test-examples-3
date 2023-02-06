'use strict';

require('should-http');

const config = require('config');
const proxyquire = require('proxyquire');
const s3 = class S3 {
    constructor() {}
    createClient() {}
    uploadFile() {}
};
const awsS3 = class AwsS3 {
    constructor() {}
    createBucket() {
        return Promise.resolve();
    }
};
const Logger = require('../../src/server/logger');
const Storage = proxyquire.load('../../src/server/adapters/s3-storage', {
    s3,
    'aws-sdk/clients/s3': awsS3,
});

const logger = new Logger('reqId', 'login');

describe('s3-storage', () => {
    let storage, sandbox;

    beforeEach(() => {
        sandbox = sinon.createSandbox();
        storage = new Storage('*******************************', logger, config.s3Storage);
    });

    afterEach(() => sandbox.restore());

    it('createBucket: должен создавать новый бакет', () => {
        const bucket = 'samadhi-test';

        sandbox.spy(awsS3.prototype, 'createBucket');

        storage.createBucket(bucket);
        assert.isTrue(awsS3.prototype.createBucket.calledWith({ Bucket: bucket }));
    });

    it('getObjectUrl: должен возвращать корректную ссылку на объект', () => {
        const bucket = 'samadhi-test';
        const key = 'darth.jpg';
        const url = 'https://samadhi-test.s3.mdst.yandex.net/darth.jpg';

        assert.equal(storage.getObjectUrl(bucket, key), url);
    });
});
