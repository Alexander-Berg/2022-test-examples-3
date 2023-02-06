import fetchMock from 'jest-fetch-mock';

import { setExpPath } from '../appConfig';

import { fetchJSON } from './fetch';

fetchMock.enableMocks();

describe('experiments handle', () => {
    beforeEach(() => {
        jest.clearAllTimers();
    });

    it('single endpoint', () => {
        setExpPath('first');

        fetchJSON('test_url');
        expect(fetch).toBeCalledTimes(1);
    });

    it('multiple endpoints', () => {
        setExpPath('fitst|second');

        fetchJSON('test_url');
        expect(fetch).toBeCalledTimes(2);
    });
});
