'use strict';

const { RuleTester } = require('eslint');
const rule = require('../../../../lib/rules/ginny/no-page-object-get.js');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2016}});

const expectedErrors = [
    { message: 'Don\'t use PageObject.get' }
];

ruleTester.run('ginny/no-pageo-bject-get', rule, {
    valid: [
        {
            code: `
                const Button = require('@/spec/page-objects/button');
            `,
        },
    ],

    invalid: [
        {
            code: `
                const {PageObject} = require('ginny');

                const Button = PageObject.get('button');
            `,
            errors: expectedErrors
        },
    ]
});
