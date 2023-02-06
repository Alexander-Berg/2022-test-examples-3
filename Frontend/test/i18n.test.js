'use strict';

const proxyquire = require('proxyquire');
const sinon = require('sinon');
const { assert } = require('chai');

const Collection = require('../helpers/Collection');

const fsStub = {};
const i18n = proxyquire('../linters/i18n.js', {
    fs: fsStub
});

const translateInTsPath = 'src/features/Bno/Bno.i18n/en.ts';
const translateInJsPath = 'blocks-desktop/serp/serp.priv-i18n/en.js';

describe('githooks / i18n', () => {
    let runner;

    beforeEach(() => {
        fsStub.readFileSync = sinon.stub();

        runner = { log: sinon.stub() };
    });

    it('should not fail if translate in typescript not contains cyrillic symbols', () => {
        const collection = new Collection([translateInTsPath]);

        fsStub.readFileSync.returns(`
        export const en = {
            "Видео": "Video",
        };
        `);

        assert.doesNotThrow(
            () => i18n(collection, runner)
        );
    });

    it('should fail if translate in typescript contains cyrillic symbols', () => {
        const collection = new Collection([translateInTsPath]);

        fsStub.readFileSync.returns(`
        export const en = {
            "Видео": "Видео",
        };
        `);

        assert.throws(
            () => i18n(collection, runner)
        );
    });

    it('should not fail if translate in javascript not contains cyrillic symbols', () => {
        const collection = new Collection([translateInJsPath]);

        fsStub.readFileSync.returns(`
        module.exports = {
            "serp": {
                "Видео": "Video"
            }
        };
        `);

        assert.doesNotThrow(
            () => i18n(collection, runner)
        );
    });

    it('should fail if translate in javascript contains cyrillic symbols', () => {
        const collection = new Collection([translateInJsPath]);

        fsStub.readFileSync.returns(`
        module.exports = {
            "serp": {
                "Видео": "Видео"
            }
        };
        `);

        assert.throws(
            () => i18n(collection, runner)
        );
    });

    it('should not fail if translate not en or tr', () => {
        const collection = new Collection(['blocks-desktop/serp/serp.priv-i18n/kk.js']);

        assert.doesNotThrow(
            () => i18n(collection, runner)
        );
    });
});
