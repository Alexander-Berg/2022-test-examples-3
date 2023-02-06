import nock from 'nock';

import API from '../../../../../src/API/index';
import raiseMocks from '../../../helpers/raise-mocks';

/**
 * @see {@link https://jestjs.io/docs/en/manual-mocks#mocking-user-modules}
 */
if (!process.env.LOG) {
    jest.mock('../../../../../utils/logs');
}

/* Mocks */
const offersInfoOk = require('./__mocks__/offers-get-info-ok');
const offersInfoError = require('./__mocks__/offers-get-info-error');
const offersInfoInvalid = require('./__mocks__/offers-get-info-invalid');

describe('Get offers API', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation (no params)', async () => {
        const mockScopes = raiseMocks(offersInfoOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.4'].offers([{}]);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail validation (no geo_id)', async () => {
        const offersIds: any = {
            offersIds: 175941311,
        };
        const mockScopes = raiseMocks(offersInfoOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.4'].offers(offersIds);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail with error status', async () => {
        const offersIdsError: any = {
            offersIds: [175941311],
            geo_id: 213,
        };
        const mockScopes = raiseMocks(offersInfoError);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.4'].offers(offersIdsError);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should fail with invalid status', async () => {
        const offersIdsInvalid: any = {
            offersIds: [175941311],
            geo_id: 213,
        };
        const mockScopes = raiseMocks(offersInfoInvalid);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.4'].offers(offersIdsInvalid);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should find offers by ids', async () => {
        const modelId: any = {
            offersIds: [175941311],
            geo_id: 213,
        };
        const mockScopes = raiseMocks(offersInfoOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.4'].offers(modelId);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeFalsy();
        expect(response).toEqual([]);
        mockScopes.forEach((scope) => scope.done());
    });
});
