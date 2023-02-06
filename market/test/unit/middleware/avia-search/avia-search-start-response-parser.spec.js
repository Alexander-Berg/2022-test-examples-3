/* eslint-disable max-len */
const aviaSearchStartResponseParser = require('./../../../../middleware/avia-search/avia-search-start/avia-search-start-response-parser');

describe('Avia search start response parser', () => {
    test("should return null if response is 'undefined'", () => {
        const actual = aviaSearchStartResponseParser();
        expect(actual).toBeNull();
    });

    test("should return null if response is not contain 'success'-'status'", () => {
        const response = { status: 'fail' };
        const actual = aviaSearchStartResponseParser(response);

        expect(actual).toBeNull();
    });

    test("should return null if response does not contain 'data'", () => {
        const response = { status: 'success' };
        const actual = aviaSearchStartResponseParser(response);

        expect(actual).toBeNull();
    });

    test("should return null if response 'data' does not contain 'search_id'", () => {
        const response = { status: 'success', data: {} };
        const actual = aviaSearchStartResponseParser(response);

        expect(actual).toBeNull();
    });

    test("should return null if response 'data' does not contain 'link'", () => {
        const response = {
            status: 'success',
            data: {
                search_id: 'search id',
            },
        };
        const actual = aviaSearchStartResponseParser(response);

        expect(actual).toBeNull();
    });

    test("should return null if response 'data' does not contain 'direction'", () => {
        const response = {
            status: 'success',
            data: {
                search_id: 'search id',
                link: 'link',
            },
        };
        const actual = aviaSearchStartResponseParser(response);

        expect(actual).toBeNull();
    });

    test("should return null if response 'direction' does not contain 'arrival'", () => {
        const response = {
            status: 'success',
            data: {
                search_id: 'search id',
                link: 'link',
                direction: {},
            },
        };
        const actual = aviaSearchStartResponseParser(response);

        expect(actual).toBeNull();
    });

    test("should return null if response 'arrival' -> 'settlement' does not contain 'title'", () => {
        const response = {
            status: 'success',
            data: {
                search_id: 'search id',
                link: 'link',
                direction: {
                    arrival: {
                        settlement: {},
                    },
                },
            },
        };
        const actual = aviaSearchStartResponseParser(response);

        expect(actual).toBeNull();
    });
});
