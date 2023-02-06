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
const offersGetInfoOk = require('./__mocks__/model-offers-ok');
const offersGetInfoError = require('./__mocks__/model-offers-error');
const offersGetInfoInvalid = require('./__mocks__/model-offers-invalid');

describe('Model Offers API', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation (no params)', async () => {
        const mockScopes = raiseMocks(offersGetInfoOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.0'].models.offers([{}]);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail validation (no geo_id)', async () => {
        const modelIdFailValidation: any = {
            modelId: 175941311,
        };
        const mockScopes = raiseMocks(offersGetInfoOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.0'].models.offers(modelIdFailValidation);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail with error status', async () => {
        const modelIdError: any = {
            modelId: 175941311,
            geo_id: 213,
        };
        const mockScopes = raiseMocks(offersGetInfoError);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.0'].models.offers(modelIdError);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should fail with invalid status', async () => {
        const modelIdInvalid: any = {
            modelId: 175941311,
            geo_id: 213,
        };
        const mockScopes = raiseMocks(offersGetInfoInvalid);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.0'].models.offers(modelIdInvalid);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeTruthy();
        expect(response).toBeUndefined();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should find model offers', async () => {
        const modelId: any = {
            modelId: 175941311,
            geo_id: 213,
        };
        const mockScopes = raiseMocks(offersGetInfoOk);
        let response;
        let errorCatched;
        try {
            response = await API.yandex.market['v2.1.0'].models.offers(modelId);
        } catch (error) {
            errorCatched = true;
        }
        expect(errorCatched).toBeFalsy();
        expect(response).toEqual([]);
        mockScopes.forEach((scope) => scope.done());
    });
});
