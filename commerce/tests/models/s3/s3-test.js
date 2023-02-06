'use strict';

const _ = require('lodash');
const config = require('yandex-config').s3;
const { expect } = require('chai');
const fs = require('fs');
const mockery = require('mockery');
const sinon = require('sinon');

const mockMailer = require('tests/helpers/mailer');
const catchError = require('tests/helpers/catchError').generator;
const mockS3 = require('tests/helpers/s3');
let S3 = require('models/s3');

describe('S3 Model', () => {
    describe('`upload`', () => {
        const file = fs.readFileSync('tests/models/s3/mock-photo');
        const expected = { status: 'OK' };
        const bucket = 'expert';
        const key = '123';
        const access = 'private';
        const credentials = _.assign({ bucket }, config);

        beforeEach(mockMailer);

        afterEach(() => {
            mockery.disable();
            mockery.deregisterAll();
        });

        it('should correctly upload file to S3', function *() {
            S3 = mockS3(expected);

            const s3 = new S3(credentials);

            const actual = yield s3.upload(file, key, access);

            expect(actual).to.be.equal(expected);
        });

        it('should upload file when access level is not specified', function *() {
            S3 = mockS3(expected);

            const s3 = new S3(credentials);
            const actual = yield s3.upload(file, key);

            expect(actual).to.be.equal(expected);
        });

        it('should upload file when key or is not specified', function *() {
            S3 = mockS3(expected);

            const s3 = new S3(credentials);
            const actual = yield s3.upload(file);

            expect(actual).to.be.equal(expected);
        });

        it('should throw 500 when file is not specified', function *() {
            S3 = mockS3(expected);

            const s3 = new S3(credentials);
            const error = yield catchError(s3.upload.bind(s3, null, key, access));

            expect(error.status).to.equal(500);
            expect(error.message).to.equal('File is not a buffer');
            expect(error.options).to.deep.equal({ internalCode: '500_FNB', bucket, key });
        });

        it('should retry failed request', function *() {
            S3 = mockS3(expected, 'retry');
            sinon.spy(S3.prototype, '_tryPutObject');

            const s3 = new S3(credentials);
            const actual = yield s3.upload(file, key);

            expect(actual).to.be.equal(expected);
            expect(S3.prototype._tryPutObject.calledTwice).to.be.true;
        });

        it('should throw 424 when all retries failed', function *() {
            S3 = mockS3(expected, 'fail');
            sinon.spy(S3.prototype, '_tryPutObject');

            const s3 = new S3(credentials);
            const error = yield catchError(s3.upload.bind(s3, file, key));

            expect(error.status).to.equal(424);
            expect(error.message).to.equal('Data not loaded to S3');
            expect(error.options).to.deep.equal({
                internalCode: '424_DNL',
                bucket,
                key: `testing/${key}`,
                error: 'Some error'
            });
        });
    });

    describe('getPathToProxyS3', () => {
        it('should return correct path to s3 with path argument', () => {
            const actual = S3.getPathToProxyS3('public', 'example.webm', 'videos');

            expect(actual).to.equal('https://yastatic.net/s3/expert/testing/videos/example.webm');
        });

        it('should return correct path to s3 withoud path argument', () => {
            const actual = S3.getPathToProxyS3('public', 'example.webm');

            expect(actual).to.equal('https://yastatic.net/s3/expert/testing/example.webm');
        });

        it('should return empty string when no file name', () => {
            const actual = S3.getPathToProxyS3('public', null);

            expect(actual).to.equal('');
        });
    });

    describe('getPathToS3Read', () => {
        it('should return correct path to file from s3', () => {
            const actual = S3.getPathToS3Read('videos', 'example.webm');

            expect(actual).to.equal('https://test.host.to.s3.net/expert/testing/videos/example.webm');
        });
    });
});
