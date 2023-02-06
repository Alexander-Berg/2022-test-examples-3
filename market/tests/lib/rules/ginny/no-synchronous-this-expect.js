'use strict';

const { RuleTester } = require('eslint');
const rule = require('../../../../lib/rules/ginny/no-synchronous-this-expect.js');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2017}});

const expectedErrors = [
    { message: 'You should not use this.expect synchronous.' }
];

ruleTester.run('ginny/no-synchronous-this-expect', rule, {
    valid: [
        {
            code: `makeCase({
                    test() {
                        return this.expect(1).to.be.equal(0);
                    },
                })`,
        },
        {
            code: `makeCase({
                    async test() {
                        await this.expect(1).to.be.equal(0);
                    },
                })`,
        },
        {
            code: `makeCase({
                    test() {
                        return this.browser.allure.runStep(
                            'message', 
                            () => this.expect(1).to.be.equal(0)
                        );
                    },
                })`,
        },
        {
            code: `makeCase({
                    async test() {
                        await this.browser.allure.runStep(
                            'message', 
                            () => this.expect(1).to.be.equal(0)
                        );
                    },
                })`,
        },
        {
            code: `makeCase({
                    test() {
                        return Promise.all([
                            this.expect(1).to.be.equal(0),
                        ]);
                    },
                })`,
        },
        {
            code: `makeCase({
                    async test() {
                        await Promise.all([
                            this.expect(1).to.be.equal(0),
                        ]);
                    },
                })`,
        },
    ],

    invalid: [
        {
            code: `makeCase({
                    test() {
                        this.expect(1).to.be.equal(0);
                    },
                })`,
            errors: expectedErrors,
        },
        {
            code: `makeCase({
                    async test() {
                        this.expect(1).to.be.equal(0);
                    },
                })`,
            errors: expectedErrors,
        },
        {
            code: `makeCase({
                    test() {
                        this.browser.allure.runStep(
                            'message', 
                            () => this.expect(1).to.be.equal(0)
                        );
                    },
                })`,
            errors: expectedErrors,
        },
        {
            code: `makeCase({
                    test() {
                        return this.browser.allure.runStep(
                            'message', 
                            () => {
                                this.expect(1).to.be.equal(0);
                            }
                        );
                    },
                })`,
            errors: expectedErrors,
        },
        {
            code: `makeCase({
                    test() {
                        Promise.all([
                            this.expect(1).to.be.equal(0),
                        ]);
                    },
                })`,
            errors: expectedErrors,
        },
        {
            code: `makeCase({
                    test() {
                        Promise.all([
                            this.expect(1).to.be.equal(0),
                        ]);
                    },
                })`,
            errors: expectedErrors,
        },
    ]
});
