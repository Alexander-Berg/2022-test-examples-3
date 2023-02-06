const { RuleTester } = require('eslint');
const rule = require('../../src/rules/no-namespace-supplemented');

RuleTester.setDefaultConfig({
    parser: require.resolve('@typescript-eslint/parser'),
    parserOptions: {
        ecmaVersion: 6,
        sourceType: 'module',
        ecmaFeatures: {
            modules: true,
        },
    },
});

const ruleTester = new RuleTester();
const langs = ['be', 'en', 'id', 'kk', 'ru', 'tr', 'tt', 'uk', 'uz'];
const options = [{
    namespaceAllowed: ['React', 'keyset'],
    exportAttention: ['@yandex-int/serp-components', '@yandex-lego/components'],
}];

ruleTester.run('no-namespace-supplemented', rule, {
    valid: [
        'import * as keyset from "./Test.i18n"',
        'import * as React from "react"',
        'export { Icon } from "@yandex-lego/components/Component/desktop"',
        'import { Component } from "@yandex-lego/components/Component"',
        ...langs.map(lang => `export * from './${lang}'`),
    ].map(code => ({
        code,
        options,
    })),

    invalid: [
        'import * as Components from "@yandex-int/serp-components"',
        'export * from "@yandex-int/serp-components"',
        'export * from "@yandex-lego/components"',
    ].map(code => ({
        code,
        options,
        errors: [{
            message: ({
                import: 'Unexpected namespace import.',
                export: 'Unexpected namespace re-export.',
            })[code.split(' ').shift()],
        }],
    })),
});
