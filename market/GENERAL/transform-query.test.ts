import {transformQuery} from './transform-query';

describe('transform query', function () {
    let query: {[id: string]: string | number | string[] | number[]};

    beforeEach(function () {
        query = {
            a: '1',
            d: 0,
            c: [1, 2, 3],
        };
    });

    test('default', function () {
        expect(transformQuery(query, 'default')).toEqual('?a=1&d=0&c=1%2C2%2C3');
    });

    test('comma', function () {
        expect(transformQuery(query, 'comma')).toEqual('?a=1&d=0&c=1,2,3');
    });

    test('bracket', function () {
        expect(transformQuery(query, 'bracket')).toEqual('?a=1&d=0&c[]=1&c[]=2&c[]=3');
    });

    test('index', function () {
        expect(transformQuery(query, 'index')).toEqual('?a=1&d=0&c[0]=1&c[1]=2&c[2]=3');
    });

    test('none', function () {
        expect(transformQuery(query, 'none')).toEqual('?a=1&d=0&c=1&c=2&c=3');
    });

    test('empty', function () {
        expect(transformQuery({}, 'none')).toBe(undefined);
    });
});
