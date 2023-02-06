'use strict';

const ApiRequest = require('./../../functional/lib/api-request');

describe('API request', () => {
    describe('constructor', () => {
        test('should be an instance of ApiRequest class', () => {
            const apiRequest = new ApiRequest('', {}, {}, 200);

            expect(apiRequest).toBeInstanceOf(ApiRequest);
        });
    });

    describe('getters', () => {
        const apiRequest = new ApiRequest('test/path', { f: 'first', s: 'second' }, { data: 'data' }, 404, true);

        test('should return path', () => {
            const expected = 'test/path';
            const actual = apiRequest.path;

            expect(actual).toBe(expected);
        });

        test('should return query', () => {
            const expected = { f: 'first', s: 'second' };
            const actual = apiRequest.query;

            expect(actual).toEqual(expected);
        });

        test('should return response data', () => {
            const expected = { data: 'data' };
            const actual = apiRequest.response;

            expect(actual).toEqual(expected);
        });

        test('should return status code', () => {
            const expected = 404;
            const actual = apiRequest.statusCode;

            expect(actual).toBe(expected);
        });

        test('should return mocked info', () => {
            const expected = true;
            const actual = apiRequest.isMocked;

            expect(actual).toBe(expected);
        });
    });

    describe('match', () => {
        test('should handle without parameters', () => {
            const apiRequest = new ApiRequest('/path', {}, {}, 200);
            const actual = apiRequest.match();

            expect(actual).toBeTruthy();
        });

        test('should handle RegExp path (1st test)', () => {
            const apiRequest = new ApiRequest('/path', {}, {}, 200);
            const actual = apiRequest.match(/^\/p.*h/);

            expect(actual).toBeTruthy();
        });

        test('should handle RegExp path (2nd test)', () => {
            const apiRequest = new ApiRequest('/path', {}, {}, 200);
            const actual = apiRequest.match(/^\/h.*p/);

            expect(actual).toBeFalsy();
        });

        test('should handle String path (1st test)', () => {
            const apiRequest = new ApiRequest('/path', {}, {}, 200);
            const actual = apiRequest.match('/path');

            expect(actual).toBeTruthy();
        });

        test('should handle String path (2nd test)', () => {
            const apiRequest = new ApiRequest('/path', {}, {}, 200);
            const actual = apiRequest.match('/not-path');

            expect(actual).toBeFalsy();
        });

        test('should handle query parameters (1st test)', () => {
            const query = {};
            const apiRequest = new ApiRequest('/path', query, {}, 200);
            const actual = apiRequest.match('/path', {});

            expect(actual).toBeTruthy();
        });

        test('should handle query parameters (2.1 test)', () => {
            const query = { first: 'first value', second: 'second value' };
            const apiRequest = new ApiRequest('/path', query, {}, 200);
            const actual = apiRequest.match(/^\/p.*/);

            expect(actual).toBeTruthy();
        });

        test('should handle query parameters (2.2 test)', () => {
            const query = { first: 'first value', second: 'second value' };
            const apiRequest = new ApiRequest('/path', query, {}, 200);
            const actual = apiRequest.match(/^\/p.*/, {});

            expect(actual).toBeTruthy();
        });

        test('should handle query parameters (2.3 test)', () => {
            const query = { first: 'first value', second: 'second value' };
            const apiRequest = new ApiRequest('/path', query, {}, 200);
            const actual = apiRequest.match('/path', { first: 'first value', second: /^s.*e/ });

            expect(actual).toBeTruthy();
        });

        test('should handle query parameters (3rd test)', () => {
            const query = { first: 'first value' };
            const apiRequest = new ApiRequest('/path', query, {}, 200);
            const actual = apiRequest.match('/path', { first: 'first value', second: /^s.*e/ });

            expect(actual).toBeFalsy();
        });

        test('should handle query parameters (4th test)', () => {
            const query = { second: 'second value' };
            const apiRequest = new ApiRequest('/path', query, {}, 200);
            const actual = apiRequest.match('/path', { second: /^f.*e/ });

            expect(actual).toBeFalsy();
        });

        test('should handle query parameters (5th test)', () => {
            const query = { first: 'first value' };
            const apiRequest = new ApiRequest('/path', query, {}, 200);
            const actual = apiRequest.match('/path', { first: 'second value' });

            expect(actual).toBeFalsy();
        });

        test('should use path parameter as query', () => {
            const query = { first: 'first value', second: 'second value' };
            const apiRequest = new ApiRequest('/path', query, {}, 200);
            const actual = apiRequest.match({ first: 'first value' });

            expect(actual).toBeTruthy();
        });
    });
});
