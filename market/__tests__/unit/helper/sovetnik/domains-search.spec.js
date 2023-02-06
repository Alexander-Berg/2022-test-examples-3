'use strict';

const { findBySubdomainsAndHigher } = require('../../../../src/helper/domains-search');

describe('Compare domains by subdomain and higher', () => {
    const testCases = [
        {
            inputDomain: 'evrochehol.ru',
            inputList: ['chehol.ru', 'evrochehol.ru'],
            expected: 'evrochehol.ru',
        },
        {
            inputDomain: 'spb.evrochehol.ru',
            inputList: ['chehol.ru', 'evrochehol.ru'],
            expected: 'evrochehol.ru',
        },
        {
            inputDomain: 'www.evrochehol.ru',
            inputList: ['chehol.ru', 'evrochehol.ru'],
            expected: 'evrochehol.ru',
        },
        {
            inputDomain: 'evrochehol.com',
            inputList: ['chehol.ru', 'evrochehol.ru'],
            expected: false,
        },
        {
            inputDomain: 'chehol.ru',
            inputList: ['evrochehol.ru'],
            expected: false,
        },
        {
            inputDomain: 'evrochehol.ru',
            inputList: ['spb.evrochehol.ru'],
            expected: false,
        },
        {
            inputDomain: 'moscow.evrochehol.ru',
            inputList: ['spb.evrochehol.ru'],
            expected: false,
        },
        {
            inputDomain: 'deliver.spb.evrochehol.ru',
            inputList: ['spb.evrochehol.ru'],
            expected: 'spb.evrochehol.ru',
        },
    ];

    testCases.forEach(({ inputDomain, inputList, expected }) => {
        test(`domain: "${inputDomain}" and list: [${inputList}] => ${expected}`, () => {
            expect(findBySubdomainsAndHigher(inputDomain, inputList)).toBe(expected);
        });
    });
});
