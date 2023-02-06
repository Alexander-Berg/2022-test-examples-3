'use strict';

const {RuleTester} = require('eslint');
const rule = require('../../../../lib/rules/ginny/no-use-deprecated-environments.js');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2017}});

const expectedErrors = [
    {message: 'This environment is deprecated.'},
];

const envs = [['all', 'prestable']];

ruleTester.run('ginny/no-use-deprecated-environments', rule, {
    valid: [
        {
            code: `
                makeCase({
                    environment: 'testing',
                    test() {
                        return this.expect(1).to.be.equal(0);
                    },
                })
            `,
            options: envs,
        },
        {
            code: `
            makeSuite('Футер.', {
                environment: 'testing',
                story: importSuite('footer-market/__mobile-link', {
                    pageObjects: {
                        footerMarket() {
                            return this.createPageObject(FooterMarket);
                        },
                    },
                }),
            })`,
            options: envs,
        },
    ],

    invalid: [
        {
            code: `
                makeCase({
                    environment: 'all',
                    test() {
                        return this.expect(1).to.be.equal(0);
                    },
                })
            `,
            options: envs,
            errors: expectedErrors,
        },
        {
            code: `
            makeSuite('Футер.', {
                environment: 'prestable',
                story: importSuite('footer-market/__mobile-link', {
                    pageObjects: {
                        footerMarket() {
                            return this.createPageObject(FooterMarket);
                        },
                    },
                }),
            })`,
            options: envs,
            errors: expectedErrors,
        },
    ],
});
