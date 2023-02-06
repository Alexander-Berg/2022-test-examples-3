'use strict';

const {RuleTester} = require('eslint');
const rule = require('../../../../lib/rules/ginny/no-import-suite.js');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2016, sourceType: 'module'}});

const message = 'Don\'t use importSuite. Use prepareSuite instead';
const expectedErrors = [{message}];

ruleTester.run('ginny/no-import-suite', rule, {
    valid: [
        {
            code: `
                import {prepareSuite} from 'ginny';
                import ButtonSuite from '@/spec/hermione/test-suites/blocks/b-button';
                
                prepareSuite(ButtonSuite);
            `,
        },
        {
            code: `
                const {prepareSuite} = require('ginny');
                const ButtonSuite = require('@/spec/hermione/test-suites/blocks/b-button');
                
                prepareSuite(ButtonSuite);
            `,
        },
        {
            code: `
                import {importSuite} from 'no-name-package';

                importSuite({a: 'b'});
            `,
        },
        {
            code: `
                const {importSuite} = require('no-name-package');

                importSuite({a: 'b'});
            `,
        },
    ],

    invalid: [
        {
            code: `
                import {importSuite} from 'ginny';
                
                importSuite('b-button');
            `,
            errors: expectedErrors
        },
        {
            code: `
                const {importSuite} = require('ginny');
                
                importSuite('b-button');
            `,
            errors: expectedErrors
        },
    ]
});
