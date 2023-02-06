/* eslint-disable */
import test from 'ava';
import validateModeratorResponse from '../../../../services/moderation/validateModeratorResponse';

test('empty response object is treated as positive resolution', t => {
    t.deepEqual(validateModeratorResponse({}), []);
});

test('category=ok is treated as positive resolution', t => {
    t.deepEqual(validateModeratorResponse({ category: 'ok' }), []);
});

test('category != ok is treated as a validation error', t => {
    const errors = validateModeratorResponse({ category: true });
    t.true(errors.length > 0);
});

test('negative skill resolution requires only `answer` field', t => {
    t.deepEqual(validateModeratorResponse({ answer: 'text' }), []);
});

test('negative skill resolution allows `secondline` and `additional` fields', t => {
    const moderatorResponse = {
        answer: 'text',
        secondline: true,
        additional: {},
    };
    t.deepEqual(validateModeratorResponse(moderatorResponse), []);
});

test('negative skill resolution `secondline` allows only booleans', t => {
    const moderatorResponse = {
        answer: 'text',
        secondline: 'not a boolean',
    };
    const errors = validateModeratorResponse(moderatorResponse);
    t.true(errors.length > 0);
});

test('negative skill resolution `additional` allows only boolean values', t => {
    const moderatorResponse = {
        answer: 'text',
        additional: {
            key: 'not a boolean',
        },
    };
    const errors = validateModeratorResponse(moderatorResponse);
    t.true(errors.length > 0);
});
