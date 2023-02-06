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
const offersGetInfoOk = require('./__mocks__/model-offers-default-ok');
const offersGetInfoError = require('./__mocks__/model-offers-default-error');
const offersGetInfoInvalid = require('./__mocks__/model-offers-default-invalid');

/* Stubs */
const offersDefaultStub = require('./__stubs__/api-models-offers-default.stub');

describe('Model Offers Default API', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    test('should fail validation (no params)', async () => {
        const mockScopes = raiseMocks(offersGetInfoOk);
        let response;
        try {
            response = await API.yandex.market['v2.1.5'].models.offers.default({});
        } catch (error) {
            response = null;
        }
        expect(response).toBeNull();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail validation (no geo_id)', async () => {
        const modelIdFailValidation: any = {
            modelId: 175941311,
        };
        const mockScopes = raiseMocks(offersGetInfoOk);
        let response;
        try {
            response = await API.yandex.market['v2.1.5'].models.offers.default(modelIdFailValidation);
        } catch (error) {
            response = null;
        }
        expect(response).toBeNull();
        mockScopes.forEach((scope) => expect(scope.isDone()).toBeFalsy());
    });

    test('should fail with error status', async () => {
        const modelIdError: any = {
            modelId: 175941311,
            geo_id: 213,
        };
        const mockScopes = raiseMocks(offersGetInfoError);

        let response;
        try {
            response = await API.yandex.market['v2.1.5'].models.offers.default(modelIdError);
        } catch (error) {
            response = null;
        }
        expect(response).toBeNull();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should fail with invalid status', async () => {
        const modelIdInvalid: any = {
            modelId: 175941311,
            geo_id: 213,
        };
        const mockScopes = raiseMocks(offersGetInfoInvalid);
        let response;
        try {
            response = await API.yandex.market['v2.1.5'].models.offers.default(modelIdInvalid);
        } catch (error) {
            response = null;
        }
        expect(response).toBeNull();
        mockScopes.forEach((scope) => scope.done());
    });

    test('should find default model offers', async () => {
        const modelId: any = {
            modelId: 175941311,
            geo_id: 213,
        };
        const mockScopes = raiseMocks(offersGetInfoOk);
        const response = await API.yandex.market['v2.1.5'].models.offers.default(modelId);
        expect(response).toEqual(offersDefaultStub);
        mockScopes.forEach((scope) => scope.done());
    });
});
