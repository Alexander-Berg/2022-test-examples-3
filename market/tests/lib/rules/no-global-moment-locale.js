'use strict';

const {RuleTester} = require('eslint');
const rule = require('../../../lib/rules/no-global-moment-locale');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2017}});

const {message} = rule;

const expectedErrors = [
    {message},
];

ruleTester.run('no-global-moment-locale', rule, {
    valid: [
        `moment().locale('ru');`,
        `moment('2020-02-05').locale('en');`,
        `moment('2020-02-05T10:29:41.314Z').locale(someVar);`,
        `moment(1580898605495).locale(anotherVar);`,
    ],
    invalid: [
        {
            code: `moment.locale('ru')`,
            errors: expectedErrors,
        },
        {
            code: `moment.locale(someVar)`,
            errors: expectedErrors,
        },
    ],
});
