'use strict';

const {RuleTester} = require('eslint');
const rule = require('../../../../lib/rules/ginny/no-use-forbidden-assertions.js');

const ruleTester = new RuleTester({parserOptions: {ecmaVersion: 2017}});

const expectedErrors = [
    {message: 'This function is deprecated. Use equal or others.'},
];

const forbiddenAssertions = [['false', 'true', 'null']];

ruleTester.run('ginny/no-use-deprecated-assertion-function', rule, {
    valid: [
        {
            code: `
                makeCase({
                    test() {
                        return this.expect(isVisible)
                            .to.be.equal(true, 'Блок должен быть виден');
                    },
                });
            `,
        }
    ],

    invalid: [
        {
            code: `
                makeCase({
                    test() {
                        return this.expect(1).to.be.false;
                    },
                })
            `,
            options: forbiddenAssertions,
            errors: expectedErrors,
        },
        {
            code: `
                makeCase({
                    test() {
                        return this.expect(1).to.be.true;
                    },
                })
            `,
            options: forbiddenAssertions,
            errors: expectedErrors,
        },
        {
            code: `
                makeCase({
                    test() {
                        return this.expect(1).to.be.null;
                    },
                })
            `,
            options: forbiddenAssertions,
            errors: expectedErrors,
        },
        {
            code: `
                makeCase({
                    async test() {
                        await this.productReviewItem
                            .hasDeleteLink()
                            .should.eventually.be.false;
                    },
                })
            `,
            options: forbiddenAssertions,
            errors: expectedErrors,
        },
    ],
});
