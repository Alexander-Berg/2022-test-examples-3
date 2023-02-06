import {getValueByPath} from '../util';

describe('get value by path', () => {
    test('get value from correct path', () => {
        const obj = {
            first: {
                second: {
                    third: 'макарошка',
                },
            },
        };

        expect(getValueByPath(['first', 'second', 'third'], obj)).toEqual('макарошка');
    });

    test('get value from not correct path', () => {
        const obj = {
            first: {
                second: {
                    third: 'макарошка',
                },
            },
        };

        expect(getValueByPath(['first', 'second', 'third', 'fourth'], obj)).toBeNull();
    });

    test('path contain null value', () => {
        const obj = {
            first: {
                second: null,
            },
        };

        expect(getValueByPath(['first', 'second', 'third'], obj)).toBeNull();
    });

    test('empty string value from correct path', () => {
        const obj = {
            first: {
                second: {
                    third: '',
                },
            },
        };

        expect(getValueByPath(['first', 'second', 'third'], obj)).toEqual('');
    });

    test('0 value from correct path', () => {
        const obj = {
            first: {
                second: {
                    third: 0,
                },
            },
        };

        expect(getValueByPath(['first', 'second', 'third'], obj)).toEqual(0);
    });
});
