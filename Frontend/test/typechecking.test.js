'use strict';

const proxyquire = require('proxyquire');
const sinon = require('sinon');
const { assert } = require('chai');
const typechecking = proxyquire('../linters/typechecking.js', {
    'fast-glob': {
        sync: () => [],
        '@global': true
    }
});

sinon.assert.expose(assert, { prefix: '' });

const Collection = require('../helpers/Collection');

describe('githooks / typechecking', () => {
    let runner;

    beforeEach(() => {
        runner = { npx: sinon.stub(), tsConfigPath: 'tsconfig.json', execSync: sinon.stub() };
    });

    it('should skip if none of ts files changed', () => {
        const collection = new Collection(['link.css', 'README.md']);

        typechecking(collection, runner);

        assert.notCalled(runner.npx);
    });

    it('should run typechecking for tsx files changed', () => {
        const collection = new Collection(['SomeComponent.tsx', 'README.md']);

        typechecking(collection, runner);

        assert.calledWith(runner.npx, 'tsc -p tsconfig.json --pretty true');
    });

    it('should run typechecking for ts files changed', () => {
        const collection = new Collection(['AdapterFeatureName.ts', 'SomeComponents.scss']);

        typechecking(collection, runner);

        assert.calledWith(runner.npx, 'tsc -p tsconfig.json --pretty true');
    });
});
