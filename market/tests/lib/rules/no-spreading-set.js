'use strict';

const {RuleTester} = require('eslint');
const rule = require('../../../lib/rules/no-spreading-set');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2017}});

const message = 'Don\'t spread Set';

const expectedErrors = [
    {message},
];

ruleTester.run('no-spreading-set', rule, {
    valid: [
        `new Set([1, 2]).has(1)`
    ],
    invalid: [
        {
            code: `[...new Set([1, 1, 2])]`,
            errors: expectedErrors,
        },
        {
            code: `[...new Set(array)]`,
            errors: expectedErrors,
        },
    ],
});
