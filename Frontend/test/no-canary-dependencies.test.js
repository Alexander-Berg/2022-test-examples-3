'use strict';

const { assert } = require('chai');
const sinon = require('sinon');

const proxyquire = require('proxyquire');
const Collection = require('../helpers/Collection');

const fsStub = {};

const noCanaryDependencies = proxyquire('../linters/no-canary-dependencies.js', {
    fs: fsStub
});

const validPackageJson = {
    scripts: {
        canary: '1.11.1-beta.0-canary.97bb434d96ef0be8b32300144d1fee84bc25ae1c-7b0b9c943d'
    },
    dependencies: {
        '@babel/preset-env': '7.14.7',
        canary: 'czaznzazrzy'
    },
    devDependencies: {
        '@babel/core': '7.1.2',
        canary: 'czaznzazrzy'
    }
};

const invalidPackageJson = {
    scripts: {
        canary: '1.11.1-beta.0-canary.97bb434d96ef0be8b32300144d1fee84bc25ae1c-7b0b9c943d',
        test: 'canary'
    },
    dependencies: {
        '@babel/preset-env': '7.14.7',
        canary: '1.11.1-beta.0-canary.97bb434d96ef0be8b32300144d1fee84bc25ae1c-7b0b9c943d',
        test: 'canary'
    },
    devDependencies: {
        '@babel/core': '7.1.2',
        canary: '1.11.1-beta.0-canary.97bb434d96ef0be8b32300144d1fee84bc25ae1c-7b0b9c943d',
        test: 'canary'
    }
};

describe('noCanaryDependencies', () => {
    let runner;

    beforeEach(() => {
        fsStub.readFileSync = sinon.stub();

        runner = {
            log: sinon.stub(),
            tool: sinon.stub()
        };
    });

    function test(data, file = 'package.json') {
        fsStub.readFileSync.withArgs(file, 'utf-8').returns(data);

        noCanaryDependencies(new Collection([file]), runner);

        assert.notCalled(runner.log);
    }

    it('should find canary version in dependencies of bad package.json', () => {
        const testData = JSON.stringify({
            dependencies: invalidPackageJson.dependencies, scripts: invalidPackageJson.dependencies
        }, null, 4);

        assert.throws(function() {
            test(testData);
        });
    });

    it('should find canary version in devDependencies of bad package.json', () => {
        const testData = JSON.stringify({
            devDependencies: invalidPackageJson.devDependencies, scripts: invalidPackageJson.dependencies
        }, null, 4);

        assert.throws(function() {
            test(testData);
        });
    });

    it('should not find canary version in good package.json', () => {
        const testData = JSON.stringify(validPackageJson, null, 4);

        assert.doesNotThrow(function() {
            test(testData);
        });
    });

    it('should ignore file names other than package.json', () => {
        const testData = JSON.stringify(invalidPackageJson, null, 4);

        assert.doesNotThrow(function() {
            test(testData, 'packages.json');
        });
    });
});
