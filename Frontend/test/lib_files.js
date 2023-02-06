/* eslint-env mocha */

const sinon = require('sinon');
const proxyquire = require('proxyquire');

describe('LinguiTanker.Lib.Files', () => {
    let fileWorkers;
    let fs;
    let npmlog;
    let mkdirp;

    const config = {
        dir: '/locale',
        target: 'target-name',
        project: 'afisha',
        keysets: 'keyset-name',
        keyset: 'keyset-name',
        locale: 'ru',
        branch: 'branch',
        token: 'access-token',
    };

    beforeEach(() => {
        fs = {
            writeFileSync: sinon.stub(),
            readdirSync: sinon.stub().returns(['ru', 'en']),
            readFileSync: sinon.stub()
                .onFirstCall()
                .returns(JSON.stringify({
                    'key-1': {
                        translation: 'key-1-translate-ru',
                        description: 'key-context-1',
                    },
                    'key-3': {
                        translation: 'key-3-translate-ru',
                        description: 'key-context-3',
                    },
                }))
                .onSecondCall()
                .returns(JSON.stringify({
                    'key-1': {
                        translation: 'key-1-translate-en',
                        description: 'key-context-1',
                    },
                    'key-3': {
                        translation: 'key-3-translate-en',
                        description: 'key-context-3',
                    },
                })),
        };
        npmlog = {
            info: sinon.stub(),
            error: sinon.stub(),
        };
        mkdirp = {
            sync: sinon.stub(),
        };

        fileWorkers = proxyquire('../lib/files', { fs, npmlog, mkdirp });
    });

    it('should read tjson translations', () => {
        fileWorkers.readLinguiJSON(config);

        sinon.assert.calledWithExactly(
            fs.readFileSync.getCall(0),
            '/locale/ru/messages.json',
            { encoding: 'utf-8' }
        );

        sinon.assert.calledWithExactly(
            fs.readFileSync.getCall(1),
            '/locale/en/messages.json',
            { encoding: 'utf-8' }
        );

        sinon.assert.calledOnce(fs.readdirSync);
    });

    it('should save tjson translations', () => {
        fileWorkers.saveLinguiJSON(config, {
            ru: {
                testkey: {
                    translation: 'testkeytranslation',
                },
            },
            en: {
                testkey: {
                    translation: 'testkeytranslation',
                },
            },
        });

        sinon.assert.calledWithExactly(
            fs.readFileSync.getCall(0),
            '/locale/ru/messages.json',
            { encoding: 'utf-8' }
        );

        sinon.assert.calledWithExactly(
            fs.readFileSync.getCall(1),
            '/locale/en/messages.json',
            { encoding: 'utf-8' }
        );

        sinon.assert.calledWithExactly(
            fs.writeFileSync.getCall(0),
            '/locale/ru/messages.json',
            JSON.stringify({
                'key-1': {
                    translation: 'key-1-translate-ru',
                    description: 'key-context-1',
                },
                'key-3': {
                    translation: 'key-3-translate-ru',
                    description: 'key-context-3',
                },
                testkey: {
                    translation: 'testkeytranslation',
                },
            }, null, 2)
        );

        sinon.assert.calledWithExactly(
            fs.writeFileSync.getCall(1),
            '/locale/en/messages.json',
            JSON.stringify({
                'key-1': {
                    translation: 'key-1-translate-en',
                    description: 'key-context-1',
                },
                'key-3': {
                    translation: 'key-3-translate-en',
                    description: 'key-context-3',
                },
                testkey: {
                    translation: 'testkeytranslation',
                },
            }, null, 2)
        );

        sinon.assert.calledOnce(fs.readdirSync);
        sinon.assert.calledTwice(fs.writeFileSync);

        sinon.assert.calledWithExactly(mkdirp.sync, '/locale');
        sinon.assert.calledWithExactly(mkdirp.sync, '/locale/ru');
        sinon.assert.calledWithExactly(mkdirp.sync, '/locale/en');
        sinon.assert.calledThrice(mkdirp.sync);
    });
});
