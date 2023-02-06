'use strict';

const { getBreadcrumbsFromCategory } = require('./../../../../middleware/prepare-query/prepare-breadcrumbs');

describe('get breadcrumbs from category', () => {
    const testData = [
        {
            input: 'Смартфоны и гаджеты/Смартфоны, мобильные телефоны/Все смартфоны/Все смартфоны Apple',
            expected: ['Смартфоны и гаджеты', 'Смартфоны, мобильные телефоны'],
        },
        {
            input: '',
            expected: null,
        },
        {
            input: 'Все смартфоны/Все смартфоны Apple',
            expected: null,
        },
        {
            input: undefined,
            expected: null,
        },
        {
            input: null,
            expected: null,
        },
        {
            input: {},
            expected: null,
        },
    ];

    testData.forEach(({ input, expected }) => {
        test(`'${input}' => '${expected}'`, () => {
            expect(getBreadcrumbsFromCategory(input)).toEqual(expected);
        });
    });
});
