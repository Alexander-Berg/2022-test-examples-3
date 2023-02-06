'use strict';

const {RuleTester} = require('eslint');
const rule = require('../../../lib/rules/epics-expressions');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2017}});

const message = 'Important "ofType" or "filter" in epic';

const expectedErrors = [
    {message},
];

ruleTester.run('epics-expressions', rule, {
    valid: [
        `action$.pipe(ofType(), map(), merge())`,
        `action$.pipe(filter(), map(), merge())`,
        `actions$.pipe(ofType(), map(), merge())`,
        `actions.pipe(ofType(), map(), merge())`,
        `supactions.pipe(map(), merge())`,
    ],
    invalid: [
        {
            code: `action$.pipe(map(), merge())`,
            errors: expectedErrors,
        },
        {
            code: `actions$.pipe(map(), merge())`,
            errors: expectedErrors,
        },
        {
            code: `actions.pipe(map(), merge())`,
            errors: expectedErrors,
        },
        {
            code: `actions.pipe(map(), filter(), merge())`,
            errors: expectedErrors,
        },
    ],
});
