const { removeEmptyOrNull } = require('./clear-object-empties');

const testCases = [
    {
        title: 'Clear object from null',
        input: {
            name: 'Iphone',
            color: 'Red',
            price: null,
        },
        output: {
            name: 'Iphone',
            color: 'Red',
        },
    },
    {
        title: 'Do not clear object - have no nulls',
        input: {
            name: 'Iphone',
            color: 'Red',
            price: 9999,
        },
        output: {
            name: 'Iphone',
            color: 'Red',
            price: 9999,
        },
    },
    {
        title: 'Clear object from undefined',
        input: {
            name: 'Iphone',
            color: 'Red',
            price: undefined,
        },
        output: {
            name: 'Iphone',
            color: 'Red',
        },
    },
    {
        title: 'Clear object from deep inside null field',
        input: {
            name: 'Iphone',
            color: 'Red',
            price: {
                current: 99999,
                old: null,
            },
        },
        output: {
            name: 'Iphone',
            color: 'Red',
            price: {
                current: 99999,
            },
        },
    },
];

describe('Should clear object from empties', () => {
    testCases.forEach(({ title, input, output }) => {
        const actual = removeEmptyOrNull(input);
        test(title, () => {
            expect(actual).toEqual(output);
        });
    });
});
