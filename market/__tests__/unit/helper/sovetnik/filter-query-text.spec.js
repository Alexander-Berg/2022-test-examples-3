'use strict';

const { filterQueryText } = require('../../../../src/helper/filter-query');

describe('filter query text without button (using strict filter):', () => {
    const testDataWithoutButton = [
        {
            inputQuery: 'Куртка красная',
            inputUrl: '',
            expected: '',
        },
        {
            inputQuery: 'Платье черное Mango',
            inputUrl: '',
            expected: 'Платье черное Mango',
        },
        {
            inputQuery: 'Платье',
            inputUrl: '',
            expected: '',
        },
        {
            inputQuery: '1 5 6',
            inputUrl: '',
            expected: '',
        },
        {
            inputQuery: '   черный       IPhone~  10',
            inputUrl: '',
            expected: 'черный IPhone 10',
        },
        {
            inputQuery: 'Крышка на чайник',
            inputUrl: '',
            expected: '',
        },
        {
            inputQuery: 'Крышка на teapot',
            inputUrl: '',
            expected: 'Крышка на teapot',
        },
        {
            inputQuery: 'Платье красное в горошек',
            inputUrl: '',
            expected: 'Платье красное в горошек',
        },
        {
            inputQuery: 'Xiaomi Redmi Note 5 Dual Sim 32GB 5.99" 3GB RAM 4G LTE Unlocked Black Gold Blue | eBay',
            inputUrl: '',
            expected: 'Xiaomi Redmi Note 5 Dual Sim 32GB 5.99" 3GB RAM 4G LTE Unlocked Black Gold Blue',
        },
        {
            inputQuery: 'PVRIS',
            inputUrl: 'https://msk.kassir.ru/koncert/izvestiya-hall/pvris_2019-08-28',
            expected: 'PVRIS',
        },
        {
            inputQuery: 'Foals',
            inputUrl: 'https://msk.kassir.ru/koncert/adrenaline-stadium/foals_2019-08-29',
            expected: 'Foals',
        },
        {
            inputQuery: 'Куртка красная',
            inputUrl: undefined,
            expected: '',
        },
        {
            inputQuery: '          Спектакль «Мастер и Маргарита»',
            inputUrl: 'https://www.ticketland.ru/teatry/mkhat-im-m-gorkogo/master-i-margarita/',
            expected: 'Спектакль «Мастер и Маргарита»',
        },
    ];

    const useStrictFilter = true;

    testDataWithoutButton.forEach(({ inputQuery, inputUrl, expected }) => {
        test(`"query: ${inputQuery} and url: ${inputUrl}" => "${expected}"`, () => {
            expect(filterQueryText(inputQuery, useStrictFilter, inputUrl)).toBe(expected);
        });
    });
});

describe('filter query text with button (without using strict filter):', () => {
    const testDataWithButton = [
        {
            inputQuery: 'Куртка красная',
            inputUrl: '',
            expected: 'Куртка красная',
        },
        {
            inputQuery: 'Платье черное Mango',
            inputUrl: '',
            expected: 'Платье черное Mango',
        },
        {
            inputQuery: 'Платье',
            inputUrl: '',
            expected: 'Платье',
        },
        {
            inputQuery: '1 5 6',
            inputUrl: '',
            expected: '1 5 6',
        },
        {
            inputQuery: '   черный       IPhone~  10',
            inputUrl: '',
            expected: 'черный IPhone 10',
        },
        {
            inputQuery: 'Крышка на чайник',
            inputUrl: '',
            expected: 'Крышка на чайник',
        },
        {
            inputQuery: 'Крышка на teapot',
            inputUrl: '',
            expected: 'Крышка на teapot',
        },
        {
            inputQuery: 'Платье красное в горошек',
            inputUrl: '',
            expected: 'Платье красное в горошек',
        },
        {
            inputQuery: 'Xiaomi Redmi Note 5 Dual Sim 32GB 5.99" 3GB RAM 4G LTE Unlocked Black Gold Blue | eBay',
            inputUrl: '',
            expected: 'Xiaomi Redmi Note 5 Dual Sim 32GB 5.99" 3GB RAM 4G LTE Unlocked Black Gold Blue',
        },
        {
            inputQuery: 'PVRIS',
            inputUrl: 'https://msk.kassir.ru/koncert/izvestiya-hall/pvris_2019-08-28',
            expected: 'PVRIS',
        },
        {
            inputQuery: 'Foals',
            inputUrl: 'https://msk.kassir.ru/koncert/adrenaline-stadium/foals_2019-08-29',
            expected: 'Foals',
        },
        {
            inputQuery: 'Foals',
            inputUrl: 'https://msk.kassir.ru/teatr/varshavskaya-melodiya-765',
            expected: 'Foals',
        },
        {
            inputQuery: 'Спектакль       «Мастер и Маргарита»       ',
            inputUrl: 'https://www.ticketland.ru/teatry/mkhat-im-m-gorkogo/master-i-margarita/',
            expected: 'Спектакль «Мастер и Маргарита»',
        },
    ];

    const useStrictFilter = false;

    testDataWithButton.forEach(({ inputQuery, inputUrl, expected }) => {
        test(`'query: "${inputQuery}" and url: "${inputUrl}"' => "${expected}"`, () => {
            expect(filterQueryText(inputQuery, useStrictFilter, inputUrl)).toBe(expected);
        });
    });
});
