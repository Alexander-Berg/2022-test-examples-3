"use strict"

const {RuleTester} = require('eslint');
const rule = require('../../../lib/rules/no-regexp-lookbehind-assertions.js');

const expectedErrors = [
    {
        message:
            "ES2018 RegExp lookbehind assertions are forbidden. " +
            "Doesn't work in Safari and breaks client-side javascript.",
    },
];

new RuleTester({parserOptions: {ecmaVersion: 2018}}).run('no-regexp-lookbehind-assertions', rule, {
    valid: [
        String.raw`/(?=a)b/`,
        String.raw`/(?!a)b/`,
        String.raw`/(\?<=a)b/`,
        String.raw`/(\?<!a)b/`,
        String.raw`/\(?<=a\)b/`,
        String.raw`/\(?<!a\)b/`,
        String.raw`/\\\(?<=a\)b/`,
        String.raw`/\\\(?<!a\)b/`,
        String.raw`new RegExp("(?=a)b")`,
        String.raw`new RegExp("(?!a)b")`,
        String.raw`new RegExp("(\\?<=a)b")`,
        String.raw`new RegExp("(\\?<!a)b")`,
        String.raw`new RegExp("\\(?<=a\\)b")`,
        String.raw`new RegExp("\\(?<!a\\)b")`,

        // Allow those in character classes.
        String.raw`/[(?<=a)b]/`,
        String.raw`/[(?<!a)b]/`,

        // Ignore syntax errors.
        String.raw`new RegExp("(?<=a(", "u")`,
    ],
    invalid: [
        {
            code: String.raw`/(?<=a)b/`,
            errors: expectedErrors,
        },
        {
            code: String.raw`/(?<!a)b/`,
            errors: expectedErrors,
        },
        {
            code: String.raw`/\\(?<=a)b/`,
            errors: expectedErrors,
        },
        {
            code: String.raw`/\\(?<!a)b/`,
            errors: expectedErrors,
        },
        {
            code: String.raw`/\(?<=a\)(?<=a)b/`,
            errors: expectedErrors,
        },
        {
            code: String.raw`/\(?<!a\)\\(?<!a)b/`,
            errors: expectedErrors,
        },
        {
            code: String.raw`new RegExp("(?<=a)b")`,
            errors: expectedErrors,
        },
        {
            code: String.raw`new RegExp("(?<!a)b")`,
            errors: expectedErrors,
        },
        {
            code: String.raw`new RegExp("\\\\(?<=a)b")`,
            errors: expectedErrors,
        },
        {
            code: String.raw`new RegExp("\\\\(?<!a)b")`,
            errors: expectedErrors,
        },
    ],
});
