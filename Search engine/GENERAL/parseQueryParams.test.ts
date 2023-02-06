import {BooleanStringType, PageSizes} from '@metrics/react';

import {PARSE} from './PARSE';
import {parseQueryParams} from './parseQueryParams';

describe('parseQueryParams', () => {
    test('parse strings', () => {
        expect(
            parseQueryParams<{a: string; b: string; c: string}>(
                '?b=25&a=a&c=false',
                {
                    b: PARSE.string(),
                    a: PARSE.string(),
                    c: PARSE.string(),
                },
            ),
        ).toEqual({
            b: '25',
            a: 'a',
            c: 'false',
        });
    });

    test('pass in result only last param in sequence', () => {
        expect(
            parseQueryParams<{a: number; b: string}>(
                '?a=25&a="1"&b=some&a=20&b=final',
                {
                    a: PARSE.positiveNumber(),
                    b: PARSE.string(),
                },
            ),
        ).toEqual({
            b: 'final',
            a: 20,
        });
    });
    test('parse positive number', () => {
        expect(
            parseQueryParams<{a: number; b: number; c: number}>(
                '?b=25&a="1"&c=-1',
                {
                    b: PARSE.positiveNumber(),
                    a: PARSE.positiveNumber(),
                    c: PARSE.positiveNumber(),
                },
            ),
        ).toEqual({
            b: 25,
        });
    });

    test('parse page size', () => {
        expect(
            parseQueryParams<{size: PageSizes}>('?size=25', {
                size: PARSE.pageSize(),
            }),
        ).toEqual({
            size: '25',
        });
        expect(
            parseQueryParams<{size: PageSizes}>('?size=30', {
                size: PARSE.pageSize(),
            }),
        ).toEqual({});
        expect(
            parseQueryParams<{size: PageSizes}>('?size="50"', {
                size: PARSE.pageSize(),
            }),
        ).toEqual({});
    });

    test('single parameter as array', () => {
        expect(
            parseQueryParams<{b: number[]; c: string[]}>('?b=2&c=a', {
                b: PARSE.arrayOfNumbers(),
                c: PARSE.arrayOfStrings(),
            }),
        ).toEqual({
            b: [2],
            c: ['a'],
        });
    });
    test('correct parse arrays', () => {
        expect(
            parseQueryParams<{b: number[]; c: string[]}>(
                '?b=2&b=3&c=a&c=b&b=4',
                {
                    b: PARSE.arrayOfNumbers(),
                    c: PARSE.arrayOfStrings(),
                },
            ),
        ).toEqual({
            b: [2, 3, 4],
            c: ['a', 'b'],
        });
    });
    test('correct parse wrong params as array', () => {
        expect(
            parseQueryParams<{b: number[]; c: number[]}>('?b=a&c="2"', {
                b: PARSE.arrayOfNumbers(),
                c: PARSE.arrayOfNumbers(),
            }),
        ).toEqual({
            b: [],
            c: [],
        });
        expect(
            parseQueryParams<{c: number[]}>('?c="2"&c=3', {
                c: PARSE.arrayOfNumbers(),
            }),
        ).toEqual({
            c: [3],
        });
    });
    test('parse booleanStringType', () => {
        expect(
            parseQueryParams<{
                isActive: BooleanStringType;
                hidden: BooleanStringType;
            }>('?isActive=1&hidden=0', {
                isActive: PARSE.booleanStringType(),
                hidden: PARSE.booleanStringType(),
            }),
        ).toEqual({});
        expect(
            parseQueryParams<{
                isActive: BooleanStringType;
                hidden: BooleanStringType;
            }>('?isActive=true&hidden=false', {
                isActive: PARSE.booleanStringType(),
                hidden: PARSE.booleanStringType(),
            }),
        ).toEqual({
            isActive: 'true',
            hidden: 'false',
        });
        expect(
            parseQueryParams<{
                isActive: BooleanStringType;
                hidden: BooleanStringType;
            }>('?isActive=trrue&hidden=fals', {
                isActive: PARSE.booleanStringType(),
                hidden: PARSE.booleanStringType(),
            }),
        ).toEqual({});
    });

    test("don't pass unexpected params into result", () => {
        expect(
            parseQueryParams<{
                a: number;
            }>('?isActive=trrue&hidden=fals&b=2&a=1', {
                a: PARSE.positiveNumber(),
            }),
        ).toEqual({a: 1});
    });
});
