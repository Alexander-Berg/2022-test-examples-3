const { RuleTester } = require('eslint');
const noCyclicalFunctionRepetitionTest = require('../../lib/no-cyclical-function-repetition');

const ruleTester = new RuleTester({
    parser: require.resolve('@typescript-eslint/parser'),
});

ruleTester.run('no-cyclical-function-repetition', noCyclicalFunctionRepetitionTest, {
    valid: [
        {
            code: 'api();',
            options: [{ apiFunctionNames: ['api'] }],
        },
        {
            code: 'func(api);',
            options: [{ apiFunctionNames: ['api'] }],
        },
        {
            code: 'api();\n        api();',
            options: [{ apiFunctionNames: ['api'] }],
        },
        {
            code: 'api();\n        Api();',
            options: [{ apiFunctionNames: ['api', 'Api'] }],
        },
        {
            code: 'notApi().then(() => api());',
            options: [{ apiFunctionNames: ['api'] }],
        },
        {
            code: 'notApi().then(() => api()).then(() => api());',
            options: [{ apiFunctionNames: ['api'] }],
        },
        {
            code: 'notApi().then(() => api().then(func)).then(func).catch(func);',
            options: [{ apiFunctionNames: ['api'] }],
        },
        {
            code: 'api(func).then(func).then(func).catch(func);',
            options: [{ apiFunctionNames: ['api'] }],
        },
    ],
    invalid: [
        {
            code: 'api().then(() => api());',
            errors: [{ message: 'Cyclical repetition of backend requests forbidden' }],
            options: [{ apiFunctionNames: ['api'] }],
        },
        {
            code: 'api().then(() => func(api, args));',
            errors: [{ message: 'Cyclical repetition of backend requests forbidden' }],
            options: [{ apiFunctionNames: ['api'] }],
        },
        {
            code: 'Api().then(() => api());',
            errors: [{ message: 'Cyclical repetition of backend requests forbidden' }],
            options: [{ apiFunctionNames: ['api', 'Api'] }],
        },
        {
            code: 'func(api, args).then(() => api());',
            errors: [{ message: 'Cyclical repetition of backend requests forbidden' }],
            options: [{ apiFunctionNames: ['api'] }],
        },
        {
            code: 'api().then(() => Promise.all([api(), api()]).then(func));',
            errors: [
                { message: 'Cyclical repetition of backend requests forbidden' },
                { message: 'Cyclical repetition of backend requests forbidden' },
            ],
            options: [{ apiFunctionNames: ['api'] }],
        },
        {
            code: 'api().then(func).then(func).catch(() => api());',
            errors: [{ message: 'Cyclical repetition of backend requests forbidden' }],
            options: [{ apiFunctionNames: ['api'] }],
        },
    ],
});
