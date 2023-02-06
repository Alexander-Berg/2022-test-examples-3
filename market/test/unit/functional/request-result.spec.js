'use strict';

const Cookie = require('./../../functional/lib/cookie');
const ApiRequest = require('./../../functional/lib/api-request');
const RequestResult = require('./../../functional/lib/request-result');

describe('Request result', () => {
    describe('constructor', () => {
        test('should be an instance of RequestResult class', () => {
            const response = { data: 'data' };
            const cookies = [new Cookie('first'), new Cookie('second')];
            const logs = { externalApi: 'tskv\ttskv_format=sovetnik-external-api-log\t' };
            const apiRequests = [new ApiRequest('\\path', {}, response, 200, true)];
            const requestResult = new RequestResult(response, cookies, logs, apiRequests);

            expect(requestResult).toBeInstanceOf(RequestResult);
        });
    });

    describe('getters', () => {
        const response = { data: 'data' };
        const cookies = [new Cookie('first'), new Cookie('second')];
        const logs = { externalApi: 'tskv\ttskv_format=sovetnik-external-api-log\t' };
        const apiRequests = [new ApiRequest('\\path', {}, response, 200, true)];
        const requestResult = new RequestResult(response, cookies, logs, apiRequests);

        test('should return response', () => {
            const expected = response.body;
            const actual = requestResult.response;

            expect(actual).toEqual(expected);
        });

        test('should return cookies', () => {
            const expected = cookies;
            const actual = requestResult.cookies;

            expect(actual).toEqual(expected);
        });

        test('should return logs', () => {
            const expected = logs;
            const actual = requestResult.logs;

            expect(actual).toEqual(expected);
        });

        test('should return apiRequests', () => {
            const expected = apiRequests;
            const actual = requestResult.apiRequests;

            expect(actual).toEqual(expected);
        });
    });
});
