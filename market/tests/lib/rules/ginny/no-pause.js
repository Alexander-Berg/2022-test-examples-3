'use strict';

const {RuleTester} = require('eslint');
const rule = require('../../../../lib/rules/ginny/no-pause.js');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2017}});

const message = 'Don\'t use pause in the tests';
const expectedErrors = [
    {message},
];

ruleTester.run('ginny/no-pause', rule, {
    valid: [],
    invalid: [
        {
            code: `this.browser.pause(500);`,
            errors: expectedErrors,
        },
        {
            code: `browser.pause(500);`,
            errors: expectedErrors,
        },
    ],
});
