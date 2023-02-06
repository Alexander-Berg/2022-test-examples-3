'use strict';

const { RuleTester } = require('eslint');
const rule = require('../../../../lib/rules/ginny/no-page-object-service-locator.js');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2016}});

const expectedErrors = [
    { message: 'You should not use ginny page object service locator. Import modules explicitly' }
];

ruleTester.run('ginny/no-page-object-service-locator', rule, {
    valid: [
        {
            code: `
                const Block = require('./page-object/block');
                this.createPageObject(Block);
            `
        },
    ],

    invalid: [
        {
            code: 'this.createPageObject(\'n-block\');',
            errors: expectedErrors
        },
        {
            code: 'this.createPageObject("n-block");',
            errors: expectedErrors
        },
    ]
});
