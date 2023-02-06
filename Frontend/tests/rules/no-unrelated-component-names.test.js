const { RuleTester } = require('eslint');
const noUnrelatedComponentNames = require('../../lib/no-unrelated-component-names');

const ruleTester = new RuleTester({
    parser: require.resolve('@typescript-eslint/parser'),
});

ruleTester.run('no-unrelated-component-names', noUnrelatedComponentNames, {
    valid: [
        {
            code: '',
            filename: 'frontend/services/staff/src/components/Block/Block.components/component.ts',
            options: [{ roots: ['/components/'] }],
        },
        {
            code: '',
            filename: 'frontend/services/staff/src/components/Block/Block.stories/story.ts',
            options: [{ roots: ['/components/'] }],
        },
        {
            code: '',
            filename: 'frontend/services/staff/src/components/Block/Block.i18n/code.ts',
            options: [{ roots: ['/components/'] }],
        },
        {
            code: '',
            filename: 'frontend/services/staff/src/components/Block/BlockContainer.tsx',
            options: [{ roots: ['/components/'] }],
        },
        {
            code: '',
            filename: 'frontend/services/staff/src/components/Block/BlockSleleton.tsx',
            options: [{ roots: ['/components/'] }],
        },
        {
            code: '',
            filename: 'frontend/services/staff/src/components/Block/Block.tsx',
            options: [{ roots: ['/components/'] }],
        },
        {
            code: '',
            filename: 'frontend/services/staff/src/components/Name/NameAnything.tsx',
            options: [{ roots: ['/components/'] }],
        },
        {
            code: '',
            filename: 'frontend/services/staff/src/components/Name/NameAnything/code.tsx',
            options: [{ roots: ['/components/'] }],
        },
        {
            code: '',
            filename: 'frontend/services/staff/src/utils/dates.ts',
            options: [{ roots: ['/components/'] }],
        },
        { // валидный тест с незаданным параметром
            code: '',
            filename: 'frontend/services/staff/src/components/Block/Block.components/component.ts',
            options: [],
        },
        { // валидный тест с двумя параметрами
            code: '',
            filename: 'frontend/services/staff/src/components/Block/Block.components/component.ts',
            options: [{ roots: ['/components/', '/bundles/'] }],
        },
        { // валидный тест с двумя параметрами
            code: '',
            filename: 'frontend/services/staff/src/bundles/Block/Block.components/component.ts',
            options: [{ roots: ['/components/', '/bundles/'] }],
        },
        { // если в главной папке лежит файл, а не папка
            code: '',
            filename: 'frontend/services/staff/src/bundles/component.ts',
            options: [{ roots: ['/bundles/'] }],
        },
    ],

    invalid: [
        { // имя блока в середине имени файла
            code: '',
            filename: 'frontend/services/staff/src/components/Block/justBlockInTheMiddle.ts',
            errors: [{ messageId: 'INVALID_NAME' }],
            options: [{ roots: ['/components/'] }],
        },
        { // имя блока в конце имени файла
            code: '',
            filename: 'frontend/services/staff/src/components/Block/InTheEndBlock.ts',
            errors: [{ messageId: 'INVALID_NAME' }],
            options: [{ roots: ['/components/'] }],
        },
        { // имя блока не содержится в имени файла
            code: '',
            filename: 'frontend/services/staff/src/components/Block/unrelatedName.ts',
            errors: [{ messageId: 'INVALID_NAME' }],
            options: [{ roots: ['/components/'] }],
        },
        { // имя блока в середине имени папки
            code: '',
            filename: 'frontend/services/staff/src/components/Block/justBlockInTheMiddle/code.ts',
            errors: [{ messageId: 'INVALID_NAME' }],
            options: [{ roots: ['/components/'] }],
        },
        { // имя блока в конце имени папки
            code: '',
            filename: 'frontend/services/staff/src/components/Block/InTheEndBlock/code.ts',
            errors: [{ messageId: 'INVALID_NAME' }],
            options: [{ roots: ['/components/'] }],
        },
        { // имя блока не содержится в имени папки
            code: '',
            filename: 'frontend/services/staff/src/components/Block/unrelatedName/code.ts',
            errors: [{ messageId: 'INVALID_NAME' }],
            options: [{ roots: ['/components/'] }],
        },
        { // не валидный тест с незаданным параметром
            code: '',
            filename: 'frontend/services/staff/src/components/Block/unrelatedName/code.ts',
            errors: [{ messageId: 'INVALID_NAME' }],
            options: [],
        },
        { // не валидный тест с двумя параметрами
            code: '',
            filename: 'frontend/services/staff/src/components/Block/unrelatedName/code.ts',
            errors: [{ messageId: 'INVALID_NAME' }],
            options: [{ roots: ['/components/', '/bundles/'] }],
        },
        { // не валидный тест с двумя параметрами
            code: '',
            filename: 'frontend/services/staff/src/bundles/Block/unrelatedName/code.ts',
            errors: [{ messageId: 'INVALID_NAME' }],
            options: [{ roots: ['/components/', '/bundles/'] }],
        },
    ],
});
