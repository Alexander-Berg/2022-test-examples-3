'use strict';

const s = require('serializr');
const mopsTaskSchema = require('./mops-task.v1.js');
const Validator = require('../lib/validator/response-validator.js');
let validator;

beforeAll(() => {
    validator = new Validator(require.resolve('./mops-task.v1.yaml'));
});

describe('mops task', () => {
    const deserialize = s.deserialize.bind(null, mopsTaskSchema);

    it('skips taskGroupId for sync task', () => {
        const result = deserialize({
            status: 'ok',
            taskType: 'sync',
            taskGroupId: 'fake'
        });
        expect(result).toEqual({
            taskType: 'sync'
        });
        validator.call(result);
    });

    it('copies taskGroupId for async task', () => {
        const result = deserialize({
            status: 'ok',
            taskType: 'async',
            taskGroupId: 'fake'
        });
        expect(result).toEqual({
            taskType: 'async',
            taskGroupId: 'fake'
        });
        validator.call(result);
    });

    it('taskType is sync by default', () => {
        const result = deserialize({
            status: 'ok'
        });
        expect(result).toEqual({
            taskType: 'sync'
        });
        validator.call(result);
    });
});
