console.log(''); // https://github.com/facebook/jest/issues/5792#issuecomment-376678248
const zlib = require('zlib');
const fs = require('mock-fs');

const { mockS3Instance, resetS3Mock } = require('aws-sdk');
const sync = require('../sync');

jest.mock('aws-sdk');

describe('e2e test', () => {
    afterEach(() => {
        resetS3Mock();
        fs.restore();
    });

    describe('fs', () => {
        it('should copy single file with minimum args', done => {
            fs({
                '/tmp/pcode/8472': {
                    'index.js': 'alert("hello world");',
                },
            });

            sync(
                {
                    from: '/tmp/pcode/8472',
                    to: 's3://s3-test-bucket/8472',
                },
                err => {
                    fs.restore(); // we need call it as soon as possible

                    expect(err).toBeFalsy();

                    const options = mockS3Instance.putObject.mock.calls.map(
                        ([opts]) => opts,
                    );
                    expect(options.map(({ Bucket }) => Bucket)).toEqual([
                        's3-test-bucket',
                    ]);
                    expect(options.map(({ Key }) => Key)).toEqual(['8472/index.js']);
                    expect(options.map(({ ContentType }) => ContentType)).toEqual([
                        'text/javascript; charset=utf-8',
                    ]);
                    expect(options.map(({ CacheControl }) => CacheControl)).toEqual([
                        'no-cache',
                    ]);
                    done();
                },
            );
        });

        it('should copy single file from directory', done => {
            fs({
                '/tmp/pcode/83759': {
                    'index.js': 'alert("hello world");',
                },
            });

            sync(
                {
                    from: '/tmp/pcode/83759',
                    to: 's3://s3-test-bucket/83759',
                    compress: true,
                },
                err => {
                    fs.restore(); // we need call it as soon as possible

                    expect(err).toBeFalsy();

                    const options = mockS3Instance.putObject.mock.calls.map(
                        ([opts]) => opts,
                    );
                    expect(options.map(({ Bucket }) => Bucket)).toEqual([
                        's3-test-bucket',
                        's3-test-bucket',
                        's3-test-bucket',
                    ]);
                    expect(options.map(({ Key }) => Key)).toEqual([
                        '83759/index.js',
                        '83759/index.js.br',
                        '83759/index.js.gz',
                    ]);
                    expect(options.map(({ ContentType }) => ContentType)).toEqual([
                        'text/javascript; charset=utf-8',
                        'text/javascript; charset=utf-8',
                        'text/javascript; charset=utf-8',
                    ]);
                    expect(options.map(({ CacheControl }) => CacheControl)).toEqual([
                        'no-cache',
                        'no-cache',
                        'no-cache',
                    ]);
                    done();
                },
            );
        });

        it('should copy single file by name', done => {
            fs({
                '/tmp/pcode/83759': {
                    'index.js': 'alert("hello world");',
                },
            });

            sync(
                {
                    from: '/tmp/pcode/83759/index.js',
                    to: 's3://s3-test-bucket/83759',
                    compress: true,
                },
                err => {
                    fs.restore(); // we need call it as soon as possible

                    expect(err).toBeFalsy();

                    const options = mockS3Instance.putObject.mock.calls.map(
                        ([opts]) => opts,
                    );
                    expect(options.map(({ Bucket }) => Bucket)).toEqual([
                        's3-test-bucket',
                        's3-test-bucket',
                        's3-test-bucket',
                    ]);
                    expect(options.map(({ Key }) => Key)).toEqual([
                        '83759/index.js',
                        '83759/index.js.br',
                        '83759/index.js.gz',
                    ]);
                    expect(options.map(({ ContentType }) => ContentType)).toEqual([
                        'text/javascript; charset=utf-8',
                        'text/javascript; charset=utf-8',
                        'text/javascript; charset=utf-8',
                    ]);
                    expect(options.map(({ CacheControl }) => CacheControl)).toEqual([
                        'no-cache',
                        'no-cache',
                        'no-cache',
                    ]);
                    done();
                },
            );
        });

        it('should compress', done => {
            fs({
                '/tmp/pcode/92734': {
                    'index.js': 'alert("hello world");',
                },
            });

            sync(
                {
                    from: '/tmp/pcode/92734',
                    to: 's3://s3-test-bucket/92734',
                    compress: true,
                },
                err => {
                    fs.restore(); // we need call it as soon as possible

                    expect(err).toBeFalsy();

                    const options = mockS3Instance.putObject.mock.calls.map(
                        ([opts]) => opts,
                    );

                    expect(options.map(({ Key }) => Key)).toEqual([
                        '92734/index.js',
                        '92734/index.js.br',
                        '92734/index.js.gz',
                    ]);

                    const files = options.map(({ Body }) => Body);

                    const [raw, br, gz] = files;

                    expect(raw.toString()).toEqual('alert("hello world");');
                    expect(zlib.brotliDecompressSync(br).toString()).toEqual(
                        'alert("hello world");',
                    );
                    expect(zlib.gunzipSync(gz).toString()).toEqual(
                        'alert("hello world");',
                    );

                    done();
                },
            );
        });

        it('should validate compression status', done => {
            fs({
                '/tmp/pcode/23510': {
                    'index.js': 'alert("hello world");',
                },
            });

            sync(
                {
                    to: 's3://s3-test-bucket/23510/',
                    from: '/tmp/pcode/23510',
                    validate: true,
                    compress: false,
                    cacheControl: 'no-cache',
                    concurrency: 1,
                },
                err => {
                    fs.restore(); // we need call it as soon as possible
                    expect(mockS3Instance.putObject).not.toHaveBeenCalled();
                    expect(err).toBeInstanceOf(Error);
                    expect(String(err)).toMatch('files not compressed');
                    done();
                },
            );
        });

        it('should copy files in deep directories', done => {
            fs({
                '/tmp/pcode/2957/': {
                    build: {
                        'index.js': 'alert("hello world");',
                    },
                    examples: {
                        'readme.txt': 'hello world',
                    },
                },
            });

            sync(
                {
                    from: '/tmp/pcode/2957',
                    to: 's3://s3-test-bucket/2957',
                    compress: true,
                },
                err => {
                    fs.restore(); // we need call it as soon as possible

                    expect(err).toBeFalsy();

                    const options = mockS3Instance.putObject.mock.calls.map(
                        ([opts]) => opts,
                    );
                    expect(options.map(({ Bucket }) => Bucket)).toEqual([
                        's3-test-bucket',
                        's3-test-bucket',
                        's3-test-bucket',
                        's3-test-bucket',
                        's3-test-bucket',
                        's3-test-bucket',
                    ]);
                    expect(options.map(({ Key }) => Key)).toEqual(
                        expect.arrayContaining([
                            '2957/build/index.js',
                            '2957/build/index.js.br',
                            '2957/build/index.js.gz',
                            '2957/examples/readme.txt',
                            '2957/examples/readme.txt.br',
                            '2957/examples/readme.txt.gz',
                        ]),
                    );
                    expect(options.map(({ ContentType }) => ContentType)).toEqual(
                        expect.arrayContaining([
                            'text/javascript; charset=utf-8',
                            'text/javascript; charset=utf-8',
                            'text/javascript; charset=utf-8',
                            'text/plain',
                            'text/plain',
                            'text/plain',
                        ]),
                    );
                    expect(options.map(({ CacheControl }) => CacheControl)).toEqual([
                        'no-cache',
                        'no-cache',
                        'no-cache',
                        'no-cache',
                        'no-cache',
                        'no-cache',
                    ]);
                    done();
                },
            );
        });
    });

    describe('s3', () => {
        it('should copy single file with minimum args', done => {
            mockS3Instance.listObjectsV2.mockImplementationOnce((opts, callback) => {
                setTimeout(
                    () => callback(null, { Contents: [{ Key: '8470/index.js' }] }),
                    100,
                );
                return opts;
            });

            sync(
                {
                    from: 's3://source-bucket/8470',
                    to: 's3://s3-test-bucket/8471',
                },
                err => {
                    fs.restore(); // we need call it as soon as possible

                    expect(err).toBeFalsy();

                    expect(mockS3Instance.listObjectsV2.mock.calls[0][0]).toEqual({
                        Bucket: 'source-bucket',
                        Prefix: '8470/',
                        Delimiter: '/',
                    });

                    const options = mockS3Instance.copyObject.mock.calls.map(
                        ([opts]) => opts,
                    );
                    expect(options.map(({ Bucket }) => Bucket)).toEqual([
                        's3-test-bucket',
                    ]);
                    expect(options.map(({ CopySource }) => CopySource)).toEqual([
                        '/source-bucket/8470/index.js',
                    ]);
                    expect(options.map(({ Key }) => Key)).toEqual(['8471/index.js']);
                    expect(options.map(({ ContentType }) => ContentType)).toEqual([
                        'text/javascript; charset=utf-8',
                    ]);
                    expect(options.map(({ CacheControl }) => CacheControl)).toEqual([
                        'no-cache',
                    ]);
                    done();
                },
            );
        });

        it('should copy sub folders', done => {
            mockS3Instance.listObjectsV2.mockImplementationOnce((opts, callback) => {
                setTimeout(
                    () =>
                        callback(null, {
                            Contents: [{ Key: '8472/index.js' }],
                            CommonPrefixes: [{ Prefix: '8472/subfolder' }],
                        }),
                    100,
                );

                return opts;
            });

            mockS3Instance.listObjectsV2.mockImplementationOnce((opts, callback) => {
                setTimeout(
                    () =>
                        callback(null, {
                            Contents: [{ Key: '8472/subfolder/subfile.js' }],
                        }),
                    100,
                );
                return opts;
            });

            sync(
                {
                    from: 's3://source-bucket/8472',
                    to: 's3://s3-test-bucket/8472',
                },
                err => {
                    fs.restore(); // we need call it as soon as possible

                    expect(err).toBeFalsy();

                    expect(mockS3Instance.listObjectsV2.mock.calls[0][0]).toEqual({
                        Bucket: 'source-bucket',
                        Prefix: '8472/',
                        Delimiter: '/',
                    });

                    expect(mockS3Instance.listObjectsV2.mock.calls[1][0]).toEqual({
                        Bucket: 'source-bucket',
                        Prefix: '8472/subfolder',
                        Delimiter: '/',
                    });

                    const options = mockS3Instance.copyObject.mock.calls.map(
                        ([opts]) => opts,
                    );
                    expect(options.map(({ Bucket }) => Bucket)).toEqual([
                        's3-test-bucket',
                        's3-test-bucket',
                    ]);
                    expect(options.map(({ CopySource }) => CopySource)).toEqual([
                        '/source-bucket/8472/index.js',
                        '/source-bucket/8472/subfolder/subfile.js',
                    ]);
                    expect(options.map(({ Key }) => Key)).toEqual([
                        '8472/index.js',
                        '8472/subfolder/subfile.js',
                    ]);
                    expect(options.map(({ ContentType }) => ContentType)).toEqual([
                        'text/javascript; charset=utf-8',
                        'text/javascript; charset=utf-8',
                    ]);
                    expect(options.map(({ CacheControl }) => CacheControl)).toEqual([
                        'no-cache',
                        'no-cache',
                    ]);
                    done();
                },
            );
        });

        it('should remove all objects with prefix', done => {
            mockS3Instance.listObjectsV2.mockImplementation((opts, callback) => {
                setTimeout(
                    () =>
                        callback(null, {
                            Contents: [
                                { Key: 'test-dir/index.js' },
                                { Key: 'test-dir/index.css' },
                                { Key: 'test-dir/index.html' },
                            ],
                        }),
                    100,
                );
                return opts;
            });

            sync.removeObjectsByS3Path({ s3Path: 's3://test-bct/test-dir' }, err => {
                expect(err).toBeFalsy();

                expect(mockS3Instance.listObjectsV2.mock.calls[0][0]).toEqual({
                    Bucket: 'test-bct',
                    Prefix: 'test-dir/',
                });

                expect(mockS3Instance.deleteObjects.mock.calls[0][0]).toEqual({
                    Bucket: 'test-bct',
                    Delete: {
                        Objects: [
                            { Key: 'test-dir/index.js' },
                            { Key: 'test-dir/index.css' },
                            { Key: 'test-dir/index.html' },
                        ],
                    },
                });

                done();
            });
        });
    });
});
