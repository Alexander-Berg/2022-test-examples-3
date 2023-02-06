'use strict';

const { RuleTester } = require('eslint');
const rule = require('../../../../lib/rules/mandrel/require-resolver-name');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2016, sourceType: 'module'}});

const errors = [
    { message: rule.meta.__message }
]

ruleTester.run('mandrel/require-resolver-name', rule, {
    invalid: [
        {
            errors,
            code: 
            `
            import {createResolver} from '@yandex-market/mandrel/resolver';

            createResolver((ctx, params) => {})
            `,
        },
        {
            errors,
            code: 
            `
            import {createResolver} from '@yandex-market/mandrel/resolver';
            createResolver((ctx, params) => {}, {name: ''})
            `,
        },
        {
            errors,
            code: 
            `
            import {createResolver} from '@yandex-market/mandrel/resolver';
            createResolver(
                (ctx, params) => {}, 
                {
                    cache: {
                        level: 'request',
                    },
                })
            `,
        },
        {
            errors,
            code: 
            `
            import {createResolver} from '@yandex-market/mandrel/resolver';
            export const resolveMeaningOfLife = createResolver((ctx, params) => 42);
            `,
        },
        {
            errors,
            code: 
            `
            import {createResolver} from '@yandex-market/mandrel/resolver';
            export const resolveMeaningOfLife = createResolver(
                (ctx, params) => {
                    return 42;
                }, 
                {
                    cache: {
                        level: 'request',
                    },
                })
            `,
        },
        {
            errors,
            code: 
            `
            import {createResolver} from '@yandex-market/mandrel/resolver';
            createResolver(function(ctx, params) {
                return 42;
            });
            `,
        },
        {
            errors,
            code: 
            `
            import {createResolver} from '@yandex-market/mandrel/resolver';
            createResolver(
                function(ctx, params) {
                    return 42;
                }, 
                {
                    cache: {
                        level: request,
                    },
                })
            `,
        },

        {
            errors,
            code: 
            `
                import {createResolver as makeResolver} from '@yandex-market/mandrel/resolver';
                makeResolver(() => {});
            `
        },
        {
            errors,
            code: 
            `
                import resolverModule from '@yandex-market/mandrel/resolver';
                resolverModule.createResolver(() => {});
            `
        },
        {
            errors,
            code: 
            `
                import * as resolverModule from '@yandex-market/mandrel/resolver';
                resolverModule.createResolver(() => {});
            `
        },

        {
            errors,
            code: 
            `
                const {createResolver: makeResolver} = require('@yandex-market/mandrel/resolver');
                makeResolver(() => {});
            `
        },
        {
            errors,
            code: 
            `
                const resolverModule = require('@yandex-market/mandrel/resolver');
                resolverModule.createResolver(() => {});
            `
        },
    ],
    valid: [
        {
            code: 
            `
                import {createResolver} from '@yandex-market/mandrel/resolver';
                
                const resolveMeaningOfLife = (ctx, params) => 42;
                createResolver(resolveMeaningOfLife);
            `,
        },
        {
            code: 
            `
                import {createResolver} from '@yandex-market/mandrel/resolver';

                function resolveMeaningOfLife(ctx, params){
                    return 42;
                }

                createResolver(resolveMeaningOfLife);
            `,
        },
        {
            code: 
            `
                import {createResolver} from '@yandex-market/mandrel/resolver';

                createResolver(
                    function resolveMeaningOfLife(ctx, params) {
                        return 42;
                    }
                );
            `,
        },
        {
            code: 
            `
                import {createResolver} from '@yandex-market/mandrel/resolver';

                createResolver(
                    (ctx, params) => 42,
                    {
                        name: 'resolveMeaningOfLife'
                    }
                );
            `,
        },


        {
            code: 
            `
                import resolverModule from '@yandex-market/mandrel/resolver';
                resolverModule.createResolver(() => {}, {name: 'resolveNone'});
            `
        },
        {
            code: 
            `
                import * as resolverModule from '@yandex-market/mandrel/resolver';
                resolverModule.createResolver(() => {}, {name: 'resolveNone'});
            `
        },
        {
            code: 
            `
                import {createResolver as makeResolver} from '@yandex-market/mandrel/resolver';
                makeResolver(() => {}, {name: 'resolveNone'});
            `
        },

        {
            code: 
            `
                const {createResolver: makeResolver} = require('@yandex-market/mandrel/resolver');
                makeResolver(() => {}, {name: 'resolveNone'});
            `
        },
        {
            code: 
            `
                const resolverModule = require('@yandex-market/mandrel/resolver');
                makeResolver(() => {}, {name: 'resolveNone'});
            `
        },

        {
            code:
            `
                const {some} = require('@yandex-market/mandrel/resolver');
                some(() => {});
            `
        },
        {
            code:
            `
                import {some} from '@yandex-market/mandrel/resolver';
                some(() => {});
            `
        }
    ],
})
