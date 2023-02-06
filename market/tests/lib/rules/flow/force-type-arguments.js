const {RuleTester} = require('eslint');
const rule = require('../../../../lib/rules/flow/force-type-arguments');

const ruleTester = new RuleTester({
    parserOptions: {
        ecmaVersion: 2018,
        sourceType: 'module',
    },
    env: {
        node: true,
    },
});

const options = [[{
    name: 'redux-actions',
    functions: ['createAction'],
}]];

ruleTester.run('flow/force-type-arguments', rule, {
    valid: [
        {
            // No flow annotation
            code: `
            import {createAction} from 'redux-actions';

            createAction(A);
            `,
            options,
        },
        {
            // @noflow annotation
            code: `
            // @noflow

            import {createAction} from 'redux-actions';

            createAction(A);
            `,
            options,
        },
        {
            code: `
            // @flow

            import {doSomething} from 'redux-actions';

            doSomething(A);
            `,
            options,
        },
        {
            code: `
            // @flow

            import {createAction} from 'something';

            doSomething(A);
            `,
            options,
        },
        {
            code: `
            // @flow

            import {createAction} from 'redux-actions';

            createAction<typeof A>(A);
            `,
            options,
            parser: 'babel-eslint',
        },
    ],

    invalid: [
        {
            code: `
            // @flow

            import {createAction} from 'redux-actions';

            createAction(A);
            `,
            options,
            errors: [{
                message: "Specify type arguments for function 'createAction'",
            }],
        },
        {
            code: `
            // @flow

            import {createAction as makeAction} from 'redux-actions';

            makeAction(A);
            `,
            options,
            errors: [{
                message: "Specify type arguments for function 'makeAction'",
            }],
        },
        {
            code: `
            // @flow

            import actions from 'redux-actions';

            actions.createAction(A);
            `,
            options,
            errors: [{
                message: "Specify type arguments for function 'createAction'",
            }],
        },
        {
            code: `
            // @flow

            import * as actions from 'redux-actions';

            actions.createAction(A);
            `,
            options,
            errors: [{
                message: "Specify type arguments for function 'createAction'",
            }],
        },
        {
            code: `
            // @flow
            
            const {createAction} = require('redux-actions');

            createAction(A);
            `,
            options,
            errors: [{
                message: "Specify type arguments for function 'createAction'",
            }],
        },
        {
            code: `
            // @flow

            const {createAction: makeAction} = require('redux-actions');

            makeAction(A);
            `,
            options,
            errors: [{
                message: "Specify type arguments for function 'makeAction'",
            }],
        },
        {
            code: `
            // @flow

            const actions = require('redux-actions');

            actions.createAction(A);
            `,
            options,
            errors: [{
                message: "Specify type arguments for function 'createAction'",
            }],
        }
    ],
});
