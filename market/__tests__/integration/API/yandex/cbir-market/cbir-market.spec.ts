import nock from 'nock';

import API from '../../../../../src/API/index';
import raiseMocks from '../../../helpers/raise-mocks';
import { CbirParams } from '../../../../../src/types/cbir';

/**
 * @see {@link https://jestjs.io/docs/en/manual-mocks#mocking-user-modules}
 */
if (!process.env.LOG) {
    jest.mock('../../../../../utils/logs');
}

/* Mocks */
const cbirMarketOk = require('./__mocks__/cbir-market-ok');
const cbirMarketError = require('./__mocks__/cbir-market-error');

describe('Test market CBIR', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation', async () => {
        const mockScopes = raiseMocks(cbirMarketOk);

        let response;
        let errorCatched;
        try {
            response = await API.yandex.images['v1.0.0'].cbir.market({} as CbirParams);
        } catch (error) {
            errorCatched = true;
        }

        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should find images', async () => {
        const mockScopes = raiseMocks(cbirMarketOk);

        const response = await API.yandex.images['v1.0.0'].cbir.market({
            url: 'http://url.ru',
            req_id: '12345',
            locationRegion: 'Moscow',
            relev: 'relev',
        });

        expect(response).toMatchSnapshot();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should handle error', async () => {
        const mockScopes = raiseMocks(cbirMarketError);

        let response;
        let error;

        try {
            response = await API.yandex.images['v1.0.0'].cbir.market({
                url: 'http://url.ru',
                req_id: '12345',
                locationRegion: 'Moscow',
                relev: 'relev',
            });
        } catch (e) {
            error = e.message;
        }

        expect(response).toBeUndefined();
        expect(error).toBe('[\n  "ащипка 101",\n  "другая ащипка"\n]');
        mockScopes.forEach((scope) => scope.done());
    });
});
