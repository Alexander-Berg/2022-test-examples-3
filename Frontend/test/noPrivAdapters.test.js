'use strict';

const sinon = require('sinon');
const { assert } = require('chai');

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');
const noPrivAdapters = require('../linters/no-priv-adapters.js');

describe('githooks / noPrivAdapters', () => {
    let runner;

    beforeEach(() => {
        runner = { log: sinon.stub() };
    });

    it('should skip if none of priv js files changed', () => {
        const collection = new Collection(['link.styl', 'link.txt']);

        assert.doesNotThrow(
            () => noPrivAdapters(collection, runner)
        );
    });

    it('should skip if adapters from whitelist were changed', () => {
        const collection = new Collection(['⁨adapters/blocks-common⁩/⁨adapter-adv⁩/adapter-adv__foo.priv.js',
            '⁨experiments/blocks-common⁩/⁨adapter-market/adapter-market.priv.js⁩']);

        assert.doesNotThrow(
            () => noPrivAdapters(collection, runner)
        );
    });

    it('should fail if new priv adapter added to directory looks like adapter name', () => {
        const collection = new Collection(['adapter-adv/adapter-wow-new.priv.js']);

        assert.throws(
            () => noPrivAdapters(collection, runner)
        );
    });

    it('should fail if new priv adapter is added ', () => {
        const collection = new Collection(['adapter-wow-new.priv.js']);

        assert.throws(
            () => noPrivAdapters(collection, runner)
        );
    });
});
