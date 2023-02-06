'use strict';

const { RuleTester } = require('eslint');
const rule = require('../../../../lib/rules/ginny/no-skip.js');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2016}});

const expectedErrors = [
    { message: 'You should not use this.skip. Create skip file in configs folder.' }
];

ruleTester.run('ginny/no-skip', rule, {
    valid: [],

    invalid: [
        {
            code: `this.skip("n-block");`,
            errors: expectedErrors
        },
    ]
});
