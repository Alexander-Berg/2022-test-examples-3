/* eslint-env mocha */

const fs = require('fs');
const { execSync } = require('child_process');
const nock = require('nock');
const sinon = require('sinon');
const proxyquire = require('proxyquire');

describe('LinguiTanker.Integration', () => {
    let runners;
    let npmlog;

    beforeEach(() => {
        npmlog = {
            info: sinon.stub(),
            error: sinon.stub(),
        };

        runners = proxyquire('..', {
            npmlog,
        });

        // eslint-disable-next-line no-console
        console.log(execSync('node ./node_modules/@lingui/cli/lingui-extract.js').toString());
    });

    afterEach(() => {
        fs.unlinkSync('./test/locale/_build/test/source/index.js.json');
        fs.unlinkSync('./test/locale/en/messages.json');
        fs.unlinkSync('./test/locale/ru/messages.json');

        fs.rmdirSync('./test/locale/_build/test/source');
        fs.rmdirSync('./test/locale/_build/test');
        fs.rmdirSync('./test/locale/_build');
    });

    it('should convert extracted lingui files to tjson', () => {
        const config = {
            dir: 'test/locale',
            target: 'target-name',
            project: 'afisha',
            keyset: 'keyset-name',
            branch: 'branch',
            token: 'access-token',
        };
        const bodyJson = JSON.stringify(
            JSON.parse(fs.readFileSync('test/tanker/upload.tjson')),
            null,
            2
        );

        const tankerApi = nock('https://tanker-api.yandex-team.ru')
            .post('/keysets/merge/', body => {
                return body.indexOf('Content-Disposition: form-data; name="file"; filename="tanker.tjson"') !== -1 &&
                    body.indexOf('Content-Type: application/octet-stream') !== -1 &&
                    body.indexOf(bodyJson) !== -1 &&
                    body.indexOf('name="project-id"\r\n\r\nafisha') !== -1 &&
                    body.indexOf('name="keyset-id"\r\n\r\nkeyset-name') !== -1 &&
                    body.indexOf('name="branch-id"\r\n\r\nbranch') !== -1;
            })
            .matchHeader('authorization', 'OAuth access-token')
            .matchHeader('accept-encoding', 'gzip, deflate')
            .reply(200);

        return runners.upload(config).then(() => {
            tankerApi.done();
        });
    });
});
describe('LinguiTanker.Index', () => {
    let runners;
    let npmlog;
    let files;

    beforeEach(() => {
        npmlog = {
            info: sinon.stub(),
            error: sinon.stub(),
        };
        files = {
            readLinguiJSON: sinon.stub(),
            saveLinguiJSON: sinon.stub(),
        };

        runners = proxyquire('..', {
            './lib/files': files,
            npmlog,
        });
    });

    it('should download tjson translations', () => {
        const config = {
            dir: '/locale',
            target: 'target-name',
            project: 'afisha',
            keyset: 'keyset-1',
            token: 'access-token',
            branch: 'master',
        };

        const linguiData = {
            ru: {
                'key-1': {
                    translation: 'key-1-translate-ru',
                },
                'key-3': {
                    translation: 'key-3-translate-ru',
                },
            },
            en: {
                'key-1': {
                    translation: 'key-1-translate-en',
                },
                'key-3': {
                    translation: 'key-3-translate-en',
                },
            },
        };

        const tankerApi = nock('https://tanker-api.yandex-team.ru')
            .get('/projects/export/tjson/')
            .matchHeader('authorization', 'OAuth access-token')
            .query({
                'project-id': 'afisha',
                'keyset-id': 'keyset-1',
                'branch-id': 'master',
            })
            .reply(200, {
                keysets: {
                    'keyset-1': {
                        meta: {
                            languages: ['ru', 'en'],
                        },
                        keys: {
                            'key-1': {
                                info: {
                                    context: 'key-context-1',
                                },
                                translations: {
                                    ru: {
                                        status: 'approved',
                                        form: 'key-1-translate-ru',
                                    },
                                    en: {
                                        status: 'approved',
                                        form: 'key-1-translate-en',
                                    },
                                },
                            },
                            'key-2': {
                                info: {
                                    context: 'key-context-2',
                                },
                                translations: {
                                    ru: {
                                        status: 'review',
                                        form: 'key-1-translate-ru',
                                    },
                                    en: {
                                        status: 'review',
                                        form: 'key-1-translate-en',
                                    },
                                },
                            },
                            'key-3': {
                                info: {
                                    context: 'key-context-3',
                                },
                                translations: {
                                    ru: {
                                        status: 'approved',
                                        form: 'key-3-translate-ru',
                                    },
                                    en: {
                                        status: 'approved',
                                        form: 'key-3-translate-en',
                                    },
                                },
                            },
                        },
                    },
                },
            });

        return runners.download(config).then(() => {
            tankerApi.done();

            sinon.assert.calledWithExactly(
                files.saveLinguiJSON,
                config,
                linguiData
            );

            sinon.assert.calledOnce(files.saveLinguiJSON);
        });
    });

    it('should upload tjson translations', () => {
        const config = {
            dir: '/locale',
            target: 'target-name',
            project: 'afisha',
            keyset: 'keyset-name',
            branch: 'branch',
            token: 'access-token',
        };
        const bodyJson = {
            // eslint-disable-next-line camelcase
            export_info: {
                request: {
                    'project-id': 'afisha',
                    'keyset-id': 'keyset-name',
                },
                branch: 'branch',
            },
            keysets: {
                'keyset-name': {
                    keys: {},
                },
            },
        };

        const tankerApi = nock('https://tanker-api.yandex-team.ru')
            .post('/keysets/merge/', body => {
                return body.indexOf('Content-Disposition: form-data; name="file"; filename="tanker.tjson"') !== -1 &&
                    body.indexOf('Content-Type: application/octet-stream') !== -1 &&
                    body.indexOf(JSON.stringify(bodyJson, null, 2)) !== -1 &&
                    body.indexOf('name="project-id"\r\n\r\nafisha') !== -1 &&
                    body.indexOf('name="keyset-id"\r\n\r\nkeyset-name') !== -1 &&
                    body.indexOf('name="branch-id"\r\n\r\nbranch') !== -1;
            })
            .matchHeader('authorization', 'OAuth access-token')
            .matchHeader('accept-encoding', 'gzip, deflate')
            .reply(200);

        return runners.upload(config).then(() => {
            tankerApi.done();

            sinon.assert.calledWithExactly(
                files.readLinguiJSON,
                config
            );

            sinon.assert.calledOnce(files.readLinguiJSON);
        });
    });
});
