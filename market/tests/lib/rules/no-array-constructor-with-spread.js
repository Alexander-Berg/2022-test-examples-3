'use strict';

const {RuleTester} = require('eslint');
const rule = require('../../../lib/rules/no-array-constructor-with-spread');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2017}});

const message = 'Don\'t spread empty array made by constructor';

const expectedErrors = [
    {message},
];

ruleTester.run('no-array-constructor-with-spread', rule, {
    valid: [
        `[...[1,2,3]]`,
        `new Array(5).fill(null)`,
        `new Array(3).fill(null).map((i, n) => i)`,
    ],
    invalid: [
        {
            code: `[...Array(5)].map((i, n) => i)`,
            errors: expectedErrors,
        },
        {
            code: `[...Array(1)]`,
            errors: expectedErrors,
        },
        {
            code: `[...new Array(1)]`,
            errors: expectedErrors,
        },
        {
            code: `[...Array(count).keys()]`,
            errors: expectedErrors,
        },
        {
            code: `[...new Array(count).keys()]`,
            errors: expectedErrors,
        },
        {
            code: `[...Array(count).keys()].map(i => i+1)`,
            errors: expectedErrors,
        },
    ],
});
