const {RuleTester} = require('eslint');

const rule = require('../lib/no-service-locator');

const ruleTester = new RuleTester();

const expectedErrors = [
    {message: 'You should not use stout service locator. Import modules explicitly'},
];

ruleTester.run('no-service-locator', rule, {
    valid: [
        {
            code: 'this.widget(SomeWidget)',
        },
        {
            code: 'this.module(SomeModule)',
        },
        {
            code: 'this.middleware(SomeMiddleware)',
        },
        {
            code: 'this.widget(require(\'path/to/widget\'))',
        },
        {
            code: 'this.module(require(\'path/to/module\'))',
        },
        {
            code: 'this.middleware(require(\'path/to/middleware\'))',
        },
    ],

    invalid: [
        {
            code: 'stout.getModule(SomeModule)',
            errors: expectedErrors,
        },
        {
            code: 'stout.getWidget(SomeWidget)',
            errors: expectedErrors,
        },
        {
            code: 'stout.getPage(SomePage)',
            errors: expectedErrors,
        },
        {
            code: 'stout.getMiddleware(SomeMiddleware)',
            errors: expectedErrors,
        },
        {
            code: 'stout.getModule(\'SomeModule\')',
            errors: expectedErrors,
        },
        {
            code: 'stout.getWidget(\'SomeWidget\')',
            errors: expectedErrors,
        },
        {
            code: 'stout.getPage(\'SomePage\')',
            errors: expectedErrors,
        },
        {
            code: 'stout.getMiddleware(\'SomeMiddleware\')',
            errors: expectedErrors,
        },
        {
            code: 'this.widget(\'SomeWidget\')',
            errors: expectedErrors,
        },
        {
            code: 'this.module(\'SomeModule\')',
            errors: expectedErrors,
        },
        {
            code: 'this.middleware(\'SomeMiddleware\')',
            errors: expectedErrors,
        },
    ],
});
