const {RuleTester} = require('eslint');
const rule = require('../../../../lib/rules/flow/suppress-comment');

const ruleTester = new RuleTester({
    parserOptions: {
        ecmaVersion: 2017,
    },
});

ruleTester.run('flow/suppress-comment', rule, {
    valid: [
        {
            code: `// $FlowIgnore MOBMARKET-1`,
            options: [[{
                suppress: '$FlowIgnore',
                pattern: 'MOBMARKET-\\d+',
            }]],
        },
        {
            code: `// $FlowIgnore MARKETVERSTKA-1`,
            options: [[{
                suppress: '$FlowIgnore',
                pattern: '(MOBMARKET|MARKETVERSTKA)-\\d+',
            }]],
        },
        {
            code: `
            /**
             * $FlowIgnore MOBMARKET-1
             */`,
            options: [[{
                suppress: '$FlowIgnore',
                pattern: 'MOBMARKET-\\d+',
            }]],
        },
        {
            code: `
            /**
             * $FlowFixMe
             */`,
            options: [[{
                suppress: '$FlowIgnore',
                pattern: 'MOBMARKET-\\d+',
            }]],
        },
        {
            code: `function f(obj: $FlowIgnore<'MOBMARKET-1'>) {}`,
            options: [[{
                suppress: '$FlowIgnore',
                pattern: 'MOBMARKET-\\d+',
            }]],
            parser: 'babel-eslint',
        },
        {
            code: `function f(obj: $FlowIgnore<mixed, 'MOBMARKET-1'>) {}`,
            options: [[{
                suppress: '$FlowIgnore',
                pattern: 'MOBMARKET-\\d+',
            }]],
            parser: 'babel-eslint',
        },
        {
            code: `// Code`,
        },
    ],

    invalid: [
        {
            code: `// $FlowIgnore`,
            options: [[{
                suppress: '$FlowIgnore',
                pattern: 'MOBMARKET-\\d+',
            }]],
            errors: [{
                message: 'Add an explanation for $FlowIgnore (pattern: /MOBMARKET-\\d+/gi).',
            }],
        },
        {
            code: `
            /**
             * $FlowIgnore
             */`,
            options: [[{
                suppress: '$FlowIgnore',
                pattern: 'MOBMARKET-\\d+',
            }]],
            errors: [{
                message: 'Add an explanation for $FlowIgnore (pattern: /MOBMARKET-\\d+/gi).',
            }],
        },
        {
            code: `
            /**
             * $FlowIgnore MARKETVERSTKA-1
             */`,
            options: [[{
                suppress: '$FlowIgnore',
                pattern: 'MOBMARKET-\\d+',
            }]],
            errors: [{
                message: 'Add an explanation for $FlowIgnore (pattern: /MOBMARKET-\\d+/gi).',
            }],
        },
        {
            code: `
            // $FlowIgnore
            function f(obj) {}
            `,
            options: [[{
                suppress: '$FlowIgnore',
                pattern: 'MOBMARKET-\\d+',
            }]],
            errors: [{
                message: 'Add an explanation for $FlowIgnore (pattern: /MOBMARKET-\\d+/gi).',
            }],
            parser: 'babel-eslint',
        },
        {
            code: `function f(obj: $FlowIgnore) {}`,
            options: [[{
                suppress: '$FlowIgnore',
                pattern: 'MOBMARKET-\\d+',
            }]],
            errors: [{
                message: 'Add an explanation for $FlowIgnore (pattern: /MOBMARKET-\\d+/gi).',
            }],
            parser: 'babel-eslint',
        },
        {
            code: `function f(obj: $FlowIgnore<mixed>) {}`,
            options: [[{
                suppress: '$FlowIgnore',
                pattern: 'MOBMARKET-\\d+',
            }]],
            errors: [{
                message: 'Add an explanation for $FlowIgnore (pattern: /MOBMARKET-\\d+/gi).',
            }],
            parser: 'babel-eslint',
        },
    ],
});
