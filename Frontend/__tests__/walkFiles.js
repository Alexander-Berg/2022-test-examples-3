const { readdirSync, statSync } = require('fs');
const { mockS3Instance, resetS3Mock } = require('aws-sdk');

const walkFiles = require('../walkFiles');

jest.mock('aws-sdk');
jest.mock('fs', () => ({
    readdirSync: jest.fn(),
    statSync: jest.fn(),
}));

describe('walkFiles', () => {
    beforeEach(() => {
        readdirSync.mockReset();
        statSync.mockReset();
        resetS3Mock();
    });

    describe('fs', () => {
        it('should handle case when readdir returned error', done => {
            readdirSync.mockImplementationOnce(() => {
                throw new Error('test');
            });
            walkFiles(
                mockS3Instance,
                'fooBar',
                () => {
                    done(new Error('should not be called'));
                },
                err => {
                    expect(err).toBeTruthy();
                    done();
                },
            );
        });

        it('should handle case when stat returned error', done => {
            readdirSync.mockImplementationOnce(() => ['foo']);
            statSync.mockImplementationOnce(() => {
                throw new Error('test');
            });
            walkFiles(
                mockS3Instance,
                'fooBar',
                () => {
                    done(new Error('should not be called'));
                },
                err => {
                    expect(err).toBeTruthy();
                    done();
                },
            );
        });

        it('should handle case with empty dir', done => {
            readdirSync.mockImplementationOnce(() => []);
            statSync.mockImplementationOnce(() => ({ isDirectory: () => true }));
            walkFiles(
                mockS3Instance,
                'fooBar',
                () => {
                    throw new Error('should not be called');
                },
                err => {
                    expect(err).toBeTruthy();
                    done();
                },
            );
        });
    });

    describe('s3', () => {
        it('should handle case when listObjectsV2 returned error', done => {
            mockS3Instance.listObjectsV2.mockImplementationOnce((path, callback) =>
                setTimeout(() => {
                    callback(new Error('test'));
                }, 100),
            );
            walkFiles(
                mockS3Instance,
                's3://bucket/fooBar',
                () => {
                    done(new Error('should not be called'));
                },
                err => {
                    expect(err).toBeTruthy();
                    done();
                },
            );
        });

        it('should handle case when copyObject returned error', done => {
            mockS3Instance.copyObject.mockImplementationOnce((path, callback) =>
                setTimeout(() => {
                    callback(new Error('test'));
                }, 100),
            );
            walkFiles(
                mockS3Instance,
                's3://bucket/fooBar',
                () => {
                    done(new Error('should not be called'));
                },
                err => {
                    expect(err).toBeTruthy();
                    done();
                },
            );
        });

        it('should handle case with empty dir', done => {
            mockS3Instance.listObjectsV2.mockImplementationOnce((opts, callback) => {
                setTimeout(() => {
                    callback(null, []);
                }, 100);
            });
            walkFiles(
                mockS3Instance,
                's3://bucket/fooBar',
                () => {
                    throw new Error('should not be called');
                },
                err => {
                    expect(err).toBeTruthy();
                    done();
                },
            );
        });
    });
});
