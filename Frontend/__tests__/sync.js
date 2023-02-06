console.log(''); // https://github.com/facebook/jest/issues/5792#issuecomment-376678248
const fs = require('fs');
const mockfs = require('mock-fs');

const mimeTypes = require('mime-types');
const { Credentials, mockS3Instance, resetS3Mock } = require('aws-sdk');
const sync = require('../sync');

jest.mock('aws-sdk');
jest.mock('mime-types', () => ({ lookup: jest.fn() }));

describe('sync', () => {
    beforeEach(() => {
        resetS3Mock();
        mimeTypes.lookup.mockClear();
    });

    afterEach(() => {
        mockfs.restore();
    });

    it('should infer mime type', done => {
        mockfs({
            '/tmp/pcode/1234': {
                'index.txt': 'hello world',
            },
        });

        mimeTypes.lookup.mockReturnValueOnce('text/test');

        sync(
            {
                from: '/tmp/pcode/1234',
                to: 's3://s3-test-bucket/1234',
            },
            () => {
                mockfs.restore(); // we need call it as soon as possible
                expect(mimeTypes.lookup).toHaveBeenCalled();
                const options = mockS3Instance.putObject.mock.calls.map(
                    ([opts]) => opts,
                );
                expect(options.map(({ ContentType }) => ContentType)).toEqual([
                    'text/test',
                ]);
                done();
            },
        );
    });

    it('should fallback to text mime type', done => {
        mockfs({
            '/tmp/pcode/1234': {
                index: 'hello world',
            },
        });

        mimeTypes.lookup.mockReturnValueOnce(null);

        sync(
            {
                from: '/tmp/pcode/1234',
                to: 's3://s3-test-bucket/1234',
            },
            () => {
                mockfs.restore(); // we need call it as soon as possible
                const options = mockS3Instance.putObject.mock.calls.map(
                    ([opts]) => opts,
                );
                expect(options.map(({ ContentType }) => ContentType)).toEqual([
                    'text/plain',
                ]);
                done();
            },
        );
    });

    it('should handle case if file read is not possible', done => {
        mockfs({
            '/tmp/pcode/1234': {
                foo: mockfs.file({
                    content: 'foo',
                    mode: 0,
                    uid: 0,
                    gid: 0,
                }),
            },
        });

        const execute = () => {
            sync(
                {
                    from: '/tmp/pcode/1234',
                    to: 's3://s3-test-bucket/1234',
                },
                err => {
                    mockfs.restore(); // we need call it as soon as possible
                    expect(mockS3Instance.putObject).not.toHaveBeenCalled();
                    expect(err).toBeTruthy();
                    expect(err.toString().includes('EACCES')).toBeTruthy();

                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should handle S3 API putObject error', done => {
        mockfs({
            '/tmp/pcode/1234': {
                index: 'hello world',
            },
        });

        const expectedErr = new Error('test error');

        mockS3Instance.putObject.mockImplementationOnce((opts, callback) => {
            setTimeout(() => callback(expectedErr), 100);
        });

        sync(
            {
                from: '/tmp/pcode/1234',
                to: 's3://s3-test-bucket/1234',
            },
            err => {
                mockfs.restore(); // we need call it as soon as possible
                expect(mockS3Instance.putObject).toHaveBeenCalled();
                expect(err).toBeTruthy();
                expect(err).toEqual(expectedErr);
                done();
            },
        );
    });

    it('should handle S3 API copyObject error', done => {
        const expectedErr = new Error('test error');

        mockS3Instance.listObjectsV2.mockImplementationOnce((opts, callback) => {
            callback(null, { Contents: [{ Key: 'sourcePath/index.js' }] });
            return opts;
        });

        mockS3Instance.copyObject.mockImplementationOnce((opts, callback) => {
            setTimeout(() => callback(expectedErr), 100);
        });

        sync(
            {
                from: 's3://sourceBucket/sourcePath',
                to: 's3://s3-test-bucket/1234',
            },
            err => {
                mockfs.restore(); // we need call it as soon as possible
                expect(mockS3Instance.copyObject).toHaveBeenCalled();
                expect(err).toBeTruthy();
                expect(err).toBeInstanceOf(Error);
                expect(err).toEqual(expectedErr);
                done();
            },
        );
    });

    it('should handle case if directory read is not possible', done => {
        mockfs({
            '/tmp/pcode/1234': {
                bar: mockfs.directory({
                    items: {
                        foo: 'bar',
                    },
                    mode: 0,
                    uid: 0,
                    gid: 0,
                }),
            },
        });

        const execute = () => {
            sync(
                {
                    from: '/tmp/pcode/1234',
                    to: 's3://s3-test-bucket/1234',
                },
                err => {
                    mockfs.restore(); // we need call it as soon as possible
                    expect(mockS3Instance.putObject).not.toHaveBeenCalled();
                    expect(err).toBeTruthy();
                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should handle case if file write is not possible', done => {
        const data = Buffer.from([10, 0, 11, 15]);

        mockS3Instance.listObjectsV2.mockImplementationOnce((opts, callback) => {
            callback(null, { Contents: [{ Key: 's3-test-bucket/index.js' }] });
            return opts;
        });

        mockS3Instance.getObject.mockImplementationOnce((opts, callback) => {
            setTimeout(() => callback(null, { Body: data }), 100);
        });

        mockfs({
            '/tmp/pcode/s3-test-bucket/': {
                'index.js': mockfs.file({
                    content: 'foo',
                    mode: 0,
                    uid: 0,
                    gid: 0,
                }),
            },
        });

        const execute = () => {
            sync(
                {
                    from: 's3://s3-test-bucket/',
                    to: '/tmp/pcode/',
                },
                err => {
                    mockfs.restore(); // we need call it as soon as possible
                    expect(mockS3Instance.getObject).toHaveBeenCalled();
                    expect(err).toBeTruthy();
                    expect(err.toString()).toMatch('EACCES');

                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should handle case with signle file from s3', done => {
        const data = Buffer.from([10, 0, 11, 15]);

        mockS3Instance.listObjectsV2.mockImplementationOnce((opts, callback) => {
            callback(null, { Contents: [{ Key: 's3-test-bucket/index.js' }] });
            return opts;
        });

        mockS3Instance.getObject.mockImplementationOnce((opts, callback) => {
            setTimeout(() => callback(null, { Body: data }), 100);
        });

        mockfs({
            '/tmp/pcode/s3-test-bucket/': {},
        });

        const execute = () => {
            sync(
                {
                    from: 's3://s3-test-bucket/',
                    to: '/tmp/pcode/',
                },
                err => {
                    expect(mockS3Instance.getObject).toHaveBeenCalled();
                    expect(err).toBeFalsy();
                    expect(fs.readFileSync('/tmp/pcode/s3-test-bucket/index.js')).toEqual(
                        data,
                    );
                    mockfs.restore(); // we need call it as soon as possible
                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should handle bad source url', done => {
        const execute = () => {
            sync(
                {
                    from: 's3://$bad-bucket/1234',
                    to: 's3://s3-test-bucket/1234',
                },
                err => {
                    mockfs.restore(); // we need call it as soon as possible
                    expect(mockS3Instance.listObjectsV2).not.toHaveBeenCalled();
                    expect(mockS3Instance.putObject).not.toHaveBeenCalled();
                    expect(err).toBeTruthy();
                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should handle bad destination url', done => {
        const execute = () => {
            sync(
                {
                    from: 's3://source-bucket/1234',
                    to: 's3://$bad-bucket/1234',
                },
                err => {
                    mockfs.restore(); // we need call it as soon as possible
                    expect(mockS3Instance.listObjectsV2).not.toHaveBeenCalled();
                    expect(mockS3Instance.putObject).not.toHaveBeenCalled();
                    expect(err).toBeTruthy();
                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should not fail on compressed files', done => {
        mockfs({
            '/tmp/pcode/1234': {
                'rundom.txt': 'hello world',
                'rundom.txt.br': 'hello world',
                'rundom.txt.gz': 'hello world',
            },
        });

        const execute = () => {
            sync(
                {
                    from: '/tmp/pcode/1234',
                    to: 's3://bucket/1234',
                    validate: true,
                },
                err => {
                    mockfs.restore(); // we need call it as soon as possible
                    expect(err).toBeFalsy();
                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should fail on missed compressed files', done => {
        mockfs({
            '/tmp/pcode/1234': {
                'index.txt': 'hello world',
                'index.txt.br': 'hello world',
                // no gz
            },
        });

        const execute = () => {
            sync(
                {
                    from: '/tmp/pcode/1234',
                    to: 's3://bucket/1234',
                    validate: true,
                },
                err => {
                    mockfs.restore(); // we need call it as soon as possible
                    expect(mockS3Instance.listObjectsV2).not.toHaveBeenCalled();
                    expect(mockS3Instance.putObject).not.toHaveBeenCalled();
                    expect(err).toBeTruthy();
                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should pass credentials to sdk', done => {
        const execute = () => {
            sync(
                {
                    from: 's3://source-bucket/1234',
                    to: 's3://dest/bucket/1234',
                    accessKeyId: 'such_access_key_id',
                    secretAccessKey: 'very_secret_key',
                },
                () => {
                    mockfs.restore(); // we need call it as soon as possible
                    const options = Credentials.mock.calls.map(([opts]) => opts);
                    expect(options.map(({ accessKeyId }) => accessKeyId)).toEqual([
                        'such_access_key_id',
                    ]);
                    expect(options.map(({ secretAccessKey }) => secretAccessKey)).toEqual(
                        ['very_secret_key'],
                    );
                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should handle case with single file', done => {
        mockfs({
            '/fooBar': {
                'index.js': 'hello world',
            },
        });

        sync(
            {
                from: '/fooBar/index.js',
                to: 's3://bucket/dir',
            },
            e => {
                mockfs.restore(); // we need call it as soon as possible
                expect(e).toBeFalsy();
                // jest bugs here, we need timeout
                setTimeout(() => done(), 50);
            },
        );
    });
});

describe('sync.removeObjectsByS3Path', () => {
    beforeEach(() => {
        resetS3Mock();
    });

    it('should pass credentials to sdk', done => {
        const execute = () => {
            sync.removeObjectsByS3Path(
                {
                    s3Path: 's3://bct/folder',
                    accessKeyId: 'such_access_key_id',
                    secretAccessKey: 'very_secret_key',
                },
                () => {
                    const options = Credentials.mock.calls.map(([opts]) => opts);
                    expect(options.map(({ accessKeyId }) => accessKeyId)).toEqual([
                        'such_access_key_id',
                    ]);
                    expect(options.map(({ secretAccessKey }) => secretAccessKey)).toEqual(
                        ['very_secret_key'],
                    );
                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should handle valid 3s path', done => {
        mockS3Instance.listObjectsV2.mockImplementationOnce((opts, callback) => {
            callback(null, { Contents: [{ Key: 'bar/index.js' }] });
            return opts;
        });

        const execute = () => {
            sync.removeObjectsByS3Path(
                {
                    s3Path: 's3://foo/bar',
                },
                err => {
                    expect(err).toBeFalsy();
                    expect(mockS3Instance.listObjectsV2).toHaveBeenCalled();
                    expect(mockS3Instance.deleteObjects).toHaveBeenCalled();
                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should handle large list of objects with prefix', done => {
        const shouldReturnTruncatedListTimes = 3;
        let returnsTruncatedListCounter = 0;

        mockS3Instance.listObjectsV2.mockImplementation((opts, callback) => {
            returnsTruncatedListCounter += 1;

            callback(null, {
                Contents: [{ Key: '/bar/someFile.txt' }],
                IsTruncated:
          returnsTruncatedListCounter <= shouldReturnTruncatedListTimes,
            });

            return opts;
        });

        const execute = () => {
            sync.removeObjectsByS3Path(
                {
                    s3Path: 's3://foo/bar',
                },
                err => {
                    expect(err).toBeFalsy();
                    expect(mockS3Instance.listObjectsV2).toBeCalledTimes(
                        shouldReturnTruncatedListTimes + 1,
                    );
                    expect(mockS3Instance.deleteObjects).toBeCalledTimes(
                        shouldReturnTruncatedListTimes + 1,
                    );
                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should not try remove objects when they not found', done => {
        mockS3Instance.listObjectsV2.mockImplementation((opts, callback) => {
            callback(null, { Contents: [] });
            return opts;
        });

        const execute = () => {
            sync.removeObjectsByS3Path(
                {
                    s3Path: 's3://foo/bar',
                },
                err => {
                    expect(err).toBeFalsy();
                    expect(mockS3Instance.listObjectsV2).toBeCalled();
                    expect(mockS3Instance.deleteObjects).not.toBeCalled();
                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should fail on non-3d path', done => {
        const execute = () => {
            sync.removeObjectsByS3Path(
                {
                    s3Path: '/foo/bar',
                },
                err => {
                    expect(mockS3Instance.listObjectsV2).not.toHaveBeenCalled();
                    expect(mockS3Instance.deleteObjects).not.toHaveBeenCalled();
                    expect(err).toBeTruthy();
                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should handle listObjectsV2 error', done => {
        const listObjectV2Error = new Error('test error');

        mockS3Instance.listObjectsV2.mockImplementation((opts, callback) => {
            callback(listObjectV2Error);
            return opts;
        });

        const execute = () => {
            sync.removeObjectsByS3Path(
                {
                    s3Path: 's3://foo/bar',
                },
                err => {
                    expect(mockS3Instance.listObjectsV2).toHaveBeenCalled();
                    expect(err).toEqual(listObjectV2Error);

                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });

    it('should handle deleteObjects error', done => {
        const deleteObjectsError = new Error('test error');

        mockS3Instance.deleteObjects.mockImplementation((opts, callback) => {
            callback(deleteObjectsError);
            return opts;
        });

        const execute = () => {
            sync.removeObjectsByS3Path(
                {
                    s3Path: 's3://foo/bar',
                },
                err => {
                    expect(mockS3Instance.listObjectsV2).toHaveBeenCalled();
                    expect(mockS3Instance.deleteObjects).not.toHaveBeenCalled();
                    expect(err).toEqual(deleteObjectsError);

                    // jest bugs here, we need timeout
                    setTimeout(() => done(), 50);
                },
            );
        };

        expect(execute).not.toThrow();
    });
});
