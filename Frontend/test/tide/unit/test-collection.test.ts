import _ from 'lodash';
import { assert } from 'chai';
import sinon from 'sinon';

import { TestCollection } from '../../../src';
import Test = require('../../../src/test');
import { BaseTestLike } from '../../../src/types';

describe('tide / test-collection', () => {
    let tests;
    let testCollection: TestCollection;

    beforeEach(() => {
        tests = {
            'test-id-2': { type: 'integration' },
            'test-id-0': { type: 'e2e' },
            'test-id-1': { type: 'e2e' },
        };
        testCollection = new TestCollection(tests);
        sinon.stub(Test, 'default').callsFake(_.identity);
    });

    afterEach(() => {
        sinon.restore();
    });

    describe('getTest', () => {
        const test = {
            type: 'unit',
            titlePath: ['feature', 'it'],
            filePath: '/dir/test.hermione.js',
        } as BaseTestLike;

        it('should find test by id', () => {
            const { id } = testCollection.addTest(test) as Test.default;

            const actualTest = _.pick(testCollection.getTest(id), [
                'type',
                'titlePath',
                'filePath',
            ]);

            assert.deepEqual(actualTest, test as unknown as Partial<Test.default>);
        });

        it('should find test by fullTitle, filePath, type', () => {
            testCollection.addTest(test);

            const actualTest = _.pick(
                testCollection.getTest({
                    type: test.type,
                    titlePath: test.titlePath,
                    filePath: test.filePath,
                }),
                ['type', 'titlePath', 'filePath'],
            );

            assert.deepEqual(actualTest, test as unknown as Partial<Test.default>);
        });
    });

    describe('getTestsByFilePath', () => {
        const test = {
            type: 'unit',
            titlePath: ['feature', 'it'],
            filePath: '/dir/test.hermione.js',
        } as BaseTestLike;

        it('should return map of file paths to tests if no file path was provided', () => {
            testCollection.addTest(test);

            const actualTest = _.pick(
                testCollection.getTestsByFilePath()['/dir/test.hermione.js'][0],
                ['type', 'titlePath', 'filePath'],
            );

            assert.deepEqual(actualTest, test);
        });

        it('should find test by file path', () => {
            testCollection.addTest(test);

            const actualTest = _.pick(
                testCollection.getTestsByFilePath('/dir/test.hermione.js')[0],
                ['type', 'titlePath', 'filePath'],
            );

            assert.deepEqual(actualTest, test);
        });
    });

    describe('eachTest', () => {
        it('should call callback on each test', () => {
            const callbackStub = sinon.stub();

            testCollection.eachTest(callbackStub);

            assert(callbackStub.callCount === 3);
            callbackStub.getCalls().forEach((call, index) => {
                assert.deepEqual(call.firstArg, _.entries(tests)[index][1]);
            });
        });
    });

    describe('mapTests', () => {
        it('should map tests to values returned by callback', () => {
            const callbackStub = sinon.stub().callsFake((test) => test.type);
            const expectedMappedValues = ['integration', 'e2e', 'e2e'];

            const actualMappedValues = testCollection.mapTests(callbackStub);

            assert.deepEqual(actualMappedValues, expectedMappedValues);
        });
    });

    describe('filterTests', () => {
        it('should filter test collection', () => {
            testCollection.filterTests((test) => test.type === 'e2e');

            assert.deepEqual((testCollection as any)._tests, {
                'test-id-0': { type: 'e2e' },
                'test-id-1': { type: 'e2e' },
            });
        });
    });

    describe('groupTests', () => {
        it('should group test collection', () => {
            const result = testCollection.groupTests((test) => [test.type]);

            assert.hasAllKeys(result, ['integration', 'e2e']);
            assert.deepEqual(result, {
                integration: [{ type: 'integration' } as Test.default],
                e2e: [{ type: 'e2e' } as Test.default, { type: 'e2e' } as Test.default],
            });
        });
    });
});
