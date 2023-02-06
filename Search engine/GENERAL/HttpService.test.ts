import {HttpServiceUtils} from '../HttpServiceUtils';

describe('HttpService', () => {
    describe('static method makeSearchString', () => {
        test('should return empty string if passed empty values', () => {
            expect(HttpServiceUtils.makeSearchString({})).toEqual('');
        });
        test('should return expected result when passes empty string', () => {
            expect(HttpServiceUtils.makeSearchString({owner: ''})).toEqual('');
        });
        test('must not be case sensitive', () => {
            expect(
                HttpServiceUtils.makeSearchString({lastName: 'lastName'}),
            ).toEqual('lastName=lastName');
            expect(
                HttpServiceUtils.makeSearchString({last_name: 'last_name'}),
            ).toEqual('last_name=last_name');
        });
        test('should correct work with numbers', () => {
            expect(
                HttpServiceUtils.makeSearchString({
                    age: 0,
                    name: '',
                    lastName: 'lastName',
                }),
            ).toEqual('age=0&lastName=lastName');
        });
        test('should correct work with booleans', () => {
            expect(
                HttpServiceUtils.makeSearchString({
                    deprecated: true,
                    trusted: false,
                }),
            ).toEqual('deprecated=true&trusted=false');
        });
        test('should correct work with key is undefined', () => {
            expect(
                HttpServiceUtils.makeSearchString({
                    deprecated: undefined,
                }),
            ).toEqual('');
        });
        test('should correct work with key is null', () => {
            expect(
                HttpServiceUtils.makeSearchString({
                    deprecated: null,
                }),
            ).toEqual('');
        });
        test('should correct work with array off numbers', () => {
            expect(
                HttpServiceUtils.makeSearchString({
                    id: [100, 102],
                }),
            ).toEqual('id=100&id=102');
        });
        test('should correct work with array dubbles', () => {
            expect(
                HttpServiceUtils.makeSearchString({
                    id: [100, 102, 100],
                }),
            ).toEqual('id=100&id=102');
        });
        test('should correct work with myltiple array params', () => {
            expect(
                HttpServiceUtils.makeSearchString({
                    id: [100, 102, 100],
                    ids: [100, 102, 100],
                }),
            ).toEqual('id=100&id=102&ids=100&ids=102');
        });
        test('should correct work with empty array', () => {
            expect(
                HttpServiceUtils.makeSearchString({
                    id: [],
                    ids: [],
                    param1: undefined,
                }),
            ).toEqual('');
        });
        test('should correct work with array of strings', () => {
            expect(
                HttpServiceUtils.makeSearchString({
                    id: ['str1', 'str2'],
                    letsTry: false,
                }),
            ).toEqual('id=str1&id=str2&letsTry=false');
        });
    });
});
