const { RuleTester } = require('eslint');

const rule = require('../../../../lib/rules/ginny/no-only-in-suitename');

const ruleTester = new RuleTester();

ruleTester.run('ginny/no-only-in-suitename', rule, {
    valid: [
        'makeSuite("asd", {})',
        'makeSuite()',
        'makeSuite({}, {})',
    ],
    invalid: [
        {
            code: 
            `makeSuite('Тег title', {  
                story: {
                    'При Only показе страницы': {
                        'тег title должен соответствовать шаблону': {}
                    }
                }
            })`,
            errors: [ { column: 21, line: 3 } ]
        },
        {
            code: 
            `makeSuite('Тег title', {  
                story: {
                    'При показе страницы': {
                        'тег title Only должен соответствовать шаблону': {}
                    }
                }
            })`,
            errors: [ { column: 25, line: 4 } ]
        },
        {
            code: 'makeSuite("Only Страница", {})',
            errors: [ { column: 1, line: 1 } ]
        },
        {
            code: 'makeSuite("Omg Страница", {})',
            errors: [ { column: 1, line: 1 } ],
            options: [['omg']],
        },
        {
            code: 'makeSuite("only Страница", {})',
            errors: [ { column: 1, line: 1 } ]
        },
         {
            code: 'makeSuite("Страница only", {})',
            errors: [ { column: 1, line: 1 } ]
        },
    ]
});
